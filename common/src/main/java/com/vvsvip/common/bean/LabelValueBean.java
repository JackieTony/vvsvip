package com.vvsvip.common.bean;

/**
 * 简单键值对
 * Created by ADMIN on 2017/4/25.
 */
public class LabelValueBean {
    /**
     * 名字
     */
    private String name;
    /**
     * 值
     */
    private String value;

    public LabelValueBean() {
    }

    public LabelValueBean(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
