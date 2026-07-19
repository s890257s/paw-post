-- 會員假資料
-- 密碼皆為 1234。
-- 注意:五筆 hash 各不相同 —— BCrypt 每次都會產生隨機 salt,
-- 因此「相同的密碼」也會得到「不同的 hash」,這正是 BCrypt 防彩虹表攻擊的關鍵。

-- 明確指定 id 而不是讓 IDENTITY 自動編號:
-- 讓其他表的 FK 引用「可搜尋、刪除不位移」,是多表假資料好維護的關鍵。
-- SQL Server 要對 IDENTITY 欄位塞明確值,必須先開 IDENTITY_INSERT,且一次只能開一張表。
SET IDENTITY_INSERT member ON;
INSERT INTO member (id, username, password, role) VALUES
(1, N'Alice', '$2a$10$T7QrO1KovLSWa9v6l/kMjeKb519anC3K57IVupWvNstbWG.L4NVZC', 'ADMIN'),
(2, N'Bob', '$2a$10$qFY/yAx1OMpdxODj5Kxyvu4/gsSg3LxjNWkUD8qMoffeI6AtylYl6', 'USER'),
(3, N'Charlie', '$2a$10$7Q5agurg2vvfz/dviNs9Uelw0V/VA0t8vaCgNhX5S0lU/yb1eafCq', 'USER'),
(4, N'David', '$2a$10$dtwGhXFnsfRtqGq/.CnHueo5227qO.hwvPN2iquPT6xNYwvuc/exu', 'USER'),
(5, N'Eve', '$2a$10$c09tTM6LEiY7DBJhwSXN.OhsaRGAA3pF4mdzW6cmVm/.hW7VyctI2', 'USER');
SET IDENTITY_INSERT member OFF;

-- 假資料佔用 id 1~999;讓應用程式執行期新增的資料從 1000 開始,一眼可分辨
DBCC CHECKIDENT ('member', RESEED, 999);
