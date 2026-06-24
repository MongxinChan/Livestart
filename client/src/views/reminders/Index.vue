<template>
  <MyReminders @back-to-square="goBackToSquare" />
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import MyReminders from '@/components/MyReminders.vue'

const router = useRouter()

function goBackToSquare() {
  void router.push({ name: 'Square' })
}
</script>
