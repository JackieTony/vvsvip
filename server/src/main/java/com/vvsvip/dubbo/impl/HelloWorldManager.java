package com.vvsvip.dubbo.impl;

import com.vvsvip.dubbo.IHelloWorldManager;

/**
 * Created by blues on 2017/4/19.
 */
//@org.springframework.stereotype.Service
@com.alibaba.dubbo.config.annotation.Service(loadbalance = "roundrobin")
public class HelloWorldManager implements IHelloWorldManager {
    @Override
    public void sayHelloWorld() {
        System.out.println("Hello dubbo");
    }
}
