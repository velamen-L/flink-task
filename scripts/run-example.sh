#!/bin/bash

# Flink作业代码生成示例脚本
# 演示如何使用AI编程脚手架生成用户域作业代码

echo "==================================="
echo "Flink作业代码生成器示例"
echo "==================================="

# 检查Python环境
if ! command -v python3 &> /dev/null; then
    echo "错误: 未找到Python3环境，请先安装Python3"
    exit 1
fi

# 检查依赖
echo "检查Python依赖..."
python3 -c "import jinja2" 2>/dev/null
if [ $? -ne 0 ]; then
    echo "安装Python依赖..."
    pip3 install jinja2
fi

# 创建示例目录
mkdir -p generated

echo ""
echo "开始生成用户域作业代码..."
echo "配置文件: examples/user-job-config.json"
echo ""

# 运行代码生成器
python3 job-generator.py examples/user-job-config.json

echo ""
echo "==================================="
echo "代码生成完成！"
echo "==================================="
echo ""
echo "生成的文件:"
echo "├── generated/user/"
echo "│   ├── UserDataStreamApp.java"
echo "│   ├── UserSqlApp.java" 
echo "│   ├── UserEventProcessor.java"
echo "│   ├── UserEvent.java"
echo "│   └── user-application.properties"
echo ""
echo "下一步:"
echo "1. 复制生成的Java文件到src/main/java对应目录"
echo "2. 复制配置文件到src/main/resources"
echo "3. 根据实际业务需求调整处理逻辑"
echo "4. 编译和测试作业代码"
echo ""
echo "更多信息请参考: docs/AI编程脚手架使用指南.md"
