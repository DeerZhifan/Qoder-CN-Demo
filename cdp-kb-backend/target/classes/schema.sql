-- 分类表
CREATE TABLE IF NOT EXISTS category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '分类名称',
    parent_id BIGINT DEFAULT 0 COMMENT '父分类ID，0表示根节点',
    sort_order INT DEFAULT 0 COMMENT '排序号',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 文档表
CREATE TABLE IF NOT EXISTS document (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL COMMENT '文档标题',
    category_id BIGINT NOT NULL COMMENT '所属分类ID',
    content TEXT COMMENT 'Markdown内容',
    status TINYINT DEFAULT 0 COMMENT '状态: 0-草稿, 1-已发布, 2-已下线',
    version INT DEFAULT 1 COMMENT '当前版本号',
    published_version INT DEFAULT 0 COMMENT '已发布的版本号',
    deleted TINYINT DEFAULT 0 COMMENT '软删除标记: 0-未删除, 1-已删除',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    published_at TIMESTAMP NULL COMMENT '发布时间'
);

-- 文档版本表
CREATE TABLE IF NOT EXISTS document_version (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id BIGINT NOT NULL COMMENT '文档ID',
    version INT NOT NULL COMMENT '版本号',
    content TEXT COMMENT '该版本的Markdown内容',
    change_log VARCHAR(500) COMMENT '变更说明',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_doc_version (document_id, version)
);

-- 初始化示例数据
INSERT INTO category (name, parent_id, sort_order) VALUES 
('前端框架', 0, 1),
('后端框架', 0, 2);
