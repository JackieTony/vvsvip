package com.vvsvip.test;

import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Scanner;

/**
 * Created by ADMIN on 2017/4/26.
 */
public class TestAOP {

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:spring-cloud.xml", "classpath:spring-data.xml");
        context.start();
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            TestTransaction testTransaction = context.getBean(TestTransaction.class);
            scanner.next();
            new TestThread(testTransaction).start();
        }
    }

    static class TestThread extends Thread {
        private TestTransaction testTransaction;

        public TestThread(TestTransaction helloWorldManager) {
            this.testTransaction = helloWorldManager;
        }

        @Test
        public void run() {

            try {
                testTransaction.test();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
