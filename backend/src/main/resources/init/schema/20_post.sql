-- 貼文(依賴:member → 編號必須大於 10)
CREATE TABLE post (
  id INT IDENTITY(1,1) PRIMARY KEY,
  member_id INT NOT NULL,
  image_data VARBINARY(MAX) NOT NULL,
  image_content_type NVARCHAR(100),
  description NVARCHAR(MAX),
  is_hidden BIT DEFAULT 0, -- SQL Server 沒有 BOOLEAN 型別,布林值用 BIT(0/1)
  created_at DATETIME2 DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE
);
