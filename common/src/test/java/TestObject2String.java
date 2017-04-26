import java.io.*;

import com.vvsvip.common.bean.FastdfsBean;
import org.junit.Test;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * Created by ADMIN on 2017/4/25.
 */
public class TestObject2String {

    private byte[] object2String(Object obj) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(obj);
        return byteArrayOutputStream.toByteArray();
    }

    private Object bytes2Object(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteArrayOutputStream = new ByteArrayInputStream(data);
        ObjectInputStream objectOutputStream = new ObjectInputStream(byteArrayOutputStream);
        return objectOutputStream.readObject();
    }

    @Test
    public void test() {
        try {
            FastdfsBean bean = new FastdfsBean("groupId", "remoteFile");
            byte[] objectByteArrray = object2String(bean);
            String objString = new BASE64Encoder().encode(objectByteArrray);
            System.out.println(objString);
            byte[] decodeObjectByteArray = new BASE64Decoder().decodeBuffer(objString);
            Object obj = bytes2Object(decodeObjectByteArray);
            System.out.println(((FastdfsBean) obj).getGroupName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
