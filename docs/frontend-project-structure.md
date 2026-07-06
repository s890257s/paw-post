# Vue 前端專案常見架構介紹

本文件介紹 Vue 3 專案（Vite + `<script setup>`）業界常見的目錄結構、每個目錄的職責，
並以本專案（paw-post）的實際程式碼作為對照範例。

---

## 一、整體結構總覽

一個常見的中型 Vue 專案長這樣（`*` 標記的是 paw-post 目前實際有的目錄）：

```text
frontend/
├── public/                  # * 不經打包、原封不動複製的靜態檔案
├── src/
│   ├── api/                 # * HTTP 請求層（對應後端 API）
│   ├── assets/              #   會被打包處理的靜態資源（圖片、全域 CSS）
│   ├── components/          # * 可複用的 UI 元件
│   ├── composables/         #   可複用的邏輯（useXxx 函式）
│   ├── directives/          #   自訂指令（v-xxx）
│   ├── layouts/             # * 頁面外框（版型）
│   ├── plugins/             #   第三方套件的初始化設定
│   ├── router/              # * 路由設定
│   ├── stores/              # * Pinia 全域狀態
│   ├── utils/               # * 與框架無關的純工具函式
│   ├── views/               # * 頁面級元件（與路由一一對應）
│   ├── App.vue              # * 根元件
│   └── main.js              # * 應用程式進入點
├── index.html               # * SPA 的 HTML 殼
├── vite.config.js           # * 建置工具設定
├── jsconfig.json            # * 編輯器的路徑提示設定（@ 別名）
└── package.json             # * 依賴與指令
```

沒有硬性規定一定要有全部目錄——**用到才建**。paw-post 目前沒有
`composables/`、`directives/`、`plugins/`，因為還沒有對應的需求，這是正確的做法，
不要為了「架構完整」而建一堆空目錄。

---

## 二、各目錄詳細職責

### 1. `public/` — 不打包的靜態檔案

放「打包時原封不動複製到輸出目錄」的檔案，例如 `favicon.ico`、`robots.txt`。

- 引用方式是**絕對路徑**：`/favicon.ico`。
- 和 `assets/` 的差別：`public/` 的檔案不會被 Vite 處理（不壓縮、檔名不加 hash），
  `assets/` 的檔案會被打包、優化、加上 hash 檔名以利瀏覽器快取。

**判斷方式**：檔案需要固定的檔名或路徑（如 favicon）→ `public/`；
其他一律放 `assets/`。

### 2. `src/api/` — HTTP 請求層

**職責：封裝所有和後端溝通的程式碼。** 元件裡不應該直接出現 `axios.get(...)`，
而是呼叫這一層提供的具名函式。

用 Spring 的概念類比：這一層才是最接近後端 **Service / Repository** 的角色——
無狀態、純粹封裝「怎麼取資料」的邏輯。

典型結構是「一個共用的 request 實例 + 依領域拆分的 API 模組」：

```text
api/
├── request.js   # axios 實例：baseURL、攔截器（帶 token、統一錯誤處理）
├── auth.js      # 認證相關 API：login()
└── post.js      # 貼文相關 API：getPosts()、hidePost()、unhidePost()...
```

paw-post 的實例（`src/api/post.js` 風格）：

```js
import request from './request';

// 每個函式對應一支後端 API，元件只需要知道「函式名」，不需要知道 URL
export const getAdminPosts = (page, size) =>
    request.get('/admin/posts', { params: { page, size } });

export const hidePost = (id) => request.post(`/admin/posts/${id}/hide`);
```

這樣分層的好處：

1. **URL 只寫一次**：後端改路徑時只改一個檔案。
2. **攔截器統一處理橫切關注點**：`request.js` 的請求攔截器自動附上 JWT token、
   回應攔截器統一跳出錯誤 toast——元件完全不用管（對應 Spring 的 Filter / Interceptor / `@ControllerAdvice`）。
3. **元件可讀性**：`getAdminPosts(0, 20)` 比一長串 axios 設定好讀得多。

**補充：有些專案叫 `services/`。** 這個目錄在別人的專案裡常見兩種用法：

- **當作 `api/` 的別名（最常見）**：內容和這裡的 `api/` 完全一樣，
  只是命名品味不同（`postService.js` vs `post.js`），看到時等價理解即可。
