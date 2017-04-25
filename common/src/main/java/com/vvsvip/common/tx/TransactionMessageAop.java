package com.vvsvip.common.tx;

import com.vvsvip.common.bean.TransactionMessage;
import com.vvsvip.common.dao.TransactionMessageMapper;
import com.vvsvip.common.security.EncryptUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.util.UUID;

/**
 * Created by blues on 2017/4/25.
 */
@Aspect
@Component
@Order(4)
public class TransactionMessageAop {

    static ThreadLocal<DistributedTransactionParams> paramsThreadLocal = new ThreadLocal<DistributedTransactionParams>();

    @Autowired
    private TransactionMessageMapper transactionMessageMapper;

    private static final String COMMIT_STATUS = "1";
    private static final String ROLLBACK_STATUS = "-1";

    @Pointcut("execution(* com.vvsvip.dubbo.impl.*(..))")
    public void pointcut() {
    }

    private String uuid = UUID.randomUUID().toString().replace("-", "");

    @Around("pointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        if (DistributedTransactionSupport.isDistributedTransaction()) {
            // zkNamespace
            StringBuffer namespace = new StringBuffer();
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
            String paramsStr = EncryptUtil.encodeBase64(params);

            // 该消息存入数据库
            TransactionMessage transactionMessage = new TransactionMessage(uuid, ip, clazzName, methodName, paramsStr);
            transactionMessageMapper.insert(transactionMessage);
        }
        Object obj = joinPoint.proceed();

        if (DistributedTransactionParams.ROLL_BACK.getValue().equals(paramsThreadLocal.get().getValue())) {

            TransactionMessage transactionMessage = transactionMessageMapper.selectByUUID(uuid);
            transactionMessage.setStatus(ROLLBACK_STATUS);

        } else if (DistributedTransactionParams.COMMITED.getValue().equals(paramsThreadLocal.get().getValue())) {
            TransactionMessage transactionMessage = transactionMessageMapper.selectByUUID(uuid);
            transactionMessage.setStatus(COMMIT_STATUS);
        }

        return obj;
    }

}
