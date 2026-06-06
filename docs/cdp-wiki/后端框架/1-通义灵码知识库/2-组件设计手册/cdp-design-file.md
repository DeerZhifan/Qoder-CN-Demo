# CDP 附件管理 设计手册

> 对应使用手册：[cdp-module-file.md](../3-组件使用手册/cdp-module-file.md)
> 通用业务模块架构模式参见：[cdp-design-business-module-pattern.md](cdp-design-business-module-pattern.md)

## 一、设计目标与背景

附件管理是企业应用的通用基础能力，业务场景涵盖表单附件、头像上传、报表文件导出等。`leatop-cdp-business-file` 模块围绕以下目标设计：

1. **存储平台抽象**：基于 x-file-storage 框架封装统一的文件操作 API，切换存储后端（本地/MinIO/OSS/数据库等）只需修改配置，业务代码零改动。
2. **大文件分片上传**：支持断点续传，分片信息持久化到 `frame_chunk` 表，上传中断后可续传已完成的分片。
3. **安全管控**：文件类型白名单、MIME 类型校验、文件大小限制三重防护，防止恶意文件上传。
4. **缩略图自动生成**：图片和 PDF 文件上传时自动生成缩略图，支持前端列表预览场景。

> 设计决策：选择 x-file-storage（Dromara 社区开源项目）作为底层存储抽象，而非自研适配层。x-file-storage 已适配 30+ 存储平台，CDP 在其基础上扩展了数据库存储（`DatabaseFileStorage`）、自定义文件记录器（`AttachFileRecorder`）和安全校验切面（`CustomFileStorageAspect`），兼顾了生态兼容与框架定制需求。

## 二、整体架构

```
AttachmentController (REST 接口：上传/下载/预览/删除)
  |
  v
AttachmentBusiness / AttachmentBusinessImpl (业务编排)
  |
  +---> FileStorageService (x-file-storage 核心服务)
  |       |
  |       +---> FileStorage 实现（LocalPlus/MinIO/AliyunOSS/Database/...）
  |       +---> AttachFileRecorder (implements FileRecorder，持久化文件元数据)
  |       +---> CustomFileStorageAspect (上传拦截：安全校验+缩略图生成)
  |
  +---> AttachmentFileService / AttachmentFileServiceImpl (附件元数据 CRUD)
  |       +---> AttachmentDAO (操作 frame_attach 表)
  |
  +---> ChunkDAO (操作 frame_chunk 表，管理分片)
  +---> FileStoragePlatform (查询可用存储平台)
  +---> SpringFileStorageProperties (统一配置)
```

**上传流程**：`AttachmentBusinessImpl.upload()` 校验文件大小和扩展名白名单 -> 调用 `FileStorageService.of(file)` 设置存储路径和平台 -> x-file-storage 触发 `CustomFileStorageAspect.uploadAround()` 执行 MIME 校验和缩略图生成 -> 底层 `FileStorage` 实现执行实际存储 -> `AttachFileRecorder.save()` 将文件元数据持久化到 `frame_attach` 表。

**下载流程**：`download()` 根据文件 ID 从 `AttachmentFileService` 获取 `FileInfo` -> 调用 `FileStorageService.download(fileInfo)` 从对应存储平台读取文件流 -> 写入 HTTP 响应输出流。多文件下载时自动打包为 ZIP。

## 三、核心设计模式

### 策略模式 -- 存储平台切换

`FileStorageType` 枚举定义了所有支持的存储类型（database、local、aliyunOss、huaweiObs、tencentCos、baiduBos、MiniIO、FastDFS、FTP 等共 12 种）。`SpringFileStorageProperties` 通过 `@ConfigurationProperties(prefix = "cdp.file-storage")` 加载每个平台的配置列表，并通过 `allPlatforms()` 方法聚合所有已启用平台的映射关系。`FileStoragePlatform` 对外提供平台查询能力。

运行时通过 `defaultPlatform` 配置或 API 参数中的 `platform` 字段选择存储后端，`AttachmentBusinessImpl.getPlatform()` 在参数为空时回退到默认平台。

### 记录器模式 -- 文件元数据持久化

`AttachFileRecorder` 实现 x-file-storage 的 `FileRecorder` 接口，作为文件生命周期事件的回调处理器：

- `save()`：将 `FileInfo` 转换为 `AttachmentPO` 并 insert，同时填充用户上下文（租户 ID、创建人、所属公司）
- `update()`：分片上传完成时更新文件元数据
- `getByUrl()`：根据 ID 查询文件信息（x-file-storage 内部下载/删除时回调）
- `delete()`：物理删除文件元数据记录
- `deleteFilePartByUploadId()`：清理分片上传的临时 chunk 记录

> 设计决策：`AttachFileRecorder` 通过 `CdpTokenHolder` 和 `IUserHelper` 双通道获取用户信息。前者用于微服务网关传递的 Token 上下文，后者用于单体模式下的 SA-Token 会话。这保证了两种部署模式下文件归属信息的正确填充。

### 拦截器模式 -- 上传安全与缩略图

`CustomFileStorageAspect` 实现 `FileStorageAspect` 接口，在 `uploadAround()` 中执行两项横切逻辑：

