import { defineStore } from 'pinia';
import { ref } from 'vue';
// HTTP 呼叫統一放在 api/ 層，store 只負責「狀態」：
// 為了避免和下方的 action 同名，import 時取別名 loginApi
import { login as loginApi } from '@/api/auth';

export const useAuthStore = defineStore(
    'auth',
    () => {
        // State
        const token = ref('');
        const user = ref(null); // 登入後存放 { username, isAdmin }

        // Actions
        /**
         * 處理登入邏輯
         * @param {Object} loginRequest 包含帳號密碼的物件
         */
        const login = async (loginRequest) => {
            const response = await loginApi(loginRequest);

            // 將後端回傳的 token 存入 state
            token.value = response.data.token;

            // 儲存後端回傳的 username 與 isAdmin
            user.value = {
                username: response.data.username,
                isAdmin: response.data.isAdmin || false
            };
        };

        /**
         * 處理登出邏輯
         */
        const logout = () => {
            // 清除狀態
            token.value = '';
            user.value = null;
        };

        return {
            token,
            user,
            login,
            logout
        };
    },
    {
        // 啟用持久化，套件會自動把這個 store 存到 localStorage
        persist: true
    }
);
