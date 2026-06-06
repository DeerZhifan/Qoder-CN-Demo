---
trigger: when_referenced
knowledge_source:
  - cdp-design-file
  - cdp-module-file
---

## 适用场景

当代码涉及以下内容时，本规则自动生效：

- 引入 `leatop-cdp-business-file-boot-starter` 或 `leatop-cdp-business-file-cloud-starter` 依赖
- 使用附件上传、下载、预览、删除相关接口
- 配置 `cdp.file-storage` 存储平台参数
- 操作 `frame_attach`、`frame_chunk` 数据表
- 使用分片上传（`initUploadPart`、`uploadPart`、`mergeUpload`）

---

## 前置依赖

1. Maven 依赖（单体模式）：

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-business-file-boot-starter</artifactId>
</dependency>
```

微服务模式使用 `leatop-cdp-business-file-cloud-starter`。

2. 如使用 MinIO 存储，需额外引入：

```xml
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>8.5.2</version>
</dependency>
```

3. 数据库表：`frame_attach`（附件元数据）、`frame_chunk`（分片临时记录），通过 Flyway 自动创建。

---

## 配置要点

### 本地存储

```yaml
cdp:
  file-storage:
    default-platform: local-plus-1
    thumbnail-suffix: ".min.jpg"
    local-plus:
      - platform: local-plus-1
        enable-storage: true
        enable-access: true
        domain: /file/
        base-path: local-plus/
        path-patterns: /file/**
        storage-path: D:/Temp/
```

### MinIO 存储

```yaml
cdp:
  file-storage:
    default-platform: minio-1
    minio:
      - platform: minio-1
        enable-storage: true
        access-key: admin
        secret-key: "!QAZ2wsx"
        end-point: http://172.17.1.83:9000
        bucket-name: test
        domain: http://172.17.1.83:9000/test/
        base-path: admin/
```

### 安全与限制配置

`SpringFileStorageProperties` 扩展属性（前缀 `cdp.file-storage`）：

| 属性 | 说明 | 默认值 |
|------|------|--------|
| `maxSize` | 单文件大小上限 | 10MB |
| `checkContentType` | 是否校验 MIME 类型 | - |
| `contentTypeWhiteList` | MIME 白名单 | - |
| `extWhiteList` | 扩展名白名单 | `*`（放行所有） |

切换存储平台只需修改 `default-platform` 和对应平台参数，业务代码无需改动。

---

## 代码模式

### 推荐写法

**通过内置 REST 接口操作附件：**

| 接口路径 | 说明 |
|---------|------|
| `POST /attachment/upload` | 普通文件上传（记录业务关联 buType/buId） |
| `POST /attachment/uploadFile` | 上传文件（不记录元数据到 frame_attach） |
| `POST /attachment/initUploadPart` | 分片上传 - 初始化，获取 uploadId |
| `POST /attachment/uploadPart` | 分片上传 - 逐片上传 |
| `POST /attachment/mergeUpload` | 分片上传 - 合并完成 |
| `GET /attachment/download` | 下载（单文件直出，多文件 ZIP 打包） |
| `GET /attachment/preview` | 文件预览 |
| `GET /attachment/thumbnail` | 缩略图展示 |

**业务关联模式：** 通过 `FileBindBusDTO` 的 `buType`/`buId` 字段将附件与业务实体关联。

**断点续传：** 调用 `checkUploadPart()` 查询已上传分片列表，若 uploadId 不存在则自动初始化，实现断点续传。

**自定义文件用户信息：** 实现 `FileUserService` 接口的 `queryUserName()` 方法，用于附件列表展示上传者姓名。

### 禁止事项

- **禁止绕过 x-file-storage 框架直接操作存储** -- 必须通过 `FileStorageService` 或内置 REST 接口操作文件
- **禁止忽略文件类型白名单配置** -- 生产环境必须配置 `extWhiteList` 和 `contentTypeWhiteList`，不得使用 `*` 放行所有类型
- **禁止使用 DatabaseFileStorage 存储大文件** -- 数据库存储使用 Base64 编码，体积膨胀约 33%，仅适合小文件（电子签章、小图标）
- **禁止在无限制条件下使用多文件 ZIP 打包下载** -- ZIP 打包在服务端内存中完成，大量大文件同时下载会导致 OOM，应限制单次下载文件数和总大小
- **禁止忽略分片上传的清理机制** -- `frame_chunk` 表中超时分片数据需通过 XXL-Job 定期清理，当前框架未内置清理任务
- **禁止在本地存储模式下将 domain 配置为绝对域名** -- 建议使用相对路径（如 `/file/`），便于后期更换域名，线上环境通过 Nginx 代理
