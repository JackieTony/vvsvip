package com.vvsvip.common.tx;

import com.alibaba.dubbo.rpc.RpcContext;
import com.atomikos.icatch.config.UserTransactionServiceImp;
import com.atomikos.icatch.jta.UserTransactionManager;
import com.vvsvip.common.bean.TransactionMessage;
import com.vvsvip.common.dao.TransactionMessageMapper;
import org.I0Itec.zkclient.ZkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.jta.JtaTransactionObject;
import org.springframework.transaction.support.DefaultTransactionStatus;

import javax.transaction.SystemException;
import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by ADMIN on 2017/4/24.
 */
public class ZkJtaTransactionManager extends JtaTransactionManager {
    Logger logger = LoggerFactory.getLogger(ZkJtaTransactionManager.class);
    @Autowired
    private TransactionMessageMapper transactionMessageMapper;
    @Autowired
    private ZkClient zkClient;

    /**
     * 事务开始
     *
     * @param transaction
     * @param definition
     */
    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
        super.doBegin(transaction, definition);
        logger.debug(RpcContext.getContext().getLocalHost() + "开启事务");
        DistributedTransactionSupport.doBegin();
    }

    /**
     * 事务回滚
     *
     * @param status
     */
    @Override
    protected void doRollback(DefaultTransactionStatus status) {
        if (DistributedTransactionSupport.isExecutable()
                && !DistributedTransactionSupport.isConsumerSide() && DistributedTransactionSupport.getZNode() != null) {
            new TransactionThread(this, status, TransactionMessageAop.threadParam.get(), DistributedTransactionParams.ROLL_BACK, zkClient, transactionMessageMapper).start();
        } else {
            super.doRollback(status);
            logger.debug(RpcContext.getContext().getLocalHost() + "回滚事务");
            TransactionMessageAop.paramsThreadLocal.set(DistributedTransactionParams.ROLL_BACK);
            DistributedTransactionSupport.doRollback();
        }
    }

    protected void zkRollback(DefaultTransactionStatus status) {
        super.rollback(status);
    }

    /**
     * 事务提交
     *
     * @param status
     */
    @Override
    protected void doCommit(DefaultTransactionStatus status) {
        if (DistributedTransactionSupport.isExecutable()
                && !DistributedTransactionSupport.isConsumerSide() && DistributedTransactionSupport.getZNode() != null) {
            new TransactionThread(this, status, TransactionMessageAop.threadParam.get(), DistributedTransactionParams.COMMITED, zkClient, transactionMessageMapper).start();
        } else {
            super.doCommit(status);
            logger.info(RpcContext.getContext().getLocalHost() + "事务提交");
            TransactionMessageAop.paramsThreadLocal.set(DistributedTransactionParams.COMMITED);
        }
    }

    protected void zkCommit(DefaultTransactionStatus status) {
        super.doCommit(status);
    }


    class TransactionThread extends Thread {
        private ZkJtaTransactionManager transactionManager;
        private DefaultTransactionStatus status;
        private Hashtable<String, Object> threadParam;
        private DistributedTransactionParams param;
        private ZkClient zkClient;
        private TransactionMessageMapper transactionMessageMapper;

        public TransactionThread(ZkJtaTransactionManager transactionManager,
                                 DefaultTransactionStatus status,
                                 Hashtable<String, Object> threadParam,
                                 DistributedTransactionParams param, ZkClient zkClient, TransactionMessageMapper transactionMessageMapper) {
            this.transactionManager = transactionManager;
            this.status = status;
            this.threadParam = threadParam;
            this.param = param;
            this.zkClient = zkClient;
            this.transactionMessageMapper = transactionMessageMapper;
        }

        @Override
        public void run() {
            this.setName("TransactionThread");
            String providerSideNode = String.valueOf(threadParam.get(TransactionMessageAop.CURRENT_ZNODE));
            if (DistributedTransactionParams.ROLL_BACK.getValue().equals(param.getValue())) {
                zkClient.writeData(providerSideNode, "0", -1);
            } else if (DistributedTransactionParams.COMMITED.getValue().equals(param.getValue())) {
                zkClient.writeData(providerSideNode, "1", -1);

            }
            ReentrantReadWriteLock readWriteLock = (ReentrantReadWriteLock) threadParam.get(TransactionMessageAop.LOCK);
            CountDownLatch countDownLatch = (CountDownLatch) threadParam.get(TransactionMessageAop.COUNT_DOWN_LATCH);
            try {
                logger.info("读锁打开");
                readWriteLock.readLock().lock();
                if (countDownLatch.getCount() > 0) {
                    countDownLatch.await(DistributedTransactionProviderSideAOP.listenerTimeout, DistributedTransactionProviderSideAOP.timeUnit);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                readWriteLock.readLock().unlock();
                logger.info("读锁关闭");
            }

            String transactionPath = String.valueOf(threadParam.get(TransactionMessageAop.TRANSACTION_ZNODE_PATH));

            String data = zkClient.readData(transactionPath);
            if (data == null || "0".equals(data)) {
                transactionManager.zkRollback(status);
            }
            if (DistributedTransactionParams.ROLL_BACK.getValue().equals(param.getValue())) {
                JtaTransactionObject txObject = (JtaTransactionObject) status.getTransaction();

                try {
                    int jtaStatus = txObject.getUserTransaction().getStatus();
                } catch (SystemException e) {
                    e.printStackTrace();
                }
                status.setRollbackOnly();
                transactionManager.zkRollback(status);
                TransactionMessage transactionMessage = transactionMessageMapper.selectByUUID(String.valueOf(threadParam.get(TransactionMessageAop.UUID_KEY)));
                transactionMessage.setStatus(TransactionMessageAop.ROLLBACK_STATUS);
                transactionMessageMapper.updateByPrimaryKey(transactionMessage);
                logger.debug("事务回滚");
            } else if (DistributedTransactionParams.COMMITED.getValue().equals(param.getValue())) {
                status.setCompleted();
                transactionManager.zkCommit(status);
                TransactionMessage transactionMessage = transactionMessageMapper.selectByUUID(String.valueOf(threadParam.get(TransactionMessageAop.UUID_KEY)));
                transactionMessage.setStatus(TransactionMessageAop.COMMIT_STATUS);
                transactionMessageMapper.updateByPrimaryKey(transactionMessage);
                logger.debug("事务提交");
            }
        }
    }
}
