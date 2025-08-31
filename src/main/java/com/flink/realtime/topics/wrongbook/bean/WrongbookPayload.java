package com.flink.realtime.topics.wrongbook.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

/**
 * 错题本系统Payload数据结构
 * 
 * @author AI代码生成器
 * @date 2025-08-29
 */
public class WrongbookPayload {

    /**
     * 错题添加事件Payload
     */
    public static class WrongbookAddPayload {
        @JsonProperty("wrong_id")
        private String wrongId;
        
        @JsonProperty("user_id")
        private String userId;
        
        @JsonProperty("question_id")
        private String questionId;
        
        @JsonProperty("pattern_id")
        private String patternId;
        
        @JsonProperty("subject")
        private String subject;
        
        @JsonProperty("chapter_id")
        private String chapterId;
        
        @JsonProperty("answer_record_id")
        private String answerRecordId;
        
        @JsonProperty("answer_image")
        private String answerImage;
        
        @JsonProperty("result")
        private Integer result;
        
        @JsonProperty("correct_status")
        private Integer correctStatus;
        
        @JsonProperty("origin")
        private String origin;
        
        @JsonProperty("tag_group")
        private String tagGroup;
        
        @JsonProperty("draft_image")
        private String draftImage;
        
        @JsonProperty("q_type")
        private Integer qType;
        
        @JsonProperty("zpd_pattern_id")
        private String zpdPatternId;
        
        @JsonProperty("create_time")
        private Long createTime;
        
        @JsonProperty("submit_time")
        private Long submitTime;
        
        @JsonProperty("question")
        private String question;
        
        @JsonProperty("answer")
        private String answer;
        
        @JsonProperty("analysis")
        private String analysis;
        
        @JsonProperty("difficulty")
        private BigDecimal difficulty;
        
        @JsonProperty("collect_reason")
        private String collectReason;
        
        @JsonProperty("collect_source")
        private String collectSource;

        // Getters and Setters
        public String getWrongId() { return wrongId; }
        public void setWrongId(String wrongId) { this.wrongId = wrongId; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getQuestionId() { return questionId; }
        public void setQuestionId(String questionId) { this.questionId = questionId; }
        
        public String getPatternId() { return patternId; }
        public void setPatternId(String patternId) { this.patternId = patternId; }
        
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        
        public String getChapterId() { return chapterId; }
        public void setChapterId(String chapterId) { this.chapterId = chapterId; }
        
        public String getAnswerRecordId() { return answerRecordId; }
        public void setAnswerRecordId(String answerRecordId) { this.answerRecordId = answerRecordId; }
        
        public String getAnswerImage() { return answerImage; }
        public void setAnswerImage(String answerImage) { this.answerImage = answerImage; }
        
        public Integer getResult() { return result; }
        public void setResult(Integer result) { this.result = result; }
        
        public Integer getCorrectStatus() { return correctStatus; }
        public void setCorrectStatus(Integer correctStatus) { this.correctStatus = correctStatus; }
        
        public String getOrigin() { return origin; }
        public void setOrigin(String origin) { this.origin = origin; }
        
        public String getTagGroup() { return tagGroup; }
        public void setTagGroup(String tagGroup) { this.tagGroup = tagGroup; }
        
        public String getDraftImage() { return draftImage; }
        public void setDraftImage(String draftImage) { this.draftImage = draftImage; }
        
        public Integer getQType() { return qType; }
        public void setQType(Integer qType) { this.qType = qType; }
        
        public String getZpdPatternId() { return zpdPatternId; }
        public void setZpdPatternId(String zpdPatternId) { this.zpdPatternId = zpdPatternId; }
        
        public Long getCreateTime() { return createTime; }
        public void setCreateTime(Long createTime) { this.createTime = createTime; }
        
        public Long getSubmitTime() { return submitTime; }
        public void setSubmitTime(Long submitTime) { this.submitTime = submitTime; }
        
        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
        
        public String getAnswer() { return answer; }
        public void setAnswer(String answer) { this.answer = answer; }
        
        public String getAnalysis() { return analysis; }
        public void setAnalysis(String analysis) { this.analysis = analysis; }
        
        public BigDecimal getDifficulty() { return difficulty; }
        public void setDifficulty(BigDecimal difficulty) { this.difficulty = difficulty; }
        
        public String getCollectReason() { return collectReason; }
        public void setCollectReason(String collectReason) { this.collectReason = collectReason; }
        
        public String getCollectSource() { return collectSource; }
        public void setCollectSource(String collectSource) { this.collectSource = collectSource; }
    }

    /**
     * 错题订正事件Payload
     */
    public static class WrongbookFixPayload {
        @JsonProperty("fix_id")
        private String fixId;
        
