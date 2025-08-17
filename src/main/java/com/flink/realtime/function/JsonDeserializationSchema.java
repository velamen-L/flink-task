package com.flink.realtime.function;

import com.alibaba.fastjson.JSON;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * JSON反序列化Schema
 * 
 * @author yangfanlin
 * @date 2025-01-17
 */
public class JsonDeserializationSchema<T> implements DeserializationSchema<T> {
    
    private static final Logger logger = LoggerFactory.getLogger(JsonDeserializationSchema.class);
    
    private final Class<T> clazz;
    
    public JsonDeserializationSchema(Class<T> clazz) {
        this.clazz = clazz;
    }
    
    @Override
    public T deserialize(byte[] message) {
        try {
            String jsonString = new String(message, StandardCharsets.UTF_8);
            return JSON.parseObject(jsonString, clazz);
        } catch (Exception e) {
            logger.error("JSON反序列化失败: {}", new String(message, StandardCharsets.UTF_8), e);
            return null;
        }
    }
    
    @Override
    public boolean isEndOfStream(T nextElement) {
        return false;
    }
    
    @Override
    public TypeInformation<T> getProducedType() {
        return TypeInformation.of(clazz);
    }
}
