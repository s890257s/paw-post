-- 貼文假資料(member_id 對應 10_member.sql 明確指定的 id)
-- image_data 以 0x(空的二進位字面值)佔位,實際照片內容由 Initialize.java 於啟動後載入 (webp 格式)。
--   注意不能像 H2 時代用 '':SQL Server 不允許字串隱式轉成 VARBINARY。
-- 中文字串一律加 N 前綴(N'...' = NVARCHAR 字面值),否則會被當成 VARCHAR 而變成 ???
SET IDENTITY_INSERT post ON;
INSERT INTO post (id, member_id, image_data, image_content_type, description) VALUES
(1, 1, 0x, 'image/webp', N'我家狗狗今天的萌照！'),
(2, 1, 0x, 'image/webp', N'下午散步的風景。'),
(3, 2, 0x, 'image/webp', N'貓咪睡整天都不想理我。'),
(4, 2, 0x, 'image/webp', N'終於買了新的貓抓板！'),
(5, 2, 0x, 'image/webp', N'等晚餐的小可愛。'),
(6, 3, 0x, 'image/webp', N'酷酷小狗！'),
(7, 3, 0x, 'image/webp', N'帶毛小孩去海邊玩水囉～'),
(8, 4, 0x, 'image/webp', N'快看看我！看看我！'),
(9, 4, 0x, 'image/webp', N'剛洗完澡，香噴噴的！'),
(10, 5, 0x, 'image/webp', N'領養的橘貓變好胖。'),
(11, 5, 0x, 'image/webp', N'睡得好香～');
SET IDENTITY_INSERT post OFF;

DBCC CHECKIDENT ('post', RESEED, 999);
