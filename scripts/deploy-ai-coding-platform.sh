#!/bin/bash

# FlinkSQL AI Coding Platform 部署脚本
# 版本: v1.0.0
# 作者: AI Coding Team

set -e  # 遇到错误立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_debug() {
    echo -e "${BLUE}[DEBUG]${NC} $1"
}

# 脚本参数
ENVIRONMENT=${1:-dev}  # dev, test, prod
ACTION=${2:-deploy}    # deploy, update, rollback, stop
VERSION=${3:-latest}   # 版本号

# 配置变量
PROJECT_NAME="flinksql-ai-coding-platform"
DOCKER_REGISTRY="your-registry.com"
NAMESPACE="ai-coding"

# 检查环境
check_environment() {
    log_info "检查部署环境..."
    
    # 检查Docker
    if ! command -v docker &> /dev/null; then
        log_error "Docker未安装，请先安装Docker"
        exit 1
    fi
    
    # 检查Docker Compose
    if ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose未安装，请先安装Docker Compose"
        exit 1
    fi
    
    # 检查环境变量
    if [[ -z "$OPENAI_API_KEY" ]]; then
        log_warn "未设置OPENAI_API_KEY环境变量"
    fi
    
    log_info "环境检查完成"
}

# 准备部署文件
prepare_deployment() {
    log_info "准备部署文件..."
    
    # 创建必要的目录
    mkdir -p data/{knowledge-base,logs,config}
    mkdir -p docker/ai-coding-platform
    
    # 复制环境特定的配置文件
    if [[ -f "config/application-${ENVIRONMENT}.yml" ]]; then
        cp "config/application-${ENVIRONMENT}.yml" docker/ai-coding-platform/application.yml
        log_info "使用环境配置: application-${ENVIRONMENT}.yml"
    else
        log_warn "环境配置文件不存在: application-${ENVIRONMENT}.yml，使用默认配置"
    fi
    
    # 设置环境变量文件
    create_env_file
    
    log_info "部署文件准备完成"
}

