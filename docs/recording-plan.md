# Paw Post 教學錄影計畫

前提:學生已完成環境安裝,包含 JDK、Node.js、SQL Server Express 與相關設定。
本文件是錄影用的順序腳本,標注每段的講解重點、手打或貼上的建議,以及預估時間。

---

## 總覽

建議分四部錄影,每部可獨立成一支影片,總長約 2 小時 40 分。

| 部 | 主題 | 預估時間 |
| --- | --- | --- |
| 第一部 | 後端基礎與登入 | 70 分 |
| 第二部 | 前端基礎與登入 | 45 分 |
| 第三部 | 貼文功能前後端 | 45 分 |
| 第四部 | 管理員功能與總複習 | 22 分 |

若必須壓在兩小時內:第四部獨立成進階影片,且所有 Vuetify 模板與 SQL 一律貼上不手打。

## 通用原則

1. **開場先展示完成品**。讓學生知道終點長什麼樣,再開始動手。
2. **手打與貼上的分界**:含新觀念的程式手打,重複性的模板、SQL、設定檔貼上。
3. **DTO 隨用隨建**。不要一開始把四支 DTO 全建完,每支 DTO 都在對應 API 出現時才建,強化 DTO 對應 API 的觀念。
4. **撞牆點是設計好的**。CORS 一段刻意讓錯誤先發生再解決,腳本要預留,不是意外翻車。
5. 程式註解已內建教學標記,錄到該檔案時照著標記補充即可。

---

## 第一部:後端基礎與登入,約 70 分

### 1. 專案目標與規格,8 分

- Demo 完成品:登入、瀏覽、發文、按讚、管理員禁文。
- README 規格走讀:三張資料表、八支 API、狀態碼慣例。
- 先用生活比喻帶過 401 與 403 的差異,後面例外處理章節會正式收斂。

### 2. 後端專案建立,6 分

- Spring Initializr 建立專案,依賴:Web、Data JPA、Lombok、Devtools、Actuator、MSSQL Driver。
- jjwt 與 spring-security-crypto 手動加進 pom,說明為什麼不用完整的 Spring Security。
- application.properties 手打,講 trustServerCertificate 與 app.db.reset 兩個設定的意義。
- 啟動一次,確認連得上資料庫。

### 3. Model 與 Repository,15 分

- `Member` 手打詳講:@Entity、@Id、IDENTITY、@CreationTimestamp、camelCase 自動轉 snake_case 的命名策略。
- `Post` 手打:LAZY 關聯、@Lob、為什麼 Entity 不用 @Data——toString 會印出整張圖片、equals 會踩 Lazy 關聯。
- `PostLike` 貼上快講。
- Repository 三支:重點講方法名稱衍生查詢,用 `findByUsername` 當第一個例子。

### 4. 假資料初始化,8 分

- schema 三支 SQL 貼上:檔案編號規則、NVARCHAR 與 N 前綴、BIT、CASCADE 只能一條路徑。
- data 三支 SQL 貼上:IDENTITY_INSERT、RESEED 999 的用意。
- 密碼欄的 BCrypt hash 一句帶過,登入章節會解釋。
- `DatabaseResetter` 直接發成品:只講它做什麼、以及正式環境絕對禁止的三個理由,不逐行講解。
- `Initialize` 快講:啟動後把示範照片載進貼文。
- 啟動應用,用 SSMS 看到假資料。這是第一個成就感點。

### 5. 全域例外處理,6 分

- 四支自訂例外貼上,講「例外型別對應 HTTP 狀態碼」的設計。
- `GlobalExceptionHandler` 貼上,重點兩個:401 與 403 的正式定義、兜底 500 不能洩漏內部細節。

### 6. 登入功能,25 分

本段是後端核心,建議全程手打。順序:

1. `JwtUtil`:builder 產生 token、parser 驗證,密鑰硬編碼的安全警告要停下來講。
2. `LoginRequestDto`、`LoginResponseDto`。
3. `AuthService`:BCrypt 驗證,呼應第 4 段的 hash,講為什麼同密碼不同 hash。
4. `AuthController`,此時先跳到第 7 段用 Postman 打通 /api/login 也可以。
5. `Role`、`LoggedInMember`、`LoggedInMemberHolder`:ThreadLocal 原理。
6. `JwtAuthFilter`:攔截流程、finally 一定要 clear、fail-closed 白名單設計。

### 7. Postman 驗證,5 分

- 登入取得 token,丟到 jwt.io 解開看 payload。
- 不帶 token 打 /api/posts 的 POST,得到 401,證明 Filter 生效。
- 帶 token 再打一次,得到 404,證明 Filter 放行、只是路徑還不存在。這個對比很有教學效果。

---

## 第二部:前端基礎與登入,約 45 分

### 8. 前端專案建立,8 分

- npm create vue 建立專案,安裝 vuetify、axios、pinia、persistedstate、toastification、mdi font。
- `main.js` 手打:講 Vuetify 全量載入的取捨。
- `.env.development`:API 位址與程式分離,對照後端的 application.properties。

### 9. Layout、Router、View 骨架,12 分

