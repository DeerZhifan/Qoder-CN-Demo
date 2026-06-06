<!--
  [场景:ECharts图表] — 图表组件集成模板
-->
<script setup lang="ts">
import * as echarts from "echarts";
import type { EChartsOption } from "echarts";

const chartRef = ref<HTMLElement>();
let chartInstance: echarts.ECharts | null = null;

const initChart = () => {
  if (!chartRef.value) return;

  chartInstance = echarts.init(chartRef.value);

  const option: EChartsOption = {
    title: { text: "示例图表" },
    tooltip: { trigger: "axis" },
    xAxis: {
      type: "category",
      data: ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"],
    },
    yAxis: { type: "value" },
    series: [
      {
        name: "数据",
        type: "bar",
        data: [120, 200, 150, 80, 70, 110, 130],
        itemStyle: { color: "#409EFF" },
      },
    ],
  };

  chartInstance.setOption(option);
};

// 响应式：窗口变化时重新渲染
const handleResize = () => {
  chartInstance?.resize();
};

onMounted(() => {
  initChart();
  window.addEventListener("resize", handleResize);
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", handleResize);
  chartInstance?.dispose();
  chartInstance = null;
});
</script>

<template>
  <div ref="chartRef" style="width: 100%; height: 400px" />
</template>
