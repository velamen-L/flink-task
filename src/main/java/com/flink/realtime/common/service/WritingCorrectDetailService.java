package com.flink.realtime.common.service;

import com.flink.realtime.bean.WritingCorrectDetailDTO;
import java.util.List;

/**
 * 学生作文批改详情服务接口
 * 对应表：dm_writing_correct_detail
 */
public interface WritingCorrectDetailService {
    
    /**
     * 根据ID查询作文批改详情
     * @param id 主键ID
     * @return 作文批改详情DTO
     */
    WritingCorrectDetailDTO getById(Long id);
    
    /**
     * 根据作文批改ID查询详情列表
     * @param writingCorrectId 作文批改ID
     * @return 作文批改详情列表
     */
    List<WritingCorrectDetailDTO> getByWritingCorrectId(Long writingCorrectId);
    
    /**
     * 根据作文批改ID和页码查询详情
     * @param writingCorrectId 作文批改ID
     * @param writingCorrectIndex 页码
     * @return 作文批改详情DTO
     */
    WritingCorrectDetailDTO getByWritingCorrectIdAndIndex(Long writingCorrectId, Integer writingCorrectIndex);
    
    /**
     * 根据作文批改ID和进度状态查询详情列表
     * @param writingCorrectId 作文批改ID
     * @param progressStatus 进度状态
     * @return 作文批改详情列表
     */
    List<WritingCorrectDetailDTO> getByWritingCorrectIdAndStatus(Long writingCorrectId, Integer progressStatus);
    
    /**
     * 保存作文批改详情
     * @param writingCorrectDetail 作文批改详情DTO
     * @return 保存后的ID
     */
    Long save(WritingCorrectDetailDTO writingCorrectDetail);
    
    /**
     * 批量保存作文批改详情
     * @param writingCorrectDetails 作文批改详情列表
     * @return 保存成功的数量
     */
    int batchSave(List<WritingCorrectDetailDTO> writingCorrectDetails);
    
    /**
     * 更新作文批改详情
     * @param writingCorrectDetail 作文批改详情DTO
     * @return 是否更新成功
     */
    boolean update(WritingCorrectDetailDTO writingCorrectDetail);
    
    /**
     * 根据ID删除作文批改详情（逻辑删除）
     * @param id 主键ID
     * @return 是否删除成功
     */
    boolean deleteById(Long id);
    
    /**
     * 根据作文批改ID删除所有详情（逻辑删除）
     * @param writingCorrectId 作文批改ID
     * @return 删除成功的数量
     */
    int deleteByWritingCorrectId(Long writingCorrectId);
    
    /**
     * 更新批改进度状态
     * @param id 主键ID
     * @param progressStatus 新的进度状态
     * @return 是否更新成功
     */
    boolean updateProgressStatus(Long id, Integer progressStatus);
    
    /**
     * 更新批改内容信息
     * @param id 主键ID
     * @param writingContent 写作内容
     * @param goodWords 好词好句
     * @param wrongWords 错字
     * @return 是否更新成功
     */
    boolean updateCorrectContent(Long id, String writingContent, String goodWords, String wrongWords);
    
    /**
     * 更新异常信息
     * @param id 主键ID
     * @param errorCode 异常代码
     * @return 是否更新成功
     */
    boolean updateErrorInfo(Long id, String errorCode);
    
    /**
     * 根据学科查询作文批改详情
     * @param subject 学科
     * @return 作文批改详情列表
     */
    List<WritingCorrectDetailDTO> getBySubject(String subject);
    
    /**
     * 根据作文类型查询作文批改详情
     * @param writingType 作文类型
     * @return 作文批改详情列表
     */
    List<WritingCorrectDetailDTO> getByWritingType(String writingType);
    
    /**
     * 查询指定时间范围内的作文批改详情
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 作文批改详情列表
     */
    List<WritingCorrectDetailDTO> getByTimeRange(String startTime, String endTime);
    
    /**
     * 统计作文批改详情数量
     * @param writingCorrectId 作文批改ID
     * @return 详情数量
     */
    int countByWritingCorrectId(Long writingCorrectId);
    
    /**
     * 统计指定状态的作文批改详情数量
     * @param progressStatus 进度状态
     * @return 详情数量
     */
    int countByProgressStatus(Integer progressStatus);
    
    /**
     * 获取作文批改的最大页码
     * @param writingCorrectId 作文批改ID
     * @return 最大页码
     */
    Integer getMaxIndexByWritingCorrectId(Long writingCorrectId);
    
    /**
     * 检查作文批改是否所有页面都已完成
     * @param writingCorrectId 作文批改ID
     * @return 是否全部完成
     */
    boolean isAllPagesCompleted(Long writingCorrectId);
    
    /**
     * 根据图片路径查询作文批改详情
     * @param image 原图路径
     * @return 作文批改详情DTO
     */
    WritingCorrectDetailDTO getByImage(String image);
    
    /**
     * 根据处理后图片路径查询作文批改详情
     * @param imageProcessed 处理后图片路径
     * @return 作文批改详情DTO
     */
    WritingCorrectDetailDTO getByImageProcessed(String imageProcessed);
}

