<script setup>
import Navbar from './components/Navbar.vue';
</script>

<template>
  <v-app>
    <!-- 頂部導覽列 -->
    <Navbar />

    <!-- 頁面主要內容區域 -->
    <v-main class="bg-grey-lighten-4">
      <router-view />
    </v-main>
  </v-app>
</template>

<style>
/* 全域樣式設定 */
body {
  font-family: 'Inter', 'Roboto', sans-serif;
  margin: 0;
  padding: 0;
}
</style>
