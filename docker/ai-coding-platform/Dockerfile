# FlinkSQL AI Coding Platform Docker 镜像

FROM openjdk:17-jdk-slim

# 设置工作目录
WORKDIR /app

# 安装必要的工具
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    vim \
    net-tools \
    && rm -rf /var/lib/apt/lists/*

# 复制应用JAR文件
COPY target/flinksql-ai-coding-platform-*.jar app.jar

# 复制配置文件
COPY job/ai-config/ ./ai-config/
COPY src/main/resources/application.yml ./application.yml

# 复制前端静态文件
COPY src/main/resources/static/ ./static/

# 创建日志目录
RUN mkdir -p /app/logs

# 创建知识库存储目录
RUN mkdir -p /app/knowledge-base

# 设置环境变量
ENV JAVA_OPTS="-Xmx2g -Xms1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
ENV SPRING_PROFILES_ACTIVE=docker
ENV AI_PLATFORM_DEBUG=false

# 暴露端口
EXPOSE 8080 8081 9090

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/ai-coding/actuator/health || exit 1

# 启动脚本
COPY docker/ai-coding-platform/start.sh /app/start.sh
RUN chmod +x /app/start.sh

# 启动应用
ENTRYPOINT ["/app/start.sh"]
