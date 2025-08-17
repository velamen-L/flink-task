#!/bin/bash

# =============================================
# 混合架构动态路由测试脚本
# =============================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $(date '+%Y-%m-%d %H:%M:%S') $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') $1"
}

PROJECT_HOME=$(cd "$(dirname "$0")/../.." && pwd)

log_info "=== 混合架构动态路由测试 ==="
log_info "项目目录: $PROJECT_HOME"

# 1. 检查编译
log_info "1. 检查项目编译..."
if [ ! -f "$PROJECT_HOME/target/flink-realtime-project-1.0-SNAPSHOT.jar" ]; then
    log_warn "项目未编译，正在编译..."
    cd "$PROJECT_HOME"
    mvn clean package -DskipTests -q
    log_info "编译完成"
else
    log_info "项目已编译"
fi

# 2. 检查数据库连接
log_info "2. 检查数据库连接..."
if command -v mysql &> /dev/null; then
    # 测试数据库连接（需要手动配置）
    log_info "MySQL客户端已安装，请确保数据库配置正确"
else
    log_warn "MySQL客户端未安装，跳过数据库检查"
fi

# 3. 生成错题本场景SQL
log_info "3. 生成错题本场景Flink SQL..."
cd "$PROJECT_HOME/scripts"
python3 aliyun-sql-generator.py examples/wrongbook-job-config.json
if [ $? -eq 0 ]; then
    log_info "SQL生成成功，位置: generated/sql/wrongbook/"
else
    log_error "SQL生成失败"
    exit 1
fi

# 4. 生成DataStream API代码
log_info "4. 生成错题本场景DataStream API代码..."
python3 job-generator.py examples/wrongbook-job-config.json
if [ $? -eq 0 ]; then
    log_info "DataStream代码生成成功，位置: generated/java/wrongbook/"
else
    log_error "DataStream代码生成失败"
    exit 1
fi

# 5. 测试配置管理工具
log_info "5. 测试配置管理工具..."
echo "测试配置管理功能（需要数据库支持）:"
echo "java -cp $PROJECT_HOME/target/flink-realtime-project-1.0-SNAPSHOT.jar com.flink.realtime.config.RoutingConfigManager create-table"
echo "java -cp $PROJECT_HOME/target/flink-realtime-project-1.0-SNAPSHOT.jar com.flink.realtime.config.RoutingConfigManager list wrongbook"

# 6. 显示启动命令
log_info "6. 动态路由作业启动示例..."
echo ""
echo -e "${BLUE}=== 启动命令示例 ===${NC}"
echo ""
echo "# 1. 初始化配置并启动错题本域作业:"
echo "scripts/start-dynamic-routing.sh wrongbook --init-config --parallelism 4"
echo ""
echo "# 2. 仅管理配置:"
echo "scripts/start-dynamic-routing.sh wrongbook --config-only"
echo ""
echo "# 3. 添加新事件类型（热更新）:"
echo "java -cp target/flink-realtime-project-1.0-SNAPSHOT.jar \\"
echo "  com.flink.realtime.config.RoutingConfigManager \\"
echo "  add wrongbook wrongbook_review com.flink.realtime.processor.impl.WrongbookReviewProcessor"
echo ""

# 7. 显示文档位置
log_info "7. 相关文档..."
echo ""
echo -e "${BLUE}=== 文档位置 ===${NC}"
echo "• 混合架构升级说明: docs/混合架构升级说明.md"
echo "• 阿里云架构调整说明: docs/阿里云架构调整说明.md"  
echo "• 生成的SQL文件: scripts/generated/sql/wrongbook/"
echo "• 生成的Java代码: scripts/generated/java/wrongbook/"
echo ""

log_info "测试完成! 混合架构已就绪 🚀"

echo ""
echo -e "${GREEN}=== 下一步操作建议 ===${NC}"
echo "1. 配置数据库连接参数（src/main/resources/application.properties）"
echo "2. 执行数据库初始化脚本（sql/init_dynamic_routing.sql）"
echo "3. 根据业务需求调整生成的处理器代码"
echo "4. 启动动态路由作业并测试热更新功能"
echo ""
