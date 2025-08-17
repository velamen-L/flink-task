#!/bin/bash

# =============================================
# Flink混合架构动态路由作业启动脚本
# =============================================

# 设置脚本参数
set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $(date '+%Y-%m-%d %H:%M:%S') $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') $1"
}

# 检查参数
if [ $# -eq 0 ]; then
    log_error "请提供业务域参数"
    echo "使用方法："
    echo "  $0 <domain> [options]"
    echo ""
    echo "可用业务域："
    echo "  wrongbook  - 错题本业务域"
    echo "  user       - 用户业务域"
    echo "  order      - 订单业务域"
    echo ""
    echo "可选参数："
    echo "  --parallelism <num>    设置并行度（默认：4）"
    echo "  --checkpoint <ms>      设置检查点间隔（默认：60000）"
    echo "  --init-config         初始化配置表并插入示例数据"
    echo "  --config-only         仅管理配置，不启动作业"
    echo ""
    echo "示例："
    echo "  $0 wrongbook --parallelism 8"
    echo "  $0 user --init-config"
    echo "  $0 wrongbook --config-only"
    exit 1
fi

# 解析参数
DOMAIN=$1
shift

PARALLELISM=4
CHECKPOINT_INTERVAL=60000
INIT_CONFIG=false
CONFIG_ONLY=false
FLINK_HOME=${FLINK_HOME:-"/opt/flink"}
PROJECT_HOME=$(cd "$(dirname "$0")/.." && pwd)

while [[ $# -gt 0 ]]; do
    case $1 in
        --parallelism)
            PARALLELISM="$2"
            shift 2
            ;;
        --checkpoint)
            CHECKPOINT_INTERVAL="$2"
            shift 2
            ;;
        --init-config)
            INIT_CONFIG=true
            shift
            ;;
        --config-only)
            CONFIG_ONLY=true
            shift
            ;;
        *)
            log_error "未知参数: $1"
            exit 1
            ;;
    esac
done

log_info "=== Flink混合架构动态路由作业启动 ==="
log_info "业务域: $DOMAIN"
log_info "并行度: $PARALLELISM"
log_info "检查点间隔: ${CHECKPOINT_INTERVAL}ms"
log_info "项目目录: $PROJECT_HOME"

# 检查环境
check_environment() {
    log_info "检查运行环境..."
    
    # 检查Java
    if ! command -v java &> /dev/null; then
        log_error "Java未安装或未添加到PATH"
        exit 1
    fi
    
    # 检查MySQL连接
    if ! command -v mysql &> /dev/null; then
        log_warn "MySQL客户端未安装，跳过数据库连接检查"
    fi
    
    # 检查项目文件
    if [ ! -f "$PROJECT_HOME/target/flink-realtime-project-1.0-SNAPSHOT.jar" ]; then
        log_error "项目JAR文件不存在，请先编译项目"
        log_info "运行: cd $PROJECT_HOME && mvn clean package -DskipTests"
        exit 1
    fi
    
    log_info "环境检查通过"
}

# 初始化配置
init_config() {
    log_info "初始化数据库配置..."
    
    mysql -u root -p < "$PROJECT_HOME/sql/init_dynamic_routing.sql"
    
    # 插入特定域的配置
    case $DOMAIN in
        wrongbook)
            log_info "初始化错题本域配置..."
            java -cp "$PROJECT_HOME/target/flink-realtime-project-1.0-SNAPSHOT.jar" \
                com.flink.realtime.config.RoutingConfigManager \
                add wrongbook wrongbook_add com.flink.realtime.processor.impl.WrongbookAddProcessor
            java -cp "$PROJECT_HOME/target/flink-realtime-project-1.0-SNAPSHOT.jar" \
                com.flink.realtime.config.RoutingConfigManager \
                add wrongbook wrongbook_fix com.flink.realtime.processor.impl.WrongbookFixProcessor
            ;;
        user)
            log_info "初始化用户域配置..."
            java -cp "$PROJECT_HOME/target/flink-realtime-project-1.0-SNAPSHOT.jar" \
                com.flink.realtime.config.RoutingConfigManager \
                add user user_login com.flink.realtime.processor.impl.UserLoginProcessor
            java -cp "$PROJECT_HOME/target/flink-realtime-project-1.0-SNAPSHOT.jar" \
                com.flink.realtime.config.RoutingConfigManager \
                add user user_register com.flink.realtime.processor.impl.UserRegisterProcessor
            ;;
        *)
            log_warn "未定义域 $DOMAIN 的默认配置"
            ;;
    esac
    
    log_info "配置初始化完成"
}

