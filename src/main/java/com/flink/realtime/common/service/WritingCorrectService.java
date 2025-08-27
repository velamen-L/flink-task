package com.flink.realtime.common.service;

import com.flink.realtime.bean.WritingCorrectDTO;
import com.flink.realtime.bean.WritingCorrectQueryDTO;
import com.flink.realtime.bean.PageResultDTO;
import java.util.List;

/**
 * 学生作文批改服务接口
 * 对应表：dm_writing_correct
 */
public interface WritingCorrectService {
    
    /**
     * 根据ID查询作文批改记录
     * @param id 主键ID
     * @return 作文批改DTO
     */
    WritingCorrectDTO getById(Long id);
    
    /**
     * 根据学生ID查询作文批改记录列表
     * @param userId 学生ID
     * @return 作文批改记录列表
     */
    List<WritingCorrectDTO> getByUserId(Long userId);
    
    /**
     * 根据学生ID和进度状态查询作文批改记录
     * @param userId 学生ID
     * @param progressStatus 进度状态
     * @return 作文批改记录列表
     */
    List<WritingCorrectDTO> getByUserIdAndStatus(Long userId, Integer progressStatus);
    
    /**
     * 保存作文批改记录
     * @param writingCorrect 作文批改DTO
     * @return 保存后的ID
     */
    Long save(WritingCorrectDTO writingCorrect);
    
    /**
     * 更新作文批改记录
     * @param writingCorrect 作文批改DTO
     * @return 是否更新成功
     */
    boolean update(WritingCorrectDTO writingCorrect);
    
    /**
     * 根据ID删除作文批改记录（逻辑删除）
     * @param id 主键ID
     * @return 是否删除成功
     */
    boolean deleteById(Long id);
    
    /**
     * 更新批改进度状态
     * @param id 主键ID
     * @param progressStatus 新的进度状态
     * @return 是否更新成功
     */
    boolean updateProgressStatus(Long id, Integer progressStatus);
    
    /**
     * 更新批改完成信息
     * @param id 主键ID
     * @param totalScore 评分
     * @param totalComment 总评
     * @param totalCommentTts 总评TTS
     * @return 是否更新成功
     */
    boolean updateCorrectResult(Long id, String totalScore, String totalComment, String totalCommentTts);
    
    /**
     * 更新异常信息
     * @param id 主键ID
     * @param errorCode 异常代码
     * @return 是否更新成功
     */
    boolean updateErrorInfo(Long id, String errorCode);
    
    /**
     * 根据学科和年级查询作文批改记录
     * @param subject 学科
     * @param grade 年级
     * @return 作文批改记录列表
     */
    List<WritingCorrectDTO> getBySubjectAndGrade(String subject, Integer grade);
    
    /**
     * 根据作文类型查询作文批改记录
     * @param writingType 作文类型
     * @return 作文批改记录列表
     */
    List<WritingCorrectDTO> getByWritingType(String writingType);
    
    /**
     * 查询指定时间范围内的作文批改记录
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 作文批改记录列表
     */
    List<WritingCorrectDTO> getByTimeRange(String startTime, String endTime);
    
    /**
     * 统计学生作文批改数量
     * @param userId 学生ID
     * @return 批改数量
     */
    int countByUserId(Long userId);
    
    /**
     * 统计指定状态的作文批改数量
     * @param progressStatus 进度状态
     * @return 批改数量
     */
    int countByProgressStatus(Integer progressStatus);
    
    /**
     * 分页查询作文批改记录
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    PageResultDTO<WritingCorrectDTO> queryByPage(WritingCorrectQueryDTO queryDTO);
    
    /**
     * 根据查询条件查询作文批改记录列表
     * @param queryDTO 查询条件
     * @return 作文批改记录列表
     */
    List<WritingCorrectDTO> queryByCondition(WritingCorrectQueryDTO queryDTO);
}
