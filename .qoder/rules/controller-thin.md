---
trigger: glob
glob: backend/**/controller/*.java
---
Controller 必须薄——只做参数绑定与 `@Valid` 校验、调用 Service、用 `Result` 包装返回；
禁止在 Controller 写业务逻辑、事务或数据访问；
每个公开接口方法必须有 Javadoc（参数/返回）。