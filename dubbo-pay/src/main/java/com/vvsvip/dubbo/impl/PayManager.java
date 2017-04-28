package com.vvsvip.dubbo.impl;

import com.vvsvip.shop.test.service.IPayManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

/**
 * Created by ADMIN on 2017/4/27.
 */
@Component
public class PayManager implements IPayManager {
    private Logger logger = LoggerFactory.getLogger(PayManager.class);

    private static int exceptionCount = 0;

    @Override
    public String pay(String orderId, String amount) {
        if (new SecureRandom().nextInt(10) < 7) {
            System.out.println("随机抛出异常" + ++exceptionCount);
            throw new RuntimeException("随机抛出异常" + exceptionCount);
        }
        logger.info("我是PayManager");

        return "execute success";
    }
}
