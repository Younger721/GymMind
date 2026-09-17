<template>
  <pre v-if="data" class="json-block">{{ text }}</pre>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ data: unknown }>()
const text = computed(() => JSON.stringify(props.data, null, 2))
</script>

<style scoped>
.json-block {
  margin: 12px 0 0;
  padding: 12px;
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: 8px;
  font-size: 12px;
  overflow: auto;
  max-height: 360px;
}
</style>
