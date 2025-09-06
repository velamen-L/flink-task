# FlinkSQL AI Coding Platform 本地开发模式
# 无需Docker，直接使用Java运行
param(
    [string]$Profile = "dev"
)

function Write-ColorOutput($ForegroundColor) {
    $fc = $host.UI.RawUI.ForegroundColor
    $host.UI.RawUI.ForegroundColor = $ForegroundColor
    if ($args) {
        Write-Output $args
    }
    else {
        $input | Write-Output
    }
    $host.UI.RawUI.ForegroundColor = $fc
}

function Log-Info($message) {
    Write-ColorOutput Green "[INFO] $message"
}

function Log-Warn($message) {
    Write-ColorOutput Yellow "[WARN] $message"
}

function Log-Error($message) {
    Write-ColorOutput Red "[ERROR] $message"
}

# 检查Java环境
function Check-JavaEnvironment {
    Log-Info "检查Java环境..."
    
    try {
        $javaVersion = java -version 2>&1 | Select-String "version"
        if ($javaVersion) {
            Log-Info "✓ Java已安装: $($javaVersion.Line)"
            
            # 检查Java版本是否为17+
            $versionNumber = [regex]::Match($javaVersion.Line, '"(\d+)').Groups[1].Value
            if ([int]$versionNumber -ge 17) {
                Log-Info "✓ Java版本满足要求 (17+)"
                return $true
            } else {
                Log-Warn "Java版本过低，推荐使用JDK 17+"
                return $true  # 仍然尝试运行
            }
        }
    } catch {
        Log-Error "Java未安装或配置不正确"
        Log-Info "请安装JDK 17+: https://adoptium.net/"
        return $false
    }
}

# 检查Maven环境
function Check-MavenEnvironment {
    Log-Info "检查Maven环境..."
    
    try {
        $mvnVersion = mvn --version 2>$null
        if ($LASTEXITCODE -eq 0) {
            Log-Info "✓ Maven已安装"
            return $true
        }
    } catch {
        Log-Warn "Maven未安装，将跳过构建步骤"
        return $false
    }
}

# 构建项目
function Build-Project {
    Log-Info "构建项目..."
    
    if (Check-MavenEnvironment) {
        try {
            mvn clean package -DskipTests=true
            if ($LASTEXITCODE -eq 0) {
                Log-Info "✓ 项目构建成功"
                return $true
            } else {
                Log-Error "项目构建失败"
                return $false
            }
        } catch {
            Log-Error "Maven构建过程中出现异常"
            return $false
        }
    } else {
        # 检查是否有已构建的JAR文件
        $jarFiles = Get-ChildItem -Path "target" -Filter "*.jar" -ErrorAction SilentlyContinue
        if ($jarFiles -and $jarFiles.Count -gt 0) {
            Log-Info "✓ 找到已构建的JAR文件: $($jarFiles[0].Name)"
            return $true
        } else {
            Log-Error "未找到JAR文件，请先安装Maven并构建项目"
            return $false
        }
    }
}

# 创建本地配置文件
function Create-LocalConfig {
    Log-Info "创建本地配置文件..."
    
    # 创建application-dev.yml
    $configContent = @"
server:
  port: 8080
  servlet:
    context-path: /ai-coding

spring:
  application:
    name: flinksql-ai-coding-platform
  profiles:
    active: dev
  
  # H2内存数据库配置（无需外部数据库）
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driverClassName: org.h2.Driver
    username: sa
    password: password
  
  # H2控制台（用于查看数据库）
  h2:
    console:
      enabled: true
      path: /h2-console
  
  # JPA配置
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    
  # Redis配置（模拟模式，无需真实Redis）
  redis:
    host: localhost
    port: 6379
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 8
        max-wait: -1ms

# AI平台配置
ai:
  platform:
    enabled: true
    debug-mode: true
  llm:
    default-model: gpt-4
    temperature: 0.1
    api-key: \${OPENAI_API_KEY:demo-key}
  quality-check:
    enabled: true
  knowledge-base:
    auto-update: false
    
# 日志配置
logging:
  level:
    com.flink.business.ai: INFO
    org.springframework: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"

# 管理端点
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always
"@

    $configDir = "src\main\resources"
    if (-not (Test-Path $configDir)) {
        New-Item -Path $configDir -ItemType Directory -Force | Out-Null
    }
    
    $configFile = "$configDir\application-dev.yml"
    $configContent | Out-File -FilePath $configFile -Encoding UTF8
    Log-Info "✓ 配置文件已创建: $configFile"
}

