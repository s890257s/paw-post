-- 會員
-- 密碼皆為 1234。
-- 注意：五筆 hash 各不相同 —— BCrypt 每次都會產生隨機 salt，
-- 因此「相同的密碼」也會得到「不同的 hash」，這正是 BCrypt 防彩虹表攻擊的關鍵。
INSERT INTO member (username, password, role) VALUES
('Alice', '$2a$10$T7QrO1KovLSWa9v6l/kMjeKb519anC3K57IVupWvNstbWG.L4NVZC', 'ADMIN'),
('Bob', '$2a$10$qFY/yAx1OMpdxODj5Kxyvu4/gsSg3LxjNWkUD8qMoffeI6AtylYl6', 'USER'),
('Charlie', '$2a$10$7Q5agurg2vvfz/dviNs9Uelw0V/VA0t8vaCgNhX5S0lU/yb1eafCq', 'USER'),
('David', '$2a$10$dtwGhXFnsfRtqGq/.CnHueo5227qO.hwvPN2iquPT6xNYwvuc/exu', 'USER'),
('Eve', '$2a$10$c09tTM6LEiY7DBJhwSXN.OhsaRGAA3pF4mdzW6cmVm/.hW7VyctI2', 'USER');

-- 貼文
-- image_data 先以空值佔位，實際照片內容由 Initialize.java 於啟動後載入 (webp 格式)
INSERT INTO post (member_id, image_data, image_content_type, description) VALUES
(1, '', 'image/webp', '我家狗狗今天的萌照！'),
(1, '', 'image/webp', '下午散步的風景。'),
(2, '', 'image/webp', '貓咪睡整天都不想理我。'),
(2, '', 'image/webp', '終於買了新的貓抓板！'),
(2, '', 'image/webp', '等晚餐的小可愛。'),
(3, '', 'image/webp', '酷酷小狗！'),
(3, '', 'image/webp', '帶毛小孩去海邊玩水囉～'),
(4, '', 'image/webp', '快看看我！看看我！'),
(4, '', 'image/webp', '剛洗完澡，香噴噴的！'),
(5, '', 'image/webp', '領養的橘貓變好胖。'),
(5, '', 'image/webp', '睡得好香～');

-- 按讚
INSERT INTO post_like (member_id, post_id) VALUES
(1, 3),
(1, 6),
(2, 1),
(2, 7),
(3, 1),
(3, 10),
(4, 2),
(4, 5),
(5, 4),
(5, 8);
