import axios from "axios";
import { useAuthStore } from "../stores/auth";
import { useToast } from "vue-toastification";
import router from "../router";

const instance = axios.create({
    // API 位址不寫死在程式裡，改由環境變數提供，見 .env.development——
    // 部署到正式環境時只要換設定檔，不用改程式
    baseURL: import.meta.env.VITE_API_BASE_URL,
    // 上傳圖片在較慢的網路可能超過 10 秒，放寬到 30 秒
    timeout: 30000,
    // 不需要手動設定 Content-Type：axios 會依資料型別自動處理——
    // 傳物件是 application/json、傳 FormData 是 multipart/form-data 並補上 boundary
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

// 回應攔截：HTTP 錯誤的 toast 統一在這裡發出，
// 各頁面的 catch 只需要 console.error，不要再 toast 一次
instance.interceptors.response.use(
    (response) => {
        return response;
    },
    (error) => {
        // 在攔截器「內部」呼叫 useToast()，理由同上方的 useAuthStore()：
        // 確保拿到的是 main.js 註冊的同一個 Toast 實例，設定只需維護一份
        const toast = useToast();

        if (!error.response) {
            toast.error("網路錯誤或伺服器無回應");
            return Promise.reject(error);
        }

        // 使用 ?. 也就是 Optional Chaining，防止後端沒有回傳 JSON 格式時發生「找不到屬性」的 TypeError
        const backendMsg = error.response?.data?.message || error.response?.data?.error;

        switch (error.response.status) {
            case 401: {
                // 【陷阱】登入 API 本身的 401 代表「帳號或密碼錯誤」，
                // 不是「登入狀態失效」，不能登出跳轉，否則錯誤訊息會被頁面跳轉吃掉。
                // 用「完全相等」而非 includes() 比對，避免未來其他路徑剛好包含 login 而誤中
                if (error.config?.url === "/login") {
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
