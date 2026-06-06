# 如何使用 CDP 字典管理功能

## 概述

字典管理提供系统内键值对数据的维护能力，支持树形字典结构、按类型查询、缓存加速。常用于下拉框选项、状态码映射等场景。

## 核心接口

**Controller 路径前缀：** `/system/dictionary`

| 接口 | 方法 | 说明 |
|------|------|------|
| `/system/dictionary/add` | POST | 添加/修改字典值 |
| `/system/dictionary/update` | POST | 更新字典值 |
| `/system/dictionary/delete` | POST | 删除字典值 |
| `/system/dictionary/listPage` | POST | 分页查询字典 |
| `/system/dictionary/options` | GET | 根据父编码获取子选项列表 |
| `/system/dictionary/listByParentCode` | POST | 根据父编码列出可用字典 |
| `/system/dictionary/childrenByParentId` | POST | 列出字典（可含子树） |
| `/system/dictionary/cacheChildren` | POST | 缓存字典子项 |

## 使用示例

### 前端获取下拉选项

```javascript
// GET /system/dictionary/options?parentCode=user_status
// Response: [{ "code": "1", "name": "启用" }, { "code": "0", "name": "禁用" }]
```

### 后端获取字典值

```java
import com.leatop.cdp.system.business.DictionaryBusiness;

@Autowired
private DictionaryBusiness dictionaryBusiness;

// 获取字典选项
List<DictionaryDTO> options = dictionaryBusiness.getOptionsByParentCode("user_status");
```

### 配合字典回显组件

字典数据可与 `leatop-cdp-base-echo` 组件配合，通过 `@Echo` 注解自动将字典码转为显示文本：

```java
@Echo(api = "dictionaryEchoServiceImpl", parentId = "user_status", ref = "statusText")
private Integer status;
private String statusText;  // 自动填充
```

## 注意事项

> 注意：新版使用统一的 `DictionaryController`，旧版 `DictionaryKindController` 和 `DictionaryValueController` 已标记 `@Deprecated`。

> 注意：字典支持树形结构，通过 `parentCode` 关联父子关系。

> 注意：高频访问的字典建议调用 `/cacheChildren` 预加载到缓存，提升查询性能。
