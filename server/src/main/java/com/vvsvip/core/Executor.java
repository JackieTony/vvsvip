package com.vvsvip.core;

import org.apache.zookeeper.*;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * Created by blues on 2017/4/20.
 */
public class Executor implements Watcher, Runnable {

    /**
     * Zookeeper 链接地址
     */
    private String hostPort = "localhost:2181";
    private ZooKeeper zooKeeper;
    CountDownLatch downLatch = new CountDownLatch(4);

    public static void main(String[] args) throws IOException, KeeperException, InterruptedException {
        Executor executor = new Executor();
        ZooKeeper zooKeeper = executor.getZooKeeper();
        System.out.println(executor.getClass().getName().toString());
        if (zooKeeper.exists("/native-service", true) == null) {
            zooKeeper.create("/native-service", "askdjflkasdfjl=adfjkl".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            Thread.sleep(20000);
            System.out.println("\n2. 查看是否创建成功： ");
            System.out.println(new String(zooKeeper.getData("/native-service", true, null)));// 添加Watch

            // 前面一行我们添加了对/zoo2节点的监视，所以这里对/zoo2进行修改的时候，会触发Watch事件。
            System.out.println("\n3. 修改节点数据 ");
            //System.out.println(new String(zooKeeper.getData("/native-service", true, null)));// 添加Watch
            zooKeeper.setData("/native-service", "shanhy20160310".getBytes(), -1);

            // 这里再次进行修改，则不会触发Watch事件，这就是我们验证ZK的一个特性“一次性触发”，也就是说设置一次监视，只会对下次操作起一次作用。
            System.out.println("\n3-1. 再次修改节点数据 ");
            // System.out.println(new String(zooKeeper.getData("/native-service", true, null)));// 添加Watch
            zooKeeper.setData("/native-service", "shanhy20160310-ABCD".getBytes(), -1);

            System.out.println("\n4. 查看是否修改成功： ");
            //System.out.println(zooKeeper.getData("/native-service", true, null));// 添加Watch
            System.out.println(new String(zooKeeper.getData("/native-service", false, null)));

            System.out.println(zooKeeper.getChildren("/native-service", true));

            System.out.println("\n5. 删除节点 ");
            zooKeeper.delete("/native-service", -1);

            System.out.println("\n6. 查看节点是否被删除： ");
            System.out.println(" 节点状态： [" + zooKeeper.exists("/native-service", false) + "]");

        }
    }

    public Executor() throws IOException {
        zooKeeper = new ZooKeeper("localhost", 3000, this);
    }

    @Override
    public void run() {

    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        System.out.println("回调watcher实例： 路径" + watchedEvent.getPath() + " 类型："
                + watchedEvent.getType());
    }

    public String getHostPort() {
        return hostPort;
    }

    public void setHostPort(String hostPort) {
        this.hostPort = hostPort;
    }

    public ZooKeeper getZooKeeper() {
        return zooKeeper;
    }

    public void setZooKeeper(ZooKeeper zooKeeper) {
        this.zooKeeper = zooKeeper;
    }
}
