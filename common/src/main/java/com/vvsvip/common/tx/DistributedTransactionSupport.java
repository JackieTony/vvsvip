package com.vvsvip.common.tx;

import com.alibaba.dubbo.rpc.RpcContext;

import java.util.Hashtable;

/**
 * 分布式事务标记
 * Created by ADMIN on 2017/4/24.
 */
public class DistributedTransactionSupport {
    private static ThreadLocal<Hashtable<String, String>> threadLocal = new ThreadLocal<Hashtable<String, String>>();

    private static boolean isConsumerSide() {
        return RpcContext.getContext().isConsumerSide();
    }

    public static void doBegin() {
        // 是否为消费者
        if (isConsumerSide()) {
            RpcContext.getContext().setAttachment(DistributedTransactionParams.TRANSACTION_STATUS.getValue(), DistributedTransactionParams.YES.getValue());
        }
    }

    public static void doCommited() {
        if (isConsumerSide()) {
            RpcContext.getContext().setAttachment(DistributedTransactionParams.TRANSACTION_STATUS.getValue(), DistributedTransactionParams.COMMITED.getValue());
        }
    }

    public static void doRollback() {
        if (isConsumerSide()) {
            RpcContext.getContext().setAttachment(DistributedTransactionParams.TRANSACTION_STATUS.getValue(), DistributedTransactionParams.ROLL_BACK.getValue());
        }
    }
}