- **在 `api/` 之上多墊一層業務邏輯層（較大型專案）**：兩者並存時，
  `api/` 只放「一個函式對一支端點」的純 HTTP 呼叫，`services/` 負責業務編排——
  組合多支 API、轉換資料格式、處理業務規則。例如「下訂單」要先查庫存、
  再建訂單、失敗要回滾，這串流程包成 `orderService.placeOrder()`，
  view 只呼叫這一個函式。這種用法才真正接近 Spring 的 service 概念：
  無狀態、封裝商業邏輯、位於 view（controller）和 api（repository）之間。

**本專案不需要第二種**：每個操作幾乎都是一支 API 打完就結束，
中間沒有可編排的業務邏輯，多墊一層只會產生一堆單行轉呼叫的空殼函式
（`hidePost(id) => api.hidePost(id)`），屬於過度設計。
等到出現「一個動作要串多支 API、還夾雜規則判斷」而且 view 開始變髒時，
再引入這一層才有意義。

### 3. `src/assets/` — 會被打包的靜態資源

放圖片、字型、全域 CSS/SCSS 等。在程式碼中用相對路徑或 `@/assets/...` 引用，
Vite 會在打包時處理它們（優化、檔名加 hash）。

```text
assets/
├── images/
│   └── logo.png
└── styles/
    └── main.css     # 全域樣式、CSS 變數
```

### 4. `src/components/` — 可複用的 UI 元件

**職責：不綁定特定路由、可以被多個頁面重複使用的元件。**

paw-post 的實例：

| 元件 | 職責 |
|---|---|
| `Navbar.vue` | 導覽列，掛在 layout 上，每一頁都會出現 |
| `PostCard.vue` | 單張貼文卡片，HomeView 用 `v-for` 渲染多張 |
| `UploadDialog.vue` | 發文對話框，由 Navbar 開啟 |

元件的溝通原則是 **「props 往下傳、emit 往上報」**：

```vue
<!-- 父層（HomeView）決定資料，子層（PostCard）只負責顯示與回報事件 -->
<PostCard
  v-for="post in posts"
  :key="post.id"
  :post="post"
  @like="handleLike"
/>
```

專案變大後，這個目錄常再分兩層：

```text
components/
├── common/      # 跨業務的通用元件：ConfirmDialog、EmptyState、BaseButton
└── post/        # 特定領域的元件：PostCard、PostImageCarousel
```

**和 `views/` 的分界**：會被路由直接渲染的是 view，被 view（或其他元件）
引用的是 component。component 不應該知道「自己在哪個網址下」。

### 5. `src/composables/` — 可複用的邏輯

**職責：把「有狀態的邏輯」抽成可重複使用的函式**，命名慣例是 `useXxx.js`。
這是 Vue 3 Composition API 帶來的核心模式。

關鍵特性：**每次呼叫都產生一份全新的、獨立的狀態**（類比：像 `new` 一個 class 的實例）。
這是它和 store 最大的差別——store 是全域單例，所有人共用同一份。

範例：把分頁邏輯抽成 composable，讓「文章管理頁」和「用戶管理頁」共用，
但兩頁的頁碼各自獨立、互不干擾：

```js
// composables/usePagination.js
import { ref, watch } from 'vue';

export function usePagination(fetcher) {
    const items = ref([]);
    const page = ref(1);          // v-pagination 從 1 開始
    const totalPages = ref(0);
    const isLoading = ref(false);

    const load = async () => {
        isLoading.value = true;
        try {
            // API 的頁碼從 0 開始，換算
            const res = await fetcher(page.value - 1);
            items.value = res.data.content;
            totalPages.value = res.data.totalPages;
        } finally {
            isLoading.value = false;
        }
    };

    watch(page, load);   // 換頁自動重載

    return { items, page, totalPages, isLoading, load };
}
```

```vue
<!-- views/admin/AdminHomeView.vue 使用它 -->
<script setup>
import { onMounted } from 'vue';
import { usePagination } from '@/composables/usePagination';
import { getAdminPosts } from '@/api/post';

const { items: posts, page, totalPages, isLoading, load } =
    usePagination((page) => getAdminPosts(page, 20));

onMounted(load);
</script>
```

**什麼時候抽 composable**：

1. 同一套邏輯出現在第二個地方（如上面的分頁）。
2. 單一元件的 `<script setup>` 太肥（兩三百行），想拆出來整理——即使不複用也可以抽。

### 6. `src/directives/` — 自訂指令

放自訂的 `v-xxx` 指令，用於「直接操作 DOM」的可複用行為，例如：

