package com.flink.realtime.function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flink.realtime.bean.BusinessEvent;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 业务事件反序列化器
 * 
 * @author yangfanlin
 * @date 2025-01-17
 */
public class BusinessEventDeserializationSchema implements DeserializationSchema<BusinessEvent> {
    
    private static final Logger logger = LoggerFactory.getLogger(BusinessEventDeserializationSchema.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public BusinessEvent deserialize(byte[] message) throws IOException {
        try {
            String jsonStr = new String(message);
            BusinessEvent event = objectMapper.readValue(jsonStr, BusinessEvent.class);
            logger.debug("反序列化业务事件成功: {}", event);
            return event;
        } catch (Exception e) {
            logger.error("反序列化业务事件失败, 原始数据: {}", new String(message), e);
            // 返回null或抛出异常，根据业务需要决定
            return null;
        }
    }
    
    @Override
    public boolean isEndOfStream(BusinessEvent nextElement) {
        return false;
    }
    
    @Override
    public TypeInformation<BusinessEvent> getProducedType() {
        return TypeInformation.of(BusinessEvent.class);
    }
}
