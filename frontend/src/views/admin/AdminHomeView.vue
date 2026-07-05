<script setup>
import { ref, onMounted } from 'vue';
import { getPosts, hidePost, unhidePost } from '../../api/post';
import { useToast } from 'vue-toastification';

const posts = ref([]);
const toast = useToast();
const isLoading = ref(false);

const loadPosts = async () => {
  isLoading.value = true;
  try {
    // 管理員後台簡單展示，一次抓 100 筆
    const response = await getPosts(0, 100);
    posts.value = response.data.content;
  } catch (error) {
    // HTTP 錯誤的 toast 統一由 request.js 的回應攔截器發出，
    // 這裡再 toast 一次會讓同一個錯誤跳出兩則訊息
    console.error('無法取得貼文列表', error);
  } finally {
    isLoading.value = false;
  }
};

const handleToggleHide = async (post) => {
  try {
    if (post.isHidden) {
      await unhidePost(post.id);
      post.isHidden = false;
      toast.success('已解除禁用');
    } else {
      await hidePost(post.id);
      post.isHidden = true;
      toast.success('已禁用該文章');
    }
  } catch (error) {
    console.error('禁用/解禁操作失敗', error);
  }
};

onMounted(() => {
  loadPosts();
});
</script>

<template>
  <div>
    <h2 class="text-h4 mb-4">文章管理與審核</h2>
    
    <v-card elevation="2" :loading="isLoading">
      <v-table>
        <thead>
          <tr>
            <th class="text-left" width="80">ID</th>
            <th class="text-left" width="120">發布者</th>
            <th class="text-left">內容預覽</th>
            <th class="text-left" width="100">狀態</th>
            <th class="text-center" width="150">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="posts.length === 0 && !isLoading">
            <td colspan="5" class="text-center text-grey py-4">目前沒有任何文章</td>
          </tr>
          <tr v-for="post in posts" :key="post.id">
            <td>{{ post.id }}</td>
            <td class="font-weight-bold">{{ post.username }}</td>
            <td class="text-truncate" style="max-width: 300px;">
              {{ post.description || '(無文字內容)' }}
            </td>
            <td>
              <v-chip :color="post.isHidden ? 'red' : 'success'" size="small" variant="flat">
                {{ post.isHidden ? '已禁用' : '正常' }}
              </v-chip>
            </td>
            <td class="text-center">
              <v-btn
                :color="post.isHidden ? 'success' : 'error'"
                variant="outlined"
                size="small"
                @click="handleToggleHide(post)"
              >
                {{ post.isHidden ? '解除禁用' : '禁用文章' }}
              </v-btn>
            </td>
          </tr>
        </tbody>
      </v-table>
    </v-card>
  </div>
</template>
