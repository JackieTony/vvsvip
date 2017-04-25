package com.vvsvip.common.tx;

import com.alibaba.dubbo.rpc.RpcContext;
import com.vvsvip.common.security.EncryptUtil;
import com.vvsvip.common.tx.annotation.DistributedTransaction;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


/**
 * Created by ADMIN on 2017/4/24.
 */
@Aspect
@Component
@Order(3)
public class DistributedTransactionAop implements Watcher {
    /**
     * zookeeper namespace间隔
     */
    private static final String INTERVAL = DistributedTransactionParams.INTERVAL.getValue();

    @Autowired
    private ZooKeeper zooKeeper;


    /**
     * 服务切点
     */
    @Pointcut("execution(* com.vvsvip.service.impl.*(..))")
    private void transactionMethod() {
    }

    /**
     * 异常抛出 回滚事务
     */
    @AfterThrowing("transactionMethod()")
    public void doAfterThrow() {

    }

    @Around("transactionMethod()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        System.out.println("进入环绕通知");

        Signature signature = joinPoint.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;
        Method targetMethod = methodSignature.getMethod();
        DistributedTransaction distributedTransaction = targetMethod.getAnnotation(DistributedTransaction.class);

        int transactionCount = 0;

        // 消费者节点 准备开启事务
        if (RpcContext.getContext().isConsumerSide()) {
            if (distributedTransaction != null) {
                transactionCount = distributedTransaction.value();
                if (transactionCount > 0) {
                    beforeConsumerSide(joinPoint, transactionCount);
                }
            }
        }

        // 执行当前方法
        Object object = joinPoint.proceed();

        // 事务尾声处理
        if (RpcContext.getContext().isConsumerSide()) {

            if (distributedTransaction != null && transactionCount > 0) {
                afterConsumerSide(joinPoint, transactionCount);
            }

        } else if (RpcContext.getContext().isProviderSide()
                &&
                RpcContext.getContext()
                        .getAttachment(DistributedTransactionParams.TRANSACTION_STATUS.getValue()).equals(DistributedTransactionParams.YES.getValue())) {

            afterProviderSide(joinPoint);
        }

        System.out.println("退出方法");

        return object;
    }

    /**
     * 消费者事务准备
     *
     * @param joinPoint
     * @param transactionCount
     * @throws Exception
     */
    private void beforeConsumerSide(ProceedingJoinPoint joinPoint, int transactionCount) throws Throwable {
        Stat stat = zooKeeper.exists(DistributedTransactionParams.ZK_PATH.getValue(), false);
        if (stat == null) {
            zooKeeper.create(DistributedTransactionParams.ZK_PATH.getValue(), new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }
        // zkNamespace
        StringBuffer namespace = new StringBuffer();
        // 获取zkSessionId
        String zkSessionId = String.valueOf(zooKeeper.getSessionId());
        // 获取本地IP地址
        String ip = InetAddress.getLocalHost().getHostAddress();
        // 获取当前方法所在的类
        Object target = joinPoint.getTarget();
        String clazzName = target.getClass().getName();

        // 获取当前方法的名字
        String methodName = joinPoint.getSignature().getName();
        // 获取当前方法的所有参数
        Object[] args = joinPoint.getArgs();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(args);
        // 将所持有参数转为二进制数据
        byte[] params = byteArrayOutputStream.toByteArray();

        // 拼接transaction znode namespace
        namespace.append(ip)
                .append(INTERVAL).append(zkSessionId)
                .append(INTERVAL).append(clazzName)
                .append(INTERVAL).append(methodName)
                .append(INTERVAL).append(EncryptUtil.encodeBase64(params));
        String namespaceStr = URLEncoder.encode(namespace.toString(), "UTF-8");

        RpcContext.getContext()
                .setAttachment(
                        DistributedTransactionParams.TRANSACTION_ZNODE.getValue()
                        , namespaceStr);

        // 创建事务节点
        zooKeeper.create(namespaceStr, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
    }


    /**
     * 消费者事务处理
     *
     * @param joinPoint
     * @param transactionCount
     */
    private void afterConsumerSide(ProceedingJoinPoint joinPoint, int transactionCount) throws Throwable {
        String znode = RpcContext.getContext().getAttachment(DistributedTransactionParams.TRANSACTION_ZNODE.getValue());
        try {
            String transactionPath = DistributedTransactionParams.ZK_PATH + "/" + znode;
            boolean isSuccess = true;

            long startTime = System.currentTimeMillis();
            while (true) {
                // 事务节点
                List<String> childreList = zooKeeper.getChildren(transactionPath, false);
                for (int i = 0; i < childreList.size(); i++) {
                    String node = childreList.get(i);
                    String subPath = transactionPath + "/" + node;
                    byte[] data = zooKeeper.getData(subPath, false, null);
                    // 确认当前节点事务是否完成
                    isSuccess &= Integer.valueOf(new String(data)) == 1;
                }
                // 是否为所有节点状态
                if (childreList.size() == transactionCount || !isSuccess) {
                    break;
                }
                if (System.currentTimeMillis() - startTime > 10000) {
                    isSuccess = false;
                }
                Thread.sleep(50);
            }
            if (isSuccess) {
                zooKeeper.setData(transactionPath, "1".getBytes(), -1);
            } else {
                zooKeeper.setData(transactionPath, "0".getBytes(), -1);
            }
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 生产者事务准备
     *
     * @param joinPoint
     * @throws Exception
     */
    private void beforeProviderSide(ProceedingJoinPoint joinPoint) throws Throwable {
        String znode = RpcContext.getContext().getAttachment(DistributedTransactionParams.TRANSACTION_ZNODE.getValue());
        String transactionPath = DistributedTransactionParams.ZK_PATH + "/" + znode;

        // zkNamespace
        StringBuffer namespace = new StringBuffer();
        // 获取zkSessionId
        String zkSessionId = String.valueOf(zooKeeper.getSessionId());
        // 获取本地IP地址
        String ip = InetAddress.getLocalHost().getHostAddress();
        // 获取当前方法所在的类
        Object target = joinPoint.getTarget();
        String clazzName = target.getClass().getName();

        // 获取当前方法的名字
        String methodName = joinPoint.getSignature().getName();
        // 获取当前方法的所有参数
        Object[] args = joinPoint.getArgs();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(args);
        // 将所持有参数转为二进制数据
        byte[] params = byteArrayOutputStream.toByteArray();
        // 拼接transaction znode namespace
        namespace.append(ip)
                .append(INTERVAL).append(zkSessionId)
                .append(INTERVAL).append(clazzName)
                .append(INTERVAL).append(methodName)
                .append(INTERVAL).append(EncryptUtil.encodeBase64(params));
        String namespaceStr = URLEncoder.encode(namespace.toString(), "UTF-8");

        //TODO 将 namespaceStr 该消息存入数据库
        

        //添加根节点监听
        zooKeeper.getData(transactionPath, this, null);

    }

    /**
     * 生产者事务处理
     *
     * @param joinPoint
     */
    private void afterProviderSide(ProceedingJoinPoint joinPoint) {

    }

    @Override
    public void process(WatchedEvent watchedEvent) {

    }
}