        @JsonProperty("wrong_id")
        private String wrongId;
        
        @JsonProperty("user_id")
        private String userId;
        
        @JsonProperty("question_id")
        private String questionId;
        
        @JsonProperty("pattern_id")
        private String patternId;
        
        @JsonProperty("fix_answer")
        private String fixAnswer;
        
        @JsonProperty("fix_image")
        private String fixImage;
        
        @JsonProperty("fix_result")
        private Integer fixResult;
        
        @JsonProperty("fix_result_desc")
        private String fixResultDesc;
        
        @JsonProperty("fix_time")
        private Long fixTime;
        
        @JsonProperty("fix_duration")
        private Integer fixDuration;
        
        @JsonProperty("fix_attempts")
        private Integer fixAttempts;
        
        @JsonProperty("fix_method")
        private String fixMethod;
        
        @JsonProperty("fix_source")
        private String fixSource;
        
        @JsonProperty("is_correct")
        private Boolean isCorrect;
        
        @JsonProperty("confidence_score")
        private BigDecimal confidenceScore;
        
        @JsonProperty("teacher_feedback")
        private String teacherFeedback;
        
        @JsonProperty("ai_feedback")
        private String aiFeedback;

        // Getters and Setters
        public String getFixId() { return fixId; }
        public void setFixId(String fixId) { this.fixId = fixId; }
        
        public String getWrongId() { return wrongId; }
        public void setWrongId(String wrongId) { this.wrongId = wrongId; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getQuestionId() { return questionId; }
        public void setQuestionId(String questionId) { this.questionId = questionId; }
        
        public String getPatternId() { return patternId; }
        public void setPatternId(String patternId) { this.patternId = patternId; }
        
        public String getFixAnswer() { return fixAnswer; }
        public void setFixAnswer(String fixAnswer) { this.fixAnswer = fixAnswer; }
        
        public String getFixImage() { return fixImage; }
        public void setFixImage(String fixImage) { this.fixImage = fixImage; }
        
        public Integer getFixResult() { return fixResult; }
        public void setFixResult(Integer fixResult) { this.fixResult = fixResult; }
        
        public String getFixResultDesc() { return fixResultDesc; }
        public void setFixResultDesc(String fixResultDesc) { this.fixResultDesc = fixResultDesc; }
        
        public Long getFixTime() { return fixTime; }
        public void setFixTime(Long fixTime) { this.fixTime = fixTime; }
        
        public Integer getFixDuration() { return fixDuration; }
        public void setFixDuration(Integer fixDuration) { this.fixDuration = fixDuration; }
        
        public Integer getFixAttempts() { return fixAttempts; }
        public void setFixAttempts(Integer fixAttempts) { this.fixAttempts = fixAttempts; }
        
        public String getFixMethod() { return fixMethod; }
        public void setFixMethod(String fixMethod) { this.fixMethod = fixMethod; }
        
        public String getFixSource() { return fixSource; }
        public void setFixSource(String fixSource) { this.fixSource = fixSource; }
        
        public Boolean getIsCorrect() { return isCorrect; }
        public void setIsCorrect(Boolean isCorrect) { this.isCorrect = isCorrect; }
        
        public BigDecimal getConfidenceScore() { return confidenceScore; }
        public void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }
        
        public String getTeacherFeedback() { return teacherFeedback; }
        public void setTeacherFeedback(String teacherFeedback) { this.teacherFeedback = teacherFeedback; }
        
        public String getAiFeedback() { return aiFeedback; }
        public void setAiFeedback(String aiFeedback) { this.aiFeedback = aiFeedback; }
    }

    /**
     * 错题删除事件Payload
     */
    public static class WrongbookDeletePayload {
        @JsonProperty("wrong_id")
        private String wrongId;
        
        @JsonProperty("user_id")
        private String userId;
        
        @JsonProperty("delete_reason")
        private String deleteReason;
        
        @JsonProperty("delete_time")
        private Long deleteTime;
        
        @JsonProperty("delete_source")
        private String deleteSource;

        // Getters and Setters
        public String getWrongId() { return wrongId; }
        public void setWrongId(String wrongId) { this.wrongId = wrongId; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getDeleteReason() { return deleteReason; }
        public void setDeleteReason(String deleteReason) { this.deleteReason = deleteReason; }
        
        public Long getDeleteTime() { return deleteTime; }
        public void setDeleteTime(Long deleteTime) { this.deleteTime = deleteTime; }
        
        public String getDeleteSource() { return deleteSource; }
        public void setDeleteSource(String deleteSource) { this.deleteSource = deleteSource; }
    }
}
