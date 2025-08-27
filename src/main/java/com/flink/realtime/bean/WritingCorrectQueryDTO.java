package com.flink.realtime.bean;

import java.time.LocalDateTime;

/**
 * 作文批改查询条件DTO
 */
public class WritingCorrectQueryDTO extends BaseBean {
    
    /**
     * 学生ID
     */
    private Long userId;
    
    /**
     * 进度状态
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
     * 标题（模糊查询）
     */
    private String title;
    
    /**
     * 是否进行过作文讲题
     */
    private Boolean hasLecture;
    
    /**
     * 查询开始时间
     */
    private LocalDateTime startTimeBegin;
    
    /**
     * 查询结束时间
     */
    private LocalDateTime startTimeEnd;
    
    /**
     * 创建时间开始
     */
    private LocalDateTime createTimeBegin;
    
    /**
     * 创建时间结束
     */
    private LocalDateTime createTimeEnd;
    
    /**
     * 页码（从1开始）
     */
    private Integer pageNum = 1;
    
    /**
     * 每页大小
     */
    private Integer pageSize = 20;
    
    /**
     * 排序字段
     */
    private String orderBy = "create_time";
    
    /**
     * 排序方向：asc/desc
     */
    private String orderDirection = "desc";
    
    /**
     * 是否包含已删除记录
     */
    private Boolean includeDeleted = false;

    // 构造函数
    public WritingCorrectQueryDTO() {
        super();
    }

    // Getter和Setter方法
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

    public Boolean getHasLecture() {
        return hasLecture;
    }

    public void setHasLecture(Boolean hasLecture) {
        this.hasLecture = hasLecture;
    }

    public LocalDateTime getStartTimeBegin() {
        return startTimeBegin;
    }

    public void setStartTimeBegin(LocalDateTime startTimeBegin) {
        this.startTimeBegin = startTimeBegin;
    }

    public LocalDateTime getStartTimeEnd() {
        return startTimeEnd;
    }

    public void setStartTimeEnd(LocalDateTime startTimeEnd) {
        this.startTimeEnd = startTimeEnd;
    }

    public LocalDateTime getCreateTimeBegin() {
        return createTimeBegin;
    }

    public void setCreateTimeBegin(LocalDateTime createTimeBegin) {
        this.createTimeBegin = createTimeBegin;
    }

    public LocalDateTime getCreateTimeEnd() {
        return createTimeEnd;
    }

    public void setCreateTimeEnd(LocalDateTime createTimeEnd) {
        this.createTimeEnd = createTimeEnd;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public String getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    public String getOrderDirection() {
        return orderDirection;
    }

    public void setOrderDirection(String orderDirection) {
        this.orderDirection = orderDirection;
    }

    public Boolean getIncludeDeleted() {
        return includeDeleted;
    }

    public void setIncludeDeleted(Boolean includeDeleted) {
        this.includeDeleted = includeDeleted;
    }

    /**
     * 获取偏移量
     * @return 偏移量
     */
    public Integer getOffset() {
        return (pageNum - 1) * pageSize;
    }

    /**
     * 检查是否有查询条件
     * @return 是否有查询条件
     */
    public boolean hasQueryConditions() {
        return userId != null || progressStatus != null || subject != null || 
               grade != null || writingType != null || title != null || 
               hasLecture != null || startTimeBegin != null || startTimeEnd != null ||
               createTimeBegin != null || createTimeEnd != null;
    }

    @Override
    public String toString() {
        return "WritingCorrectQueryDTO{" +
                "userId=" + userId +
                ", progressStatus=" + progressStatus +
                ", subject='" + subject + '\'' +
                ", grade=" + grade +
                ", writingType='" + writingType + '\'' +
                ", title='" + title + '\'' +
                ", hasLecture=" + hasLecture +
                ", startTimeBegin=" + startTimeBegin +
                ", startTimeEnd=" + startTimeEnd +
                ", createTimeBegin=" + createTimeBegin +
                ", createTimeEnd=" + createTimeEnd +
                ", pageNum=" + pageNum +
                ", pageSize=" + pageSize +
                ", orderBy='" + orderBy + '\'' +
                ", orderDirection='" + orderDirection + '\'' +
                ", includeDeleted=" + includeDeleted +
                '}';
    }
}

