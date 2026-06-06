<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'
import DOMPurify from 'dompurify'
import { anchorPlugin } from '@/utils/tocParser.js'

const props = defineProps({
  content: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['scroll'])

// 内容容器引用
const contentRef = ref(null)

// 当前活跃的 heading ID（用于 scrollspy）
const activeHeadingId = ref('')

// Intersection Observer 实例
let observer = null

// 初始化 markdown-it
const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
  highlight: function (str, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return `<pre class="hljs"><code>${hljs.highlight(str, { language: lang }).value}</code></pre>`
      } catch (__) {}
    }
    return `<pre class="hljs"><code>${md.utils.escapeHtml(str)}</code></pre>`
  }
})

// 应用锚点插件
md.use(anchorPlugin)

// 渲染后的 HTML（含 XSS 净化与异常兜底）
const renderedHtml = computed(() => {
  if (!props.content) {
    return '<p class="empty-content">请从左侧选择一篇文档</p>'
  }
  try {
    const raw = md.render(props.content)
    return DOMPurify.sanitize(raw)
  } catch (e) {
    console.error('Markdown 渲染失败:', e)
    return '<p class="render-error">文档渲染失败，请检查文档格式</p>'
  }
})

// 设置 Intersection Observer
const setupObserver = () => {
  // 清理旧的 observer
  if (observer) {
    observer.disconnect()
    observer = null
  }
  
  if (!contentRef.value) return
  
  // 获取所有 h1, h2, h3 元素
  const headings = contentRef.value.querySelectorAll('h1, h2, h3')
  if (headings.length === 0) return
  
  // 创建 Intersection Observer
  const options = {
    root: contentRef.value.parentElement,
    rootMargin: '-10% 0px -80% 0px',
    threshold: 0
  }
  
  observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        activeHeadingId.value = entry.target.id
        emit('scroll', entry.target.id)
      }
    })
  }, options)
  
  // 观察所有 heading
  headings.forEach(heading => {
    if (heading.id) {
      observer.observe(heading)
    }
  })
}

// 滚动到指定锚点
const scrollToAnchor = (anchorId) => {
  if (!contentRef.value) return
  
  const target = contentRef.value.querySelector(`#${CSS.escape(anchorId)}`)
  if (target) {
    target.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

// 监听内容变化，重新设置 observer
watch(() => props.content, () => {
  nextTick(() => {
    setupObserver()
  })
})

onMounted(() => {
  nextTick(() => {
    setupObserver()
  })
})

onUnmounted(() => {
  if (observer) {
    observer.disconnect()
  }
})

// 暴露方法供父组件调用
defineExpose({
  scrollToAnchor
})
</script>

<template>
  <div class="wiki-content">
    <div class="content-header">
      <slot name="header"></slot>
    </div>
    <div class="content-body markdown-body" ref="contentRef" v-html="renderedHtml"></div>
  </div>
</template>

<style scoped>
.wiki-content {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.content-header {
  flex-shrink: 0;
  padding: 16px 24px;
  border-bottom: 1px solid #e4e7ed;
  background: #fff;
}

.content-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px 32px;
  background: #fff;
}

.empty-content {
  color: #909399;
  text-align: center;
  padding: 100px 0;
  font-size: 16px;
}
</style>

<style>
/* github-markdown-css 样式覆盖 */
.markdown-body {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "Noto Sans", Helvetica, Arial, sans-serif;
  font-size: 15px;
  line-height: 1.7;
  word-wrap: break-word;
  color: #24292f;
}

.markdown-body h1,
.markdown-body h2,
.markdown-body h3,
.markdown-body h4,
.markdown-body h5,
.markdown-body h6 {
  margin-top: 24px;
  margin-bottom: 16px;
  font-weight: 600;
  line-height: 1.25;
}

.markdown-body h1 {
  font-size: 2em;
  padding-bottom: 0.3em;
  border-bottom: 1px solid #d0d7de;
}

.markdown-body h2 {
  font-size: 1.5em;
  padding-bottom: 0.3em;
  border-bottom: 1px solid #d0d7de;
}

.markdown-body h3 {
  font-size: 1.25em;
}

.markdown-body p {
  margin-top: 0;
  margin-bottom: 16px;
}

.markdown-body ul,
.markdown-body ol {
  margin-top: 0;
  margin-bottom: 16px;
  padding-left: 2em;
}

.markdown-body li {
  margin-top: 0.25em;
}

.markdown-body code {
  padding: 0.2em 0.4em;
  margin: 0;
  font-size: 85%;
  background-color: rgba(175, 184, 193, 0.2);
  border-radius: 6px;
  font-family: ui-monospace, SFMono-Regular, "SF Mono", Menlo, Consolas, "Liberation Mono", monospace;
}

.markdown-body pre {
  padding: 16px;
  overflow: auto;
  font-size: 85%;
  line-height: 1.45;
  background-color: #f6f8fa;
  border-radius: 6px;
  margin-bottom: 16px;
}

.markdown-body pre code {
  padding: 0;
  margin: 0;
  font-size: 100%;
  background-color: transparent;
  border: 0;
}

.markdown-body blockquote {
  margin: 0 0 16px 0;
  padding: 0 1em;
  color: #57606a;
  border-left: 0.25em solid #d0d7de;
}

.markdown-body table {
  display: block;
  width: max-content;
  max-width: 100%;
  overflow: auto;
  border-spacing: 0;
  border-collapse: collapse;
  margin-bottom: 16px;
}

.markdown-body table th,
.markdown-body table td {
  padding: 6px 13px;
  border: 1px solid #d0d7de;
}

.markdown-body table th {
  font-weight: 600;
  background-color: #f6f8fa;
}

.markdown-body table tr:nth-child(2n) {
  background-color: #f6f8fa;
}

.markdown-body a {
  color: #0969da;
  text-decoration: none;
}

.markdown-body a:hover {
  text-decoration: underline;
}

.markdown-body img {
  max-width: 100%;
  height: auto;
  border-radius: 6px;
}

.markdown-body hr {
  height: 0.25em;
  padding: 0;
  margin: 24px 0;
  background-color: #d0d7de;
  border: 0;
}

/* 代码高亮样式 */
.markdown-body .hljs {
  background: #f6f8fa;
  padding: 16px;
  border-radius: 6px;
  overflow-x: auto;
}
</style>
