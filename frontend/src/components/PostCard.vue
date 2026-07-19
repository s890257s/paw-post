<script setup>
// defineProps / defineEmits 是編譯器巨集，不需要 import，
// import 反而會在 console 產生警告
import { ref, computed } from 'vue';
import { useAuthStore } from '@/stores/auth';
import { likePost, unlikePost } from '@/api/post';
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

// props 的欄位可以直接使用，例如 template 裡的 post.username；
// 只有「需要加工」的資料才值得包成 computed，例如下面兩個：

// 被禁用的文章是否要對「目前的觀看者」遮罩：
// 後端對管理員不清空內容——方便判斷該不該解禁——所以管理員照常顯示、只加標記
const isMaskedForViewer = computed(() => props.post.isHidden && !authStore.user?.isAdmin);

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

// 防止快速連點造成重複請求
const isProcessing = ref(false);

// props 唯讀,子元件不能直接修改 post。
// 改用 emit 把更新後的資料交給父層替換——這是單向資料流的核心慣例
const handleLike = async () => {
  if (!authStore.token) {
    toast.info('請先登入才能按讚喔！');
    return;
  }

  if (isProcessing.value) return;
  isProcessing.value = true;

  try {
    if (props.post.isLiked) {
      await unlikePost(props.post.id);
      emit('update-post', { ...props.post, isLiked: false, likeCount: props.post.likeCount - 1 });
    } else {
      await likePost(props.post.id);
      emit('update-post', { ...props.post, isLiked: true, likeCount: props.post.likeCount + 1 });
    }
  } catch (error) {
    console.error('按讚操作失敗', error);
  } finally {
    isProcessing.value = false;
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

    <!-- 照片展示 或 禁用提示 -->
    <div v-if="isMaskedForViewer" class="bg-grey-lighten-3 d-flex align-center justify-center flex-column" style="height: 400px;">
      <v-icon icon="mdi-shield-off-outline" size="64" color="grey-darken-1" class="mb-2"></v-icon>
      <span class="text-h6 text-grey-darken-1 font-weight-bold">此文章已被管理員禁用</span>
    </div>
    <v-img
      v-else
      :src="post.imageBase64"
      height="400"
      cover
      class="bg-grey-lighten-2"
    >
      <!-- 管理員視角：內容照常顯示，但標示禁用狀態 -->
      <v-chip
        v-if="post.isHidden"
        color="red"
        variant="flat"
        size="small"
        class="ma-2"
        prepend-icon="mdi-shield-off-outline"
      >
        已禁用（僅管理員可見）
      </v-chip>
      <template v-slot:placeholder>
        <v-row class="fill-height ma-0" align="center" justify="center">
          <v-progress-circular indeterminate color="primary"></v-progress-circular>
        </v-row>
      </template>
    </v-img>

    <!-- 互動按鈕 -->
    <v-card-actions class="px-4 pt-2 pb-0" v-if="!post.isHidden">
      <v-btn
        icon
        variant="text"
        :color="post.isLiked ? 'red' : 'grey-darken-1'"
        @click="handleLike"
      >
        <v-icon :icon="post.isLiked ? 'mdi-heart' : 'mdi-heart-outline'" size="x-large"></v-icon>
      </v-btn>
      <span class="text-subtitle-2 font-weight-bold ml-1">{{ post.likeCount }} 個讚</span>
    </v-card-actions>

    <!-- 文字描述 -->
    <v-card-text class="pt-2 px-4 pb-4 text-body-1" v-if="!isMaskedForViewer && post.description">
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
