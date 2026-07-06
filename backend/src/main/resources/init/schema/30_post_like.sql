-- 按讚(依賴:member、post → 編號必須大於 10 和 20)
-- 注意:member_id 的外鍵「不能」加 ON DELETE CASCADE——
-- 否則刪 member 會有兩條串聯路徑通到 post_like(直接一條、經 post 間接一條),
-- SQL Server 禁止多重串聯路徑,建表時就會報錯 1785(H2 時代不會擋)。
-- 本專案沒有刪除會員的功能,所以拿掉這條 CASCADE 沒有實際影響。
CREATE TABLE post_like (
  id INT IDENTITY(1,1) PRIMARY KEY,
  member_id INT NOT NULL,
  post_id INT NOT NULL,
  created_at DATETIME2 DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (member_id) REFERENCES member(id),
  FOREIGN KEY (post_id) REFERENCES post(id) ON DELETE CASCADE,
  UNIQUE (member_id, post_id)
);