- `UserLayout`、`AdminLayout` 模板貼上,講 layout 加巢狀路由的外框模式。
- `router/index.js` 手打:巢狀路由、meta、全域守衛。
- 必講:前端守衛只是使用者體驗層,真正的權限防線在後端。
- `HomeView`、`LoginView` 先放空殼,能看到頁面切換即可。

### 10. 全域 API 工具,10 分

- `request.js` 手打,前端最有含金量的檔案:baseURL 讀環境變數、請求攔截器自動帶 token、回應攔截器統一發錯誤 toast。
- 401 特例必講:登入 API 的 401 是帳密錯誤,不能當成登入失效去登出跳轉。
- `api/auth.js` 建立,`api/post.js` 先建 getPosts 一支。

### 11. 前端登入,15 分

- `stores/auth.js` 手打:setup store 寫法、persist 持久化、import 取別名避免同名。
- `LoginView` 模板貼上、script 手打。
- **設計好的撞牆點**:按下登入,瀏覽器出現 CORS 錯誤。
  - 講為什麼 Postman 沒事、瀏覽器有事。
  - 回後端補 `WebConfig`,講 CorsFilter 必須排在 JwtAuthFilter 之前的原因。
  - 再登入一次,成功。
- `Navbar` 貼上:依登入狀態切換按鈕、isAdmin 才顯示後台入口。

---

## 第三部:貼文功能前後端,約 45 分

### 12. 後端貼文功能,18 分

- `PostResponseDto` 建立。
- `PostService` 手打:
  - getPosts 與 toDto:LAZY 關聯觸發查詢、Base64 編碼、Page.map 保留分頁資訊。
  - 隱藏貼文的清空邏輯照寫,一句預告「這是管理員功能,第四部詳講」。
  - createPost:廉價驗證放前面、Content-Type 不可盡信。
  - likePost、unlikePost:check then act 的併發備註帶過即可。
- `PostController` 手打:分頁參數 clamp 防禦、image 設 required false 的理由、雙排序鍵。
- **N+1 短示範,4 分**:打開 application.properties 裡註解掉的 show-sql,重整列表,看主控台噴出幾十條 SQL。一句話點出這叫 N+1,優化方向在 README 進階課題,點到為止。

### 13. 前端貼文功能,25 分

- `api/post.js` 補齊 createPost、likePost、unlikePost。
- `HomeView` script 手打、模板貼上:載入更多、offset 分頁的已知取捨。
- `PostCard` script 手打、模板貼上:
  - props 唯讀,更新用 emit 回報、父層替換,這是 Vue 核心觀念。
  - 按讚的防連點與樂觀更新。
- `stores/feed.js`:沒有父子關係的元件如何互相通知,refreshSignal 模式。
- `UploadDialog` script 手打、模板貼上:defineModel、input 清空讓同檔可重選的技巧。
- `utils/image.js` 貼上:canvas 壓縮流程、JPEG 不支援透明所以先鋪白底。
- 完整流程 demo:發文、列表自動刷新、按讚、取消讚。

---

## 第四部:管理員功能與總複習,約 22 分

### 14. 管理員功能,18 分

- 後端:
  - `AdminPostSummaryDto`:API 只回剛好夠用的資料,對比含圖片的 DTO 會多傳幾十 MB。
  - `AdminController`:requireAdmin 丟 403,對照 Filter 的 401,收斂第一部埋的伏筆。
  - `togglePostHidden` 與 likePost 的禁文檢查。
- 前端:
  - `AdminHomeView` 貼上:v-pagination 頁碼從 1 開始的換算。
  - 回頭看 `PostCard` 的遮罩邏輯:一般人看遮罩、管理員看原文加標記。
- 用 Alice 與 Bob 兩個帳號實際 demo 禁文前後的差異。

### 15. 總複習,4 分

- 一個請求的完整旅程:View、api 層、攔截器、Filter、Controller、Service、Repository。
- 三個貫穿全課的觀念:前後端雙防線、DTO 剛好夠用、例外對應狀態碼。
- 指向 README 進階課題,交代後續自學方向。

---

## 檔案與段落對照表

| 段落 | 檔案 | 處理方式 |
| --- | --- | --- |
| 2 | pom.xml、application.properties | 手打設定檔 |
| 3 | Member、Post、PostLike、三支 Repository | Member 與 Post 手打 |
| 4 | init 下六支 SQL、DatabaseResetter、Initialize | SQL 貼上、Resetter 發成品 |
| 5 | 四支例外、GlobalExceptionHandler | 貼上講重點 |
| 6 | JwtUtil、AuthService、AuthController、JwtAuthFilter、Holder、Role、Login 兩支 DTO | 手打 |
| 8 | main.js、.env.development | 手打 |
| 9 | 兩支 Layout、router、View 空殼 | Layout 貼上、router 手打 |
| 10 | request.js、api/auth.js | 手打 |
| 11 | stores/auth.js、LoginView、Navbar、WebConfig | script 手打、模板貼上 |
| 12 | PostResponseDto、PostService、PostController | 手打 |
| 13 | HomeView、PostCard、UploadDialog、stores/feed.js、utils/image.js、api/post.js | script 手打、模板貼上 |
| 14 | AdminPostSummaryDto、AdminController、AdminHomeView | 貼上講重點 |
