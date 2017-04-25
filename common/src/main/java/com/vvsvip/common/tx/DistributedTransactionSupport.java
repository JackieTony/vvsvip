package com.vvsvip.common.tx;

import com.alibaba.dubbo.rpc.RpcContext;

/**
 * 分布式事务标记
 * Created by ADMIN on 2017/4/24.
 */
public class DistributedTransactionSupport {

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
            RpcContext.getContext().setAttachment(DistributedTransactionParams.TRANSACTION_STATUS.getValue(), null);
        }
    }

    public static void doRollback() {
        if (isConsumerSide()) {
            RpcContext.getContext().setAttachment(DistributedTransactionParams.TRANSACTION_STATUS.getValue(), null);
        }
    }

    /**
     * 是否开启分布式事务
     *
     * @return
     */
    public static boolean isDistributedTransaction() {
        return RpcContext.getContext().getAttachment(DistributedTransactionParams.TRANSACTION_STATUS.getValue()) != null;
    }
}