# 启动应用
function Start-Application {
    Log-Info "启动AI Coding Platform..."
    
    # 设置环境变量
    $env:SPRING_PROFILES_ACTIVE = $Profile
    if ($env:OPENAI_API_KEY) {
        Log-Info "✓ 检测到OPENAI_API_KEY环境变量"
    } else {
        Log-Warn "未设置OPENAI_API_KEY，将使用演示模式"
        $env:OPENAI_API_KEY = "demo-key"
    }
    
    # 查找JAR文件
    $jarFiles = Get-ChildItem -Path "target" -Filter "*.jar" -ErrorAction SilentlyContinue
    if (-not $jarFiles -or $jarFiles.Count -eq 0) {
        Log-Error "未找到JAR文件，请先构建项目"
        return $false
    }
    
    $jarFile = $jarFiles[0].FullName
    Log-Info "使用JAR文件: $($jarFiles[0].Name)"
    
    # 启动应用
    Log-Info "启动Spring Boot应用..."
    Write-Host ""
    Write-Host "======================================" -ForegroundColor Cyan
    Write-Host "  AI Coding Platform 正在启动..." -ForegroundColor Cyan  
    Write-Host "======================================" -ForegroundColor Cyan
    Write-Host ""
    
    try {
        java -jar $jarFile --spring.profiles.active=$Profile
    } catch {
        Log-Error "应用启动失败: $_"
        return $false
    }
}

# 显示访问信息
function Show-AccessInfo {
    Write-Host ""
    Write-Host "======================================" -ForegroundColor Green
    Write-Host "  AI Coding Platform 启动成功！" -ForegroundColor Green
    Write-Host "======================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "🌐 访问地址:" -ForegroundColor Yellow
    Write-Host "  主应用: http://localhost:8080/ai-coding" -ForegroundColor White
    Write-Host "  健康检查: http://localhost:8080/ai-coding/actuator/health" -ForegroundColor White
    Write-Host "  H2数据库控制台: http://localhost:8080/ai-coding/h2-console" -ForegroundColor White
    Write-Host ""
    Write-Host "📋 数据库连接信息:" -ForegroundColor Yellow
    Write-Host "  JDBC URL: jdbc:h2:mem:testdb" -ForegroundColor White
    Write-Host "  用户名: sa" -ForegroundColor White
    Write-Host "  密码: password" -ForegroundColor White
    Write-Host ""
    Write-Host "💡 使用提示:" -ForegroundColor Yellow
    Write-Host "  - 这是开发模式，使用内存数据库" -ForegroundColor White
    Write-Host "  - 重启后数据会丢失" -ForegroundColor White
    Write-Host "  - 按 Ctrl+C 停止应用" -ForegroundColor White
    Write-Host ""
}

# 主函数
function Main {
    Write-Host "======================================"
    Write-Host "FlinkSQL AI Coding Platform 本地启动"
    Write-Host "======================================"
    Write-Host "配置: $Profile"
    Write-Host "======================================"
    
    # 检查环境
    if (-not (Check-JavaEnvironment)) {
        return
    }
    
    # 创建配置
    Create-LocalConfig
    
    # 构建项目
    if (-not (Build-Project)) {
        return
    }
    
    # 显示启动信息
    Show-AccessInfo
    
    # 启动应用
    Start-Application
}

# 执行主函数
Main
