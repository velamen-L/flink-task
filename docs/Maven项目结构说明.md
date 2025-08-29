# Maven 项目结构说明

## 项目概述

本项目采用标准的 Maven 构建模式，遵循 Maven 标准目录结构，便于项目管理和构建。

## 目录结构

```
flink-task/
├── src/
│   ├── main/
│   │   ├── java/                    # Java 源代码
│   │   │   └── com/flink/realtime/
│   │   │       ├── bean/            # 数据模型类
│   │   │       ├── business/        # 业务逻辑主类
│   │   │       ├── common/          # 公共工具类
│   │   │       ├── config/          # 配置管理
│   │   │       ├── function/        # Flink 函数
│   │   │       ├── metrics/         # 监控指标
│   │   │       ├── processor/       # 事件处理器
│   │   │       ├── sink/            # 数据输出
│   │   │       ├── source/          # 数据源
│   │   │       └── util/            # 工具类
│   │   └── resources/               # 资源文件
│   │       ├── application.properties
│   │       ├── log4j2.xml
│   │       └── sql/
│   └── test/
│       └── java/                    # 测试代码
│           └── com/flink/realtime/
│               └── app/
├── target/                          # Maven 构建输出目录
├── config/                          # 配置文件
├── docker/                          # Docker 配置
├── docs/                            # 项目文档
├── scripts/                         # 脚本文件
├── sql/                             # SQL 脚本
├── pom.xml                          # Maven 配置文件
├── .gitignore                       # Git 忽略文件
└── README.md                        # 项目说明
```

## Maven 配置说明

### 1. 基本信息
- **GroupId**: `com.flink`
- **ArtifactId**: `flink-realtime-project`
- **Version**: `1.0.0`
- **Packaging**: `jar`

### 2. 依赖管理

#### 核心依赖
- **Flink**: 1.17.1
- **Kafka**: 3.4.0
- **MySQL**: 8.0.33
- **Redis**: 4.3.1
- **FastJSON**: 2.0.32

#### 测试依赖
- **JUnit**: 4.13.2
- **Mockito**: 5.3.1
- **Flink Test Utils**: 1.17.1

### 3. 构建插件

#### Maven Compiler Plugin
- Java 版本: 8
- 编码: UTF-8
- 支持参数名保留

#### Maven Shade Plugin
- 创建可执行 JAR
- 主类: `com.flink.realtime.business.WrongbookWideTableApp`
- 排除签名文件
- 合并服务配置文件

#### Maven Surefire Plugin
- 运行单元测试
- 排除集成测试

#### Maven Failsafe Plugin
- 运行集成测试
- 支持 `*IT.java` 命名规范

### 4. 环境配置

#### Profiles
- **dev**: 开发环境（默认）
- **test**: 测试环境
- **prod**: 生产环境

#### 仓库配置
- Apache Snapshots Repository
- Maven Central Repository

## 构建命令

### 基本构建
```bash
# 编译项目
mvn compile

# 运行测试
mvn test

# 打包项目
mvn package

# 清理构建文件
mvn clean

# 安装到本地仓库
mvn install
```

### 环境特定构建
```bash
# 开发环境
mvn clean package -Pdev

# 测试环境
mvn clean package -Ptest

# 生产环境
mvn clean package -Pprod
```

### 跳过测试
```bash
# 跳过测试编译
mvn package -Dmaven.test.skip=true

# 跳过测试执行
mvn package -DskipTests
```

## 运行应用

### 本地运行
```bash
# 使用 Maven 运行
mvn exec:java -Dexec.mainClass="com.flink.realtime.business.WrongbookWideTableApp"

# 使用打包后的 JAR
java -jar target/flink-realtime-project-1.0.0.jar
```

### Flink 集群运行
```bash
# 提交到 Flink 集群
flink run target/flink-realtime-project-1.0.0.jar
```

## 开发规范

### 1. 包命名规范
- 使用反向域名命名: `com.flink.realtime`
- 按功能模块分包: `bean`, `business`, `config` 等

### 2. 类命名规范
- 主类: `*App.java`
- 配置类: `*Config.java`
- 工具类: `*Utils.java`
- 处理器: `*Processor.java`

### 3. 测试规范
- 单元测试: `*Test.java`
- 集成测试: `*IT.java`
- 测试类放在对应的测试包中

### 4. 资源文件规范
- 配置文件: `src/main/resources/`
- 测试资源: `src/test/resources/`
- SQL 脚本: `src/main/resources/sql/`

## 注意事项

1. **依赖版本管理**: 所有依赖版本都在 `properties` 中统一管理
2. **插件版本管理**: 所有插件版本都在 `properties` 中统一管理
3. **编码设置**: 统一使用 UTF-8 编码
4. **测试分离**: 单元测试和集成测试分离管理
5. **环境配置**: 支持多环境配置切换

## 常见问题

### 1. 编译错误
- 检查 Java 版本是否匹配
- 确认所有依赖都已下载
- 清理并重新编译: `mvn clean compile`

### 2. 测试失败
- 检查测试环境配置
- 确认测试数据准备
- 查看测试日志定位问题

### 3. 打包失败
- 检查主类配置是否正确
- 确认所有依赖都已解析
- 查看构建日志定位问题

### 4. 运行时错误
- 检查配置文件路径
- 确认外部依赖服务可用
- 查看应用日志定位问题
