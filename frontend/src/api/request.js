import axios from "axios";
import { useAuthStore } from "../stores/auth";
import { createToastInterface } from "vue-toastification";
import router from "../router";

// 建立獨立的 Toast 實例給這個檔案使用 (設定與 main.js 保持一致)
const toast = createToastInterface({
    position: "bottom-right",
    timeout: 3000,
});

const instance = axios.create({
    baseURL: "http://localhost:8080/api",
    // 上傳圖片在較慢的網路可能超過 10 秒，放寬到 30 秒
    timeout: 30000,
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

        // 使用 ?. (Optional Chaining) 防止後端沒有回傳 JSON 格式時發生「找不到屬性」的程式錯誤 (TypeError)
        const backendMsg = error.response?.data?.message || error.response?.data?.error;

        switch (error.response.status) {
            case 401: {
                // 【特例】登入 API 本身的 401 代表「帳號或密碼錯誤」，
                // 不是「登入狀態失效」，不能登出跳轉，否則錯誤訊息會被頁面跳轉吃掉
                if (error.config?.url?.includes("/login")) {
                    toast.error(backendMsg || "帳號或密碼錯誤");
                    break;
                }

                // 其他 API 的 401：Token 無效或過期，清除狀態並回到登入頁。
                // 用 router.push 而不是 window.location.href —— 後者會整頁重載，
                // SPA 狀態全部消失，toast 也會來不及顯示
                toast.error(backendMsg || "登入已失效，請重新登入");
                const authStore = useAuthStore();
                authStore.logout();
                router.push("/login");
                break;
            }
            case 403:
                toast.error(backendMsg || "權限不足，您無法執行此操作");
                break;
            case 404:
                toast.error(backendMsg || "找不到資源");
                break;
            case 500:
                toast.error("伺服器發生錯誤");
                break;
            default:
                // 嘗試讀取後端的 message 或 error 欄位，若都沒有則顯示 axios 的預設錯誤訊息
                toast.error("發生錯誤: " + (backendMsg || error.message));
        }

        return Promise.reject(error);
    }
);

export default instance;
