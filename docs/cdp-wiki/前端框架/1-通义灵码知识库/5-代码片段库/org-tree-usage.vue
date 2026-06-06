<!--
  [场景:组织树选择] — OrgunitTree / OrgRoleUser 组件使用
  全局注册组件: OrgunitTree、OrgRoleUser
-->
<script setup lang="ts">

// ==================== OrgunitTree 组织树 ====================
const selectedOrg = ref<any>(null);

function handleOrgClick(data: any) {
  selectedOrg.value = data;
  console.log("选中组织:", data.orgunitId, data.orgunitName);
  // 根据组织 ID 加载该组织下的数据
}

// ==================== OrgRoleUser 组织/角色/用户选择器 ====================
const selectedUsers = ref<any[]>([]);

function handleUserConfirm(data: any) {
  selectedUsers.value = data;
  console.log("选中用户:", data);
}
</script>

<template>
  <div>
    <!-- 组织树（通常放在页面左侧） -->
    <div style="width: 250px; border-right: 1px solid #eee;">
      <OrgunitTree @node-click="handleOrgClick" />
    </div>

    <!-- 组织/角色/用户选择器（通常作为弹窗使用） -->
    <OrgRoleUser @confirm="handleUserConfirm" />

    <!-- 显示选中结果 -->
    <div v-if="selectedOrg">
      当前组织: {{ selectedOrg.orgunitName }}
    </div>
  </div>
</template>
