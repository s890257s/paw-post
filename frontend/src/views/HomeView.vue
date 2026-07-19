<script setup>
import { ref, onMounted, watch } from 'vue';
import { getPosts } from '@/api/post';
import { useFeedStore } from '@/stores/feed';
import PostCard from '@/components/PostCard.vue';

const feedStore = useFeedStore();
const posts = ref([]);
const page = ref(0);
const totalPages = ref(1);
const isLoading = ref(false);

// 【進階】已知取捨 1——offset 分頁的位移：停在列表時若有人發了新文，
// 「載入更多」抓下一頁時同一篇貼文可能被擠到下一頁而重複出現。
// 正解是 cursor-based pagination，見 README 進階課題，教學版接受此現象。
// 【進階】已知取捨 2：下面的 isLoading 防連點會把「載入中收到的刷新訊號」一起擋掉，
// 正解需要 AbortController 請求取消或佇列，超出本課程範圍。
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

// 監聽 feed store 的刷新訊號：Navbar 的 UploadDialog 發文成功後會觸發，
// 這裡重新載入列表，貼文依 createdAt 降序，新貼文就會出現在最頂端
watch(() => feedStore.refreshSignal, () => {
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
