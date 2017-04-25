package com.vvsvip.common.tx;

import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Created by ADMIN on 2017/4/24.
 */
@Aspect
@Component
@Order(3)
public class DistributedTransactionBegin {

    @Autowired
    private ZooKeeper zooKeeper;

    @Pointcut("execution(* com.vvsvip.service.impl.*(..))")
    private void transactionMethod() {
    }//定义一个切入点

    @Before("transactionMethod() && args(name)")
    public void doAccessCheck(JoinPoint joinPoint, String name) {
        System.out.println(name);
        System.out.println("前置通知");
    }

    @AfterReturning(value = "transactionMethod()", returning = "returnValue")
    public void afterReturning(JoinPoint point, Object returnValue) {
        System.out.println("后置通知");
    }

    @After("transactionMethod()")
    public void after(JoinPoint point) {
        System.out.println("最终通知");
    }

    @AfterThrowing("transactionMethod()")
    public void doAfterThrow() {

        System.out.println("例外通知");
    }

    @Around("transactionMethod()")
    public Object doAround(ProceedingJoinPoint pjp) throws Throwable {
        System.out.println("进入环绕通知");
        Stat stat = zooKeeper.exists(DistributedTransactionParams.ZK_PATH.getValue(), false);
        if (stat == null) {

        }
        Object object = pjp.proceed();

        System.out.println("退出方法");
        return object;
    }
}
