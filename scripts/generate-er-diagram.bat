@echo off
:: FlinkSQL ER图生成工具
:: 使用方法: generate-er-diagram.bat <sql-file> [domain] [output-dir]

echo ========================================
echo FlinkSQL ER图生成工具
echo ========================================

if "%1"=="" (
    echo 用法: %0 ^<sql-file^> [domain] [output-dir]
    echo 示例: %0 job\wrongbook\sql\wrongbook_fix_wide_table_v2.sql wrongbook er-output
    pause
    exit /b 1
)

set SQL_FILE=%1
set DOMAIN=%2
set OUTPUT_DIR=%3

if "%DOMAIN%"=="" set DOMAIN=default
if "%OUTPUT_DIR%"=="" set OUTPUT_DIR=er-diagram

echo SQL文件: %SQL_FILE%
echo 业务域: %DOMAIN%
echo 输出目录: %OUTPUT_DIR%
echo.

:: 检查Java
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Java未安装
    pause
    exit /b 1
)

:: 检查SQL文件是否存在
if not exist "%SQL_FILE%" (
    echo ❌ SQL文件不存在: %SQL_FILE%
    pause
    exit /b 1
)

:: 创建输出目录
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

echo 📊 开始生成ER图...

:: 编译Java文件
echo 编译Java代码...
javac -d . src\ERDiagramGenerator.java
if %errorlevel% neq 0 (
    echo ❌ Java编译失败
    pause
    exit /b 1
)

:: 运行ER图生成器
echo 运行ER图生成器...
java ERDiagramGenerator "%SQL_FILE%" --domain "%DOMAIN%" --output "%OUTPUT_DIR%"

if %errorlevel% equ 0 (
    echo.
    echo ✅ ER图生成完成！
    echo 📁 输出目录: %OUTPUT_DIR%
    echo.
    echo 生成的文件:
    dir "%OUTPUT_DIR%" /B
    echo.
    echo 是否打开输出目录？ (y/n)
    set /p OPEN_DIR=
    if /i "%OPEN_DIR%"=="y" explorer "%OUTPUT_DIR%"
) else (
    echo ❌ ER图生成失败
)

pause
