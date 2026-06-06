# 如何使用 CDP 自定义表单组件

## 概述

自定义表单组件（`leatop-cdp-business-form`）提供低代码表单设计和管理能力，支持可视化设计表单、配置列表页面、与工作流集成，减少业务开发中的重复编码工作。

## 启用方式

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-business-form-boot-starter</artifactId>
</dependency>
```

## 核心功能

### 表单设计

- **DesignForm**：表单设计器，定义表单布局和字段
- **DesignFormButton**：表单按钮配置（提交、保存、重置等）
- **FormMetaObject**：表单元数据对象定义
- **FormMetaField**：表单字段元数据定义

### 列表设计

- **FormDesignList**：列表页面设计（查询条件、数据列、操作按钮）
- **FormDesignListButton**：列表按钮配置
- **FormDesignListColumn**：列表列配置

### 数据管理

- **FormRecord**：表单数据记录
- **FormValueSource**：表单值来源配置（静态值、字典、接口等）
- **FormDatabase**：表单关联的数据库配置
- **FormCatalog**：表单分类目录

### 扩展功能

- **FormReport**：表单报表
- **FormWorkflow**：表单与工作流集成

## 注意事项

> 注意：表单设计器通过前端可视化界面操作，后端提供配置存储和数据管理 API。

> 注意：自定义表单与工作流集成后，可实现"表单 + 审批流"的完整业务流程。

> 注意：表单元数据（MetaObject/MetaField）定义了表单的数据结构，修改后需同步更新关联的数据库表。
