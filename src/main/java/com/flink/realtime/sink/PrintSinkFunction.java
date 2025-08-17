package com.flink.realtime.sink;

import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 控制台打印Sink函数
 * 
 * @author yangfanlin
 * @date 2025-01-17
 */
public class PrintSinkFunction<T> implements SinkFunction<T> {
    
    private static final Logger logger = LoggerFactory.getLogger(PrintSinkFunction.class);
    
    private final String prefix;
    
    public PrintSinkFunction() {
        this("OUTPUT");
    }
    
    public PrintSinkFunction(String prefix) {
        this.prefix = prefix;
    }
    
    @Override
    public void invoke(T value, Context context) {
        logger.info("{}: {}", prefix, value);
        System.out.println(prefix + ": " + value);
    }
}
