<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Document, Reading } from '@element-plus/icons-vue'

const route = useRoute()

// 根据当前路由高亮菜单
const activeMenu = computed(() => {
  if (route.path.startsWith('/knowledge-base')) {
    return '/knowledge-base/list'
  }
  if (route.path.startsWith('/wiki')) {
    return '/wiki'
  }
  return ''
})
</script>

<template>
  <div id="app">
    <el-container>
      <!-- 顶部导航栏 -->
      <el-header class="app-header">
        <div class="header-left">
          <h2>CDP知识库平台</h2>
        </div>
        <div class="header-right">
          <el-menu mode="horizontal" :default-active="activeMenu" router>
            <el-menu-item index="/knowledge-base/list">
              <el-icon><Document /></el-icon>
              <span>知识库管理</span>
            </el-menu-item>
            <el-menu-item index="/wiki">
              <el-icon><Reading /></el-icon>
              <span>Wiki浏览</span>
            </el-menu-item>
          </el-menu>
        </div>
      </el-header>

      <!-- 主内容区 -->
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<style scoped>
#app {
  height: 100vh;
}
.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #409eff;
  color: white;
  padding: 0 20px;
}
.header-left h2 {
  margin: 0;
  font-size: 20px;
}
.header-right .el-menu {
  background-color: transparent;
  border: none;
}
.header-right .el-menu-item {
  color: white;
}
.header-right .el-menu-item.is-active {
  background-color: rgba(255, 255, 255, 0.2);
  color: white;
}
.app-main {
  padding: 0;
  overflow-y: auto;
}
</style>
