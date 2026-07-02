<script setup>
import { defineProps, defineEmits, computed } from 'vue';
import { useAuthStore } from '../stores/auth';
import { likePost, unlikePost } from '../api/post';
import { useToast } from 'vue-toastification';

const props = defineProps({
  post: {
    type: Object,
    required: true
  }
});

const emit = defineEmits(['update-post']);
const authStore = useAuthStore();
const toast = useToast();

const isLiked = computed(() => props.post.isLiked);
const likeCount = computed(() => props.post.likeCount);
const imageSrc = computed(() => props.post.imageBase64);

// 格式化日期顯示
const formattedDate = computed(() => {
  if (!props.post.createdAt) return '';
  const date = new Date(props.post.createdAt);
  return date.toLocaleDateString('zh-TW', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
});

const handleLike = async () => {
  if (!authStore.token) {
    toast.info('請先登入才能按讚喔！');
    return;
  }

  try {
    if (isLiked.value) {
      await unlikePost(props.post.id);
      emit('update-post', { ...props.post, isLiked: false, likeCount: likeCount.value - 1 });
    } else {
      await likePost(props.post.id);
      emit('update-post', { ...props.post, isLiked: true, likeCount: likeCount.value + 1 });
    }
  } catch (error) {
    console.error('按讚操作失敗', error);
  }
};
</script>

<template>
  <v-card class="mb-6 rounded-lg shadow-sm overflow-hidden" elevation="2">
    <!-- 發布者資訊 -->
    <v-card-item>
      <template v-slot:prepend>
        <v-avatar color="primary" size="40">
          <span class="text-h6 text-white">{{ post.username?.charAt(0).toUpperCase() }}</span>
        </v-avatar>
      </template>
      <v-card-title class="text-subtitle-1 font-weight-bold">
        {{ post.username }}
      </v-card-title>
      <v-card-subtitle class="text-caption text-grey">
        {{ formattedDate }}
      </v-card-subtitle>
    </v-card-item>

    <!-- 照片展示 -->
    <v-img
      :src="imageSrc"
      height="400"
      cover
      class="bg-grey-lighten-2"
    >
      <template v-slot:placeholder>
        <v-row class="fill-height ma-0" align="center" justify="center">
          <v-progress-circular indeterminate color="primary"></v-progress-circular>
        </v-row>
      </template>
    </v-img>

    <!-- 互動按鈕 -->
    <v-card-actions class="px-4 pt-2 pb-0">
      <v-btn
        icon
        variant="text"
        :color="isLiked ? 'red' : 'grey-darken-1'"
        @click="handleLike"
      >
        <v-icon :icon="isLiked ? 'mdi-heart' : 'mdi-heart-outline'" size="x-large"></v-icon>
      </v-btn>
      <span class="text-subtitle-2 font-weight-bold ml-1">{{ likeCount }} 個讚</span>
    </v-card-actions>

    <!-- 文字描述 -->
    <v-card-text class="pt-2 px-4 pb-4 text-body-1" v-if="post.description">
      <span class="font-weight-bold mr-2">{{ post.username }}</span>
      <span class="text-grey-darken-3">{{ post.description }}</span>
    </v-card-text>
  </v-card>
</template>

<style scoped>
.shadow-sm {
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.05) !important;
}
</style>
