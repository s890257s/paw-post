<script setup>
import { ref, watch } from 'vue';
import { compressImage } from '../utils/image';
import { createPost } from '../api/post';
import { useToast } from 'vue-toastification';

const props = defineProps({
  modelValue: Boolean
});

const emit = defineEmits(['update:modelValue', 'post-created']);

const toast = useToast();

const dialog = ref(false);
const isUploading = ref(false);
const description = ref('');
const selectedFile = ref(null);
const previewUrl = ref('');

// 同步外部的 v-model
watch(() => props.modelValue, (newVal) => {
  dialog.value = newVal;
});

watch(dialog, (newVal) => {
  emit('update:modelValue', newVal);
  if (!newVal) {
    resetForm();
  }
});

const resetForm = () => {
  description.value = '';
  selectedFile.value = null;
  previewUrl.value = '';
};

const handleFileChange = (event) => {
  const file = event.target.files[0];
  if (!file) return;

  if (!file.type.startsWith('image/')) {
    toast.error('請選擇圖片檔案！');
    return;
  }

  selectedFile.value = file;

  // 產生預覽圖
  const reader = new FileReader();
  reader.onload = (e) => {
    previewUrl.value = e.target.result;
  };
  reader.readAsDataURL(file);

  // 清空 input 的值：change 事件只在「值改變」時觸發，
  // 若不清空，使用者取消後再選「同一張」圖片會沒有反應
  event.target.value = '';
};

const submitPost = async () => {
  if (!selectedFile.value) {
    toast.warning('請先選擇一張照片');
    return;
  }

  isUploading.value = true;
  try {
    // 壓縮圖片
    const compressedBlob = await compressImage(selectedFile.value, 1200, 0.8);
    
    // 準備 FormData
    const formData = new FormData();
    formData.append('image', compressedBlob, selectedFile.value.name);
    formData.append('description', description.value);

    // 發送請求
    const response = await createPost(formData);
    toast.success('發布成功！');
    
    // 通知父元件更新列表
    emit('post-created', response.data);
    dialog.value = false;
  } catch (error) {
    console.error('上傳失敗', error);
  } finally {
    isUploading.value = false;
  }
};
</script>

<template>
  <v-dialog v-model="dialog" max-width="500" persistent>
    <v-card class="rounded-lg">
      <v-card-title class="d-flex justify-space-between align-center pa-4 bg-primary text-white">
        <span class="text-h6 font-weight-bold">建立新貼文</span>
        <v-btn icon="mdi-close" variant="text" color="white" @click="dialog = false" :disabled="isUploading"></v-btn>
      </v-card-title>
      
      <v-card-text class="pa-4">
        <!-- 圖片上傳區塊 -->
        <div 
          class="upload-area mb-4 rounded-lg d-flex flex-column align-center justify-center bg-grey-lighten-4 border-dashed"
          @click="$refs.fileInput.click()"
        >
          <template v-if="!previewUrl">
            <v-icon icon="mdi-cloud-upload-outline" size="48" color="grey"></v-icon>
            <p class="text-grey-darken-1 mt-2">點擊選擇照片</p>
          </template>
          <v-img 
            v-else 
            :src="previewUrl" 
            height="100%" 
            width="100%" 
            cover 
            class="rounded-lg"
          ></v-img>
          
          <input 
            type="file" 
            ref="fileInput" 
            class="d-none" 
            accept="image/*" 
            @change="handleFileChange"
          >
        </div>

        <!-- 描述輸入區塊 -->
        <v-textarea
          v-model="description"
          label="說些什麼吧..."
          variant="outlined"
          auto-grow
          rows="3"
          hide-details
          class="mt-4"
        ></v-textarea>
      </v-card-text>

      <v-card-actions class="pa-4 pt-0">
        <v-spacer></v-spacer>
        <v-btn
          color="grey-darken-1"
          variant="text"
          class="text-none"
          @click="dialog = false"
          :disabled="isUploading"
        >
          取消
        </v-btn>
        <v-btn
          color="primary"
          variant="flat"
          class="text-none px-6"
          @click="submitPost"
          :loading="isUploading"
          :disabled="!selectedFile"
        >
          發布
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.upload-area {
  height: 250px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;
}
.border-dashed {
  border: 2px dashed #e0e0e0;
}
.upload-area:hover {
  background-color: #f5f5f5 !important;
  border-color: #bdbdbd;
}
</style>
