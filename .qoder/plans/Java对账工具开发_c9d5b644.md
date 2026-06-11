# Java 对账工具开发计划

## Task 1: 编写 TollReconcile.java
- 单文件实现，支持 `java TollReconcile.java` 直接运行
- 内部包含 TollRecord 记录类（交易号、车牌、金额、时间）
- 生成方法：生成各 50 行的 `toll.csv` 和 `bank.csv`
  - 包含正常匹配记录、仅 toll 存在、仅 bank 存在、金额不一致四种情况
- 对账方法：读取两份 CSV，按交易号比对
  - 找出"仅 toll 存在"、"仅 bank 存在"、"金额不一致"的记录
- 报告方法：结果打印到控制台，同时写出 `reconcile-report.md`

## Task 2: 编译并运行验证
- 执行 `java src/TollReconcile.java`（或先 cd 到 src 再运行）
- 检查控制台输出和生成的 reconcile-report.md
- 确认 toll.csv、bank.csv 也已生成

## 文件清单
- `src/TollReconcile.java`（新增）
- `toll.csv`（运行后生成）
- `bank.csv`（运行后生成）
- `reconcile-report.md`（运行后生成）