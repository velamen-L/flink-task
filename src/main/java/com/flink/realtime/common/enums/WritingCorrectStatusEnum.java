package com.flink.realtime.common.enums;

/**
 * 作文批改进度状态枚举
 */
public enum WritingCorrectStatusEnum {
    
    /**
     * 批改异常
     */
    ERROR(-1, "批改异常"),
    
    /**
     * 未批改
     */
    NOT_STARTED(0, "未批改"),
    
    /**
     * 批改中
     */
    IN_PROGRESS(1, "批改中"),
    
    /**
     * 批改完成
     */
    COMPLETED(2, "批改完成");
    
    private final Integer code;
    private final String description;
    
    WritingCorrectStatusEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public Integer getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据状态码获取枚举
     * @param code 状态码
     * @return 对应的枚举值，如果不存在返回null
     */
    public static WritingCorrectStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (WritingCorrectStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
    
    /**
     * 检查状态码是否有效
     * @param code 状态码
     * @return 是否有效
     */
    public static boolean isValidCode(Integer code) {
        return getByCode(code) != null;
    }
    
    /**
     * 检查是否为完成状态
     * @param code 状态码
     * @return 是否为完成状态
     */
    public static boolean isCompleted(Integer code) {
        return COMPLETED.getCode().equals(code);
    }
    
    /**
     * 检查是否为异常状态
     * @param code 状态码
     * @return 是否为异常状态
     */
    public static boolean isError(Integer code) {
        return ERROR.getCode().equals(code);
    }
    
    /**
     * 检查是否为进行中状态
     * @param code 状态码
     * @return 是否为进行中状态
     */
    public static boolean isInProgress(Integer code) {
        return IN_PROGRESS.getCode().equals(code);
    }
    
    /**
     * 检查是否为未开始状态
     * @param code 状态码
     * @return 是否为未开始状态
     */
    public static boolean isNotStarted(Integer code) {
        return NOT_STARTED.getCode().equals(code);
    }
    
    @Override
    public String toString() {
        return "WritingCorrectStatusEnum{" +
                "code=" + code +
                ", description='" + description + '\'' +
                '}';
    }
}

