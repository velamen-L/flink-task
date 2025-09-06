# FlinkSQL AI Coding Platform Windows部署脚本
# PowerShell版本
# 版本: v1.0.0

param(
    [string]$Environment = "dev",
    [string]$Action = "deploy", 
    [string]$Version = "latest"
)

# 颜色定义
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

function Log-Debug($message) {
    Write-ColorOutput Blue "[DEBUG] $message"
}

# 配置变量
$PROJECT_NAME = "flinksql-ai-coding-platform"
$DOCKER_REGISTRY = "your-registry.com"
$NAMESPACE = "ai-coding"

# 检查环境
function Check-Environment {
    Log-Info "检查Windows部署环境..."
    
    # 检查PowerShell版本
    if ($PSVersionTable.PSVersion.Major -lt 5) {
        Log-Error "PowerShell版本过低，需要5.0+版本"
        exit 1
    }
    
    # 检查Docker Desktop
    try {
        $dockerVersion = docker --version 2>$null
        if ($LASTEXITCODE -eq 0) {
            Log-Info "✓ Docker已安装: $dockerVersion"
        } else {
            throw "Docker未安装"
        }
    } catch {
        Log-Error "Docker未安装或未启动，请安装Docker Desktop for Windows"
        Log-Info "下载地址: https://www.docker.com/products/docker-desktop"
        exit 1
    }
    
    # 检查Docker Desktop是否运行
    try {
        docker ps >$null 2>&1
        if ($LASTEXITCODE -eq 0) {
            Log-Info "✓ Docker Desktop正在运行"
        } else {
            throw "Docker未运行"
        }
    } catch {
        Log-Error "Docker Desktop未运行，请启动Docker Desktop"
        exit 1
    }
    
    # 检查Git
    try {
        $gitVersion = git --version 2>$null
        if ($LASTEXITCODE -eq 0) {
            Log-Info "✓ Git已安装: $gitVersion"
        }
    } catch {
        Log-Warn "Git未安装，建议安装Git for Windows"
    }
    
    # 检查Java (可选)
    try {
        $javaVersion = java -version 2>&1 | Select-String "version"
        if ($javaVersion) {
            Log-Info "✓ Java已安装: $($javaVersion.Line)"
        }
    } catch {
        Log-Warn "Java未安装，如需本地开发请安装JDK 17+"
    }
    
    # 检查环境变量
    if (-not $env:OPENAI_API_KEY) {
        Log-Warn "未设置OPENAI_API_KEY环境变量"
    }
    
    Log-Info "环境检查完成"
}

# 准备部署文件
function Prepare-Deployment {
    Log-Info "准备部署文件..."
    
    # 创建必要的目录
    $directories = @(
        "data\knowledge-base",
        "data\logs", 
        "data\config",
        "docker\ai-coding-platform"
    )
    
    foreach ($dir in $directories) {
        if (-not (Test-Path $dir)) {
            New-Item -Path $dir -ItemType Directory -Force | Out-Null
            Log-Debug "创建目录: $dir"
        }
    }
    
    # 创建环境变量文件
    Create-EnvFile
    
    Log-Info "部署文件准备完成"
}

# 创建环境变量文件
function Create-EnvFile {
    $envContent = @"
# AI Coding Platform 环境变量
ENVIRONMENT=$Environment
VERSION=$Version

# API Keys
OPENAI_API_KEY=$($env:OPENAI_API_KEY ?? "your-openai-key")
CLAUDE_API_KEY=$($env:CLAUDE_API_KEY ?? "your-claude-key")

# Database
DATABASE_HOST=postgres
DATABASE_PORT=5432
DATABASE_NAME=ai_coding
DATABASE_USERNAME=ai_coding
DATABASE_PASSWORD=ai_coding_password

# Redis
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=redis_password

# Elasticsearch
ELASTICSEARCH_HOST=elasticsearch
ELASTICSEARCH_PORT=9200

# JVM配置
JAVA_OPTS=-Xmx4g -Xms2g -XX:+UseG1GC

# 监控配置
PROMETHEUS_ENABLED=true
GRAFANA_ENABLED=true
"@

    $envFile = "docker\ai-coding-platform\.env"
    $envContent | Out-File -FilePath $envFile -Encoding UTF8
    Log-Debug "环境变量文件已创建: $envFile"
}

