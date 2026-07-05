# 寵物照片分享平台 Paw Post 專案規格書

本教學專案提供寵物照片分享平台，支援使用者登入、上傳照片、瀏覽及按讚功能。

## 技術棧

- **前端**: Vue.js 搭配 Vuetify
- **後端**: Spring Boot
- **資料庫**: H2 Database
- **ORM**: Spring Data JPA

---

## 如何啟動

### 後端 (需要 JDK)

```bash
cd backend
./mvnw spring-boot:run
```

- API 服務位於 `http://localhost:8080`
- H2 資料庫主控台：`http://localhost:8080/h2-console`（JDBC URL: `jdbc:h2:mem:pawpostdb`，帳號 `sa`，密碼空白）
- 資料庫為 in-memory，每次重啟都會重建並載入示範資料

### 前端 (需要 Node.js)

```bash
cd frontend
npm install
npm run dev
```

- 開發伺服器位於 `http://localhost:5173`
- 後端 API 位址設定在 `frontend/.env.development`（`VITE_API_BASE_URL`），
  部署到其他環境時只需調整設定檔，不用改程式碼

### 示範帳號

密碼皆為 `1234`：

| 帳號 | 角色 |
| --- | --- |
| `Alice` | ADMIN (管理員) |
| `Bob`、`Charlie`、`David`、`Eve` | USER (一般使用者) |

---

## 功能需求

1. **登入與登出**
   - 透過帳號密碼登入。
   - 前端清除 Token 即完成登出，無須後端 API。
   - 注意：這是 JWT 的固有特性 —— Token 一經簽發，在效期內後端無法「撤銷」它，
     登出只是前端不再使用該 Token。若需要真正的撤銷能力（例如封鎖帳號立即生效），
     需引入黑名單或改用 Session 等機制，這是進階課題。

2. **照片瀏覽** `不需登入`
   - 列表展示所有發布的照片。
   - 任何人皆可查看照片與發布者資訊。

3. **照片發布** `需登入`
   - 上傳照片並可附加文字描述。

4. **按讚與取消讚** `需登入`
   - 可對任何照片按讚，每張照片限按一次。
   - 再次點擊已按讚的照片即可取消。

5. **圖片壓縮** `前端實作`
   - 照片上傳前由前端自動壓縮，以節省頻寬與儲存空間。

---

## 資料庫設計

核心資料表包含 member、post 與 post_like。

### 1. member 會員表
儲存使用者帳號與密碼。

| 欄位名稱 | 資料型別 | 說明 | 備註 |
| --- | --- | --- | --- |
| `id` | INT | 唯一識別碼 | 主鍵, Auto Increment |
| `username` | VARCHAR | 登入帳號 | 唯一值 |
| `password` | VARCHAR | 登入密碼 |  |
| `role` | VARCHAR | 使用者角色 | 預設 'USER', 管理員為 'ADMIN' |
| `created_at`| DATETIME | 建立時間 | 預設當前時間 |

### 2. post 貼文表
儲存使用者發布的照片與描述。

| 欄位名稱 | 資料型別 | 說明 | 備註 |
| --- | --- | --- | --- |
| `id` | INT | 唯一識別碼 | 主鍵, Auto Increment |
| `member_id` | INT | 發布者 | 外鍵 |
| `image_data` | BLOB | 圖片二進位資料 | 後端存為 byte[] |
| `description`| TEXT | 文字描述 | 允許空白 |
| `is_hidden` | BOOLEAN | 是否被禁用 | 預設 FALSE |
| `created_at`| DATETIME | 發布時間 | 預設當前時間 |

### 3. post_like 按讚記錄表
防範重複按讚並記錄狀態。

| 欄位名稱 | 資料型別 | 說明 | 備註 |
| --- | --- | --- | --- |
| `id` | INT | 唯一識別碼 | 主鍵, Auto Increment |
| `member_id` | INT | 按讚者 | 外鍵 |
| `post_id` | INT | 目標照片 | 外鍵 |
| `created_at`| DATETIME | 按讚時間 | 預設當前時間 |

---

## API 規格

**身分驗證機制 JWT**
本專案透過 JWT 進行身分驗證。
標示為「需登入」的 API，前端請求必須在 HTTP Header 中夾帶 Token：
`Authorization: Bearer <JWT_Token>`

**HTTP 狀態碼慣例**（錯誤回應統一為 `{ "error": "...", "message": "..." }` 格式）

| 狀態碼 | 語意 | 範例情境 |
| --- | --- | --- |
| `401 Unauthorized` | 未登入或 Token 無效/過期 | 沒帶 Token 就按讚、密碼錯誤 |
| `403 Forbidden` | 已登入但權限不足 | 一般使用者呼叫管理員 API、對已禁用文章按讚 |
| `404 Not Found` | 查無資源 | 貼文 ID 不存在 |
| `400 Bad Request` | 請求內容不合法 | 發文未附圖片 |

### 1. 使用者登入
- **方法**: `POST`
- **路徑**: `/api/login`
- **權限**: 無
- **Request Body**:
  ```json
  {
    "username": "user123",
    "password": "password123"
  }
  ```
