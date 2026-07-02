import axios from "axios";
import { useAuthStore } from "../stores/auth";
import { createToastInterface } from "vue-toastification";

// 建立獨立的 Toast 實例給這個檔案使用 (設定與 main.js 保持一致)
const toast = createToastInterface({
    position: "top-right",
    timeout: 3000,
});

const instance = axios.create({
    baseURL: "http://localhost:8080/api",
    timeout: 10000,
    headers: {
        "Content-Type": "application/json",
    },
});

// 請求攔截
instance.interceptors.request.use(
    (config) => {
        // 在攔截器「內部」呼叫 useAuthStore()，確保 Pinia 已經初始化完畢
        const authStore = useAuthStore();
        if (authStore.token) {
            // 如果有 Token，就加入 Authorization Header
            config.headers.Authorization = `Bearer ${authStore.token}`;
        }
        return config;
    },
    (error) => {
        // 如果請求設定有誤，會執行這裡
        return Promise.reject(error);
    }
);

// 回應攔截
instance.interceptors.response.use(
    (response) => {
        return response;
    },
    (error) => {
        if (!error.response) {
            toast.error("網路錯誤或伺服器無回應");
            return Promise.reject(error);
        }

        switch (error.response.status) {
            case 401:
                toast.error("未授權，請重新登入");
                // 處理 401 錯誤：呼叫 store 的 logout 來清除狀態
                const authStore = useAuthStore();
                authStore.logout();
                // 強制讓使用者回到登入頁面
                window.location.href = "/login";
                break;
            case 403:
                toast.error("權限不足，您無法執行此操作");
                break;
            case 404:
                toast.error("找不到資源");
                break;
            case 500:
                toast.error("伺服器發生錯誤");
                break;
            default:
                // 使用 ?. (Optional Chaining) 防止後端沒有回傳 JSON 格式時發生「找不到屬性」的程式錯誤 (TypeError)
                // 嘗試讀取後端的 message 或 error 欄位，若都沒有則顯示 axios 的預設錯誤訊息
                const backendMsg = error.response?.data?.message || error.response?.data?.error;
                toast.error("發生錯誤: " + (backendMsg || error.message));
        }

        return Promise.reject(error);
    }
);

export default instance;