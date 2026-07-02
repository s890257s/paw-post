<script setup>
import { ref, onMounted } from 'vue';
import { getPosts } from '../api/post';
import PostCard from '../components/PostCard.vue';
import { useToast } from 'vue-toastification';

const toast = useToast();
const posts = ref([]);
const page = ref(0);
const totalPages = ref(1);
const isLoading = ref(false);

const loadPosts = async (reset = false) => {
  if (isLoading.value) return;
  
  if (reset) {
    page.value = 0;
    posts.value = [];
  }

  isLoading.value = true;
  try {
    const response = await getPosts(page.value, 10);
    const data = response.data;
    
    if (reset) {
      posts.value = data.content;
    } else {
      posts.value = [...posts.value, ...data.content];
    }
    
    totalPages.value = data.totalPages;
    page.value++;
  } catch (error) {
    console.error('讀取貼文失敗', error);
  } finally {
    isLoading.value = false;
  }
};

const handlePostUpdate = (updatedPost) => {
  const index = posts.value.findIndex(p => p.id === updatedPost.id);
  if (index !== -1) {
    posts.value[index] = updatedPost;
  }
};

onMounted(() => {
  loadPosts(true);
});
</script>

<template>
  <v-container class="py-8" max-width="600">
    <!-- 貼文列表 -->
    <div v-if="posts.length > 0">
      <PostCard
        v-for="post in posts"
        :key="post.id"
        :post="post"
        @update-post="handlePostUpdate"
      />
    </div>

    <!-- 空狀態 -->
    <v-row v-else-if="!isLoading" justify="center" class="mt-10">
      <v-col cols="12" class="text-center">
        <v-icon icon="mdi-image-off-outline" size="64" color="grey-lighten-1"></v-icon>
        <p class="text-h6 text-grey mt-4">目前還沒有貼文喔，趕快來發布第一篇吧！</p>
      </v-col>
    </v-row>

    <!-- 載入更多按鈕 -->
    <v-row justify="center" class="mt-4 mb-8" v-if="page < totalPages">
      <v-btn
        variant="tonal"
        color="primary"
        class="text-none"
        rounded="pill"
        @click="loadPosts(false)"
        :loading="isLoading"
      >
        載入更多
      </v-btn>
    </v-row>
    
    <!-- 載入中動畫 -->
    <v-row justify="center" class="mt-4 mb-8" v-if="isLoading && posts.length === 0">
      <v-progress-circular indeterminate color="primary" size="48"></v-progress-circular>
    </v-row>
  </v-container>
</template>
