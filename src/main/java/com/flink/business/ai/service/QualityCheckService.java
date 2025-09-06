package com.flink.business.ai.service;

import com.flink.business.ai.model.ParsedRequirement;
import com.flink.business.ai.model.QualityCheckResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * SQL质量检查服务
 * 
 * 核心功能：
 * 1. 语法检查
 * 2. 性能分析
 * 3. 业务规则验证
 * 4. 安全检查
 * 5. 最佳实践检查
 * 6. 综合质量评分
 */
@Slf4j
@Service
public class QualityCheckService {

    @Autowired
    private SQLSyntaxChecker syntaxChecker;

    @Autowired
    private PerformanceAnalyzer performanceAnalyzer;

    @Autowired
    private BusinessRuleValidator businessRuleValidator;

    @Autowired
    private SecurityChecker securityChecker;

    @Autowired
    private BestPracticeChecker bestPracticeChecker;

    /**
     * 执行完整的质量检查
     */
    public QualityCheckResult checkQuality(String sql, ParsedRequirement requirement) {
        log.info("开始执行SQL质量检查");
        
        long startTime = System.currentTimeMillis();
        
        try {
            QualityCheckResult result = QualityCheckResult.builder()
                .sql(sql)
                .requirement(requirement)
                .checkTime(System.currentTimeMillis())
                .build();

            // 1. 语法检查
            SyntaxCheckResult syntaxResult = performSyntaxCheck(sql);
            result.setSyntaxCheck(syntaxResult);
            
            // 2. 性能分析（只有语法正确才进行）
            if (syntaxResult.isValid()) {
                PerformanceCheckResult perfResult = performPerformanceCheck(sql, requirement);
                result.setPerformanceCheck(perfResult);
            }
            
            // 3. 业务规则验证
            BusinessRuleCheckResult businessResult = performBusinessRuleCheck(sql, requirement);
            result.setBusinessRuleCheck(businessResult);
            
            // 4. 安全检查
            SecurityCheckResult securityResult = performSecurityCheck(sql);
            result.setSecurityCheck(securityResult);
            
            // 5. 最佳实践检查
            BestPracticeCheckResult practiceResult = performBestPracticeCheck(sql, requirement);
            result.setBestPracticeCheck(practiceResult);
            
            // 6. 计算综合评分
            double overallScore = calculateOverallScore(result);
            result.setOverallScore(overallScore);
            
            // 7. 生成改进建议
            List<QualityIssue> issues = generateQualityIssues(result);
            result.setQualityIssues(issues);
            
            // 8. 设置质量等级
            QualityLevel level = determineQualityLevel(overallScore);
            result.setQualityLevel(level);
            
            long duration = System.currentTimeMillis() - startTime;
            result.setCheckDuration(duration);
            
            log.info("质量检查完成，耗时: {}ms, 综合评分: {}", duration, overallScore);
            
            return result;
            
        } catch (Exception e) {
            log.error("质量检查执行失败", e);
            throw new QualityCheckException("质量检查失败: " + e.getMessage(), e);
        }
    }

    /**
     * 语法检查
     */
    private SyntaxCheckResult performSyntaxCheck(String sql) {
        log.debug("执行语法检查");
        
        try {
            SyntaxCheckResult result = new SyntaxCheckResult();
            
            // 1. 基础语法验证
            List<SyntaxError> syntaxErrors = syntaxChecker.validateSyntax(sql);
            result.setSyntaxErrors(syntaxErrors);
            result.setValid(syntaxErrors.isEmpty());
            
            // 2. FlinkSQL特定语法检查
            List<SyntaxWarning> flinkWarnings = syntaxChecker.checkFlinkSpecific(sql);
            result.setFlinkWarnings(flinkWarnings);
            
            // 3. 计算语法得分
            double syntaxScore = calculateSyntaxScore(syntaxErrors, flinkWarnings);
            result.setSyntaxScore(syntaxScore);
            
            return result;
            
        } catch (Exception e) {
            log.error("语法检查失败", e);
            return SyntaxCheckResult.failed("语法检查执行失败: " + e.getMessage());
        }
    }

    /**
     * 性能检查
     */
    private PerformanceCheckResult performPerformanceCheck(String sql, ParsedRequirement requirement) {
        log.debug("执行性能检查");
        
        try {
            PerformanceCheckResult result = new PerformanceCheckResult();
            
            // 1. JOIN性能分析
            JoinPerformanceAnalysis joinAnalysis = performanceAnalyzer.analyzeJoins(sql);
            result.setJoinAnalysis(joinAnalysis);
            
            // 2. 聚合性能分析
            AggregationPerformanceAnalysis aggAnalysis = performanceAnalyzer.analyzeAggregations(sql);
            result.setAggregationAnalysis(aggAnalysis);
            
            // 3. 窗口性能分析
            WindowPerformanceAnalysis windowAnalysis = performanceAnalyzer.analyzeWindows(sql);
            result.setWindowAnalysis(windowAnalysis);
            
            // 4. 数据倾斜风险评估
            DataSkewRiskAssessment skewRisk = performanceAnalyzer.assessDataSkewRisk(sql, requirement);
            result.setDataSkewRisk(skewRisk);
            
            // 5. 内存使用评估
            MemoryUsageEstimation memoryUsage = performanceAnalyzer.estimateMemoryUsage(sql, requirement);
            result.setMemoryUsage(memoryUsage);
            
            // 6. 计算性能得分
            double performanceScore = calculatePerformanceScore(result);
            result.setPerformanceScore(performanceScore);
            
            return result;
            
        } catch (Exception e) {
            log.error("性能检查失败", e);
            return PerformanceCheckResult.failed("性能检查执行失败: " + e.getMessage());
        }
    }

