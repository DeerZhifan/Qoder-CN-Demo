-- ============================================
-- 知识库管理系统 - 数据库初始化脚本
-- 数据库: H2 (MySQL兼容模式)
-- ============================================

-- 1. 知识库分类表
CREATE TABLE IF NOT EXISTS kb_category (
    id BIGINT PRIMARY KEY COMMENT '主键ID（雪花算法生成）',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父分类ID，0表示根节点',
    name VARCHAR(100) NOT NULL COMMENT '分类名称',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序号',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT NOT NULL DEFAULT 0 COMMENT '逻辑删除标识：0-未删除，1-已删除'
);

-- 创建索引
CREATE INDEX idx_kb_category_parent_id ON kb_category(parent_id);
CREATE INDEX idx_kb_category_deleted ON kb_category(deleted);

-- 2. 知识库文档表
CREATE TABLE IF NOT EXISTS kb_document (
    id BIGINT PRIMARY KEY COMMENT '主键ID（雪花算法生成）',
    category_id BIGINT NOT NULL COMMENT '分类ID',
    title VARCHAR(200) NOT NULL COMMENT '文档标题',
    content TEXT COMMENT '文档内容（Markdown格式）',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '文档状态：DRAFT-草稿，PUBLISHED-已发布，OFFLINE-已下线',
    version INT NOT NULL DEFAULT 1 COMMENT '当前版本号',
    publish_time TIMESTAMP COMMENT '发布时间',
    create_by VARCHAR(50) COMMENT '创建人',
    update_by VARCHAR(50) COMMENT '更新人',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT NOT NULL DEFAULT 0 COMMENT '逻辑删除标识：0-未删除，1-已删除'
);

-- 创建索引
CREATE INDEX idx_kb_document_category_id ON kb_document(category_id);
CREATE INDEX idx_kb_document_status ON kb_document(status);
CREATE INDEX idx_kb_document_deleted ON kb_document(deleted);
CREATE INDEX idx_kb_document_create_time ON kb_document(create_time);

-- 3. 知识库文档版本表
CREATE TABLE IF NOT EXISTS kb_document_version (
    id BIGINT PRIMARY KEY COMMENT '主键ID（雪花算法生成）',
    document_id BIGINT NOT NULL COMMENT '文档ID',
    version INT NOT NULL COMMENT '版本号',
    title VARCHAR(200) NOT NULL COMMENT '版本文档标题',
    content TEXT COMMENT '版本文档内容（Markdown格式）',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_by VARCHAR(50) COMMENT '创建人'
);

-- 创建索引
CREATE INDEX idx_kb_document_version_doc_id ON kb_document_version(document_id);
CREATE INDEX idx_kb_document_version_version ON kb_document_version(document_id, version);

-- 4. 知识库文档标签表
CREATE TABLE IF NOT EXISTS kb_document_tag (
    id BIGINT PRIMARY KEY COMMENT '主键ID（雪花算法生成）',
    document_id BIGINT NOT NULL COMMENT '文档ID',
    tag_name VARCHAR(50) NOT NULL COMMENT '标签名称',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT NOT NULL DEFAULT 0 COMMENT '逻辑删除标识：0-未删除，1-已删除'
);

-- 创建索引
CREATE INDEX idx_kb_document_tag_document_id ON kb_document_tag(document_id);
CREATE INDEX idx_kb_document_tag_tag_name ON kb_document_tag(tag_name);
CREATE INDEX idx_kb_document_tag_deleted ON kb_document_tag(deleted);
