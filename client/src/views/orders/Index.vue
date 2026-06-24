<template>
  <MyTickets @back-to-square="goBackToSquare" />
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import MyTickets from '@/components/MyTickets.vue'

const router = useRouter()

function goBackToSquare() {
  void router.push({ name: 'Square' })
}
</script>