# 简化部署 - 只启动核心服务
function Deploy-CoreServices {
    Log-Info "部署AI Coding Platform核心服务..."
    
    # 创建简化的docker-compose文件
    Create-SimplifiedDockerCompose
    
    # 进入docker目录
    Push-Location "docker\ai-coding-platform"
    
    try {
        # 停止现有服务
        Log-Info "停止现有服务..."
        docker-compose down 2>$null
        
        # 拉取镜像
        Log-Info "拉取Docker镜像..."
        docker-compose pull
        
        # 启动服务
        Log-Info "启动服务..."
        docker-compose up -d
        
        # 等待服务启动
        Log-Info "等待服务启动..."
        Start-Sleep -Seconds 30
        
        # 检查服务状态
        Check-ServiceHealth
        
    } finally {
        Pop-Location
    }
}

# 创建简化的docker-compose文件
function Create-SimplifiedDockerCompose {
    $composeContent = @"
version: '3.8'

services:
  # PostgreSQL 数据库
  postgres:
    image: postgres:15-alpine
    container_name: ai-coding-postgres
    environment:
      - POSTGRES_DB=ai_coding
      - POSTGRES_USER=ai_coding
      - POSTGRES_PASSWORD=ai_coding_password
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    restart: unless-stopped

  # Redis 缓存
  redis:
    image: redis:7-alpine
    container_name: ai-coding-redis
    command: redis-server --requirepass redis_password
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    restart: unless-stopped

  # 简化版AI应用 (使用Spring Boot内嵌服务器)
  ai-app:
    image: openjdk:17-jdk-slim
    container_name: ai-coding-app
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=dev
      - DATABASE_URL=jdbc:postgresql://postgres:5432/ai_coding
      - DATABASE_USERNAME=ai_coding
      - DATABASE_PASSWORD=ai_coding_password
      - REDIS_HOST=redis
      - REDIS_PORT=6379
      - REDIS_PASSWORD=redis_password
    volumes:
      - ../../target:/app
      - ./data:/app/data
    working_dir: /app
    command: java -jar flink-task-1.0-SNAPSHOT.jar
    depends_on:
      - postgres
      - redis
    restart: unless-stopped

volumes:
  postgres_data:
  redis_data:
"@

    $composeFile = "docker\ai-coding-platform\docker-compose.yml"
    $composeContent | Out-File -FilePath $composeFile -Encoding UTF8
    Log-Debug "Docker Compose文件已创建: $composeFile"
}

# 检查服务健康状态
function Check-ServiceHealth {
    Log-Info "检查服务健康状态..."
    
    $maxRetries = 10
    $retry = 0
    
    # 检查PostgreSQL
    while ($retry -lt $maxRetries) {
        try {
            docker exec ai-coding-postgres pg_isready -U ai_coding >$null 2>&1
            if ($LASTEXITCODE -eq 0) {
                Log-Info "✓ PostgreSQL 数据库运行正常"
                break
            }
        } catch {}
        
        $retry++
        if ($retry -eq $maxRetries) {
            Log-Error "✗ PostgreSQL 数据库启动失败"
        } else {
            Start-Sleep -Seconds 3
        }
    }
    
    # 检查Redis
    $retry = 0
    while ($retry -lt $maxRetries) {
        try {
            docker exec ai-coding-redis redis-cli -a redis_password ping >$null 2>&1
            if ($LASTEXITCODE -eq 0) {
                Log-Info "✓ Redis 缓存运行正常"
                break
            }
        } catch {}
        
        $retry++
        if ($retry -eq $maxRetries) {
            Log-Error "✗ Redis 缓存启动失败"
        } else {
            Start-Sleep -Seconds 3
        }
    }
    
    Log-Info "基础服务健康检查完成"
}

