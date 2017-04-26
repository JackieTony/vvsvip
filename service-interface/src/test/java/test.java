import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * Created by ADMIN on 2017/4/24.
 */
public class test {
    public static void main(String[] args) throws UnsupportedEncodingException {
        System.out.println(URLDecoder.decode("consumer%3A%2F%2F192.168.2.121%2Fcom.vvsvip.shop.test.service.IHelloWorldManager%3Fapplication%3Dconsumer-of-helloworld-app-my%26category%3Dconsumers%26check%3Dfalse%26dubbo%3D2.5.3%26interface%3Dcom.vvsvip.shop.test.service.IHelloWorldManager%26logger%3Dslf4j%26methods%3DsayHelloWorld%26mock%3Dreturn%2Bnull%26pid%3D10696%26side%3Dconsumer%26timestamp%3D1493025978754, consumer%3A%2F%2F192.168.2.121%2Fcom.vvsvip.shop.test.service.IHelloWorldManager%3Fapplication%3Dconsumer-of-helloworld-app-my%26category%3Dconsumers%26check%3Dfalse%26dubbo%3D2.5.3%26interface%3Dcom.vvsvip.shop.test.service.IHelloWorldManager%26logger%3Dslf4j%26methods%3DsayHelloWorld%26mock%3Dreturn%2Bnull%26pid%3D11168%26side%3Dconsumer%26timestamp%3D1493026189304", "utf-8"));
    }
}
