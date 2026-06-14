# MCP H2 服务配置指南

## 已完成的修改

### 1. 数据库配置修改 ✅
已将 H2 从内存数据库改为文件数据库：

**文件**: `backend/src/main/resources/application.yml`

```yaml
spring:
  datasource:
    driver-class-name: org.h2.Driver
    url: jdbc:h2:file:./data/kbdb;MODE=MySQL;DB_CLOSE_DELAY=-1
    username: sa
    password: 
```

**说明**:
- `jdbc:h2:file:./data/kbdb` - 使用文件数据库，数据保存在 `backend/data/kbdb.mv.db`
- `MODE=MySQL` - MySQL 兼容模式
- `DB_CLOSE_DELAY=-1` - 保持数据库连接

---

## 需要手动完成的配置

### 2. 修改 MCP 配置文件

**文件位置**: `C:\Users\yangzf\AppData\Roaming\QoderCN\SharedClientCache\mcp.json`

**当前配置**:
```json
"h2": { 
  "command": "jbang", 
  "args": ["jdbc@quarkiverse/quarkus-mcp-servers"] 
}
```

**修改为**:
```json
"h2": { 
  "command": "jbang", 
  "args": [
    "jdbc@quarkiverse/quarkus-mcp-servers",
    "--url", "jdbc:h2:file:D:/BaiduSyncdisk/files/利通资料/研发中心/20.部门工作/通用技术研发平台/2026年/AI编程助手深化应用/深化应用/任务四/Qoder-CN-Demo/backend/data/kbdb;MODE=MySQL",
    "--user", "sa",
    "--password", ""
  ] 
}
```

**注意**: 
- 路径中的反斜杠 `\` 需要改为正斜杠 `/`
- 确保路径与你的实际项目路径一致

---

## 重启步骤

### 1. 重启后端服务
停止当前运行的后端服务，然后重新启动：

```bash
cd backend
mvn spring-boot:run
```

启动后会创建 `backend/data/kbdb.mv.db` 文件，并自动执行 `schema.sql` 创建表结构。

### 2. 重启 Qoder
完全退出 Qoder 并重新启动，让 MCP 配置生效。

### 3. 验证 MCP 连接
在 Qoder 中调用 MCP h2 服务：

```
查询数据库表列表
```

应该能看到 `kb_category`, `kb_document`, `kb_document_version`, `kb_document_tag` 等业务表。

---

## 常见问题

### Q1: 如果 jbang 还未安装？
需要先安装 jbang：

**Windows PowerShell**:
```powershell
Set-ExecutionPolicy Bypass -Scope CurrentUser
iex "& { $(irm https://ps.jbang.dev) }"
```

**验证安装**:
```bash
jbang --version
```

### Q2: 数据库文件在哪里？
数据库文件位于：
```
backend/data/kbdb.mv.db
backend/data/kbdb.trace.db (日志文件)
```

### Q3: 如何查看数据库内容？
可以通过以下方式：
1. **H2 控制台**: http://localhost:8081/h2-console
   - JDBC URL: `jdbc:h2:file:./data/kbdb;MODE=MySQL`
   - 用户名: `sa`
   - 密码: (留空)

2. **MCP 服务**: 通过 Qoder 的 MCP h2 工具查询

3. **后端 API**: 通过前端页面调用后端接口

---

## 注意事项

⚠️ **重要提示**:
1. 文件数据库会持久化保存数据，删除 `backend/data/` 目录会清空所有数据
2. 确保只有一个进程访问数据库文件（Spring Boot 应用运行时，MCP 也可以读取）
3. 如果需要重置数据库，删除 `backend/data/` 目录后重启应用即可
