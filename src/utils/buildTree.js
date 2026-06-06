/**
 * 构建知识库目录树
 * 使用 Vite 的 import.meta.glob 扫描文档目录
 */

// 使用 import.meta.glob 懒加载所有 Markdown 文件（点击时才加载内容）
const mdModules = import.meta.glob('../../docs/cdp-wiki/**/*.md', {
  query: '?raw',
  import: 'default'
})

/**
 * 从文件路径中提取显示名称
 * @param {string} path - 文件路径
 * @param {boolean} isFile - 是否为文件
 * @returns {string} 显示名称
 */
function getDisplayName(path, isFile) {
  const parts = path.split('/')
  const name = parts[parts.length - 1]
  
  if (isFile) {
    // 移除 .md 后缀，保留有意义的名称
    return name.replace(/\.md$/, '')
  }
  return name
}

/**
 * 构建树形数据结构
 * @returns {Array} 树形节点数组
 */
export function buildTree() {
  const root = []
  const pathMap = new Map()
  
  // 获取所有文件路径并排序
  const paths = Object.keys(mdModules).sort()
  
  paths.forEach(filePath => {
    // 移除 docs/cdp-wiki/ 之前的所有前缀，直接从有意义的目录开始
    const relativePath = filePath.replace(/^.*docs\/cdp-wiki\//, '')
    const parts = relativePath.split('/')
    
    let currentLevel = root
    let currentPath = ''
    
    parts.forEach((part, index) => {
      currentPath = currentPath ? `${currentPath}/${part}` : part
      const isFile = index === parts.length - 1
      
      // 查找当前层级是否已存在该节点
      let existingNode = currentLevel.find(node => node.name === part)
      
      if (!existingNode) {
        existingNode = {
          id: currentPath,
          name: part,
          label: getDisplayName(currentPath, isFile),
          isFile,
          children: isFile ? undefined : [],
          filePath: isFile ? filePath : undefined
        }
        currentLevel.push(existingNode)
      }
      
      if (!isFile) {
        currentLevel = existingNode.children
      }
    })
  })
  
  return root
}

/**
 * 获取第一个可显示的文档路径
 * @param {Array} tree - 树形结构
 * @returns {string|null} 第一个文档的路径
 */
export function getFirstDocPath(tree) {
  for (const node of tree) {
    if (node.isFile) {
      return node.id
    }
    if (node.children && node.children.length > 0) {
      const childPath = getFirstDocPath(node.children)
      if (childPath) return childPath
    }
  }
  return null
}

/**
 * 根据路径查找文档内容
 * @param {Array} tree - 树形结构
 * @param {string} path - 文档路径
 * @returns {Object|null} 文档节点
 */
export function findDocByPath(tree, path) {
  for (const node of tree) {
    if (node.id === path) {
      return node
    }
    if (node.children) {
      const found = findDocByPath(node.children, path)
      if (found) return found
    }
  }
  return null
}

/**
 * 将树形结构转换为 el-tree 组件所需的数据格式
 * @param {Array} tree - 原始树形结构
 * @returns {Array} el-tree 格式的数据
 */
export function toElTreeData(tree) {
  return tree.map(node => ({
    id: node.id,
    label: node.label,
    isFile: node.isFile,
    filePath: node.filePath,
    children: node.children ? toElTreeData(node.children) : undefined
  }))
}

/**
 * 根据搜索关键词过滤树形数据
 * @param {Array} tree - 树形结构
 * @param {string} keyword - 搜索关键词
 * @returns {Array} 过滤后的树形结构
 */
export function filterTree(tree, keyword) {
  if (!keyword) return tree
  
  const lowerKeyword = keyword.toLowerCase()
  
  const filterNode = (nodes) => {
    return nodes
      .reduce((acc, node) => {
        const nameMatch = node.label.toLowerCase().includes(lowerKeyword)
        
        if (node.isFile) {
          if (nameMatch) acc.push({ ...node })
          return acc
        }
        
        // 对于目录节点，递归过滤子节点
        const filteredChildren = node.children ? filterNode(node.children) : []
        
        // 如果目录名匹配或有任何匹配的子节点，保留该目录
        if (nameMatch || filteredChildren.length > 0) {
          acc.push({
            ...node,
            children: filteredChildren.length > 0 ? filteredChildren : (node.children ? [...node.children] : [])
          })
        }
        
        return acc
      }, [])
  }
  
  return filterNode(tree)
}

/**
 * 异步加载文档内容（懒加载）
 * @param {string} filePath - 文档文件路径
 * @returns {Promise<string>} 文档内容
 */
export async function loadDocContent(filePath) {
  const loader = mdModules[filePath]
  if (!loader) return ''
  return await loader()
}

/**
 * 根据路径查找文档节点的 filePath
 * @param {Array} tree - 树形结构
 * @param {string} path - 文档节点 id
 * @returns {string|null} 文件路径
 */
export function findFilePathByPath(tree, path) {
  for (const node of tree) {
    if (node.id === path && node.isFile) {
      return node.filePath
    }
    if (node.children) {
      const found = findFilePathByPath(node.children, path)
      if (found) return found
    }
  }
  return null
}
