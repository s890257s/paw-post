import { defineStore } from 'pinia';
import { ref } from 'vue';

/**
 * 貼文列表的跨元件溝通 store。
 *
 * 【為什麼需要這個 store？】
 * 「發布照片」的 UploadDialog 掛在 Navbar 底下，而貼文列表在 HomeView，
 * 兩者沒有父子關係，事件 (emit) 傳不到。
 * 遇到「不相關的元件要互相通知」時，Pinia store 就是最適合的橋樑：
 * 發布成功後呼叫 notifyPostCreated()，HomeView 以 watch 監聽 refreshSignal 重新載入列表。
 */
export const useFeedStore = defineStore('feed', () => {
    // 每次發文成功就 +1，數值本身沒有意義，變動本身就是「該刷新了」的訊號
    const refreshSignal = ref(0);

    const notifyPostCreated = () => {
        refreshSignal.value++;
    };

    return {
        refreshSignal,
        notifyPostCreated
    };
});
