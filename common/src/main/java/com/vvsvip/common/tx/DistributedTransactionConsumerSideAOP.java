package com.vvsvip.common.tx;

import com.alibaba.dubbo.rpc.RpcContext;
import com.vvsvip.common.security.EncryptUtil;
import com.vvsvip.common.tx.annotation.DistributedTransaction;
import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by ADMIN on 2017/4/27.
 */
public class DistributedTransactionConsumerSideAOP {
    Logger logger = LoggerFactory.getLogger(DistributedTransactionAop.class);

    /**
     * zookeeper namespace间隔
     */
    private static final String INTERVAL = DistributedTransactionParams.INTERVAL.getValue();
    private String namespace;
    private String consumerSideNode;
    private String znode;
    @Autowired
    private ZkClient zkClient;

    static final long listenerTimeout = 30000;
    static final TimeUnit timeUnit = TimeUnit.MILLISECONDS;

    @AfterThrowing("transactionMethod()")
    public void doAfterThrow() throws Exception {
        logger.info("异常拦截开始回滚事务");
        String transactionPath = consumerSideNode;
        try {
            if (transactionPath != null) {
                boolean stat = zkClient.exists(DistributedTransactionParams.ZK_PATH.getValue());
                if (stat) {
                    stat = zkClient.exists(transactionPath);
                    if (stat) {
                        try {
                            zkClient.writeData(transactionPath, "0", -1);
                            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                            throw new Exception("rollback " + transactionPath);
                        } finally {
                            zkClient.deleteRecursive(transactionPath);
                        }
                    }
                }
            }
        } catch (KeeperException | InterruptedException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            throw e;
        }
    }


    public Object doAround(final ProceedingJoinPoint joinPoint) throws Throwable {

        TransactionMessageAop.threadParam.get().put(TransactionMessageAop.IS_CONSUMER_SIDE, true);

        Boolean exec = Boolean.parseBoolean(String.valueOf(TransactionMessageAop.threadParam.get().get(TransactionMessageAop.EXECUTE_SIGN)));

        Signature signature = joinPoint.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;
        Method targetMethod = methodSignature.getMethod();
        DistributedTransaction distributedTransaction = targetMethod.getAnnotation(DistributedTransaction.class);
        CountDownLatch countDownLatch = null;

        if (distributedTransaction != null) {
            int transactionCount = 0;
            if (exec) {
                // 消费者节点 准备开启事务
                transactionCount = distributedTransaction.value();
                if (transactionCount > 0) {
                    logger.info("ConsumerSide doAround begin");
                    beforeConsumerSide(joinPoint);
                }
            }
            // 事务尾声处理
            if (distributedTransaction != null && transactionCount > 0) {
                countDownLatch = new CountDownLatch(1);
                new DistributedTransactionConsumerSideAOP.ConsumerSideTread(joinPoint, transactionCount, countDownLatch).start();
            }
        }


        // 执行当前方法
        Object object = joinPoint.proceed();
        if (countDownLatch != null) {
            countDownLatch.await(listenerTimeout, TimeUnit.MILLISECONDS);
        }
        return object;
    }

    /**
     * 消费者事务准备
     *
     * @param joinPoint
     * @throws Exception
     */
    private void beforeConsumerSide(ProceedingJoinPoint joinPoint) throws Throwable {
        boolean stat = zkClient.exists(DistributedTransactionParams.ZK_PATH.getValue());
        if (!stat) {
            zkClient.create(DistributedTransactionParams.ZK_PATH.getValue(), "", ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }
        // zkNamespace
        StringBuffer namespaceBuffer = new StringBuffer();

        // 获取本地IP地址
        String ip = InetAddress.getLocalHost().getHostAddress();
        // 获取当前方法所在的类
        Object target = joinPoint.getTarget();
        String clazzName = target.getClass().getName();

        // 获取当前方法的名字
        String methodName = joinPoint.getSignature().getName();
        // 获取当前方法的所有参数
        Object[] args = joinPoint.getArgs();

        byte[] params = null;
        if (args != null && args.length > 0) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(args);
            // 将所持有参数转为二进制数据
            params = byteArrayOutputStream.toByteArray();
        }
        // 拼接transaction znode namespace

        namespaceBuffer.append(ip)
                .append(INTERVAL).append(System.currentTimeMillis() + new SecureRandom().nextInt(100000) + "")
                .append(INTERVAL).append(clazzName)
                .append(INTERVAL).append(methodName)
                .append(INTERVAL).append(EncryptUtil.encodeBase64(params));
        this.namespace = URLEncoder.encode(namespaceBuffer.toString(), "UTF-8");

        znode = DistributedTransactionParams.ZK_PATH + "/" + this.namespace;


        RpcContext.getContext()
                .setAttachment(
                        DistributedTransactionParams.TRANSACTION_ZNODE.getValue()
                        , znode);
        // 创建事务节点
        consumerSideNode = zkClient.create(znode, "", ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        TransactionMessageAop.threadParam.get().put(TransactionMessageAop.TRANSACTION_ZNODE_PATH, znode);
    }

    /**
     * 消费者事务处理
     *
     * @param joinPoint
     * @param transactionCount
     */
    private void afterConsumerSide(ProceedingJoinPoint joinPoint, int transactionCount) throws Throwable {
        logger.info("afterConsumerSide begin");
        String transactionPath = znode;
        try {
            boolean isSuccess = true;
            long startTime = System.currentTimeMillis();
            while (true) {
                // 事务节点
                List<String> childreList = zkClient.getChildren(transactionPath);
                for (int i = 0; i < childreList.size(); i++) {
                    String node = childreList.get(i);
                    String subPath = transactionPath + "/" + node;
                    try {
                        String data = zkClient.readData(subPath, true);
                        // 确认当前节点事务是否完成
                        if (data != null && !data.isEmpty()) {
                            isSuccess &= Integer.valueOf(data) == 1;
                        }
                    } catch (Exception e) {

                    }
                }
                // 是否为所有节点状态
                if (childreList.size() == transactionCount || !isSuccess) {
                    break;
                }
                if (System.currentTimeMillis() - startTime > listenerTimeout) {
                    isSuccess = false;
                }
                Thread.sleep(50);
            }
            if (isSuccess) {
                zkClient.writeData(transactionPath, "1", -1);
            } else {
                zkClient.writeData(transactionPath, "0", -1);
                //TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                throw new Exception("Rollback " + transactionPath);
            }
        } catch (KeeperException | InterruptedException e) {
            throw e;
        } finally {
            Thread.sleep(500);
            zkClient.deleteRecursive(transactionPath);
            logger.info("afterConsumerSide end");
        }
    }

    class ConsumerSideTread extends Thread {
        private ProceedingJoinPoint point;
        private int count;
        private CountDownLatch countDownLatch;

        public ConsumerSideTread(ProceedingJoinPoint joinPoint, int count, CountDownLatch countDownLatch) {
            this.point = joinPoint;
            this.count = count;
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void run() {
            this.setName("ConsumerSide");
            try {
                afterConsumerSide(point, count);
            } catch (Throwable throwable) {
                logger.error("Transaction Listener Exception", throwable);
            }
            countDownLatch.countDown();
        }
    }
}