1. **文件类型校验**：当 `checkContentType=true` 且白名单非 `*` 时，通过 `ContentTypeDetect` 检测实际 MIME 类型，与 `extWhiteList` 和 `contentTypeWhiteList` 交叉验证，防止扩展名伪造。
2. **缩略图生成**：对 `image/*` 和 `application/pdf` 类型文件，调用 `FileToThumbnail` 工具生成缩略图字节数组，设置到 `UploadPretreatment` 中随主文件一并存储。

## 四、关键类说明

### AttachmentBusinessImpl

业务编排核心，标注 `@BusinessService`。主要方法：

- `upload()` / `uploadFile()`：普通文件上传，前者记录业务关联（buType/buId），后者不记录元数据到 `frame_attach`
- `initUploadPart()` / `uploadPart()` / `mergeUpload()`：分片上传三步流程 -- 初始化获取 uploadId、逐片上传并记录 chunk、合并完成
- `checkUploadPart()`：断点续传检查，查询已上传分片列表，若 uploadId 不存在则自动初始化
- `download()`：支持单文件直出和多文件 ZIP 打包下载，同名文件自动加序号后缀
- `preview()` / `showImage()` / `thumbnail()`：文件预览和图片/缩略图展示
- `copyFiles()`：附件复制，用于业务单据复制场景

### DatabaseFileStorage

CDP 扩展的数据库存储平台，实现 `FileStorage` 接口。`save()` 将文件内容 Base64 编码后存入 `AttachContentPO.content` 字段（对应 `frame_attach_content` 表），适用于文件量小且不便部署对象存储的场景。不支持缩略图下载（`downloadTh()` 抛出 `UnsupportedOperationException`）。

### SpringFileStorageProperties

统一配置类，前缀 `cdp.file-storage`。除继承 x-file-storage 的平台配置外，CDP 扩展了以下属性：`checkContentType`（是否校验 MIME 类型）、`contentTypeWhiteList`（MIME 白名单）、`extWhiteList`（扩展名白名单，默认 `*` 放行所有）、`maxSize`（单文件大小上限，默认 10MB）、`skipUpload`（是否跳过分片上传，用于调试）、`database`（数据库存储开关）。

### 数据表结构

- **frame_attach**：附件元数据，`AttachmentPO` 映射。关键字段：`id`、`fileName`、`originalFileName`、`fileSize`、`contentType`、`fileDir`（存储路径/URL）、`platform`（存储平台标识）、`fileType`（`FileStorageType` 枚举值）、`fileMd5`（文件哈希，用于秒传判断）、`uploadId`（分片上传标识）、`uploadStatus`（上传状态：1=上传中，2=完成）、`buid`/`butype`（业务关联）、`tenantId`、`compId`、`createUser`
- **frame_chunk**：分片临时记录，`ChunkPO` 映射。关键字段：`uploadId`、`chunkNumber`（分片序号）、`currentChunkSize`、`md5`（分片 ETag）、`identifier`（文件标识符，用于断点续传匹配）、`filename`、`relativePath`（平台标识）

## 五、扩展机制

1. **新增存储平台**：CDP 已适配 x-file-storage 支持的全部平台。如需对接自研存储系统，实现 `FileStorage` 接口并在 `FileStorageAutoConfiguration` 中注册即可。`DatabaseFileStorage` 是一个完整的扩展参考。
2. **文件处理扩展**：`CustomFileStorageAspect` 的 `uploadAround()` 方法可作为扩展点，在上传前后增加水印、病毒扫描、OCR 识别等处理逻辑。
3. **业务关联扩展**：`FileBindBusDTO` 提供了 `buType`/`buId` 两个字段将附件与业务实体关联。如需更复杂的关联（如版本管理），可扩展 `AttachmentPO` 增加版本号字段。
4. **用户信息扩展**：`FileUserService` 接口提供 `queryUserName()` 方法，用于在附件列表中展示上传者姓名。默认实现 `DefaultFileUserService` 返回空映射，项目可自行实现该接口对接用户体系。

## 六、模块协作（简要）

- **leatop-cdp-common-auth**：上传/下载接口受 SA-Token 认证保护，附件元数据中的用户信息通过 `IUserHelper` 和 `CdpTokenHolder` 获取。
- **leatop-cdp-business-system**：系统设置中的 Logo 上传依赖文件模块。
- **leatop-cdp-base-echo**：附件信息可通过 Echo 机制回显文件名、下载地址等。

## 七、设计权衡与约束（简要）

- **数据库存储的局限性**：`DatabaseFileStorage` 使用 Base64 编码存储文件内容，体积膨胀约 33%，且大文件会导致数据库单行数据过大。此方案仅适合小文件（如电子签章、小图标）和无对象存储基础设施的环境。
- **分片上传的清理机制**：`checkUploadPart()` 中通过 1 小时时间窗口过滤有效分片记录，超时的分片数据需要定期清理。当前未内置定时清理任务，建议项目通过 XXL-Job 定期扫描并清理 `frame_chunk` 表中的过期记录。
- **多文件下载的内存消耗**：ZIP 打包下载在服务端内存中完成，大量大文件同时下载可能导致 OOM。生产环境建议限制单次下载的文件数量和总大小。
