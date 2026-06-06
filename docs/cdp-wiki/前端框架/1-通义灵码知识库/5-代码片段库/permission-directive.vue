<!--
  [场景:使用权限指令] — v-hasPerm 按钮级权限控制
  源码参考: src/cdp-common-ctrl/directive/permission/index.ts
-->
<script setup lang="ts">
/**
 * v-hasPerm 工作原理:
 * 1. 从 useUserStoreHook().user 获取 roles 和 permission
 * 2. roles 包含 "ROOT" → 超级管理员，跳过校验
 * 3. 检查 permission 数组是否包含指定权限标识（满足其一即可）
 * 4. 无权限 → removeChild 直接移除 DOM 节点
 *
 * 权限标识来源: 后端 getUserInfoApi 返回的 permissions 数组
 */
</script>

<template>
  <div class="app-container">
    <!-- 单个权限 -->
    <el-button v-hasPerm="['sys:user:add']" type="primary">
      新增用户
    </el-button>

    <!-- 多个权限（满足其一即可显示） -->
    <el-button v-hasPerm="['sys:user:edit', 'sys:user:update']" type="warning">
      编辑用户
    </el-button>

    <!-- 删除权限 -->
    <el-button v-hasPerm="['sys:user:delete']" type="danger">
      删除用户
    </el-button>

    <!--
      注意事项:
      1. v-hasPerm 是直接移除 DOM，不是隐藏（display:none）
      2. ROOT 角色拥有所有权限，不受 v-hasPerm 限制
      3. 当前只注册了 v-hasPerm，v-hasRole 未全局注册
      4. 权限标识格式通常为: 模块:资源:操作，如 sys:user:add
    -->
  </div>
</template>