# 创建环境变量文件
create_env_file() {
    cat > docker/ai-coding-platform/.env << EOF
# AI Coding Platform 环境变量
ENVIRONMENT=${ENVIRONMENT}
VERSION=${VERSION}

# API Keys
OPENAI_API_KEY=${OPENAI_API_KEY:-your-openai-key}
CLAUDE_API_KEY=${CLAUDE_API_KEY:-your-claude-key}

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

# Catalog配置
CATALOG_JDBC_URL=${CATALOG_JDBC_URL:-jdbc:mysql://localhost:3306/metadata}
CATALOG_USERNAME=${CATALOG_USERNAME:-catalog_user}
CATALOG_PASSWORD=${CATALOG_PASSWORD:-catalog_password}

# JVM配置
JAVA_OPTS=-Xmx4g -Xms2g -XX:+UseG1GC

# 监控配置
PROMETHEUS_ENABLED=true
GRAFANA_ENABLED=true
METRICS_EXPORT_ENABLED=true

EOF
}

# 构建应用
build_application() {
    log_info "构建应用..."
    
    # Maven构建
    if [[ -f "pom.xml" ]]; then
        log_info "使用Maven构建项目..."
        mvn clean package -DskipTests=true
        
        if [[ $? -eq 0 ]]; then
            log_info "Maven构建成功"
        else
            log_error "Maven构建失败"
            exit 1
        fi
    else
        log_error "未找到pom.xml文件"
        exit 1
    fi
    
    # 构建Docker镜像
    log_info "构建Docker镜像..."
    docker build -t ${PROJECT_NAME}:${VERSION} -f docker/ai-coding-platform/Dockerfile .
    
    if [[ $? -eq 0 ]]; then
        log_info "Docker镜像构建成功"
    else
        log_error "Docker镜像构建失败"
        exit 1
    fi
}

# 部署服务
deploy_services() {
    log_info "部署AI Coding Platform服务..."
    
    cd docker/ai-coding-platform
    
    # 停止现有服务
    docker-compose down
    
    # 启动服务
    docker-compose up -d
    
    # 等待服务启动
    log_info "等待服务启动..."
    sleep 30
    
    # 检查服务状态
    check_service_health
    
    cd - > /dev/null
}

# 检查服务健康状态
check_service_health() {
    log_info "检查服务健康状态..."
    
    # 检查主应用
    if curl -f http://localhost:8080/ai-coding/actuator/health > /dev/null 2>&1; then
        log_info "✓ AI Coding Platform 主应用运行正常"
    else
        log_error "✗ AI Coding Platform 主应用启动失败"
        return 1
    fi
    
    # 检查数据库
    if docker-compose exec -T postgres pg_isready > /dev/null 2>&1; then
        log_info "✓ PostgreSQL 数据库运行正常"
    else
        log_error "✗ PostgreSQL 数据库启动失败"
        return 1
    fi
    
    # 检查Redis
    if docker-compose exec -T redis redis-cli ping > /dev/null 2>&1; then
        log_info "✓ Redis 缓存运行正常"
    else
        log_error "✗ Redis 缓存启动失败"
        return 1
    fi
    
    # 检查Elasticsearch
    if curl -f http://localhost:9200/_cluster/health > /dev/null 2>&1; then
        log_info "✓ Elasticsearch 搜索引擎运行正常"
    else
        log_error "✗ Elasticsearch 搜索引擎启动失败"
        return 1
    fi
    
    # 检查ChromaDB
    if curl -f http://localhost:8000/api/v1/heartbeat > /dev/null 2>&1; then
        log_info "✓ ChromaDB 向量数据库运行正常"
    else
        log_error "✗ ChromaDB 向量数据库启动失败"
        return 1
    fi
    
    log_info "所有服务健康检查通过"
}

# 初始化知识库
init_knowledge_base() {
    log_info "初始化知识库..."
    
    # 等待主应用完全启动
    sleep 10
    
    # 调用知识库初始化API
    curl -X POST "http://localhost:8080/ai-coding/api/knowledge-base/init" \
         -H "Content-Type: application/json" \
         -d '{"force": true}'
    
    if [[ $? -eq 0 ]]; then
        log_info "知识库初始化完成"
    else
        log_warn "知识库初始化失败，请手动初始化"
    fi
}

# 更新服务
update_services() {
    log_info "更新AI Coding Platform服务..."
    
    # 构建新版本
    build_application
    
    # 执行滚动更新
    cd docker/ai-coding-platform
    docker-compose up -d --force-recreate ai-coding-platform
    cd - > /dev/null
    
    # 检查更新后的服务
    sleep 20
    check_service_health
    
    log_info "服务更新完成"
}

# 回滚服务
rollback_services() {
    log_info "回滚AI Coding Platform服务..."
    
    PREVIOUS_VERSION=${3:-previous}
    
    # 使用之前的镜像版本
    cd docker/ai-coding-platform
    docker-compose down
    
    # 修改docker-compose.yml中的镜像版本
    sed -i "s/:${VERSION}/:${PREVIOUS_VERSION}/g" docker-compose.yml
    
    docker-compose up -d
    cd - > /dev/null
    
    sleep 20
    check_service_health
    
    log_info "服务回滚完成"
}

# 停止服务
stop_services() {
    log_info "停止AI Coding Platform服务..."
    
    cd docker/ai-coding-platform
    docker-compose down
    cd - > /dev/null
    
    log_info "服务已停止"
}

# 显示服务状态
show_status() {
    log_info "AI Coding Platform 服务状态:"
    
    cd docker/ai-coding-platform
    docker-compose ps
    cd - > /dev/null
    
    echo ""
    log_info "服务访问地址:"
    echo "  📝 AI Coding Studio: http://localhost"
    echo "  🔧 管理界面: http://localhost:8080/ai-coding/actuator"
    echo "  📊 Grafana监控: http://localhost:3000 (admin/admin)"
    echo "  🔍 Prometheus: http://localhost:9091"
    echo "  📈 Elasticsearch: http://localhost:9200"
}

# 主函数
main() {
    echo "======================================"
    echo "FlinkSQL AI Coding Platform 部署工具"
    echo "======================================"
    echo "环境: $ENVIRONMENT"
    echo "操作: $ACTION"
    echo "版本: $VERSION"
    echo "======================================"
    
    case $ACTION in
        "deploy")
            check_environment
            prepare_deployment
            build_application
            deploy_services
            init_knowledge_base
            show_status
            ;;
        "update")
            check_environment
            update_services
            show_status
            ;;
        "rollback")
            check_environment
            rollback_services
            show_status
            ;;
        "stop")
            stop_services
            ;;
        "status")
            show_status
            ;;
        *)
            echo "用法: $0 <环境> <操作> [版本]"
            echo "环境: dev|test|prod"
            echo "操作: deploy|update|rollback|stop|status"
            echo "版本: latest|v1.0.0|..."
            echo ""
            echo "示例:"
            echo "  $0 dev deploy latest     # 部署开发环境"
            echo "  $0 prod update v1.0.1    # 更新生产环境到v1.0.1"
            echo "  $0 test rollback v1.0.0  # 回滚测试环境到v1.0.0"
            echo "  $0 dev status            # 查看开发环境状态"
            exit 1
            ;;
    esac
}

# 执行主函数
main "$@"
