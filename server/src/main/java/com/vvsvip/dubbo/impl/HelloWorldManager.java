package com.vvsvip.dubbo.impl;


import com.vvsvip.shop.test.service.IHelloWorldManager;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by blues on 2017/4/19.
 */
@org.springframework.stereotype.Service
//@com.alibaba.dubbo.config.annotation.Service(loadbalance = "roundrobin")
public class HelloWorldManager implements IHelloWorldManager {

    @Override
    @Transactional
    public void sayHelloWorld() {
        System.out.println("Hello dubbo");
    }
}
