@echo off
:: Flink SQL ER图生成工具 - 专业版脚本
:: 使用方法: generate-flink-er.bat <sql-file> [domain]

setlocal

if "%1"=="" (
    echo.
    echo ========================================
    echo Flink SQL ER图生成工具 - 专业版
    echo ========================================
    echo.
    echo 用法: %0 ^<sql-file^> [domain]
    echo.
    echo 示例:
    echo   %0 job\wrongbook\sql\wrongbook_fix_wide_table_v2.sql wrongbook
    echo   %0 job\user-stats\sql\user_daily_stats.sql user
    echo.
    echo 功能特性:
    echo   ✓ 专门针对Flink SQL优化
    echo   ✓ 正确识别BusinessEvent结构
    echo   ✓ 准确提取JOIN链式关联
    echo   ✓ 区分源表/维表/结果表类型
    echo   ✓ 提取payload字段映射
    echo   ✓ 生成详细的关联关系
    echo   ✓ 支持Mermaid和PlantUML格式
    echo.
    pause
    exit /b 1
)

set SQL_FILE=%1
set DOMAIN=%2
if "%DOMAIN%"=="" set DOMAIN=default

echo ========================================
echo Flink SQL ER图生成工具 - 专业版
echo ========================================
echo SQL文件: %SQL_FILE%
echo 业务域: %DOMAIN%
echo 时间: %date% %time%
echo ========================================
echo.

:: 检查SQL文件是否存在
if not exist "%SQL_FILE%" (
    echo ❌ SQL文件不存在: %SQL_FILE%
    echo.
    pause
    exit /b 1
)

:: 使用JDK 17编译和运行
echo 📊 正在分析Flink SQL文件...
echo.

:: 设置JDK路径
set JAVA_HOME=C:\Program Files\Java\jdk-17.0.1
set JAVAC="%JAVA_HOME%\bin\javac.exe"
set JAVA="%JAVA_HOME%\bin\java.exe"

:: 编译Java文件（如果需要）
if not exist "src\FlinkERGenerator.class" (
    echo 🔨 编译Flink ER图生成器...
    %JAVAC% src\FlinkERGenerator.java
    if errorlevel 1 (
        echo ❌ 编译失败
        pause
        exit /b 1
    )
    echo ✅ 编译成功
    echo.
)

:: 运行Flink ER图生成器
echo 🚀 生成Flink SQL ER图...
%JAVA% -cp src FlinkERGenerator "%SQL_FILE%" "%DOMAIN%"

if errorlevel 1 (
    echo.
    echo ❌ ER图生成失败
    pause
    exit /b 1
)

echo.
echo ========================================
echo 🎉 Flink SQL ER图生成完成！
echo ========================================
echo.

:: 显示生成的文件
echo 📁 生成的文件:
if exist "er-diagram\%DOMAIN%-flink-er-diagram.mermaid" (
    echo   ✓ %DOMAIN%-flink-er-diagram.mermaid - Mermaid ER图 (Flink专用)
)
if exist "er-diagram\%DOMAIN%-flink-er-diagram.puml" (
    echo   ✓ %DOMAIN%-flink-er-diagram.puml - PlantUML ER图 (Flink专用)
)
if exist "er-diagram\%DOMAIN%-flink-er-report.md" (
    echo   ✓ %DOMAIN%-flink-er-report.md - 详细分析报告
)

echo.
echo 📊 生成结果概览:
if exist "er-diagram\%DOMAIN%-flink-er-report.md" (
    echo ----------------------------------------
    findstr /C:"Total Tables" /C:"Relations" /C:"Source Tables" /C:"Dimension Tables" /C:"Result Tables" "er-diagram\%DOMAIN%-flink-er-report.md"
    echo ----------------------------------------
)

echo.
echo 💡 Flink SQL ER图特性:
echo   • 源表 (SOURCE): BusinessEvent标准结构
echo   • 维表 (DIMENSION): 维度查询表，支持FOR SYSTEM_TIME AS OF PROCTIME()
echo   • 结果表 (RESULT): 宽表输出，聚合多表数据
echo   • 关联关系: 准确提取JOIN链和payload字段映射
echo.

set /p OPEN_DIR="是否打开输出目录？ (y/n): "
if /i "%OPEN_DIR%"=="y" explorer er-diagram

echo.
echo 🔄 要生成其他Flink SQL的ER图吗？
echo   示例: %0 job\another\sql\file.sql another_domain
echo.

pause