- `v-focus`：元素掛載後自動聚焦。
- `v-permission="'admin'"`：沒有權限就把按鈕從 DOM 移除。

用得比 composable 少很多；大多數邏輯複用應優先考慮 composable，
只有需要直接碰 DOM 元素本身時才用指令。

### 7. `src/layouts/` — 頁面外框（版型）

**職責：定義「多個頁面共用的外框」**——導覽列、側邊欄、頁尾，
中間留一個 `<router-view />` 給實際頁面內容。

paw-post 的實例：

```text
layouts/
├── UserLayout.vue    # 前台外框：上方 Navbar + 內容區
└── AdminLayout.vue   # 後台外框：管理側邊欄 + 內容區
```

搭配路由的巢狀結構使用：

```js
// router/index.js
const routes = [
    {
        path: '/',
        component: UserLayout,        // 外框
        children: [
            { path: '', component: HomeView },      // 塞進外框的 <router-view />
        ],
    },
    {
        path: '/admin',
        component: AdminLayout,       // 後台換一個外框
        children: [
            { path: '', component: AdminHomeView },
        ],
    },
];
```

好處：前台/後台的外觀骨架只寫一次，新增頁面時只要專注內容本身。

### 8. `src/plugins/` — 第三方套件初始化

當 `main.js` 因為一堆套件設定（Vuetify、i18n、toast…）越長越亂時，
把每個套件的初始化拆成一個檔案放這裡，`main.js` 只負責 `app.use(...)`：

```js
// plugins/vuetify.js
import { createVuetify } from 'vuetify';
export default createVuetify({ /* 主題、icon 設定 */ });
```

```js
// main.js —— 保持乾淨，只做「組裝」
import vuetify from '@/plugins/vuetify';
app.use(vuetify);
```

小專案（如 paw-post）直接寫在 `main.js` 也完全可以，設定變多再拆。

### 9. `src/router/` — 路由設定

**職責：定義「網址 ↔ 頁面」的對應**，以及**導航守衛**（前端的存取控制）。

```text
router/
└── index.js    # 路由表 + 全域守衛
```

導航守衛是這一層的重點，類比 Spring Security 的 filter chain
（但注意：前端守衛只是 UX 層面的擋路，真正的授權必須由後端做）：

```js
router.beforeEach((to) => {
    const authStore = useAuthStore();

    // 需要登入的頁面，沒 token 就導去登入頁
    if (to.meta.requiresAuth && !authStore.token) {
        return { name: 'login' };
    }
    // 後台頁面要檢查管理員身分
    if (to.meta.requiresAdmin && !authStore.user?.isAdmin) {
        return { name: 'home' };
    }
});
```

路由多了之後，常依模組拆檔（`routes/admin.js`、`routes/user.js`），
`index.js` 只做合併。

### 10. `src/stores/` — Pinia 全域狀態

**職責：持有「全域只有一份、多個元件共享」的狀態。**

和 Spring service 的類比是**錯誤的**——service 是無狀態的邏輯，
store 恰恰相反，存在的意義就是**持有狀態**。前端是單一使用者環境，
所以「全域可變狀態」在這裡是安全且必要的。

**只有符合以下情境才建 store**：

1. **多個不相關的元件要讀同一份資料** —— `auth.js`：
   Navbar 顯示使用者名稱、路由守衛檢查 `isAdmin`、axios 攔截器取 `token`，
   三個互不相干的地方共用同一份登入狀態。
2. **沒有父子關係的元件要互相通知** —— `feed.js`：
   UploadDialog（掛在 Navbar 下）發文成功後，要通知 HomeView 刷新列表，
   兩者 emit 傳不到，用 store 的 `refreshSignal` 當橋樑。
3. **狀態要跨路由存活**：例如購物車，切到別頁再回來內容還在。

**反例**：頁面自己用的 `posts`、`isLoading`（如 AdminHomeView）**不要**搬進 store——
狀態只有一頁在用，搬進全域單例只會造成「離開頁面後舊資料還留著」的問題。

本專案採用的 setup store 寫法，第二個參數其實就是一個 composable：

```js
// store = composable + 「Pinia 把執行結果快取成全域單例」
export const useAuthStore = defineStore('auth', () => {
    const token = ref('');
    const login = async (loginRequest) => { /* ... */ };
    return { token, login };
}, {
    persist: true,   // 搭配 persist 套件自動存 localStorage，重新整理不掉登入
});
```

### 11. `src/utils/` — 純工具函式

