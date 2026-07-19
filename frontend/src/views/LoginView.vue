<script setup>
// 登入頁。
// HTTP 錯誤的 toast 由 request.js 的回應攔截器統一發出,
// 這裡的 catch 只留 console.error,避免同一個錯誤跳出兩則訊息
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { useToast } from 'vue-toastification';

const router = useRouter();
const authStore = useAuthStore();
const toast = useToast();

const username = ref('');
const password = ref('');
const isLoading = ref(false);

const handleLogin = async () => {
  if (!username.value || !password.value) {
    toast.warning('請輸入帳號與密碼');
    return;
  }

  isLoading.value = true;
  try {
    await authStore.login({
      username: username.value,
      password: password.value
    });
    
    toast.success('登入成功！');
    router.push('/');
  } catch (error) {
    console.error('登入失敗', error);
  } finally {
    isLoading.value = false;
  }
};
</script>

<template>
  <v-container class="fill-height d-flex align-center justify-center">
    <v-card class="pa-8 rounded-xl shadow-lg" width="100%" max-width="400" elevation="4">
      <div class="text-center mb-8">
        <v-icon icon="mdi-paw" size="48" color="primary" class="mb-4"></v-icon>
        <h2 class="text-h4 font-weight-bold text-primary">登入 Paw Post</h2>
        <p class="text-grey-darken-1 mt-2">分享您愛寵的每一刻</p>
      </div>

      <v-form @submit.prevent="handleLogin">
        <v-text-field
          v-model="username"
          label="帳號"
          prepend-inner-icon="mdi-account"
          variant="outlined"
          color="primary"
          class="mb-4"
          hide-details
        ></v-text-field>

        <v-text-field
          v-model="password"
          label="密碼"
          prepend-inner-icon="mdi-lock"
          type="password"
          variant="outlined"
          color="primary"
          class="mb-6"
          hide-details
        ></v-text-field>

        <v-btn
          type="submit"
          color="primary"
          size="x-large"
          block
          variant="flat"
          class="text-none font-weight-bold"
          :loading="isLoading"
        >
          登入
        </v-btn>
      </v-form>

      <div class="text-center mt-6">
        <v-btn
          variant="text"
          color="grey-darken-1"
          class="text-none"
          @click="router.push('/')"
        >
          先去逛逛
        </v-btn>
      </div>
    </v-card>
  </v-container>
</template>

<style scoped>
.shadow-lg {
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1) !important;
}
.fill-height {
  min-height: calc(100vh - 64px); /* 扣除 navbar 高度 */
}
</style>
