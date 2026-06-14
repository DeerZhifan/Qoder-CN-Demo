---
trigger: model_decision
description: 当用户要求为 service 业务逻辑生成或补充单元测试时应用`（对照项目已有的 `KbDocumentServiceTest`）
---
JUnit5 + Mockito，Mapper 用 @Mock 注入；
命名 should_xxx_when_yyy；
每个用例只验一个行为；
必须覆盖状态机非法流转（如对非 DRAFT 文档调用 publishDocument 应抛 IllegalStateException）与资源不存在（NoSuchElementException）分支