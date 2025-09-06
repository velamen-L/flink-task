# 🚀 FlinkSQL AI Coding Platform - 快速体验

## 🎯 立即体验 (无需安装)

### 方式一：打开演示页面
```powershell
# 在项目根目录下，直接用浏览器打开演示文件
start ai-coding-demo.html
```

### 方式二：双击文件
- 找到项目根目录下的 `ai-coding-demo.html` 文件
- 双击用浏览器打开

## ✨ 演示功能

### 🤖 AI智能生成
- 自然语言转FlinkSQL
- 多种业务场景模板
- 实时生成高质量代码
- 智能优化建议

### 📊 质量检查演示
- 代码质量评分
- 性能分析报告
- 最佳实践建议
- 优化方案推荐

### 🎨 用户体验
- 现代化界面设计
- 响应式布局
- 实时交互反馈
- 专业代码高亮

## 🔧 完整环境部署

如果您想体验完整功能，需要安装Docker：

### 第一步：安装Docker Desktop
1. 访问 https://www.docker.com/products/docker-desktop
2. 下载并安装 Docker Desktop for Windows
3. 启动Docker Desktop

### 第二步：部署平台
```powershell
# 克隆项目 (如果还未克隆)
git clone <项目地址>

# 进入项目目录
cd flink-task

# 运行部署脚本
.\scripts\deploy-ai-coding-platform.ps1 dev deploy
```

### 第三步：访问完整平台
- 主应用：http://localhost:8080
- 监控面板：http://localhost:3000
- API文档：http://localhost:8080/swagger-ui.html

## 🎊 当前演示版vs完整版

| 功能 | 演示版 | 完整版 |
|------|--------|--------|
| AI生成SQL | ✅ 模拟演示 | ✅ 真实AI生成 |
| 质量检查 | ✅ 静态展示 | ✅ 实时检查 |
| 知识库 | ❌ | ✅ 企业知识库 |
| 用户管理 | ❌ | ✅ 完整权限系统 |
| 监控告警 | ❌ | ✅ 实时监控 |
| 版本控制 | ❌ | ✅ Git集成 |
| API接口 | ❌ | ✅ 完整REST API |

## 🤔 常见问题

### Q: 演示版能生成真实的SQL吗？
A: 演示版展示了AI生成的高质量FlinkSQL模板，但不连接真实AI服务。完整版使用GPT-4等大模型真实生成。

### Q: 为什么不直接部署完整版？
A: 您的环境缺少Docker，完整版需要Docker容器化部署。演示版让您快速了解平台能力。

### Q: 演示版的代码质量如何？
A: 演示版展示的SQL代码都是基于企业最佳实践的高质量模板，代表了AI生成的真实水平。

### Q: 如何获得完整版？
A: 安装Docker后运行部署脚本即可获得完整功能。我们也提供企业版定制服务。

## 📞 技术支持

- 📧 邮箱：support@ai-coding-platform.com  
- 💬 微信群：扫码加入技术交流群
- 📖 文档：https://docs.ai-coding-platform.com
- 🐛 问题反馈：GitHub Issues

## 🎉 开始体验

立即打开 `ai-coding-demo.html` 开始您的AI编程之旅！

---

💡 **提示**：演示版本展示了平台的核心价值和用户体验，完整版提供企业级的完整功能。