- **Response**: `200 OK` 成功 / `401 Unauthorized` 失敗
  成功時回傳 Token 以及是否為管理員：
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "username": "user123",
    "isAdmin": true
  }
  ```

### 2. 取得貼文列表
- **方法**: `GET`
- **路徑**: `/api/posts?page=0&size=10`
- **權限**: 無
- **說明**: 
  - 帶有 Token 則計算 isLiked 狀態，未帶 Token 則 isLiked 一律為 false。
  - 貼文預設依 createdAt 降序排列。
  - isLiked 用於標示當前發出請求的登入者是否已對此貼文按過讚。
  - **【重要】**若文章已被管理員禁用 (isHidden=true)，且當前請求者不是管理員，則 `imageBase64` 與 `description` 會被清空 (null)。
  - `imageBase64` 的 data URI 前綴會依圖片實際格式而定（例如 `data:image/webp;base64,...` 或 `data:image/jpeg;base64,...`）。
- **Query Parameters**:
  - `page`: 頁碼，從 0 開始，預設 0
  - `size`: 每頁筆數，預設 10
- **Response**: 遵循 Spring Data JPA Page 格式
  ```json
  {
    "content": [
      {
        "id": 1,
        "memberId": 1,
        "username": "user123",
        "imageBase64": "data:image/jpeg;base64,/9j/4AAQSk...",
        "description": "可愛的狗狗",
        "createdAt": "2026-06-30T10:00:00",
        "likeCount": 5,
        "isLiked": false,
        "isHidden": false
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10
    },
    "totalElements": 50,
    "totalPages": 5,
    "last": false
  }
  ```

### 3. 發布新照片
- **方法**: `POST`
- **路徑**: `/api/posts`
- **權限**: 需登入
- **Request**: Content-Type 為 multipart/form-data
  - `image`: 圖片檔案實體，前端需先壓縮（必填，缺少時回 `400`）
  - `description`: 文字描述
- **Response**: `201 Created`
  ```json
  {
    "id": 2,
    "imageBase64": "data:image/jpeg;base64,/9j/4AAQSk...",
    "description": "文字描述"
  }
  ```

### 4. 對照片按讚
- **方法**: `POST`
- **路徑**: `/api/posts/{id}/likes`
- **權限**: 需登入
- **Response**: `200 OK`

### 5. 取消照片按讚
- **方法**: `DELETE`
- **路徑**: `/api/posts/{id}/likes`
- **權限**: 需登入
- **Response**: `200 OK`

### 6. 禁用文章 (隱藏)
- **方法**: `PUT`
- **路徑**: `/api/admin/posts/{id}/hide`
- **權限**: 僅限管理員 (ADMIN)
- **Response**: `200 OK`；已登入但非管理員回 `403 Forbidden`

### 7. 解除禁用文章
- **方法**: `PUT`
- **路徑**: `/api/admin/posts/{id}/unhide`
- **權限**: 僅限管理員 (ADMIN)
- **Response**: `200 OK`；已登入但非管理員回 `403 Forbidden`

### 8. 後台貼文列表（摘要）
- **方法**: `GET`
- **路徑**: `/api/admin/posts?page=0&size=20`
- **權限**: 僅限管理員 (ADMIN)
- **說明**: 供後台管理表格使用，**刻意不含 `imageBase64`**。
  教學點：API 只回傳「剛好夠用」的資料——後台表格只顯示文字，
  沿用含圖片的貼文列表 API 會讓管理頁面下載大量用不到的圖片。
- **Query Parameters**:
  - `page`: 頁碼，從 0 開始，預設 0
  - `size`: 每頁筆數，預設 20
- **Response**: 遵循 Spring Data JPA Page 格式
  ```json
  {
    "content": [
      {
        "id": 1,
        "username": "user123",
        "description": "可愛的狗狗",
        "createdAt": "2026-06-30T10:00:00",
        "likeCount": 5,
        "isHidden": false
      }
    ],
    "totalElements": 50,
    "totalPages": 3
  }
  ```

---

## 專案教學與求職面試評估分析

此段落為針對「四個月培訓期（Java > Spring Boot > Vue）」新手工程師的專案規格評估。

### 一、 學習目標契合度

目前的規格精準涵蓋了核心技術，是優質的總結性練習專案：
*   **後端架構**：涵蓋 RESTful API、JWT 身分驗證、分頁查詢、關聯資料庫設計與基本權限控制。
*   **前端架構**：涵蓋單頁應用程式、Token 狀態管理、組件化開發及前端圖片壓縮。
*   **資料庫**：涵蓋基礎的 CRUD 操作與外鍵應用。
*   **結論**：絕對足夠讓學生學會並整合該學的知識。

### 二、 求職面試作品評估

作為教學範例，本專案僅提供基礎的串接演示。若要作為求職面試的主打作品，還缺乏足夠的競爭力：
1.  **架構缺陷**：將圖片存入資料庫會嚴重拖垮效能，且 H2 資料庫僅適合本地測試，加上缺乏實際上線部署。
2.  **商業邏輯單純**：僅有基礎功能與按讚，技術深度稍嫌單薄。

### 三、 學生的進階課題

教學範例僅此而已，若學生希望將其轉化為高含金量的面試作品，後續的優化將是他們自己的課題。這包含但不限於：
*   **資料庫與儲存**：遷移至正式的 SQL Server（本專案的 H2 已使用 MSSQL 相容模式與語法，遷移門檻最低），並導入雲端物件儲存處理圖片。
*   **實務架構**：前後端分離部署上線、串接第三方登入、導入 Redis 效能優化。
*   **工程品質**：撰寫自動化測試與 CI/CD 流程。
