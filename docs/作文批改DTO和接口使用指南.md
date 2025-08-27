# 作文批改DTO和接口使用指南

## 概述

本文档介绍了作文批改相关的DTO对象和服务接口的使用方法，包括数据模型、接口定义和使用示例。

## 数据模型

### 1. WritingCorrectDTO - 作文批改主表DTO

对应数据库表：`dm_writing_correct`

**主要字段：**
- `id`: 主键ID
- `userId`: 学生ID
- `progressStatus`: 进度状态（-1：批改异常，0：未批改，1：批改中，2：批改完成）
- `subject`: 学科
- `grade`: 年级
- `writingType`: 作文类别
- `title`: 标题
- `coverImg`: 封面图片
- `stem`: 题干
- `outline`: 作文大纲
- `totalScore`: 评分
- `totalComment`: 总评
- `totalCommentTts`: 总评TTS
- `startTime`: 批改开始时间
- `endTime`: 批改结束时间
- `errorCode`: 异常原因
- `hasLecture`: 是否进行过作文讲题
- `isDelete`: 删除标志
- `createTime`: 创建时间
- `modifyTime`: 修改时间

### 2. WritingCorrectDetailDTO - 作文批改详情DTO

对应数据库表：`dm_writing_correct_detail`

**主要字段：**
- `id`: 主键ID
- `image`: 作文原图
- `imageProcessed`: 矫正后作文图
- `writingCorrectId`: 作文批改ID
- `writingCorrectIndex`: 作文批改页码
- `progressStatus`: 进度状态
- `subject`: 学科
- `writingType`: 作文类别
- `writingContent`: 写作内容
- `goodWords`: 好词好句
- `wrongWords`: 错字
- `startTime`: 批改开始时间
- `endTime`: 批改结束时间
- `errorCode`: 异常原因
- `isDelete`: 删除标志
- `createTime`: 创建时间
- `modifyTime`: 修改时间

### 3. WritingCorrectQueryDTO - 查询条件DTO

用于复杂查询场景，支持多条件组合查询和分页。

**主要字段：**
- `userId`: 学生ID
- `progressStatus`: 进度状态
- `subject`: 学科
- `grade`: 年级
- `writingType`: 作文类别
- `title`: 标题（模糊查询）
- `hasLecture`: 是否进行过作文讲题
- `startTimeBegin/startTimeEnd`: 批改开始时间范围
- `createTimeBegin/createTimeEnd`: 创建时间范围
- `pageNum`: 页码
- `pageSize`: 每页大小
- `orderBy`: 排序字段
- `orderDirection`: 排序方向
- `includeDeleted`: 是否包含已删除记录

### 4. PageResultDTO - 分页结果DTO

通用的分页结果包装类，支持泛型。

**主要字段：**
- `data`: 数据列表
- `total`: 总记录数
- `pageNum`: 当前页码
- `pageSize`: 每页大小
- `totalPages`: 总页数
- `hasNext`: 是否有下一页
- `hasPrevious`: 是否有上一页

### 5. WritingCorrectStatusEnum - 状态枚举

定义作文批改的进度状态常量。

**状态值：**
- `ERROR(-1)`: 批改异常
- `NOT_STARTED(0)`: 未批改
- `IN_PROGRESS(1)`: 批改中
- `COMPLETED(2)`: 批改完成

## 服务接口

### 1. WritingCorrectService - 作文批改主表服务接口

提供作文批改主表的CRUD操作和业务方法。

**主要方法：**
- `getById(Long id)`: 根据ID查询
- `getByUserId(Long userId)`: 根据学生ID查询
- `getByUserIdAndStatus(Long userId, Integer progressStatus)`: 根据学生ID和状态查询
- `save(WritingCorrectDTO writingCorrect)`: 保存记录
- `update(WritingCorrectDTO writingCorrect)`: 更新记录
- `deleteById(Long id)`: 逻辑删除
- `updateProgressStatus(Long id, Integer progressStatus)`: 更新进度状态
- `updateCorrectResult(Long id, String totalScore, String totalComment, String totalCommentTts)`: 更新批改结果
- `updateErrorInfo(Long id, String errorCode)`: 更新异常信息
- `queryByPage(WritingCorrectQueryDTO queryDTO)`: 分页查询
- `queryByCondition(WritingCorrectQueryDTO queryDTO)`: 条件查询

### 2. WritingCorrectDetailService - 作文批改详情服务接口

提供作文批改详情的CRUD操作和业务方法。

**主要方法：**
- `getById(Long id)`: 根据ID查询
- `getByWritingCorrectId(Long writingCorrectId)`: 根据作文批改ID查询详情列表
- `getByWritingCorrectIdAndIndex(Long writingCorrectId, Integer writingCorrectIndex)`: 根据作文批改ID和页码查询
- `save(WritingCorrectDetailDTO writingCorrectDetail)`: 保存详情
- `batchSave(List<WritingCorrectDetailDTO> writingCorrectDetails)`: 批量保存
- `update(WritingCorrectDetailDTO writingCorrectDetail)`: 更新详情
- `deleteById(Long id)`: 逻辑删除
- `deleteByWritingCorrectId(Long writingCorrectId)`: 根据作文批改ID删除所有详情
- `updateProgressStatus(Long id, Integer progressStatus)`: 更新进度状态
- `updateCorrectContent(Long id, String writingContent, String goodWords, String wrongWords)`: 更新批改内容
- `updateErrorInfo(Long id, String errorCode)`: 更新异常信息
- `getMaxIndexByWritingCorrectId(Long writingCorrectId)`: 获取最大页码
- `isAllPagesCompleted(Long writingCorrectId)`: 检查是否所有页面都已完成