    /**
     * 业务规则检查
     */
    private BusinessRuleCheckResult performBusinessRuleCheck(String sql, ParsedRequirement requirement) {
        log.debug("执行业务规则检查");
        
        try {
            BusinessRuleCheckResult result = new BusinessRuleCheckResult();
            
            // 1. 必需字段检查
            List<RequiredFieldCheck> fieldChecks = businessRuleValidator.checkRequiredFields(sql, requirement);
            result.setRequiredFieldChecks(fieldChecks);
            
            // 2. 数据类型检查
            List<DataTypeCheck> typeChecks = businessRuleValidator.checkDataTypes(sql, requirement);
            result.setDataTypeChecks(typeChecks);
            
            // 3. 业务逻辑一致性检查
            List<BusinessLogicCheck> logicChecks = businessRuleValidator.checkBusinessLogic(sql, requirement);
            result.setBusinessLogicChecks(logicChecks);
            
            // 4. 数据范围检查
            List<DataRangeCheck> rangeChecks = businessRuleValidator.checkDataRanges(sql, requirement);
            result.setDataRangeChecks(rangeChecks);
            
            // 5. 计算业务规则得分
            double businessScore = calculateBusinessRuleScore(result);
            result.setBusinessRuleScore(businessScore);
            
            return result;
            
        } catch (Exception e) {
            log.error("业务规则检查失败", e);
            return BusinessRuleCheckResult.failed("业务规则检查执行失败: " + e.getMessage());
        }
    }

    /**
     * 安全检查
     */
    private SecurityCheckResult performSecurityCheck(String sql) {
        log.debug("执行安全检查");
        
        try {
            SecurityCheckResult result = new SecurityCheckResult();
            
            // 1. SQL注入检查
            List<SQLInjectionRisk> injectionRisks = securityChecker.checkSQLInjection(sql);
            result.setInjectionRisks(injectionRisks);
            
            // 2. 权限检查
            List<PrivilegeIssue> privilegeIssues = securityChecker.checkPrivileges(sql);
            result.setPrivilegeIssues(privilegeIssues);
            
            // 3. 敏感数据访问检查
            List<SensitiveDataAccess> sensitiveAccess = securityChecker.checkSensitiveDataAccess(sql);
            result.setSensitiveDataAccess(sensitiveAccess);
            
            // 4. 计算安全得分
            double securityScore = calculateSecurityScore(result);
            result.setSecurityScore(securityScore);
            
            return result;
            
        } catch (Exception e) {
            log.error("安全检查失败", e);
            return SecurityCheckResult.failed("安全检查执行失败: " + e.getMessage());
        }
    }

    /**
     * 最佳实践检查
     */
    private BestPracticeCheckResult performBestPracticeCheck(String sql, ParsedRequirement requirement) {
        log.debug("执行最佳实践检查");
        
        try {
            BestPracticeCheckResult result = new BestPracticeCheckResult();
            
            // 1. 代码风格检查
            List<CodeStyleIssue> styleIssues = bestPracticeChecker.checkCodeStyle(sql);
            result.setCodeStyleIssues(styleIssues);
            
            // 2. 性能最佳实践检查
            List<PerformanceBestPractice> perfPractices = bestPracticeChecker.checkPerformancePractices(sql);
            result.setPerformancePractices(perfPractices);
            
            // 3. 可维护性检查
            List<MaintainabilityIssue> maintainabilityIssues = bestPracticeChecker.checkMaintainability(sql);
            result.setMaintainabilityIssues(maintainabilityIssues);
            
            // 4. 可读性检查
            List<ReadabilityIssue> readabilityIssues = bestPracticeChecker.checkReadability(sql);
            result.setReadabilityIssues(readabilityIssues);
            
            // 5. 计算最佳实践得分
            double practiceScore = calculateBestPracticeScore(result);
            result.setBestPracticeScore(practiceScore);
            
            return result;
            
        } catch (Exception e) {
            log.error("最佳实践检查失败", e);
            return BestPracticeCheckResult.failed("最佳实践检查执行失败: " + e.getMessage());
        }
    }