# 配置管理菜单
config_management() {
    log_info "=== 配置管理菜单 ==="
    echo "1. 查看当前配置"
    echo "2. 添加新配置"
    echo "3. 启用配置"
    echo "4. 禁用配置"
    echo "5. 删除配置"
    echo "0. 退出"
    
    read -p "请选择操作: " choice
    
    case $choice in
        1)
            java -cp "$PROJECT_HOME/target/flink-realtime-project-1.0-SNAPSHOT.jar" \
                com.flink.realtime.config.RoutingConfigManager list $DOMAIN
            ;;
        2)
            read -p "请输入事件类型: " event_type
            read -p "请输入处理器类名: " processor_class
            java -cp "$PROJECT_HOME/target/flink-realtime-project-1.0-SNAPSHOT.jar" \
                com.flink.realtime.config.RoutingConfigManager \
                add $DOMAIN $event_type $processor_class
            ;;
        3)
            read -p "请输入事件类型: " event_type
            java -cp "$PROJECT_HOME/target/flink-realtime-project-1.0-SNAPSHOT.jar" \
                com.flink.realtime.config.RoutingConfigManager \
                enable $DOMAIN $event_type
            ;;
        4)
            read -p "请输入事件类型: " event_type
            java -cp "$PROJECT_HOME/target/flink-realtime-project-1.0-SNAPSHOT.jar" \
                com.flink.realtime.config.RoutingConfigManager \
                disable $DOMAIN $event_type
            ;;
        5)
            read -p "请输入事件类型: " event_type
            read -p "确认删除配置 $DOMAIN:$event_type? (y/N): " confirm
            if [[ $confirm =~ ^[Yy]$ ]]; then
                java -cp "$PROJECT_HOME/target/flink-realtime-project-1.0-SNAPSHOT.jar" \
                    com.flink.realtime.config.RoutingConfigManager \
                    delete $DOMAIN $event_type
            fi
            ;;
        0)
            log_info "退出配置管理"
            exit 0
            ;;
        *)
            log_error "无效选择"
            ;;
    esac
}

# 启动Flink作业
start_flink_job() {
    log_info "启动Flink动态路由作业..."
    
    # 构建启动命令
    FLINK_CMD="java -cp $PROJECT_HOME/target/flink-realtime-project-1.0-SNAPSHOT.jar"
    FLINK_CMD="$FLINK_CMD com.flink.realtime.app.DynamicRoutingFlinkApp"
    FLINK_CMD="$FLINK_CMD $DOMAIN"
    
    # 设置JVM参数
    export JAVA_OPTS="-Xmx2g -Xms1g"
    export FLINK_CONF_DIR="$PROJECT_HOME/src/main/resources"
    
    # 设置Flink参数
    export FLINK_PARALLELISM=$PARALLELISM
    export FLINK_CHECKPOINT_INTERVAL=$CHECKPOINT_INTERVAL
    
    log_info "执行命令: $FLINK_CMD"
    
    # 启动作业
    $FLINK_CMD
}

# 主执行流程
main() {
    check_environment
    
    if [ "$INIT_CONFIG" = true ]; then
        init_config
    fi
    
    if [ "$CONFIG_ONLY" = true ]; then
        while true; do
            config_management
            echo ""
            read -p "继续管理配置? (y/N): " continue_config
            if [[ ! $continue_config =~ ^[Yy]$ ]]; then
                break
            fi
        done
    else
        start_flink_job
    fi
}

# 信号处理
trap 'log_info "接收到退出信号，正在清理..."; exit 0' SIGINT SIGTERM

# 执行主函数
main
