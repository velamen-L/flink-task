@echo off
:: FlinkSQL ER图生成工具 - 便捷脚本
:: 使用方法: generate-er.bat <sql-file> [domain]

setlocal

if "%1"=="" (
    echo.
    echo ========================================
    echo FlinkSQL ER图生成工具
    echo ========================================
    echo.
    echo 用法: %0 ^<sql-file^> [domain]
    echo.
    echo 示例:
    echo   %0 job\wrongbook\sql\wrongbook_fix_wide_table_v2.sql wrongbook
    echo   %0 job\user-daily-stats\sql\user_stats.sql user
    echo.
    echo 功能:
    echo   ✓ 从SQL文件提取表关联关系
    echo   ✓ 生成Mermaid ER图
    echo   ✓ 生成PlantUML ER图  
    echo   ✓ 生成详细报告
    echo   ✓ 检测表关联冲突
    echo   ✓ 汇总到企业知识库
    echo.
    pause
    exit /b 1
)

set SQL_FILE=%1
set DOMAIN=%2
if "%DOMAIN%"=="" set DOMAIN=default

echo ========================================
echo FlinkSQL ER图生成工具
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
echo 📊 正在分析SQL文件...
echo.

:: 设置JDK路径
set JAVA_HOME=C:\Program Files\Java\jdk-17.0.1
set JAVAC="%JAVA_HOME%\bin\javac.exe"
set JAVA="%JAVA_HOME%\bin\java.exe"

:: 编译Java文件（如果需要）
if not exist "src\SimpleERGenerator.class" (
    echo 🔨 编译ER图生成器...
    %JAVAC% src\SimpleERGenerator.java
    if errorlevel 1 (
        echo ❌ 编译失败
        pause
        exit /b 1
    )
    echo ✅ 编译成功
    echo.
)

:: 运行ER图生成器
echo 🚀 生成ER图...
%JAVA% -cp src SimpleERGenerator "%SQL_FILE%" "%DOMAIN%"

if errorlevel 1 (
    echo.
    echo ❌ ER图生成失败
    pause
    exit /b 1
)

echo.
echo ========================================
echo 🎉 ER图生成完成！
echo ========================================
echo.

:: 显示生成的文件
echo 📁 生成的文件:
if exist "er-diagram\%DOMAIN%-er-diagram.mermaid" (
    echo   ✓ %DOMAIN%-er-diagram.mermaid - Mermaid ER图
)
if exist "er-diagram\%DOMAIN%-er-diagram.puml" (
    echo   ✓ %DOMAIN%-er-diagram.puml - PlantUML ER图
)
if exist "er-diagram\%DOMAIN%-er-report.md" (
    echo   ✓ %DOMAIN%-er-report.md - 详细报告
)

echo.
echo 📊 查看报告内容:
if exist "er-diagram\%DOMAIN%-er-report.md" (
    echo ----------------------------------------
    type "er-diagram\%DOMAIN%-er-report.md"
    echo ----------------------------------------
)

echo.
echo 💡 提示:
echo   • 使用支持Mermaid的编辑器查看 .mermaid 文件
echo   • 使用PlantUML工具查看 .puml 文件
echo   • 查看 .md 报告了解详细信息
echo.

set /p OPEN_DIR="是否打开输出目录？ (y/n): "
if /i "%OPEN_DIR%"=="y" explorer er-diagram

echo.
echo 🔄 要生成其他SQL的ER图吗？
echo   示例: %0 job\another\sql\file.sql another_domain
echo.

pause
