@echo off
:: SQL-based ER图生成工具
:: 使用方法: generate-sql-er.bat <sql-file> [domain]

setlocal

if "%1"=="" (
    echo.
    echo ========================================
    echo SQL-based ER图生成工具
    echo ========================================
    echo.
    echo 用法: %0 ^<sql-file^> [domain]
    echo.
    echo 示例:
    echo   %0 job\wrongbook\sql\wrongbook_fix_wide_table_v2.sql wrongbook
    echo.
    echo 特性:
    echo   ✓ 基于CREATE TABLE语句提取表结构
    echo   ✓ 从JOIN条件中提取准确的关联关系
    echo   ✓ 正确识别关系类型 (1:1, 1:N, M:1, M:N)
    echo   ✓ 不猜测表结构，基于真实输入
    echo   ✓ 支持BusinessEvent和JSON_VALUE解析
    echo.
    pause
    exit /b 1
)

set SQL_FILE=%1
set DOMAIN=%2
if "%DOMAIN%"=="" set DOMAIN=default

echo ========================================
echo SQL-based ER图生成工具
echo ========================================
echo SQL文件: %SQL_FILE%
echo 业务域: %DOMAIN%
echo ========================================
echo.

:: 检查SQL文件
if not exist "%SQL_FILE%" (
    echo ❌ SQL文件不存在: %SQL_FILE%
    pause
    exit /b 1
)

:: 设置JDK路径
set JAVA_HOME=C:\Program Files\Java\jdk-17.0.1
set JAVAC="%JAVA_HOME%\bin\javac.exe"
set JAVA="%JAVA_HOME%\bin\java.exe"

:: 编译
if not exist "src\SQLBasedERGenerator.class" (
    echo 🔨 编译中...
    %JAVAC% src\SQLBasedERGenerator.java
    if errorlevel 1 (
        echo ❌ 编译失败
        pause
        exit /b 1
    )
)

:: 运行
echo 📊 分析SQL结构和关联关系...
%JAVA% -cp src SQLBasedERGenerator "%SQL_FILE%" "%DOMAIN%"

if errorlevel 1 (
    echo ❌ 生成失败
    pause
    exit /b 1
)

echo.
echo 🎉 ER图生成完成！
echo.
echo 生成的文件:
dir /b er-diagram\%DOMAIN%-sql-based-*

echo.
pause
