<script setup>
import { ref } from 'vue';

const drawer = ref(true);
</script>

<template>
  <!-- 後台側邊導覽列 -->
  <v-navigation-drawer v-model="drawer" app color="grey-darken-4" theme="dark">
    <v-list>
      <v-list-item title="Paw Post Admin" subtitle="後台管理系統"></v-list-item>
      <v-divider></v-divider>
      
      <!-- 選單項目 -->
      <v-list-item link to="/admin" prepend-icon="mdi-view-dashboard" title="儀表板 (Dashboard)"></v-list-item>
      <v-list-item link to="/" prepend-icon="mdi-home" title="返回前台"></v-list-item>
    </v-list>
  </v-navigation-drawer>

  <!-- 後台上方的 App Bar -->
  <v-app-bar app color="white" elevation="1">
    <v-app-bar-nav-icon @click="drawer = !drawer"></v-app-bar-nav-icon>
    <v-app-bar-title>管理員儀表板</v-app-bar-title>
  </v-app-bar>

  <!-- 後台內容區域 -->
  <v-main class="bg-grey-lighten-4">
    <v-container fluid class="pa-6">
      <!-- 這裡會掛載子路由 (例如 AdminHomeView) -->
      <router-view />
    </v-container>
  </v-main>
</template>
