<template>
  <img v-if="dataUrl" class="ticket-qr" :src="dataUrl" :alt="`电子票 ${code} 的二维码`" />
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import QRCode from 'qrcode'

const props = defineProps<{ code: string }>()
const dataUrl = ref('')

watch(() => props.code, async (code) => {
  dataUrl.value = code ? await QRCode.toDataURL(code, { width: 180, margin: 2, errorCorrectionLevel: 'M' }) : ''
}, { immediate: true })
</script>

<style scoped>
.ticket-qr { display: block; width: min(100%, 120px); height: auto; aspect-ratio: 1; background: white; image-rendering: crisp-edges; }
</style>
