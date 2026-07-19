import { createRouter, createWebHistory } from 'vue-router'
// 「@」是 src 資料夾的別名，設定見 vite.config.js 與 jsconfig.json。
// 專案內的 import 統一使用 @，路徑不會因檔案搬移而跟著改
import UserLayout from '@/layouts/UserLayout.vue'
import AdminLayout from '@/layouts/AdminLayout.vue'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: UserLayout,
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('@/views/HomeView.vue')
        },
        {
          path: 'login',
          name: 'login',
          component: () => import('@/views/LoginView.vue')
        }
      ]
    },
    {
      path: '/admin',
      component: AdminLayout,
      // meta 是路由的「附加資訊」，搭配下方的全域守衛使用
      meta: { requiresAdmin: true },
      children: [
        {
          path: '',
          name: 'admin-home',
          component: () => import('@/views/admin/AdminHomeView.vue')
        }
      ]
    },
    {
      // Catch-all 路由：比對不到任何路徑時導回首頁，
      // 避免使用者輸入錯誤網址看到一片空白
      path: '/:pathMatch(.*)*',
      redirect: { name: 'home' }
    }
  ],
})

// 全域導航守衛：進入每個路由前執行。
// 【安全】前端守衛只是「使用者體驗」層的防線，避免看到打不開的頁面；
// 真正的權限防線在後端 API —— 就算有人繞過前端，後端仍會回 401 / 403。
router.beforeEach((to) => {
  const authStore = useAuthStore();

  // 已登入者不需要再看到登入頁，直接導回首頁
  if (to.name === 'login' && authStore.token) {
    return { name: 'home' };
  }

  // matched 包含巢狀路由的所有層級，只要其中一層要求管理員就需要檢查
  const requiresAdmin = to.matched.some((record) => record.meta.requiresAdmin);

  if (requiresAdmin) {
    // 未登入或不是管理員，一律導回首頁
    if (!authStore.token || !authStore.user?.isAdmin) {
      return { name: 'home' };
    }
  }
});

export default router
