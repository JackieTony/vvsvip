package com.vvsvip.common.tx;

import com.vvsvip.common.bean.LabelValueBean;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.util.*;

/**
 * Created by ADMIN on 2017/4/24.
 */
public class ZkJtaTransactionManager extends JtaTransactionManager {

    protected static ThreadLocal<List<LabelValueBean>> threadLocal = new ThreadLocal<List<LabelValueBean>>();

    /**
     * 事务开始
     *
     * @param transaction
     * @param definition
     */
    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
        // sign();
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
        //removeSign();
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
        //removeSign();
        DistributedTransactionSupport.doCommited();
    }

    private void sign() {
        if (threadLocal.get() == null) {
            threadLocal.set(new ArrayList<LabelValueBean>());
        }
        threadLocal.get().add(new LabelValueBean(DistributedTransactionParams.YES.getValue(), DistributedTransactionParams.NEW.getValue()));
    }

    private void removeSign() {
        if (threadLocal.get() != null) {
            List<LabelValueBean> list = threadLocal.get();
            if (!list.isEmpty()) {
                list.remove(list.remove(list.size() - 1));
            }
        }
    }
}
