# Windows环境部署指南

## 🎯 快速开始

### 第一步：检查是否需要安装组件

```powershell
# 运行安装指南检查
.\scripts\deploy-ai-coding-platform.ps1 dev install-guide
```

### 第二步：安装必要组件

#### 1. 安装Docker Desktop
- 访问 https://www.docker.com/products/docker-desktop
- 下载并安装Docker Desktop for Windows
- 启动Docker Desktop（确保状态显示为"Running"）

#### 2. 验证Docker安装
```powershell
docker --version
docker ps
```

#### 3. 设置API密钥 (可选)
```powershell
$env:OPENAI_API_KEY = "your-openai-api-key"
$env:CLAUDE_API_KEY = "your-claude-api-key"
```

### 第三步：一键部署

```powershell
# 进入项目目录
cd C:\Users\14867\cursorProject\flink-task\flink-task

# 执行部署
.\scripts\deploy-ai-coding-platform.ps1 dev deploy
```

## 🔧 详细部署步骤

### 1. 环境检查
```powershell
# 检查系统要求
.\scripts\deploy-ai-coding-platform.ps1 dev install-guide
```

### 2. 部署核心服务
```powershell
# 部署PostgreSQL + Redis + 基础应用
.\scripts\deploy-ai-coding-platform.ps1 dev deploy
```

### 3. 检查服务状态
```powershell
# 查看所有服务状态
.\scripts\deploy-ai-coding-platform.ps1 dev status

# 或者直接使用docker命令
docker ps
```

### 4. 访问应用
- **主应用**: http://localhost:8080
- **数据库**: localhost:5432 (用户名: ai_coding, 密码: ai_coding_password)
- **Redis**: localhost:6379 (密码: redis_password)

## 🐛 常见问题解决

### 问题1: Docker命令不识别
**现象**: `docker : 无法将"docker"项识别为 cmdlet`

**解决方案**:
1. 确保Docker Desktop已安装并启动
2. 重启PowerShell
3. 检查环境变量PATH中是否包含Docker路径

### 问题2: Docker Desktop未启动
**现象**: `error during connect: This error may indicate that the docker daemon is not running`

**解决方案**:
1. 启动Docker Desktop应用
2. 等待Docker状态变为"Running"
3. 重新运行部署脚本

### 问题3: 端口冲突
**现象**: `port is already allocated`

**解决方案**:
```powershell
# 查看端口占用
netstat -ano | findstr :8080
netstat -ano | findstr :5432

# 停止冲突的服务或修改端口配置
```

### 问题4: Maven未安装但需要构建Java应用
**现象**: Maven构建失败

**解决方案**:
1. **临时方案**: 使用预构建的JAR文件(如果存在)
2. **完整方案**: 安装Maven
   ```powershell
   # 使用Chocolatey安装Maven (需要先安装Chocolatey)
   choco install maven
   
   # 或手动下载安装
   # https://maven.apache.org/download.cgi
   ```

### 问题5: 内存不足
**现象**: 服务启动后很快停止

**解决方案**:
1. 在Docker Desktop设置中增加内存分配
2. 修改JVM参数减少内存使用:
   ```yaml
   environment:
     - JAVA_OPTS=-Xmx2g -Xms1g
   ```

## 🎨 自定义配置

### 修改端口
编辑 `docker\ai-coding-platform\docker-compose.yml`:
```yaml
services:
  ai-app:
    ports:
      - "8081:8080"  # 改为8081端口
```

### 修改数据库配置
编辑环境变量文件 `docker\ai-coding-platform\.env`:
```env
DATABASE_NAME=my_ai_coding
DATABASE_USERNAME=my_user
DATABASE_PASSWORD=my_password
```

### 添加卷挂载
```yaml
services:
  ai-app:
    volumes:
      - ./config:/app/config
      - ./logs:/app/logs
```

## 🔄 管理命令

### 启动服务
```powershell
.\scripts\deploy-ai-coding-platform.ps1 dev deploy
```

### 停止服务
```powershell
.\scripts\deploy-ai-coding-platform.ps1 dev stop
```

### 查看状态
```powershell
.\scripts\deploy-ai-coding-platform.ps1 dev status
```

### 查看日志
```powershell
# 进入docker目录
cd docker\ai-coding-platform

# 查看所有服务日志
docker-compose logs

# 查看特定服务日志
docker-compose logs ai-app
docker-compose logs postgres
docker-compose logs redis
```

### 重启服务
```powershell
cd docker\ai-coding-platform
docker-compose restart
```

### 清理数据
```powershell
cd docker\ai-coding-platform
docker-compose down -v  # 删除数据卷
```

## 📋 验证部署成功

### 1. 检查服务运行状态
```powershell
docker ps
```
应该看到3个容器在运行:
- ai-coding-postgres
- ai-coding-redis  
- ai-coding-app

### 2. 测试数据库连接
```powershell
# 连接PostgreSQL
docker exec -it ai-coding-postgres psql -U ai_coding -d ai_coding

# 在数据库中运行
\dt  # 查看表
\q   # 退出
```

### 3. 测试Redis连接
```powershell
docker exec -it ai-coding-redis redis-cli -a redis_password
# 在Redis中运行
ping  # 应该返回PONG
exit
```

### 4. 测试Web应用
```powershell
# 健康检查
curl http://localhost:8080/actuator/health

# 或在浏览器中访问
# http://localhost:8080
```

## 🚀 下一步

部署成功后，您可以：

1. **开发模式**: 如果需要本地开发，安装Java JDK 17+和Maven
2. **生产部署**: 参考生产环境部署指南
3. **功能探索**: 访问Web界面开始使用AI编程功能
4. **监控设置**: 配置监控和日志收集

## 📞 获取帮助

如果遇到其他问题：
1. 查看Docker容器日志: `docker-compose logs`
2. 检查系统资源: 任务管理器
3. 重启Docker Desktop
4. 联系技术支持