    /**
     * 计算综合评分
     */
    private double calculateOverallScore(QualityCheckResult result) {
        // 权重配置
        double syntaxWeight = 0.25;
        double performanceWeight = 0.30;
        double businessWeight = 0.25;
        double securityWeight = 0.15;
        double practiceWeight = 0.05;
        
        double totalScore = 0.0;
        
        // 语法得分
        if (result.getSyntaxCheck() != null) {
            totalScore += result.getSyntaxCheck().getSyntaxScore() * syntaxWeight;
        }
        
        // 性能得分
        if (result.getPerformanceCheck() != null) {
            totalScore += result.getPerformanceCheck().getPerformanceScore() * performanceWeight;
        }
        
        // 业务规则得分
        if (result.getBusinessRuleCheck() != null) {
            totalScore += result.getBusinessRuleCheck().getBusinessRuleScore() * businessWeight;
        }
        
        // 安全得分
        if (result.getSecurityCheck() != null) {
            totalScore += result.getSecurityCheck().getSecurityScore() * securityWeight;
        }
        
        // 最佳实践得分
        if (result.getBestPracticeCheck() != null) {
            totalScore += result.getBestPracticeCheck().getBestPracticeScore() * practiceWeight;
        }
        
        return Math.min(totalScore, 1.0); // 确保不超过1.0
    }

    /**
     * 生成质量问题列表
     */
    private List<QualityIssue> generateQualityIssues(QualityCheckResult result) {
        List<QualityIssue> issues = new ArrayList<>();
        
        // 从各个检查结果中提取问题
        if (result.getSyntaxCheck() != null) {
            issues.addAll(extractSyntaxIssues(result.getSyntaxCheck()));
        }
        
        if (result.getPerformanceCheck() != null) {
            issues.addAll(extractPerformanceIssues(result.getPerformanceCheck()));
        }
        
        if (result.getBusinessRuleCheck() != null) {
            issues.addAll(extractBusinessRuleIssues(result.getBusinessRuleCheck()));
        }
        
        if (result.getSecurityCheck() != null) {
            issues.addAll(extractSecurityIssues(result.getSecurityCheck()));
        }
        
        if (result.getBestPracticeCheck() != null) {
            issues.addAll(extractBestPracticeIssues(result.getBestPracticeCheck()));
        }
        
        // 按严重程度排序
        issues.sort((i1, i2) -> i2.getSeverity().compareTo(i1.getSeverity()));
        
        return issues;
    }

    /**
     * 确定质量等级
     */
    private QualityLevel determineQualityLevel(double score) {
        if (score >= 0.95) return QualityLevel.EXCELLENT;
        if (score >= 0.85) return QualityLevel.GOOD;
        if (score >= 0.70) return QualityLevel.ACCEPTABLE;
        if (score >= 0.50) return QualityLevel.POOR;
        return QualityLevel.UNACCEPTABLE;
    }

    // 辅助方法的基础实现...
    private double calculateSyntaxScore(List<SyntaxError> errors, List<SyntaxWarning> warnings) {
        if (!errors.isEmpty()) return 0.0;
        return Math.max(0.0, 1.0 - warnings.size() * 0.1);
    }

    private double calculatePerformanceScore(PerformanceCheckResult result) {
        // 基于各种性能分析结果计算得分
        return 0.85; // 示例值
    }

    private double calculateBusinessRuleScore(BusinessRuleCheckResult result) {
        // 基于业务规则检查结果计算得分
        return 0.90; // 示例值
    }

    private double calculateSecurityScore(SecurityCheckResult result) {
        // 基于安全检查结果计算得分
        return 0.95; // 示例值
    }

    private double calculateBestPracticeScore(BestPracticeCheckResult result) {
        // 基于最佳实践检查结果计算得分
        return 0.80; // 示例值
    }

    private List<QualityIssue> extractSyntaxIssues(SyntaxCheckResult syntaxCheck) {
        return List.of(); // 实际实现需要从语法检查结果中提取问题
    }

    private List<QualityIssue> extractPerformanceIssues(PerformanceCheckResult performanceCheck) {
        return List.of(); // 实际实现需要从性能检查结果中提取问题
    }

    private List<QualityIssue> extractBusinessRuleIssues(BusinessRuleCheckResult businessRuleCheck) {
        return List.of(); // 实际实现需要从业务规则检查结果中提取问题
    }

    private List<QualityIssue> extractSecurityIssues(SecurityCheckResult securityCheck) {
        return List.of(); // 实际实现需要从安全检查结果中提取问题
    }

    private List<QualityIssue> extractBestPracticeIssues(BestPracticeCheckResult bestPracticeCheck) {
        return List.of(); // 实际实现需要从最佳实践检查结果中提取问题
    }

    public enum QualityLevel {
        EXCELLENT,    // 优秀 (95+)
        GOOD,         // 良好 (85-94)
        ACCEPTABLE,   // 可接受 (70-84)
        POOR,         // 较差 (50-69)
        UNACCEPTABLE  // 不可接受 (<50)
    }
}
