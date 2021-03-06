package com.vvsvip.test;

import com.vvsvip.common.tx.annotation.DistributedTransaction;
import com.vvsvip.shop.test.service.IHelloWorldManager;
import com.vvsvip.shop.test.service.IPayManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by ADMIN on 2017/4/26.
 */
@Component
public class TestTransaction {

    @Autowired
    private IHelloWorldManager helloWorldManager;
    @Autowired
    private IPayManager payManager;

    @Transactional
    @DistributedTransaction(value = 2, consumerSide = true)
    public void test() {
        helloWorldManager.sayHelloWorld();
        String result = payManager.pay("1212", "adsfa");
        System.out.println(result);
    }
}
