<!--
  [场景:主题切换] — 使用 settings store 切换主题
  源码参考: src/cdp-admin/store/modules/settings.ts
-->
<script setup lang="ts">
import { useSettingsStore } from "@/cdp-admin/store/modules/settings";

const settingsStore = useSettingsStore();

// 当前主题（light / dark / blue）
const currentTheme = computed(() => settingsStore.theme);

// 切换主题
function switchTheme(theme: "light" | "dark" | "blue") {
  settingsStore.changeSetting({ key: "theme", value: theme });
  // 内部实现: document.documentElement.className = `theme-${theme}`
}

// 切换布局（left / top / mix）
function switchLayout(layout: "left" | "top" | "mix") {
  settingsStore.changeSetting({ key: "layout", value: layout });
}

// 修改主题色
function changeThemeColor(color: string) {
  settingsStore.changeSetting({ key: "themeColor", value: color });
}
</script>

<template>
  <div>
    <h3>主题切换</h3>
    <el-radio-group :model-value="currentTheme" @change="switchTheme">
      <el-radio label="light">浅色</el-radio>
      <el-radio label="dark">暗黑</el-radio>
      <el-radio label="blue">蓝色</el-radio>
    </el-radio-group>

    <h3>布局切换</h3>
    <el-radio-group :model-value="settingsStore.layout" @change="switchLayout">
      <el-radio label="left">左侧菜单</el-radio>
      <el-radio label="top">顶部菜单</el-radio>
      <el-radio label="mix">混合菜单</el-radio>
    </el-radio-group>

    <h3>主题色</h3>
    <el-color-picker
      :model-value="settingsStore.themeColor"
      @change="changeThemeColor"
    />

    <!--
      说明:
      1. 主题设置通过 useStorage 持久化到 localStorage
      2. 切换主题会修改 document.documentElement.className
      3. CSS 自定义属性（--menuBg 等）根据主题类名变化
      4. Element Plus 暗黑模式通过 html.dark 类名触发
    -->
  </div>
</template>
