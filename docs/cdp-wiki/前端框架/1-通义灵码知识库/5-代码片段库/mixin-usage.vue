<!--
  [场景:使用mixin] — @include 使用全局 SCSS mixin
  mixin 已通过 Vite additionalData 自动注入，无需手动 import
-->
<script setup lang="ts">
// mixin 仅在 <style> 中使用
</script>

<template>
  <div class="mixin-demo">
    <p class="single-line">单行文本超出省略效果，这是一段很长很长的文本内容</p>
    <p class="multi-line">多行文本省略效果，这是一段很长很长的文本内容，最多显示两行，超出部分显示省略号</p>
    <div class="centered-box">绝对居中的盒子</div>
    <div class="scroll-area">
      <p v-for="i in 50" :key="i">滚动内容 {{ i }}（自定义滚动条）</p>
    </div>
  </div>
</template>

<style scoped lang="scss">
// ✅ 直接 @include，不需要 @import mixin.scss

// 单行文本省略
.single-line {
  width: 200px;
  @include ellipsis;
  // 展开为: overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}

// 多行文本省略（传参数）
.multi-line {
  width: 200px;
  @include ellipsis(2);
  // 展开为: overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; ...
}

// 绝对居中
.centered-box {
  @include center(200px, 100px);
  // 展开为: position: absolute; left: 50%; top: 50%; margin-left: -100px; margin-top: -50px;
  background: $c-m;
  color: #fff;
}

// 自定义滚动条
.scroll-area {
  height: 200px;
  overflow-y: auto;
  @include scrollBar;
  // 展开为: 6px 宽度的圆角滚动条
}

// 清除浮动
.float-container {
  @include clearfix;
}
</style>
