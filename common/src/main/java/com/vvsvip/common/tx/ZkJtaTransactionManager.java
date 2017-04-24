package com.vvsvip.common.tx;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

/**
 * Created by ADMIN on 2017/4/24.
 */
public class ZkJtaTransactionManager extends JtaTransactionManager {

    /**
     * 事务开始
     *
     * @param transaction
     * @param definition
     */
    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
        DistributedTransactionSupport.doBegin();
        super.doBegin(transaction, definition);
    }

    /**
     * 事务回滚
     *
     * @param status
     */
    @Override
    protected void doRollback(DefaultTransactionStatus status) {
        super.doRollback(status);
        DistributedTransactionSupport.doRollback();
    }

    /**
     * 事务提交
     *
     * @param status
     */
    @Override
    protected void doCommit(DefaultTransactionStatus status) {
        super.doCommit(status);
        DistributedTransactionSupport.doCommited();
    }
}
