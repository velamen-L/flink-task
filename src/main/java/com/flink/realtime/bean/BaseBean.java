package com.flink.realtime.bean;

import com.alibaba.fastjson.JSON;

import java.io.Serializable;

/**
 * 基础数据对象
 * 
 * @author yangfanlin
 * @date 2025-01-17
 */
public class BaseBean implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 转换为JSON字符串
     * 
     * @return JSON字符串
     */
    public String toJsonString() {
        return JSON.toJSONString(this);
    }
    
    @Override
    public String toString() {
        return this.toJsonString();
    }
}
