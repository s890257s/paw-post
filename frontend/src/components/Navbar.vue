<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import { useFeedStore } from '../stores/feed';
import UploadDialog from './UploadDialog.vue';
import { useToast } from 'vue-toastification';

const router = useRouter();
const authStore = useAuthStore();
const feedStore = useFeedStore();
const toast = useToast();

const showUpload = ref(false);

const handleLogout = () => {
  authStore.logout();
  toast.success('已登出');
  router.push('/login');
};
</script>

<template>
  <v-app-bar color="white" elevation="1">
    <v-container class="d-flex align-center px-4" fluid>
      <!-- Logo 區塊 -->
      <v-toolbar-title 
        class="font-weight-bold text-primary cursor-pointer"
        @click="router.push('/')"
      >
        <div class="d-flex align-center">
          <img src="/favicon.ico" alt="Paw Post Logo" class="mr-2" width="24" height="24" />
          <span>Paw Post</span>
        </div>
      </v-toolbar-title>

      <v-spacer></v-spacer>

      <!-- 導覽按鈕 -->
      <div v-if="authStore.token">
        <v-btn 
          v-if="authStore.user?.isAdmin"
          color="secondary" 
          variant="flat" 
          class="mr-2 text-none"
          prepend-icon="mdi-shield-account"
          @click="router.push('/admin')"
        >
          管理員後台
        </v-btn>
        <v-btn 
          color="primary" 
          variant="flat" 
          class="mr-2 text-none"
          prepend-icon="mdi-camera-plus"
          @click="showUpload = true"
        >
          發布照片
        </v-btn>
        <v-btn 
          variant="text" 
          class="text-none text-grey-darken-1"
          prepend-icon="mdi-logout"
          @click="handleLogout"
        >
          登出
        </v-btn>
      </div>
      <div v-else>
        <v-btn 
          color="primary" 
          variant="outlined"
          class="text-none"
          prepend-icon="mdi-login"
          @click="router.push('/login')"
        >
          登入
        </v-btn>
      </div>
    </v-container>

    <!-- 上傳照片元件：發布成功後透過 feed store 通知 HomeView 重新載入列表 -->
    <UploadDialog v-model="showUpload" @post-created="feedStore.notifyPostCreated()" />
  </v-app-bar>
</template>

<style scoped>
.cursor-pointer {
  cursor: pointer;
}
</style>