**職責：與 Vue 完全無關的純函式**——不 import `ref`、不碰元件、
給相同輸入永遠得到相同輸出。

paw-post 的實例：`utils/image.js`（圖片壓縮/轉檔之類的處理）。
其他常見例子：日期格式化、金額千分位、debounce。

**和 composable 的分界**：函式裡用到了 `ref`、`watch` 等響應式 API → composable；
純資料進、純資料出 → utils。

### 12. `src/views/` — 頁面級元件

**職責：與路由一一對應的「頁面」**，也叫 pages（Nuxt 的慣例）。
每個 view 負責：組裝 components、呼叫 api、管理這一頁自己的狀態。

paw-post 的實例：

```text
views/
├── LoginView.vue          # /login
├── HomeView.vue           # /（前台動態牆）
└── admin/                 # 依模組建子目錄
    └── AdminHomeView.vue  # /admin（文章管理）
```

view 就是「這一頁的 controller」：它知道要載什麼資料、什麼時候載、
資料怎麼分配給底下的 components。頁面私有的狀態（列表、頁碼、loading）
就留在 view 裡，不要外移。

### 13. 根層級檔案

| 檔案 | 職責 |
|---|---|
| `index.html` | SPA 唯一的 HTML，只有一個 `<div id="app">`，Vue 掛載在上面 |
| `src/main.js` | 進入點：`createApp` → 註冊 router、pinia、UI 套件 → `mount('#app')` |
| `src/App.vue` | 根元件，通常只有一個 `<router-view />`，一切內容由路由決定 |
| `vite.config.js` | 建置設定：`@` 路徑別名、dev server 的 proxy（把 `/api` 轉給後端避開 CORS） |
| `jsconfig.json` | 讓編輯器看懂 `@/` 別名，提供路徑自動完成與跳轉 |

---

## 三、完整範例：一個請求從頭到尾經過哪些層

以「管理員在後台禁用一篇文章」為例，追一遍資料流：

```text
① views/admin/AdminHomeView.vue
   使用者點「禁用文章」按鈕 → 呼叫 handleToggleHide(post)
        │
② api/post.js
   handleToggleHide 呼叫 hidePost(post.id)
   （view 不知道 URL、不知道 axios 的存在）
        │
③ api/request.js（請求攔截器）
   自動從 stores/auth.js 取出 token，加到 Authorization header
        │
④ ──── HTTP ────▶ 後端 Spring Boot 驗證 JWT、執行禁用、回傳 200
        │
⑤ api/request.js（回應攔截器）
   若失敗（401/403/500），統一跳 toast 錯誤訊息
        │
⑥ views/admin/AdminHomeView.vue
   成功 → 更新 post.isHidden = true，畫面上的 chip 立即變成「已禁用」
```

每一層只做自己的事：view 管畫面與頁面狀態、api 管 HTTP、
store 管跨元件共享的 token、攔截器管橫切關注點。
改其中一層不需要動到其他層，這就是分層的價值。

---

## 四、常見判斷問題速查

| 問題 | 判斷 |
|---|---|
| 這段狀態要放 view 還是 store？ | 只有這一頁用 → view；多個不相關元件共用 → store |
| 邏輯太長想抽出去，抽成什麼？ | 仍是單頁/各自獨立的狀態 → composable；需要全域唯一 → store |
| 這是 component 還是 view？ | 路由直接渲染 → view；被別人引用 → component |
| 這函式放 utils 還是 composables？ | 用到 `ref`/`watch` → composable；純函式 → utils |
| 靜態檔放 public 還是 assets？ | 需要固定檔名（favicon）→ public；其他 → assets |
| 可以直接在元件裡寫 axios 嗎？ | 不要，一律經過 `api/` 層 |

---

## 五、延伸：專案更大之後的演進方向

目前的「依技術角色分層」（api/、components/、views/…）適合中小型專案。
當專案大到數十個頁面、多人協作時，業界常改採 **feature-based（依功能模組分）**：

```text
src/
├── features/
│   ├── auth/
│   │   ├── api.js
│   │   ├── LoginView.vue
│   │   └── store.js
│   └── post/
│       ├── api.js
│       ├── components/
│       └── views/
└── shared/            # 跨功能共用的 components、utils
```

好處是「改一個功能時，相關檔案都在同一個資料夾」；
代價是結構規範要靠團隊紀律維持。**paw-post 的規模用現在的分層方式就是最合適的**，
這裡只是先知道有這個方向。
