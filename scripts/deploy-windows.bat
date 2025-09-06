@echo off
:: FlinkSQL AI Coding Platform Windows部署脚本
:: 适用于Windows环境的实际部署

echo ========================================
echo FlinkSQL AI Coding Platform 部署工具
echo ========================================
echo.

:: 检查Java环境
echo [1/5] 检查Java环境...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Java未安装或未配置
    echo 请安装JDK 8+: https://adoptium.net/
    pause
    exit /b 1
) else (
    echo ✅ Java环境正常
    java -version
)

echo.

:: 检查Maven（可选）
echo [2/5] 检查Maven环境...
mvn --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ⚠️ Maven未安装，将跳过构建步骤
    set SKIP_BUILD=true
) else (
    echo ✅ Maven环境正常
    set SKIP_BUILD=false
)

echo.

:: 构建项目
echo [3/5] 构建项目...
if "%SKIP_BUILD%"=="false" (
    echo 正在使用Maven构建...
    call mvn clean package -DskipTests=true
    if %errorlevel% neq 0 (
        echo ❌ Maven构建失败
        pause
        exit /b 1
    )
    echo ✅ Maven构建成功
) else (
    echo 检查是否有已构建的JAR文件...
    if exist "target\*.jar" (
        echo ✅ 找到已构建的JAR文件
    ) else (
        echo ❌ 未找到JAR文件且Maven未安装
        echo 请先安装Maven或提供已构建的JAR文件
        pause
        exit /b 1
    )
)

echo.

:: 创建配置文件
echo [4/5] 创建配置文件...
if not exist "config" mkdir config

:: 创建application.yml
echo # FlinkSQL AI Coding Platform 配置 > config\application.yml
echo server: >> config\application.yml
echo   port: 8080 >> config\application.yml
echo   servlet: >> config\application.yml
echo     context-path: /ai-coding >> config\application.yml
echo. >> config\application.yml
echo spring: >> config\application.yml
echo   application: >> config\application.yml
echo     name: flinksql-ai-coding-platform >> config\application.yml
echo   profiles: >> config\application.yml
echo     active: standalone >> config\application.yml
echo. >> config\application.yml
echo   # H2内存数据库（无需外部数据库） >> config\application.yml
echo   datasource: >> config\application.yml
echo     url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1 >> config\application.yml
echo     driverClassName: org.h2.Driver >> config\application.yml
echo     username: sa >> config\application.yml
echo     password: password >> config\application.yml
echo. >> config\application.yml
echo   h2: >> config\application.yml
echo     console: >> config\application.yml
echo       enabled: true >> config\application.yml
echo       path: /h2-console >> config\application.yml
echo. >> config\application.yml
echo   jpa: >> config\application.yml
echo     database-platform: org.hibernate.dialect.H2Dialect >> config\application.yml
echo     hibernate: >> config\application.yml
echo       ddl-auto: create-drop >> config\application.yml
echo     show-sql: false >> config\application.yml
echo. >> config\application.yml
echo # AI平台配置 >> config\application.yml
echo ai: >> config\application.yml
echo   platform: >> config\application.yml
echo     enabled: true >> config\application.yml
echo     debug-mode: true >> config\application.yml
echo   llm: >> config\application.yml
echo     default-model: gpt-4 >> config\application.yml
echo     api-key: ${OPENAI_API_KEY:demo-key} >> config\application.yml
echo. >> config\application.yml
echo # 管理端点 >> config\application.yml
echo management: >> config\application.yml
echo   endpoints: >> config\application.yml
echo     web: >> config\application.yml
echo       exposure: >> config\application.yml
echo         include: health,info,metrics >> config\application.yml

echo ✅ 配置文件已创建

echo.

:: 启动应用
echo [5/5] 启动AI Coding Platform...
echo.

:: 设置环境变量
if not defined OPENAI_API_KEY (
    echo ⚠️ 未设置OPENAI_API_KEY环境变量
    echo 将使用演示模式
    set OPENAI_API_KEY=demo-key
)

:: 查找JAR文件
for %%f in (target\*.jar) do set JAR_FILE=%%f

if not defined JAR_FILE (
    echo ❌ 未找到JAR文件
    pause
    exit /b 1
)

echo 使用JAR文件: %JAR_FILE%
echo.
echo ========================================
echo   启动信息
echo ========================================
echo 应用端口: 8080
echo 访问地址: http://localhost:8080/ai-coding
echo 健康检查: http://localhost:8080/ai-coding/actuator/health
echo H2控制台: http://localhost:8080/ai-coding/h2-console
echo ========================================
echo.
echo 正在启动应用...
echo 按 Ctrl+C 停止应用
echo.

:: 启动Spring Boot应用
java -jar "%JAR_FILE%" --spring.config.location=file:config/application.yml

pause