# 构建Java应用
function Build-JavaApplication {
    Log-Info "构建Java应用..."
    
    # 检查Maven
    try {
        $mvnVersion = mvn --version 2>$null
        if ($LASTEXITCODE -eq 0) {
            Log-Info "使用Maven构建..."
            mvn clean package -DskipTests=true
            
            if ($LASTEXITCODE -eq 0) {
                Log-Info "Maven构建成功"
                return $true
            } else {
                Log-Error "Maven构建失败"
                return $false
            }
        }
    } catch {
        Log-Warn "Maven未安装，尝试使用已有JAR文件"
        
        # 检查是否有现成的JAR文件
        $jarFiles = Get-ChildItem -Path "target" -Filter "*.jar" -ErrorAction SilentlyContinue
        if ($jarFiles) {
            Log-Info "找到现有JAR文件: $($jarFiles[0].Name)"
            return $true
        } else {
            Log-Error "未找到JAR文件且Maven未安装"
            return $false
        }
    }
}

# 显示服务状态
function Show-Status {
    Log-Info "AI Coding Platform 服务状态:"
    
    Push-Location "docker\ai-coding-platform"
    docker-compose ps
    Pop-Location
    
    Write-Host ""
    Log-Info "服务访问地址:"
    Write-Host "  📝 AI Coding Studio: http://localhost:8080"
    Write-Host "  🗄️  PostgreSQL: localhost:5432"
    Write-Host "  🔄 Redis: localhost:6379"
    Write-Host ""
    Log-Info "快速测试:"
    Write-Host "  curl http://localhost:8080/actuator/health"
}

# 安装指南
function Show-InstallGuide {
    Log-Info "Windows环境安装指南:"
    Write-Host ""
    Write-Host "1. 安装Docker Desktop for Windows:"
    Write-Host "   下载: https://www.docker.com/products/docker-desktop"
    Write-Host ""
    Write-Host "2. 安装Java JDK 17+ (可选):"
    Write-Host "   下载: https://adoptium.net/"
    Write-Host ""
    Write-Host "3. 安装Maven (可选):"
    Write-Host "   下载: https://maven.apache.org/download.cgi"
    Write-Host ""
    Write-Host "4. 设置环境变量:"
    Write-Host "   `$env:OPENAI_API_KEY = 'your-api-key'"
    Write-Host ""
    Write-Host "5. 重新运行部署:"
    Write-Host "   .\scripts\deploy-ai-coding-platform.ps1 dev deploy"
}

# 主函数
function Main {
    Write-Host "======================================"
    Write-Host "FlinkSQL AI Coding Platform Windows部署"
    Write-Host "======================================"
    Write-Host "环境: $Environment"
    Write-Host "操作: $Action"
    Write-Host "版本: $Version"
    Write-Host "======================================"
    
    switch ($Action) {
        "deploy" {
            Check-Environment
            Prepare-Deployment
            
            # 尝试构建Java应用
            $buildSuccess = Build-JavaApplication
            if (-not $buildSuccess) {
                Log-Warn "Java应用构建失败，将仅启动基础服务"
            }
            
            Deploy-CoreServices
            Show-Status
        }
        "stop" {
            Log-Info "停止AI Coding Platform服务..."
            Push-Location "docker\ai-coding-platform"
            docker-compose down
            Pop-Location
            Log-Info "服务已停止"
        }
        "status" {
            Show-Status
        }
        "install-guide" {
            Show-InstallGuide
        }
        default {
            Write-Host "用法: .\scripts\deploy-ai-coding-platform.ps1 <环境> <操作> [版本]"
            Write-Host "环境: dev|test|prod"
            Write-Host "操作: deploy|stop|status|install-guide"
            Write-Host "版本: latest|v1.0.0|..."
            Write-Host ""
            Write-Host "示例:"
            Write-Host "  .\scripts\deploy-ai-coding-platform.ps1 dev deploy"
            Write-Host "  .\scripts\deploy-ai-coding-platform.ps1 dev status"
            Write-Host "  .\scripts\deploy-ai-coding-platform.ps1 dev install-guide"
        }
    }
}

# 执行主函数
Main
