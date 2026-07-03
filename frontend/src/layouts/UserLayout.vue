<script setup>
import Navbar from '../components/Navbar.vue';
</script>

<template>
  <!-- 頂部導覽列 -->
  <Navbar />

  <!-- 頁面主要內容區域 -->
  <v-main class="bg-grey-lighten-4">
    <router-view />
  </v-main>
</template>
