package com.flink.ai.service;

import com.flink.ai.model.ValidationResult;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataValidationService {
    
    public ValidationResult validateSql(File sqlFile, File validationOutputDir) throws IOException {
        ValidationResult result = new ValidationResult();
        
        // 读取SQL内容
        String sqlContent = new String(Files.readAllBytes(sqlFile.toPath()));
        
        // 执行各种验证
        validateSyntax(sqlContent, result);
        validateSchema(sqlContent, result);
        validateDataQuality(sqlContent, result);
        analyzePerformance(sqlContent, result);
        
        // 生成验证报告
        generateValidationReports(result, validationOutputDir, sqlFile.getName());
        
        return result;
    }
    
    private void validateSyntax(String sqlContent, ValidationResult result) {
        List<String> syntaxErrors = new ArrayList<>();
        List<String> syntaxWarnings = new ArrayList<>();
        
        // 基本SQL语法检查
        if (!sqlContent.trim().toUpperCase().startsWith("INSERT INTO")) {
            syntaxErrors.add("SQL应该以INSERT INTO开始");
        }
        
        if (!sqlContent.trim().endsWith(";")) {
            syntaxWarnings.add("SQL语句建议以分号结尾");
        }
        
        // 检查必要的关键字
        if (!sqlContent.toUpperCase().contains("SELECT")) {
            syntaxErrors.add("缺少SELECT关键字");
        }
        
        if (!sqlContent.toUpperCase().contains("FROM")) {
            syntaxErrors.add("缺少FROM关键字");
        }
        
        // Flink特定语法检查
        if (sqlContent.contains("FOR SYSTEM_TIME AS OF PROCTIME()")) {
            // 检查维表时态JOIN语法
            if (!sqlContent.matches(".*LEFT JOIN\\s+\\w+\\s+FOR SYSTEM_TIME AS OF PROCTIME\\(\\).*")) {
                syntaxWarnings.add("时态JOIN语法建议使用LEFT JOIN");
            }
        }
        
        result.setSyntaxChecksPassed(syntaxErrors.isEmpty());
        result.getErrors().addAll(syntaxErrors);
        result.getWarnings().addAll(syntaxWarnings);
    }
    
    private void validateSchema(String sqlContent, ValidationResult result) {
        List<String> schemaErrors = new ArrayList<>();
        List<String> schemaWarnings = new ArrayList<>();
        
        // 检查表名格式
        Pattern tablePattern = Pattern.compile("FROM\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
        Matcher tableMatcher = tablePattern.matcher(sqlContent);
        
        while (tableMatcher.find()) {
            String tableName = tableMatcher.group(1);
            if (!tableName.matches("^[a-zA-Z][a-zA-Z0-9_]*$")) {
                schemaErrors.add("表名格式不规范: " + tableName);
            }
        }
        
        // 检查字段映射
        Pattern selectPattern = Pattern.compile("SELECT(.*?)FROM", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher selectMatcher = selectPattern.matcher(sqlContent);
        
        if (selectMatcher.find()) {
            String selectClause = selectMatcher.group(1);
            
            // 检查payload字段访问
            if (selectClause.contains("payload.") && !selectClause.contains("JSON_VALUE")) {
                schemaWarnings.add("payload字段访问建议使用JSON_VALUE函数");
            }
            
            // 检查类型转换
            if (!selectClause.contains("CAST(") && selectClause.contains("AS BIGINT")) {
                schemaWarnings.add("数据类型转换建议使用CAST函数");
            }
        }
        
        result.setSchemaValidationPassed(schemaErrors.isEmpty());
        result.getErrors().addAll(schemaErrors);
        result.getWarnings().addAll(schemaWarnings);
    }
    
    private void validateDataQuality(String sqlContent, ValidationResult result) {
        int totalChecks = 0;
        int passedChecks = 0;
        List<String> qualityWarnings = new ArrayList<>();
        
        // 检查1: 空值处理
        totalChecks++;
        if (sqlContent.contains("CAST(NULL AS STRING)") || sqlContent.contains("COALESCE")) {
            passedChecks++;
        } else {
            qualityWarnings.add("建议对可能为空的字段进行空值处理");
        }
        
        // 检查2: 时间字段处理
        totalChecks++;
        if (sqlContent.contains("TO_TIMESTAMP_LTZ")) {
            passedChecks++;
        } else {
            qualityWarnings.add("时间字段建议使用TO_TIMESTAMP_LTZ进行时区处理");
        }
        
        // 检查3: WHERE条件存在
        totalChecks++;
        if (sqlContent.toUpperCase().contains("WHERE")) {
            passedChecks++;
        } else {
            qualityWarnings.add("建议添加WHERE条件进行数据过滤");
        }
        
        // 检查4: 删除标识过滤
        totalChecks++;
        if (sqlContent.contains("is_delete = 0") || sqlContent.contains("is_delete = false")) {
            passedChecks++;
        } else {
            qualityWarnings.add("建议过滤已删除的记录");
        }
        
        // 检查5: 业务规则验证
        totalChecks++;
        if (sqlContent.contains("CASE WHEN")) {
            passedChecks++;
        } else {
            qualityWarnings.add("建议添加业务规则验证逻辑");
        }
        
        result.setTotalDataQualityChecks(totalChecks);
        result.setDataQualityChecksPassed(passedChecks);
        result.getWarnings().addAll(qualityWarnings);
    }
    
    private void analyzePerformance(String sqlContent, ValidationResult result) {
        int performanceScore = 100;
        List<String> performanceWarnings = new ArrayList<>();
        
        // 性能分析1: JOIN数量
        int joinCount = countOccurrences(sqlContent.toUpperCase(), "JOIN");
        if (joinCount > 5) {
            performanceScore -= 15;
            performanceWarnings.add("JOIN数量过多(" + joinCount + ")，可能影响性能");
        } else if (joinCount > 3) {
            performanceScore -= 5;
            performanceWarnings.add("JOIN数量较多(" + joinCount + ")，注意性能监控");
        }
        
        // 性能分析2: 子查询
        int subqueryCount = countOccurrences(sqlContent.toUpperCase(), "SELECT");
        if (subqueryCount > 1) {
            performanceScore -= 10;
            performanceWarnings.add("存在子查询，建议优化为JOIN");
        }
        
        // 性能分析3: 函数使用
        if (sqlContent.contains("JSON_VALUE")) {
            performanceScore -= 5;
            performanceWarnings.add("JSON解析可能影响性能，考虑预处理");
        }
        
        // 性能分析4: CASE WHEN数量
        int caseCount = countOccurrences(sqlContent.toUpperCase(), "CASE WHEN");
        if (caseCount > 3) {
            performanceScore -= 10;
            performanceWarnings.add("CASE WHEN语句过多，考虑使用映射表");
        }
        
        // 性能分析5: 字符串函数
        if (sqlContent.contains("CONCAT") || sqlContent.contains("SUBSTRING")) {
            performanceScore -= 5;
            performanceWarnings.add("字符串处理函数可能影响性能");
        }
        
        result.setPerformanceScore(Math.max(0, performanceScore));
        result.getWarnings().addAll(performanceWarnings);
    }
    
    private void generateValidationReports(ValidationResult result, File outputDir, String sqlFileName) throws IOException {
        outputDir.mkdirs();
        
        // 生成HTML报告
        generateHtmlReport(result, outputDir, sqlFileName);
        
        // 生成JSON摘要
        generateJsonSummary(result, outputDir);
        
        // 生成Markdown性能分析
        generatePerformanceAnalysis(result, outputDir);
    }
    
    private void generateHtmlReport(ValidationResult result, File outputDir, String sqlFileName) throws IOException {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"zh-CN\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>Flink SQL验证报告</title>\n");
        html.append("    <style>\n");
        html.append("        body { font-family: 'Microsoft YaHei', sans-serif; margin: 20px; }\n");
        html.append("        .header { background: #f8f9fa; padding: 20px; border-radius: 5px; margin-bottom: 20px; }\n");
        html.append("        .score { font-size: 24px; font-weight: bold; color: ").append(getScoreColor(result.getPerformanceScore())).append("; }\n");
        html.append("        .section { margin: 20px 0; padding: 15px; border: 1px solid #ddd; border-radius: 5px; }\n");
        html.append("        .pass { color: #28a745; }\n");
        html.append("        .fail { color: #dc3545; }\n");
        html.append("        .warn { color: #ffc107; }\n");
        html.append("        ul { list-style-type: none; padding: 0; }\n");
        html.append("        li { padding: 5px 0; border-bottom: 1px solid #eee; }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        
        // 报告头部
        html.append("    <div class=\"header\">\n");
        html.append("        <h1>Flink SQL验证报告</h1>\n");
        html.append("        <p><strong>文件:</strong> ").append(sqlFileName).append("</p>\n");
        html.append("        <p><strong>生成时间:</strong> ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("</p>\n");
        html.append("        <p class=\"score\">性能评分: ").append(result.getPerformanceScore()).append("/100</p>\n");
        html.append("    </div>\n");
        
        // 验证摘要
        html.append("    <div class=\"section\">\n");
        html.append("        <h2>验证摘要</h2>\n");
        html.append("        <table border=\"1\" style=\"width:100%; border-collapse: collapse;\">\n");
        html.append("            <tr><th>验证项</th><th>结果</th></tr>\n");
        html.append("            <tr><td>语法检查</td><td class=\"").append(result.getSyntaxChecksPassed() ? "pass\">✅ 通过" : "fail\">❌ 失败").append("</td></tr>\n");
        html.append("            <tr><td>结构验证</td><td class=\"").append(result.getSchemaValidationPassed() ? "pass\">✅ 通过" : "fail\">❌ 失败").append("</td></tr>\n");
        html.append("            <tr><td>数据质量</td><td>").append(result.getDataQualityChecksPassed()).append("/").append(result.getTotalDataQualityChecks()).append(" 通过</td></tr>\n");
        html.append("        </table>\n");
        html.append("    </div>\n");
        
        // 错误信息
        if (!result.getErrors().isEmpty()) {
            html.append("    <div class=\"section\">\n");
            html.append("        <h2 class=\"fail\">❌ 错误信息</h2>\n");
            html.append("        <ul>\n");
            for (String error : result.getErrors()) {
                html.append("            <li class=\"fail\">").append(error).append("</li>\n");
            }
            html.append("        </ul>\n");
            html.append("    </div>\n");
        }
        
        // 警告信息
        if (!result.getWarnings().isEmpty()) {
            html.append("    <div class=\"section\">\n");
            html.append("        <h2 class=\"warn\">⚠️ 警告信息</h2>\n");
            html.append("        <ul>\n");
            for (String warning : result.getWarnings()) {
                html.append("            <li class=\"warn\">").append(warning).append("</li>\n");
            }
            html.append("        </ul>\n");
            html.append("    </div>\n");
        }
        
        html.append("</body>\n");
        html.append("</html>\n");
        
        File htmlFile = new File(outputDir, "validation-report.html");
        Files.write(htmlFile.toPath(), html.toString().getBytes());
    }
    
    private void generateJsonSummary(ValidationResult result, File outputDir) throws IOException {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"timestamp\": \"").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\",\n");
        json.append("  \"syntaxChecksPassed\": ").append(result.getSyntaxChecksPassed()).append(",\n");
        json.append("  \"schemaValidationPassed\": ").append(result.getSchemaValidationPassed()).append(",\n");
        json.append("  \"dataQualityChecksPassed\": ").append(result.getDataQualityChecksPassed()).append(",\n");
        json.append("  \"totalDataQualityChecks\": ").append(result.getTotalDataQualityChecks()).append(",\n");
        json.append("  \"performanceScore\": ").append(result.getPerformanceScore()).append(",\n");
        json.append("  \"errorCount\": ").append(result.getErrors().size()).append(",\n");
        json.append("  \"warningCount\": ").append(result.getWarnings().size()).append("\n");
        json.append("}\n");
        
        File jsonFile = new File(outputDir, "validation-summary.json");
        Files.write(jsonFile.toPath(), json.toString().getBytes());
    }
    
    private void generatePerformanceAnalysis(ValidationResult result, File outputDir) throws IOException {
        StringBuilder md = new StringBuilder();
        md.append("# Flink SQL性能分析报告\n\n");
        md.append("生成时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n\n");
        md.append("## 性能评分\n\n");
        md.append("**总分: ").append(result.getPerformanceScore()).append("/100**\n\n");
        
        String scoreLevel;
        String scoreDescription;
        if (result.getPerformanceScore() >= 90) {
            scoreLevel = "优秀";
            scoreDescription = "SQL性能表现优秀，可以直接投入生产使用";
        } else if (result.getPerformanceScore() >= 70) {
            scoreLevel = "良好";
            scoreDescription = "SQL性能表现良好，建议关注警告信息进行优化";
        } else if (result.getPerformanceScore() >= 50) {
            scoreLevel = "一般";
            scoreDescription = "SQL性能有待提升，建议根据建议进行优化";
        } else {
            scoreLevel = "较差";
            scoreDescription = "SQL存在明显性能问题，强烈建议进行优化";
        }
        
        md.append("**评级: ").append(scoreLevel).append("**\n\n");
        md.append("**说明: ").append(scoreDescription).append("**\n\n");
        
        md.append("## 优化建议\n\n");
        md.append("1. **索引优化**: 确保JOIN字段上有适当的索引\n");
        md.append("2. **数据分区**: 考虑按时间或业务维度进行分区\n");
        md.append("3. **缓存策略**: 合理配置维表缓存TTL\n");
        md.append("4. **并行度调优**: 根据数据量调整作业并行度\n");
        md.append("5. **资源配置**: 监控内存和CPU使用情况\n\n");
        
        md.append("## 监控指标\n\n");
        md.append("- **吞吐量**: records/second\n");
        md.append("- **延迟**: 处理延迟 < 5秒(P95)\n");
        md.append("- **背压**: 无持续背压\n");
        md.append("- **检查点**: 检查点时间 < 30秒\n");
        md.append("- **故障恢复**: 故障恢复时间 < 2分钟\n");
        
        File mdFile = new File(outputDir, "performance-analysis.md");
        Files.write(mdFile.toPath(), md.toString().getBytes());
    }
    
    private String getScoreColor(int score) {
        if (score >= 90) return "#28a745";
        if (score >= 70) return "#ffc107";
        return "#dc3545";
    }
    
    private int countOccurrences(String text, String substring) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }
}
