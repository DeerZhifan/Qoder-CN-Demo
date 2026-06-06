# 如何使用 CDP 附件管理组件

## 概述

附件管理组件（`leatop-cdp-business-file`）基于 x-file-storage 框架，提供附件上传、下载、预览和管理功能。支持本地存储、MinIO、阿里云 OSS、腾讯云 COS、华为云 OBS、百度云 BOS、FTP、FastDFS 等多种存储方式，支持大文件断点续传。

## 启用方式

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-business-file-boot-starter</artifactId>
</dependency>

<!-- 按需引入存储平台依赖，如 MinIO -->
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>8.5.2</version>
</dependency>
```

## 配置项

### 本地存储

```yaml
cdp:
  file-storage:
    default-platform: local-plus-1     # 默认存储平台
    thumbnail-suffix: ".min.jpg"       # 缩略图后缀
    local-plus:
      - platform: local-plus-1
        enable-storage: true
        enable-access: true
        domain: /file/                 # 访问路径前缀
        base-path: local-plus/
        path-patterns: /file/**
        storage-path: D:/Temp/         # 本地存储目录
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

### 其他存储平台

阿里云 OSS、腾讯云 COS、华为云 OBS、百度云 BOS 等需引入对应 SDK 依赖并在 `cdp.file-storage` 下配置。

## 相关数据库表

| 表名 | 说明 |
|------|------|
| `frame_attach` | 附件信息表 |
| `frame_chunk` | 大文件分块临时表 |

## 注意事项

> 注意：切换存储平台只需修改 `default-platform` 配置和对应平台参数，业务代码无需改动。

> 注意：本地存储的 `domain` 建议使用相对路径（如 `/file/`），便于后期更换域名。线上环境建议使用 Nginx 代理静态文件。

> 注意：大文件上传使用分块方式，临时数据存储在 `frame_chunk` 表中，上传完成后自动清理。

> 注意：附件类型和大小限制可在管理界面配置。
