import { createRouter, createWebHistory } from 'vue-router'
import UserLayout from '../layouts/UserLayout.vue'
import AdminLayout from '../layouts/AdminLayout.vue'
import { useAuthStore } from '../stores/auth'

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
          component: () => import('../views/HomeView.vue')
        },
        {
          path: 'login',
          name: 'login',
          component: () => import('../views/LoginView.vue')
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
          component: () => import('../views/admin/AdminHomeView.vue')
        }
      ]
    }
  ],
})

// 全域導航守衛：進入每個路由前執行。
// 注意：前端守衛只是「使用者體驗」層的防線（避免看到打不開的頁面），
// 真正的權限防線在後端 API —— 就算有人繞過前端，後端仍會回 401 / 403。
router.beforeEach((to) => {
  // matched 包含巢狀路由的所有層級，只要其中一層要求管理員就需要檢查
  const requiresAdmin = to.matched.some((record) => record.meta.requiresAdmin);

  if (requiresAdmin) {
    const authStore = useAuthStore();
    // 未登入或不是管理員，一律導回首頁
    if (!authStore.token || !authStore.user?.isAdmin) {
      return { name: 'home' };
    }
  }
});

export default router
