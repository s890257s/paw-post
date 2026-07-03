<script setup>
// 應用程式根元件，只負責掛載 v-app 與 router-view
</script>

<template>
  <v-app>
    <router-view />
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
