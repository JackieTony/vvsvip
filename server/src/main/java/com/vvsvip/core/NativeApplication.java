package com.vvsvip.core;


import com.vvsvip.shop.test.service.IHelloWorldManager;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Created by blues on 2017/4/19.
 */
public class NativeApplication {
    ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:spring-cloud-2.xml");

    public void main(String[] args) {
        NativeApplication application = new NativeApplication();
        application.context.start();
        for (int i = 0; i < 1; i++) {
            new TestDubbo(application.context).start();
        }
    }
}

class TestDubbo extends Thread {
    private ClassPathXmlApplicationContext context = null;

    public TestDubbo(ClassPathXmlApplicationContext context) {
        this.context = context;
    }

    @Override
    public void run() {
        try {
            while (true) {
                IHelloWorldManager iHelloWorldManager = (IHelloWorldManager) context.getBean("helloWorldManager");
                iHelloWorldManager.sayHelloWorld();
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
