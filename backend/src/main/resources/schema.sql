-- 知识库管理后台数据库表结构
-- H2数据库兼容语法

-- ============================================
-- 分类表 (kb_category)
-- ============================================
CREATE TABLE IF NOT EXISTS kb_category (
    id BIGINT PRIMARY KEY,
    parent_id BIGINT DEFAULT 0 COMMENT '父分类ID,0表示根节点',
    name VARCHAR(100) NOT NULL COMMENT '分类名称',
    sort_order INT DEFAULT 0 COMMENT '排序号',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除:0-未删除,1-已删除'
);

CREATE INDEX IF NOT EXISTS idx_parent_id ON kb_category(parent_id);
CREATE INDEX IF NOT EXISTS idx_deleted ON kb_category(deleted);

-- ============================================
-- 文档表 (kb_document)
-- ============================================
CREATE TABLE IF NOT EXISTS kb_document (
    id BIGINT PRIMARY KEY,
    category_id BIGINT NOT NULL COMMENT '分类ID',
    title VARCHAR(200) NOT NULL COMMENT '文档标题',
    content TEXT COMMENT '文档内容(Markdown)',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态:DRAFT/PUBLISHED/OFFLINE',
    version INT DEFAULT 1 COMMENT '当前版本号',
    publish_time TIMESTAMP COMMENT '发布时间',
    create_by VARCHAR(50) DEFAULT 'system' COMMENT '创建人',
    update_by VARCHAR(50) DEFAULT 'system' COMMENT '更新人',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除:0-未删除,1-已删除'
);

CREATE INDEX IF NOT EXISTS idx_category_id ON kb_document(category_id);
CREATE INDEX IF NOT EXISTS idx_status ON kb_document(status);
CREATE INDEX IF NOT EXISTS idx_deleted ON kb_document(deleted);
CREATE INDEX IF NOT EXISTS idx_create_time ON kb_document(create_time);

-- ============================================
-- 文档版本表 (kb_document_version)
-- ============================================
CREATE TABLE IF NOT EXISTS kb_document_version (
    id BIGINT PRIMARY KEY,
    document_id BIGINT NOT NULL COMMENT '文档ID',
    version INT NOT NULL COMMENT '版本号',
    title VARCHAR(200) NOT NULL COMMENT '版本文档标题',
    content TEXT COMMENT '版本文档内容',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_by VARCHAR(50) DEFAULT 'system' COMMENT '创建人'
);

CREATE INDEX IF NOT EXISTS idx_doc_id ON kb_document_version(document_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_doc_version ON kb_document_version(document_id, version);

-- ============================================
-- 初始化测试数据（可选）
-- ============================================
-- INSERT INTO kb_category (id, parent_id, name, sort_order) VALUES 
--     (1, 0, '技术文档', 1),
--     (2, 1, '前端开发', 1),
--     (3, 1, '后端开发', 2);

