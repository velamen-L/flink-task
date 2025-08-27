package com.flink.realtime.bean;

import java.time.LocalDateTime;

/**
 * 学生作文批改详情DTO
 * 对应表：dm_writing_correct_detail
 */
public class WritingCorrectDetailDTO extends BaseBean {
    
    /**
     * 主键id
     */
    private Long id;
    
    /**
     * 作文原图
     */
    private String image;
    
    /**
     * 矫正后作文图
     */
    private String imageProcessed;
    
    /**
     * 作文批改id
     */
    private Long writingCorrectId;
    
    /**
     * 作文批改页码
     */
    private Integer writingCorrectIndex;
    
    /**
     * 进度状态，-1：批改异常，1：批改中，2：批改完成
     */
    private Integer progressStatus;
    
    /**
     * 学科
     */
    private String subject;
    
    /**
     * 作文类别
     */
    private String writingType;
    
    /**
     * 写作内容
     */
    private String writingContent;
    
    /**
     * 好词好句
     */
    private String goodWords;
    
    /**
     * 错字
     */
    private String wrongWords;
    
    /**
     * 批改开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 批改结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 异常原因
     */
    private String errorCode;
    
    /**
     * 删除标志：0-未删除；1-已删除
     */
    private Integer isDelete;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 修改时间
     */
    private LocalDateTime modifyTime;

    // 构造函数
    public WritingCorrectDetailDTO() {
        super();
    }

    public WritingCorrectDetailDTO(String image, String imageProcessed, Long writingCorrectId, 
                                   Integer writingCorrectIndex, Integer progressStatus) {
        this.image = image;
        this.imageProcessed = imageProcessed;
        this.writingCorrectId = writingCorrectId;
        this.writingCorrectIndex = writingCorrectIndex;
        this.progressStatus = progressStatus;
        this.isDelete = 0;
        this.createTime = LocalDateTime.now();
        this.modifyTime = LocalDateTime.now();
    }

    // Getter和Setter方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getImageProcessed() {
        return imageProcessed;
    }

    public void setImageProcessed(String imageProcessed) {
        this.imageProcessed = imageProcessed;
    }

    public Long getWritingCorrectId() {
        return writingCorrectId;
    }

    public void setWritingCorrectId(Long writingCorrectId) {
        this.writingCorrectId = writingCorrectId;
    }

    public Integer getWritingCorrectIndex() {
        return writingCorrectIndex;
    }

    public void setWritingCorrectIndex(Integer writingCorrectIndex) {
        this.writingCorrectIndex = writingCorrectIndex;
    }

    public Integer getProgressStatus() {
        return progressStatus;
    }

    public void setProgressStatus(Integer progressStatus) {
        this.progressStatus = progressStatus;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getWritingType() {
        return writingType;
    }

    public void setWritingType(String writingType) {
        this.writingType = writingType;
    }

    public String getWritingContent() {
        return writingContent;
    }

    public void setWritingContent(String writingContent) {
        this.writingContent = writingContent;
    }

    public String getGoodWords() {
        return goodWords;
    }

    public void setGoodWords(String goodWords) {
        this.goodWords = goodWords;
    }

    public String getWrongWords() {
        return wrongWords;
    }

    public void setWrongWords(String wrongWords) {
        this.wrongWords = wrongWords;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public Integer getIsDelete() {
        return isDelete;
    }

    public void setIsDelete(Integer isDelete) {
        this.isDelete = isDelete;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getModifyTime() {
        return modifyTime;
    }

    public void setModifyTime(LocalDateTime modifyTime) {
        this.modifyTime = modifyTime;
    }

    @Override
    public String toString() {
        return "WritingCorrectDetailDTO{" +
                "id=" + id +
                ", image='" + image + '\'' +
                ", imageProcessed='" + imageProcessed + '\'' +
                ", writingCorrectId=" + writingCorrectId +
                ", writingCorrectIndex=" + writingCorrectIndex +
                ", progressStatus=" + progressStatus +
                ", subject='" + subject + '\'' +
                ", writingType='" + writingType + '\'' +
                ", writingContent='" + writingContent + '\'' +
                ", goodWords='" + goodWords + '\'' +
                ", wrongWords='" + wrongWords + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", errorCode='" + errorCode + '\'' +
                ", isDelete=" + isDelete +
                ", createTime=" + createTime +
                ", modifyTime=" + modifyTime +
                '}';
    }
}

