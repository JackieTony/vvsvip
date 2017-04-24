package com.vvsvip.shop.core.action;

import com.alibaba.dubbo.common.json.JSONArray;
import com.alibaba.dubbo.common.json.JSONObject;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by blues on 2017/4/19.
 */
@Controller
public class TestAction {

    @RequestMapping("/")
    public String index() {
        return "index";
    }

    @RequestMapping("/toFreemarker")
    public ModelAndView toFreemarker() {
        ModelAndView modelAndView = new ModelAndView("test");
        modelAndView.addObject("title", "Spring MVC And Freemarker");
        modelAndView.addObject("content", " Hello world ， test my first spring mvc ! ");
        return modelAndView;
    }

    @RequestMapping("/1")
    public String index1() {
        return "html/index1";
    }

    @RequestMapping("/2")
    public String index2() {
        return "redirect:/index1.html";
    }

    @RequestMapping("/toJSON")
    @ResponseBody
    public Object toJson() {
        Map<String, String> map = new HashMap<>();
        map.put("test", "test");
        return map;
    }
}
