package com.vvsvip.common.tx;

import com.vvsvip.common.bean.TransactionMessage;
import com.vvsvip.common.dao.TransactionMessageMapper;
import com.vvsvip.common.security.EncryptUtil;
import com.vvsvip.common.tx.annotation.DistributedTransaction;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.Hashtable;
import java.util.UUID;

/**
 * Created by blues on 2017/4/25.
 */
public class TransactionMessageAop {
    Logger logger = LoggerFactory.getLogger(TransactionMessageAop.class);
    static ThreadLocal<DistributedTransactionParams> paramsThreadLocal = new ThreadLocal<DistributedTransactionParams>();
    static ThreadLocal<Hashtable<String, Object>> threadParam = new ThreadLocal<Hashtable<String, Object>>();

    static final String EXECUTE_SIGN = "exec";
    static final String IS_CONSUMER_SIDE = "IS_CONSUMER_SIDE";
    static final String UUID_KEY = "UUID_KEY";
    @Autowired
    private TransactionMessageMapper transactionMessageMapper;

    private static final String COMMIT_STATUS = "1";
    private static final String ROLLBACK_STATUS = "-1";

    @Pointcut("execution(public * com.vvsvip.dubbo..*.*(..))")
    public void pointcut() {
    }


    @Around("pointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        logger.debug("进入消息环绕");
        Signature signature = joinPoint.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;
        Method targetMethod = methodSignature.getMethod();

        Object target = joinPoint.getTarget();
        boolean exec = true;

        if (threadParam.get() == null) {
            threadParam.set(new Hashtable<String, Object>());
        }
        if (targetMethod.getName() == target.getClass().getConstructors()[0].getName()) {
            exec = false;
            threadParam.get().put(EXECUTE_SIGN, exec);
        } else {
            threadParam.get().put(EXECUTE_SIGN, true);
            String uuid = UUID.randomUUID().toString().replace("-", "");
            threadParam.get().put(UUID_KEY, uuid);
        }
        DistributedTransaction distributedTransaction = targetMethod.getAnnotation(DistributedTransaction.class);
        if (distributedTransaction != null) {
            threadParam.get().put(IS_CONSUMER_SIDE, distributedTransaction.consumerSide());
        } else {
            threadParam.get().put(IS_CONSUMER_SIDE, false);
        }
        if (exec && DistributedTransactionSupport.isDistributedTransaction()) {
            logger.debug("保存当前方法的参数");
            // zkNamespace
            StringBuffer namespace = new StringBuffer();
            // 获取本地IP地址
            String ip = InetAddress.getLocalHost().getHostAddress();
            // 获取当前方法所在的类
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
            String paramsStr = EncryptUtil.encodeBase64(params);

            // 该消息存入数据库
            TransactionMessage transactionMessage = new TransactionMessage(String.valueOf(threadParam.get().get(UUID_KEY)), ip, clazzName, methodName, paramsStr);
            transactionMessageMapper.insert(transactionMessage);
        }
        Object obj = joinPoint.proceed();

        logger.debug("切面方法执行完毕");
        if (exec && paramsThreadLocal.get() != null) {
            if (DistributedTransactionParams.ROLL_BACK.getValue().equals(paramsThreadLocal.get().getValue())) {
                TransactionMessage transactionMessage = transactionMessageMapper.selectByUUID(String.valueOf(threadParam.get().get(UUID_KEY)));
                transactionMessage.setStatus(ROLLBACK_STATUS);
                transactionMessageMapper.updateByPrimaryKey(transactionMessage);
                logger.debug("事务回滚");
            } else if (DistributedTransactionParams.COMMITED.getValue().equals(paramsThreadLocal.get().getValue())) {
                TransactionMessage transactionMessage = transactionMessageMapper.selectByUUID(String.valueOf(threadParam.get().get(UUID_KEY)));
                transactionMessage.setStatus(COMMIT_STATUS);
                transactionMessageMapper.updateByPrimaryKey(transactionMessage);
                logger.debug("事务提交");
            }
        }
        return obj;
    }

}
