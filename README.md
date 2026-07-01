# 寵物照片分享平台 Paw Post 專案規格書

本教學專案提供寵物照片分享平台，支援使用者登入、上傳照片、瀏覽及按讚功能。

## 技術棧

- **前端**: Vue.js 搭配 Vuetify
- **後端**: Spring Boot
- **資料庫**: H2 Database
- **ORM**: Spring Data JPA

---

## 功能需求

1. **登入與登出**
   - 透過帳號密碼登入。
   - 前端清除 Token 即完成登出，無須後端 API。

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
| `created_at`| DATETIME | 建立時間 | 預設當前時間 |

### 2. post 貼文表
儲存使用者發布的照片與描述。

| 欄位名稱 | 資料型別 | 說明 | 備註 |
| --- | --- | --- | --- |
| `id` | INT | 唯一識別碼 | 主鍵, Auto Increment |
| `member_id` | INT | 發布者 | 外鍵 |
| `image_data` | BLOB | 圖片二進位資料 | 後端存為 byte[] |
| `description`| TEXT | 文字描述 | 允許空白 |
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
  成功時回傳 Token：
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
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
        "isLiked": false
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
  - `image`: 圖片檔案實體，前端需先壓縮
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
