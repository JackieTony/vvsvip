package com.vvsvip.common.tx;

/**
 * Created by ADMIN on 2017/4/24.
 */
public enum DistributedTransactionParams {
    YES("YES"), NO("NO"), COMMITED("COMMITED"), TRANSACTION_KEY("DISTRIBUTED_TRANSACTION"), ROLL_BACK("rollback");
    private String value;

    DistributedTransactionParams(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
