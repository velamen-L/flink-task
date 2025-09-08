# Job 目录结构规范

## 📁 标准目录结构

每个业务域 (`{domain}`) 应该遵循以下标准目录结构：

```
job/
├── {domain}/                          # 业务域目录
│   ├── flink-sql-request-v3.md        # AI 请求文件（必需）
│   ├── config/                        # 配置文件目录
│   │   └── {domain}-job.yml           # 作业配置
│   ├── sql/                           # SQL 文件目录（AI 生成）
│   │   ├── {domain}_wide_table_v3.sql # 主要 SQL 文件
│   │   └── *.sql                      # 其他相关 SQL
│   ├── docs/                          # 文档目录
│   │   ├── README-AI-Generated-v3.md  # AI 生成说明文档
│   │   └── *.md                       # 其他文档
│   ├── deployment/                    # 部署配置目录
│   │   ├── deploy-{domain}-v3.yaml    # Kubernetes 部署配置
│   │   └── *.yaml                     # 其他部署文件
│   └── validation/                    # 验证文件目录
│       ├── data-quality-check-v3.sql  # 数据质量检查
│       └── *.sql                      # 其他验证 SQL
├── flink-sql-request-template-v3.md   # 请求模板文件
└── ai-coding-templates/               # AI 编码模板
    └── flink-sql-ai-template.md       # Flink SQL AI 生成模板
```

## 🎯 目录说明

### 1. 根目录 (`job/{domain}/`)
- **主要文件**: `flink-sql-request-v3.md` - AI 生成的输入文件
- **作用**: 包含完整的 YAML 配置和业务逻辑描述

### 2. 配置目录 (`config/`)
- **文件类型**: `*.yml`, `*.yaml`, `*.properties`
- **作用**: 存放作业运行时配置、环境变量等

### 3. SQL 目录 (`sql/`)
- **主文件**: `{domain}_wide_table_v3.sql` - AI 生成的主要 SQL
- **其他文件**: 历史版本、辅助 SQL、测试 SQL
- **命名规范**: `{domain}_{purpose}_{version}.sql`

### 4. 文档目录 (`docs/`)
- **主文件**: `README-AI-Generated-v3.md` - AI 生成说明
- **其他文件**: 业务文档、架构说明、开发指南
- **格式**: Markdown 为主

### 5. 部署目录 (`deployment/`)
- **主文件**: `deploy-{domain}-v3.yaml` - Kubernetes 部署配置
- **其他文件**: Docker 配置、环境部署脚本
- **格式**: YAML 为主

### 6. 验证目录 (`validation/`)
- **主文件**: `data-quality-check-v3.sql` - 数据质量检查
- **其他文件**: 单元测试、集成测试、性能测试
- **作用**: 确保数据质量和作业正确性

## 🚀 AI 生成工作流

### 输入
1. **请求文件**: `job/{domain}/flink-sql-request-v3.md`
2. **AI 规则**: `.cursor/rules/intelligent-sql-job-generator.mdc`

### 输出
1. **SQL 文件**: `job/{domain}/sql/{domain}_wide_table_v3.sql`
2. **部署配置**: `job/{domain}/deployment/deploy-{domain}-v3.yaml`
3. **质量检查**: `job/{domain}/validation/data-quality-check-v3.sql`
4. **说明文档**: `job/{domain}/docs/README-AI-Generated-v3.md`

## 📋 命名规范

### 文件命名
- **请求文件**: `flink-sql-request-v{version}.md`
- **SQL 文件**: `{domain}_{purpose}_v{version}.sql`
- **部署文件**: `deploy-{domain}-v{version}.yaml`
- **文档文件**: `README-{purpose}-v{version}.md`

### 版本控制
- **v1.0**: 初始版本
- **v2.0**: 重大功能更新
- **v3.0**: 架构升级（当前 AI 生成版本）
- **v3.x**: 功能增强或 bug 修复

## 🔄 更新流程

### 1. 新增业务域
```bash
# 创建标准目录结构
mkdir -p job/{new_domain}/{config,sql,docs,deployment,validation}

# 基于模板创建请求文件
cp job/flink-sql-request-template-v3.md job/{new_domain}/flink-sql-request-v3.md

# 编辑请求文件，配置业务逻辑
# 使用 AI 生成 SQL 和配置文件
```

### 2. 更新现有业务域
```bash
# 修改请求文件
vim job/{domain}/flink-sql-request-v3.md

# 重新生成 SQL（通过 AI）
# 更新版本号
# 验证生成结果
```

## 📊 当前状态

### ✅ 已完成的业务域
- **wrongbook**: 错题本修正记录实时宽表
  - 完整的 v3 AI 生成结果
  - 包含所有标准目录和文件

### 🔄 待完善的业务域
- **user-daily-stats**: 用户日统计
  - 已创建目录结构
  - 需要完善请求文件和 AI 生成

## 🎯 最佳实践

1. **严格遵循目录结构**: 保持一致性，便于维护
2. **版本化管理**: 所有文件都要包含版本信息
3. **文档完整**: 每个生成结果都要有说明文档
4. **AI 优先**: 新的生成都使用 AI Agent 方式
5. **质量检查**: 必须包含数据质量验证脚本

---

通过这个标准化的目录结构，我们实现了：
- 🏗️ **统一架构**: 所有业务域使用相同结构
- 🤖 **AI 驱动**: 完全基于 AI Agent 生成
- 📋 **规范化**: 清晰的命名和版本控制
- 🔍 **可维护**: 易于查找和管理文件
