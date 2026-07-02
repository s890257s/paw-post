import { defineStore } from 'pinia';
import { ref } from 'vue';
import axios from 'axios';
// 如果有登入用的獨立 api 方法可以放這裡，或是直接使用共用的 request
import request from '@/api/request';

export const useAuthStore = defineStore(
    'auth',
    () => {
        // State
        const token = ref('');
        const user = ref(null); // 預留給未來存放使用者名稱或 ID 的欄位

        // Actions
        /**
         * 處理登入邏輯
         * @param {Object} loginRequest 包含帳號密碼的物件
         */
        const login = async (loginRequest) => {
            // 使用共用的 request 打 API，或者直接用 axios。
            // 由於登入不需要帶 token，用共用的 request 也是沒問題的
            const response = await request.post('/login', loginRequest);

            // 將後端回傳的 token 存入 state
            token.value = response.data.token;

            // 儲存後端回傳的 username
            user.value = {
                username: response.data.username
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
