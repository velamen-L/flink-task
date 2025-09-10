package com.flink.wrongbook.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 错题本事件Kafka测试数据生成器
 * 基于wrongbook-request-simple.md配置自动生成
 * 生成器: simple-sql-generator.mdc v1.0
 */
public class WrongbookKafkaTestProducer {
    
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String TOPIC_NAME = "business-events";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // 测试数据常量
    private static final String[] SUBJECTS = {"MATH", "ENGLISH", "CHINESE", "PHYSICS", "CHEMISTRY", "BIOLOGY"};
    private static final String[] USER_IDS = {"user_001", "user_002", "user_003", "user_004", "user_005"};
    private static final String[] PATTERN_IDS = {"pattern_001", "pattern_002", "pattern_003", "pattern_004"};
    private static final int[] FIX_RESULTS = {0, 1}; // 0=未订正, 1=订正
    
    public static void main(String[] args) {
        WrongbookKafkaTestProducer producer = new WrongbookKafkaTestProducer();
        
        try {
            // 发送测试数据
            producer.sendTestData(100); // 发送100条测试消息
            System.out.println("测试数据发送完成！");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 发送测试数据
     */
    public void sendTestData(int messageCount) throws Exception {
        Properties props = createKafkaProperties();
        
        try (Producer<String, String> producer = new KafkaProducer<>(props)) {
            
            for (int i = 0; i < messageCount; i++) {
                // 生成BusinessEvent消息
                BusinessEvent event = generateWrongbookEvent(i);
                String eventJson = objectMapper.writeValueAsString(event);
                
                // 发送到Kafka
                ProducerRecord<String, String> record = new ProducerRecord<>(
                    TOPIC_NAME, 
                    event.getPayload().getUserId(), // 使用userId作为key
                    eventJson
                );
                
                producer.send(record, new Callback() {
                    @Override
                    public void onCompletion(RecordMetadata metadata, Exception exception) {
                        if (exception != null) {
                            System.err.println("发送消息失败: " + exception.getMessage());
                        } else {
                            System.out.println("消息发送成功 - Topic: " + metadata.topic() + 
                                             ", Partition: " + metadata.partition() + 
                                             ", Offset: " + metadata.offset());
                        }
                    }
                });
                
                // 控制发送频率
                Thread.sleep(100); // 每100ms发送一条
            }
            
            producer.flush();
        }
    }
    
    /**
     * 生成错题本修正事件
     */
    private BusinessEvent generateWrongbookEvent(int index) {
        BusinessEvent event = new BusinessEvent();
        event.setDomain("wrongbook");
        event.setType("wrongbook_fix");
        event.setEventTime(Instant.now().toString());
        
        // 生成WrongbookFixPayload
        WrongbookFixPayload payload = new WrongbookFixPayload();
        payload.setFixId("fix_" + String.format("%06d", index));
        payload.setWrongId("wrong_" + String.format("%06d", index));
        payload.setUserId(getRandomElement(USER_IDS));
        payload.setSubject(getRandomElement(SUBJECTS));
        payload.setQuestionId("question_" + String.format("%06d", index));
        payload.setPatternId(getRandomElement(PATTERN_IDS));
        payload.setCreateTime(System.currentTimeMillis() - ThreadLocalRandom.current().nextLong(0, 86400000)); // 24小时内
        payload.setSubmitTime(System.currentTimeMillis());
        payload.setFixResult(getRandomElement(FIX_RESULTS));
        
        event.setPayload(payload);
        return event;
    }
    
    /**
     * 创建Kafka配置
     */
    private Properties createKafkaProperties() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        return props;
    }
    
    /**
     * 随机选择数组元素
     */
    private <T> T getRandomElement(T[] array) {
        return array[ThreadLocalRandom.current().nextInt(array.length)];
    }
    
    private int getRandomElement(int[] array) {
        return array[ThreadLocalRandom.current().nextInt(array.length)];
    }
    
    // ========================================================================
    // 数据模型类
    // ========================================================================
    
    /**
     * BusinessEvent标准事件结构
     */
    public static class BusinessEvent {
        private String domain;
        private String type;
        private String eventTime;
        private WrongbookFixPayload payload;
        
        // Getters and Setters
        public String getDomain() { return domain; }
        public void setDomain(String domain) { this.domain = domain; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getEventTime() { return eventTime; }
        public void setEventTime(String eventTime) { this.eventTime = eventTime; }
        
        public WrongbookFixPayload getPayload() { return payload; }
        public void setPayload(WrongbookFixPayload payload) { this.payload = payload; }
    }
    
    /**
     * WrongbookFixPayload错题本修正载荷
     * 基于wrongbook-request-simple.md中的payload_structure定义
     */
    public static class WrongbookFixPayload {
        private String fixId;        // 修正记录ID
        private String wrongId;      // 错题记录ID
        private String userId;       // 用户ID
        private String subject;      // 学科代码
        private String questionId;   // 题目ID
        private String patternId;    // 题型ID
        private Long createTime;     // 创建时间
        private Long submitTime;     // 提交时间
        private Integer fixResult;   // 修正结果(0/1)
        
        // Getters and Setters
        public String getFixId() { return fixId; }
        public void setFixId(String fixId) { this.fixId = fixId; }
        
        public String getWrongId() { return wrongId; }
        public void setWrongId(String wrongId) { this.wrongId = wrongId; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        
        public String getQuestionId() { return questionId; }
        public void setQuestionId(String questionId) { this.questionId = questionId; }
        
        public String getPatternId() { return patternId; }
        public void setPatternId(String patternId) { this.patternId = patternId; }
        
        public Long getCreateTime() { return createTime; }
        public void setCreateTime(Long createTime) { this.createTime = createTime; }
        
        public Long getSubmitTime() { return submitTime; }
        public void setSubmitTime(Long submitTime) { this.submitTime = submitTime; }
        
        public Integer getFixResult() { return fixResult; }
        public void setFixResult(Integer fixResult) { this.fixResult = fixResult; }
    }
    
    // ========================================================================
    // 特殊测试场景生成器
    // ========================================================================
    
    /**
     * 生成边界测试数据
     */
    public void sendBoundaryTestData() throws Exception {
        Properties props = createKafkaProperties();
        
        try (Producer<String, String> producer = new KafkaProducer<>(props)) {
            
            // 测试场景1: 极值数据
            BusinessEvent extremeEvent = generateWrongbookEvent(9999);
            extremeEvent.getPayload().setCreateTime(0L); // 最小时间
            extremeEvent.getPayload().setSubmitTime(System.currentTimeMillis());
            sendEvent(producer, extremeEvent, "极值时间测试");
            
            // 测试场景2: 特殊学科
            BusinessEvent specialSubjectEvent = generateWrongbookEvent(9998);
            specialSubjectEvent.getPayload().setSubject("AOSHU"); // 数学思维
            sendEvent(producer, specialSubjectEvent, "特殊学科测试");
            
            // 测试场景3: 修正失败
            BusinessEvent failedFixEvent = generateWrongbookEvent(9997);
            failedFixEvent.getPayload().setFixResult(0); // 未订正
            sendEvent(producer, failedFixEvent, "修正失败测试");
            
            // 测试场景4: 相同用户多次修正
            String sameUserId = "test_user_001";
            for (int i = 0; i < 5; i++) {
                BusinessEvent sameUserEvent = generateWrongbookEvent(9990 + i);
                sameUserEvent.getPayload().setUserId(sameUserId);
                sameUserEvent.getPayload().setSubmitTime(System.currentTimeMillis() + i * 1000);
                sendEvent(producer, sameUserEvent, "相同用户测试_" + i);
            }
            
            producer.flush();
            System.out.println("边界测试数据发送完成！");
        }
    }
    
    /**
     * 发送单个事件
     */
    private void sendEvent(Producer<String, String> producer, BusinessEvent event, String description) throws Exception {
        String eventJson = objectMapper.writeValueAsString(event);
        ProducerRecord<String, String> record = new ProducerRecord<>(
            TOPIC_NAME, 
            event.getPayload().getUserId(),
            eventJson
        );
        
        producer.send(record);
        System.out.println("发送测试事件: " + description);
    }
    
    /**
     * 性能测试 - 批量发送
     */
    public void performanceTest(int messageCount, int batchSize) throws Exception {
        Properties props = createKafkaProperties();
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSize);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 10); // 增加批处理等待时间
        
        try (Producer<String, String> producer = new KafkaProducer<>(props)) {
            
            long startTime = System.currentTimeMillis();
            
            for (int i = 0; i < messageCount; i++) {
                BusinessEvent event = generateWrongbookEvent(i);
                String eventJson = objectMapper.writeValueAsString(event);
                
                ProducerRecord<String, String> record = new ProducerRecord<>(
                    TOPIC_NAME, 
                    event.getPayload().getUserId(),
                    eventJson
                );
                
                producer.send(record);
                
                if (i % 1000 == 0) {
                    System.out.println("已发送 " + i + " 条消息");
                }
            }
            
            producer.flush();
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            double throughput = (double) messageCount / duration * 1000;
            
            System.out.println("性能测试完成:");
            System.out.println("- 消息数量: " + messageCount);
            System.out.println("- 耗时: " + duration + "ms");
            System.out.println("- 吞吐量: " + String.format("%.2f", throughput) + " 消息/秒");
        }
    }
}
