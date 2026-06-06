# 启用 CDP 附件管理组件

## 描述

在已有 CDP 项目中启用附件管理组件（`leatop-cdp-business-file`），支持本地存储、MinIO、阿里云 OSS、腾讯云 COS、华为云 OBS 等多种存储方式，基于 x-file-storage 框架提供统一的文件操作 API，切换存储后端只需修改配置。

## 输入

请向用户确认以下信息：

1. **模块名称**（当前模块的 artifactId，如 `leatop-cdp-myapp`）
2. **部署模式**（`boot` 单体 或 `cloud` 微服务，默认 `boot`）
3. **存储平台**（`local` 本地存储 或 `minio` MinIO 存储，默认 `local`）
4. **存储路径**（本地存储目录或 MinIO 的 bucket/endpoint 信息）

---

## 步骤 1：添加 Maven 依赖

> 附件管理作为业务模块，提供 boot-starter 和 cloud-starter 两种接入方式。版本号由父 POM 的 BOM 管理，不需要手动指定。

在 `pom.xml` 的 `<dependencies>` 中添加：

**单体模式：**

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-business-file-boot-starter</artifactId>
</dependency>
```

**微服务模式：**

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-business-file-cloud-starter</artifactId>
</dependency>
```

如使用 MinIO 存储，需额外引入：

```xml
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>8.5.2</version>
</dependency>
```

## 步骤 2：添加存储配置

> 以下为两种常用存储平台的配置。切换存储平台只需修改 `default-platform` 和对应平台参数，业务代码无需改动。

在 `application-dev.yaml` 中添加：

**本地存储方式：**

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
        storage-path: {存储路径}
```

**MinIO 存储方式：**

```yaml
cdp:
  file-storage:
    default-platform: minio-1
    minio:
      - platform: minio-1
        enable-storage: true
        access-key: {MinIO 访问密钥}
        secret-key: {MinIO 秘密密钥}
        end-point: {MinIO 服务地址}
        bucket-name: {桶名称}
        domain: {MinIO 文件访问域名}
        base-path: admin/
```

## 步骤 3：确认数据库表

> 组件依赖以下数据库表，通过 Flyway 自动创建。如未启用 Flyway，需手动执行对应 SQL 脚本。

| 表名 | 说明 |
|------|------|
| `frame_attach` | 附件元数据表（文件名、大小、类型、存储路径、业务关联等） |
| `frame_chunk` | 大文件分片临时表（分片序号、uploadId、MD5 等） |

## 步骤 4：验证上传下载

> 组件引入后自动注册 REST 接口，无需编写 Controller。

启动应用后，使用以下接口测试：

- **上传文件**：`POST /attachment/upload`（multipart/form-data，参数：file、buType、buId）
- **下载文件**：`GET /attachment/download?id={文件ID}`
- **预览文件**：`GET /attachment/preview?id={文件ID}`
- **缩略图**：`GET /attachment/thumbnail?id={文件ID}`

分片上传流程：

1. `POST /attachment/initUploadPart` -- 初始化，获取 uploadId
2. `POST /attachment/uploadPart` -- 逐片上传
3. `POST /attachment/mergeUpload` -- 合并完成

## 步骤 5：（可选）自定义文件用户信息

> 默认实现 `DefaultFileUserService` 返回空映射。如需在附件列表中展示上传者姓名，实现 `FileUserService` 接口。

```java
import com.leatop.cdp.business.file.service.FileUserService;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.Set;

@Service
public class CustomFileUserService implements FileUserService {

    @Override
    public Map<String, String> queryUserName(Set<String> userIds) {
        // 根据用户 ID 集合批量查询用户姓名
        // 返回 userId -> userName 映射
        return Map.of();
    }
}
```

---

## 完成后提醒

1. 切换存储平台只需修改 `default-platform` 配置和对应平台参数，业务代码无需改动
2. 本地存储的 `domain` 建议使用相对路径（如 `/file/`），线上环境通过 Nginx 代理静态文件
3. 生产环境必须配置 `extWhiteList` 和 `contentTypeWhiteList`，不要使用 `*` 放行所有文件类型
4. 数据库存储（`DatabaseFileStorage`）仅适合小文件场景，大文件请使用 MinIO 或 OSS
5. 分片上传的临时数据存储在 `frame_chunk` 表中，建议通过 XXL-Job 定期清理超时分片记录
