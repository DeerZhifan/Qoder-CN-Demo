<template>
  <div class="kb-layout">
    <el-container>
      <el-aside width="250px" class="sidebar">
        <CategoryTreeManager @refresh="handleRefresh" />
      </el-aside>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import CategoryTreeManager from '@/components/CategoryTreeManager.vue'

const handleRefresh = () => {
  // 触发子组件刷新
}
</script>

<style scoped>
.kb-layout {
  height: 100vh;
}
.sidebar {
  border-right: 1px solid #e6e6e6;
  padding: 10px;
}
</style>
