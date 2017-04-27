package com.vvsvip.dubbo.impl;

import com.vvsvip.shop.test.service.IPayManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by ADMIN on 2017/4/27.
 */
@Component
public class PayManager implements IPayManager {
    private Logger logger = LoggerFactory.getLogger(PayManager.class);

    @Override
    public String pay(String orderId, String amount) {
        logger.info("我是PayManager");
        return "execute success";
    }
}
