package com.vvsvip.common.util;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.BeanSerializer;
import com.vvsvip.common.bean.FastdfsBean;
import org.junit.Test;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * 反射工具类
 * Created by ADMIN on 2017/4/25.
 */
public class ReflectUtil {
    private String getParamTypes(Object[] args) throws NoSuchMethodException, IOException {
        if (args == null || args.length == 0) {
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        String str = mapper.writeValueAsString(args);
        return null;
    }

    @Test
    public void test() throws Exception {
        List<String> list = new ArrayList<>();
        List<FastdfsBean> ssList = new ArrayList<>();
        ssList.add(new FastdfsBean("ada", "112"));
        String param1 = new String("123");
        Object obj = new Object();
        Object[] args = new Object[]{list, ssList, param1, null};
        getParamTypes(args);
    }
}
