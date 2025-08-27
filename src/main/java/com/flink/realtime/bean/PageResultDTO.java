package com.flink.realtime.bean;

import java.util.List;

/**
 * 分页结果DTO
 * @param <T> 数据类型
 */
public class PageResultDTO<T> extends BaseBean {
    
    /**
     * 数据列表
     */
    private List<T> data;
    
    /**
     * 总记录数
     */
    private Long total;
    
    /**
     * 当前页码
     */
    private Integer pageNum;
    
    /**
     * 每页大小
     */
    private Integer pageSize;
    
    /**
     * 总页数
     */
    private Integer totalPages;
    
    /**
     * 是否有下一页
     */
    private Boolean hasNext;
    
    /**
     * 是否有上一页
     */
    private Boolean hasPrevious;

    // 构造函数
    public PageResultDTO() {
        super();
    }

    public PageResultDTO(List<T> data, Long total, Integer pageNum, Integer pageSize) {
        this.data = data;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.calculatePages();
    }

    /**
     * 计算分页信息
     */
    private void calculatePages() {
        if (total != null && pageSize != null && pageSize > 0) {
            this.totalPages = (int) Math.ceil((double) total / pageSize);
            this.hasNext = pageNum != null && totalPages != null && pageNum < totalPages;
            this.hasPrevious = pageNum != null && pageNum > 1;
        } else {
            this.totalPages = 0;
            this.hasNext = false;
            this.hasPrevious = false;
        }
    }

    /**
     * 创建空的分页结果
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param <T> 数据类型
     * @return 空的分页结果
     */
    public static <T> PageResultDTO<T> empty(Integer pageNum, Integer pageSize) {
        return new PageResultDTO<>(List.of(), 0L, pageNum, pageSize);
    }

    /**
     * 创建成功的分页结果
     * @param data 数据列表
     * @param total 总记录数
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param <T> 数据类型
     * @return 分页结果
     */
    public static <T> PageResultDTO<T> success(List<T> data, Long total, Integer pageNum, Integer pageSize) {
        return new PageResultDTO<>(data, total, pageNum, pageSize);
    }

    // Getter和Setter方法
    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
        this.calculatePages();
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
        this.calculatePages();
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
        this.calculatePages();
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    public Boolean getHasNext() {
        return hasNext;
    }

    public void setHasNext(Boolean hasNext) {
        this.hasNext = hasNext;
    }

    public Boolean getHasPrevious() {
        return hasPrevious;
    }

    public void setHasPrevious(Boolean hasPrevious) {
        this.hasPrevious = hasPrevious;
    }

    /**
     * 获取当前页的记录数
     * @return 当前页记录数
     */
    public Integer getCurrentPageSize() {
        return data != null ? data.size() : 0;
    }

    /**
     * 检查是否为空
     * @return 是否为空
     */
    public boolean isEmpty() {
        return data == null || data.isEmpty();
    }

    /**
     * 检查是否不为空
     * @return 是否不为空
     */
    public boolean isNotEmpty() {
        return !isEmpty();
    }

    @Override
    public String toString() {
        return "PageResultDTO{" +
                "data=" + data +
                ", total=" + total +
                ", pageNum=" + pageNum +
                ", pageSize=" + pageSize +
                ", totalPages=" + totalPages +
                ", hasNext=" + hasNext +
                ", hasPrevious=" + hasPrevious +
                '}';
    }
}

