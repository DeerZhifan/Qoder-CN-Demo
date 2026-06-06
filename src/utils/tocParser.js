/**
 * TOC (Table of Contents) 解析工具
 * 从 Markdown 内容中提取 H1-H3 标题生成目录
 */

/**
 * 将标题文本转换为规范的基础 ID
 * @param {string} text - 标题文本
 * @returns {string} 基础 ID
 */
function toBaseId(text) {
  return text
    .toLowerCase()
    .replace(/[^\w\u4e00-\u9fa5\s-]/g, '')
    .replace(/\s+/g, '-')
    .replace(/^-+|-+$/g, '')
}

/**
 * 根据 idCount 计数器生成唯一 heading ID（公共函数，parseToc 与 anchorPlugin 共用）
 * @param {string} text - 标题文本
 * @param {Object} idCount - 计数器对象（会被原地修改）
 * @returns {string} 唯一锚点 ID
 */
function generateHeadingId(text, idCount) {
  const baseId = toBaseId(text)
  idCount[baseId] = (idCount[baseId] || 0) + 1
  return idCount[baseId] === 1 ? baseId : `${baseId}-${idCount[baseId] - 1}`
}

/**
 * 从 Markdown 原始文本中解析 TOC
 * @param {string} markdown - Markdown 原始文本
 * @returns {Array} TOC 条目数组 [{ level, text, id }]
 */
export function parseToc(markdown) {
  if (!markdown) return []
  
  const toc = []
  const lines = markdown.split('\n')
  const idCount = {}
  
  lines.forEach(line => {
    // 匹配 H1-H3 标题（# ## ###）
    const match = line.match(/^(#{1,3})\s+(.+)$/)
    if (match) {
      const level = match[1].length
      const text = match[2].trim()
      const id = generateHeadingId(text, idCount)
      
      toc.push({
        level,
        text,
        id
      })
    }
  })
  
  return toc
}

/**
 * 为 Markdown 渲染后的 HTML 中的标题添加锚点 ID
 * 这个函数作为 markdown-it 的插件使用
 * @param {Object} md - markdown-it 实例
 */
export function anchorPlugin(md) {
  md.renderer.rules.heading_open = (tokens, idx, options, env, self) => {
    const token = tokens[idx]
    const level = parseInt(token.tag.slice(1))
    
    // 只处理 H1-H3
    if (level > 3) {
      return self.renderToken(tokens, idx, options)
    }
    
    // 获取标题文本（下一个 token）
    const textToken = tokens[idx + 1]
    const text = textToken ? textToken.content : ''
    
    // 使用 env 挂载 idCount，跟随每次 render 调用的生命周期
    if (!env.__idCount) env.__idCount = {}
    const id = generateHeadingId(text, env.__idCount)
    
    // 添加到 token 属性
    token.attrSet('id', id)
    
    return self.renderToken(tokens, idx, options)
  }
}


