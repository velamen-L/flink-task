@echo off
echo ========================================
echo Testing Flink AI Gradle Plugin
echo ========================================

echo.
echo 1. Checking Gradle tasks...
call gradle tasks --group flink-ai

echo.
echo 2. Testing Flink SQL generation...
call gradle generateFlinkSql

echo.
echo 3. Testing ER knowledge base update...
call gradle updateERKnowledgeBase

echo.
echo 4. Testing data validation...
call gradle validateFlinkSqlData

echo.
echo 5. Testing complete workflow...
call gradle flinkAiWorkflow

echo.
echo ========================================
echo Test completed! Check the output in:
echo - build/flink-ai-output/
echo - er-knowledge-base/
echo ========================================
pause
