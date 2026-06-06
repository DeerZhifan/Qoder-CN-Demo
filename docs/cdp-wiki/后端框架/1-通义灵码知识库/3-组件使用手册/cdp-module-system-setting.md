# 如何使用 CDP 系统配置功能

## 概述

系统配置包含三部分：系统设置（界面、Logo 等）、安全设置（密码策略、登录策略）和自动编号规则。

## 核心接口

### 系统设置

**Controller 路径前缀：** `/system/sysSetting`

| 接口 | 方法 | 说明 |
|------|------|------|
| `/system/sysSetting/save` | POST | 保存系统设置 |
| `/system/sysSetting/getSettingByVisitType` | GET | 按设备类型获取设置 |
| `/system/sysSetting/public` | GET | 获取公开设置（`dt`: 0=PC, 1=移动端） |
| `/system/sysSetting/getSettingByCompanyId` | POST | 按公司获取设置 |

### 安全设置

**Controller 路径前缀：** `/system/securitySetting`

| 接口 | 方法 | 说明 |
|------|------|------|
| `/system/securitySetting/setSetting` | POST | 保存安全策略 |
| `/system/securitySetting/getSetting` | POST | 获取当前安全设置 |
| `/system/securitySetting/getAllSetting` | POST | 获取所有安全设置 |
| `/system/securitySetting/getEnableSecuritySetting` | POST | 获取已启用的安全策略 |

安全设置内容包括：密码复杂度要求、密码有效期、登录失败锁定策略、会话超时时间等。

### 自动编号

**Controller 路径前缀：** `/system/autonumber`

| 接口 | 方法 | 说明 |
|------|------|------|
| `/system/autonumber/add` | POST | 创建编号规则 |
| `/system/autonumber/update` | POST | 更新编号规则 |
| `/system/autonumber/delete/{ids}` | POST | 删除编号规则 |
| `/system/autonumber/listPage` | POST | 分页查询编号规则 |
| `/system/autonumber/dataResource/list` | POST | 查询所有编号规则 |

自动编号用于生成业务单据号、流水号等，支持自定义前缀、日期格式、序列位数等。

## 注意事项

> 注意：`/public` 接口在认证白名单中，无需登录即可访问，用于登录页展示系统名称和 Logo。

> 注意：安全设置修改后立即生效，影响所有用户的登录和密码策略。

> 注意：自动编号规则在并发场景下保证唯一性，框架内部使用分布式锁保护序列生成。
