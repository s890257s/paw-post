-- 按讚假資料(member_id、post_id 分別對應 10_member.sql、20_post.sql 明確指定的 id)
SET IDENTITY_INSERT post_like ON;
INSERT INTO post_like (id, member_id, post_id) VALUES
(1, 1, 3),
(2, 1, 6),
(3, 2, 1),
(4, 2, 7),
(5, 3, 1),
(6, 3, 10),
(7, 4, 2),
(8, 4, 5),
(9, 5, 4),
(10, 5, 8);
SET IDENTITY_INSERT post_like OFF;

DBCC CHECKIDENT ('post_like', RESEED, 999);