## 使用示例

### 1. 创建作文批改记录

```java
// 创建作文批改DTO
WritingCorrectDTO writingCorrect = new WritingCorrectDTO();
writingCorrect.setUserId(12345L);
writingCorrect.setProgressStatus(WritingCorrectStatusEnum.NOT_STARTED.getCode());
writingCorrect.setSubject("语文");
writingCorrect.setGrade(6);
writingCorrect.setWritingType("记叙文");
writingCorrect.setTitle("我的理想");
writingCorrect.setCoverImg("http://example.com/cover.jpg");
writingCorrect.setStem("请以'我的理想'为题，写一篇记叙文");

// 保存记录
Long id = writingCorrectService.save(writingCorrect);
```

### 2. 创建作文批改详情

```java
// 创建作文批改详情DTO
WritingCorrectDetailDTO detail = new WritingCorrectDetailDTO();
detail.setImage("http://example.com/original.jpg");
detail.setImageProcessed("http://example.com/processed.jpg");
detail.setWritingCorrectId(writingCorrectId);
detail.setWritingCorrectIndex(1);
detail.setProgressStatus(WritingCorrectStatusEnum.IN_PROGRESS.getCode());
detail.setSubject("语文");
detail.setWritingType("记叙文");

// 保存详情
Long detailId = writingCorrectDetailService.save(detail);
```

### 3. 分页查询作文批改记录

```java
// 创建查询条件
WritingCorrectQueryDTO queryDTO = new WritingCorrectQueryDTO();
queryDTO.setUserId(12345L);
queryDTO.setProgressStatus(WritingCorrectStatusEnum.COMPLETED.getCode());
queryDTO.setSubject("语文");
queryDTO.setPageNum(1);
queryDTO.setPageSize(10);
queryDTO.setOrderBy("create_time");
queryDTO.setOrderDirection("desc");

// 执行分页查询
PageResultDTO<WritingCorrectDTO> result = writingCorrectService.queryByPage(queryDTO);

// 处理结果
if (result.isNotEmpty()) {
    for (WritingCorrectDTO item : result.getData()) {
        System.out.println("作文标题: " + item.getTitle());
        System.out.println("批改状态: " + item.getProgressStatus());
    }
}
```

### 4. 更新批改进度

```java
// 更新为批改中状态
writingCorrectService.updateProgressStatus(writingCorrectId, 
    WritingCorrectStatusEnum.IN_PROGRESS.getCode());

// 更新批改结果
writingCorrectService.updateCorrectResult(writingCorrectId, 
    "85", "文章结构清晰，语言生动，但存在一些错别字", "韩雪总评TTS内容");

// 更新为完成状态
writingCorrectService.updateProgressStatus(writingCorrectId, 
    WritingCorrectStatusEnum.COMPLETED.getCode());
```

### 5. 更新详情内容

```java
// 更新批改内容
writingCorrectDetailService.updateCorrectContent(detailId, 
    "今天，我想和大家分享我的理想...", 
    "理想、追求、奋斗", 
    "理象、追球、奋头");

// 更新为完成状态
writingCorrectDetailService.updateProgressStatus(detailId, 
    WritingCorrectStatusEnum.COMPLETED.getCode());
```

### 6. 查询作文批改详情

```java
// 查询某个作文的所有详情
List<WritingCorrectDetailDTO> details = 
    writingCorrectDetailService.getByWritingCorrectId(writingCorrectId);

// 查询特定页码的详情
WritingCorrectDetailDTO detail = 
    writingCorrectDetailService.getByWritingCorrectIdAndIndex(writingCorrectId, 1);

// 检查是否所有页面都已完成
boolean allCompleted = 
    writingCorrectDetailService.isAllPagesCompleted(writingCorrectId);
```

### 7. 状态检查

```java
// 检查状态是否有效
boolean isValid = WritingCorrectStatusEnum.isValidCode(progressStatus);

// 检查是否为完成状态
boolean isCompleted = WritingCorrectStatusEnum.isCompleted(progressStatus);

// 检查是否为异常状态
boolean isError = WritingCorrectStatusEnum.isError(progressStatus);

// 获取状态描述
WritingCorrectStatusEnum status = WritingCorrectStatusEnum.getByCode(progressStatus);
if (status != null) {
    System.out.println("状态描述: " + status.getDescription());
}
```

## 注意事项

1. **状态管理**: 使用`WritingCorrectStatusEnum`枚举类来管理状态，避免硬编码状态值。

2. **时间处理**: 所有时间字段使用`LocalDateTime`类型，确保时间处理的准确性。

3. **逻辑删除**: 删除操作采用逻辑删除方式，通过`isDelete`字段标记。

4. **分页查询**: 使用`WritingCorrectQueryDTO`进行复杂查询，支持多条件组合和分页。

5. **批量操作**: 对于大量数据操作，使用批量方法提高性能。

6. **异常处理**: 在业务逻辑中正确处理异常情况，及时更新`errorCode`字段。

7. **数据验证**: 在保存和更新操作前，验证数据的完整性和有效性。

## 扩展建议

1. **缓存优化**: 对于频繁查询的数据，可以考虑添加缓存机制。

2. **异步处理**: 对于耗时的批改操作，可以考虑使用异步处理。

3. **监控告警**: 添加批改异常监控和告警机制。

4. **数据统计**: 基于现有接口，可以扩展统计报表功能。

5. **权限控制**: 根据业务需求，添加相应的权限控制逻辑。

