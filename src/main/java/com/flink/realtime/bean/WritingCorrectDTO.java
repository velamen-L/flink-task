package com.flink.realtime.bean;

import java.time.LocalDateTime;

/**
 * 学生作文批改DTO
 * 对应表：dm_writing_correct
 */
public class WritingCorrectDTO extends BaseBean {
    
    /**
     * 主键id
     */
    private Long id;
    
    /**
     * 学生id
     */
    private Long userId;
    
    /**
     * 进度状态，-1：批改异常，0：未批改，1：批改中，2：批改完成
     */
    private Integer progressStatus;
    
    /**
     * 学科
     */
    private String subject;
    
    /**
     * 年级
     */
    private Integer grade;
    
    /**
     * 作文类别
     */
    private String writingType;
    
    /**
     * 标题
     */
    private String title;
    
    /**
     * 封面
     */
    private String coverImg;
    
    /**
     * 题干
     */
    private String stem;
    
    /**
     * 作文大纲
     */
    private String outline;
    
    /**
     * 评分
     */
    private String totalScore;
    
    /**
     * 总评
     */
    private String totalComment;
    
    /**
     * 韩雪总评tts
     */
    private String totalCommentTts;
    
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
     * 是否进行过作文讲题
     */
    private Boolean hasLecture;
    
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
    public WritingCorrectDTO() {
        super();
    }

    public WritingCorrectDTO(Long userId, Integer progressStatus, Integer grade) {
        this.userId = userId;
        this.progressStatus = progressStatus;
        this.grade = grade;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public Integer getGrade() {
        return grade;
    }

    public void setGrade(Integer grade) {
        this.grade = grade;
    }

    public String getWritingType() {
        return writingType;
    }

    public void setWritingType(String writingType) {
        this.writingType = writingType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCoverImg() {
        return coverImg;
    }

    public void setCoverImg(String coverImg) {
        this.coverImg = coverImg;
    }

    public String getStem() {
        return stem;
    }

    public void setStem(String stem) {
        this.stem = stem;
    }

    public String getOutline() {
        return outline;
    }

    public void setOutline(String outline) {
        this.outline = outline;
    }

    public String getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(String totalScore) {
        this.totalScore = totalScore;
    }

    public String getTotalComment() {
        return totalComment;
    }

    public void setTotalComment(String totalComment) {
        this.totalComment = totalComment;
    }

    public String getTotalCommentTts() {
        return totalCommentTts;
    }

    public void setTotalCommentTts(String totalCommentTts) {
        this.totalCommentTts = totalCommentTts;
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

    public Boolean getHasLecture() {
        return hasLecture;
    }

    public void setHasLecture(Boolean hasLecture) {
        this.hasLecture = hasLecture;
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
        return "WritingCorrectDTO{" +
                "id=" + id +
                ", userId=" + userId +
                ", progressStatus=" + progressStatus +
                ", subject='" + subject + '\'' +
                ", grade=" + grade +
                ", writingType='" + writingType + '\'' +
                ", title='" + title + '\'' +
                ", coverImg='" + coverImg + '\'' +
                ", stem='" + stem + '\'' +
                ", outline='" + outline + '\'' +
                ", totalScore='" + totalScore + '\'' +
                ", totalComment='" + totalComment + '\'' +
                ", totalCommentTts='" + totalCommentTts + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", errorCode='" + errorCode + '\'' +
                ", hasLecture=" + hasLecture +
                ", isDelete=" + isDelete +
                ", createTime=" + createTime +
                ", modifyTime=" + modifyTime +
                '}';
    }
}

