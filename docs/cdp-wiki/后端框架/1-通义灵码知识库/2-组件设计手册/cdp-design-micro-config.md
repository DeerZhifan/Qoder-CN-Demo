# CDP 配置中心模块设计手册

> 对应使用手册：[cdp-module-micro-config.md](../3-组件使用手册/cdp-module-micro-config.md)

## 一、设计目标与背景

在微服务架构中，各服务实例的配置管理是一个核心问题。CDP 配置中心模块的定位是提供一个轻量级的 Nacos 配置中心集成层，使业务服务可以从 Nacos 拉取配置并支持动态刷新，同时不引入任何自定义代码，最大程度复用 Spring Cloud Alibaba 的原生能力。

设计目标如下：

1. **零代码封装** -- 模块本身不包含任何 Java 源码，仅作为依赖聚合器（POM 模块），将 `spring-cloud-starter-alibaba-nacos-config` 作为传递依赖引入。业务侧只需声明对 `leatop-cdp-micro-config` 的依赖，无需关心底层依赖坐标。
2. **版本统一** -- 通过 CDP 的 BOM（`leatop-cdp-dependencies`）统一管理 Spring Cloud Alibaba Nacos Config 的版本号，避免各业务服务因版本不一致导致的兼容性问题。
3. **两种配置模式兼容** -- 支持 Spring Boot 3.x 推荐的 `spring.config.import` 方式和兼容旧版的 `bootstrap.yml` 方式，满足不同项目的迁移需求。

## 二、整体架构

模块结构极为简洁：

```
leatop-cdp-micro-config/
└── pom.xml       # packaging=pom，仅声明对 spring-cloud-starter-alibaba-nacos-config 的依赖
```

该模块没有 `src/main/java` 目录，也没有自动配置类。其核心价值在于依赖聚合和版本收敛。

配置加载的优先级链路（由 Spring Cloud Alibaba Nacos Config 原生实现）：

```
Nacos 远程配置（高优先级）
  -> spring.config.import 声明的配置源
    -> 本地 application.yaml
      -> 本地 application-{profile}.yaml
```

当 Nacos 中的配置项与本地配置冲突时，Nacos 配置优先。通过 `refreshEnabled=true`（默认）开启动态刷新后，Nacos 配置变更会通过长轮询实时推送到应用，`@RefreshScope` 标注的 Bean 和 `@ConfigurationProperties` 绑定的属性类会自动更新。

> 设计决策：将配置中心模块设计为纯 POM 聚合模块而非包含自定义代码的 JAR 模块，是因为 Spring Cloud Alibaba Nacos Config 的功能已经足够完善，CDP 无需在其上层增加额外抽象。这种"薄封装"策略降低了维护成本，也避免了与上游版本升级时的兼容问题。

## 三、关键类说明

本模块不包含自定义 Java 类。以下是配置加载过程中涉及的关键 Spring Cloud Alibaba 组件：

| 组件 | 来源 | 职责 |
|------|------|------|
| `NacosConfigBootstrapConfiguration` | Spring Cloud Alibaba | bootstrap 模式下的配置加载入口 |
| `NacosConfigAutoConfiguration` | Spring Cloud Alibaba | config.import 模式下的配置加载入口 |
| `NacosConfigProperties` | Spring Cloud Alibaba | 绑定 `spring.cloud.nacos.config.*` 前缀的配置属性 |
| `NacosPropertySourceLocator` | Spring Cloud Alibaba | 从 Nacos 服务端拉取配置并转换为 Spring PropertySource |

CDP 框架层面与配置中心交互的类：

| 类名 | 所属模块 | 与配置中心的关系 |
|------|---------|----------------|
| `CustomEncryptor` | leatop-cdp-common-jasypt | 对从 Nacos 拉取的 `ENC(...)` 密文进行解密 |

## 四、扩展机制

### 4.1 多配置源聚合

通过 `spring.config.import` 可声明多个 Nacos 配置源，每个配置源可指定不同的 group 和 refreshEnabled 参数。这使得一个服务可以同时读取共享配置（如数据库连接信息）和私有配置（如业务参数），实现配置的分层管理。

### 4.2 命名空间隔离

Nacos 的命名空间（namespace）机制天然支持多环境隔离。CDP 通过 `${NACOS_NS:dev}` 环境变量控制当前使用的命名空间，在开发、测试、生产环境间切换时只需修改环境变量，应用代码和配置文件无需改动。

### 4.3 配置加密

CDP 的 Jasypt 模块（`leatop-cdp-common-jasypt`）提供了配置项加密能力。在 Nacos 配置中心存储的敏感配置（如数据库密码）可使用 `ENC(密文)` 格式，应用启动时由 CustomEncryptor 自动解密。这一能力对配置中心模块透明，无需额外配置。
