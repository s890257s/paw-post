-- 會員
INSERT INTO member (username, password) VALUES
('Alice', '$2a$10$XnH32heSv/TzbU9P.IMk7.sZi7pdYFVw4CpPV0JB7nF6r3kYnHkZG'),
('Bob', '$2a$10$XnH32heSv/TzbU9P.IMk7.sZi7pdYFVw4CpPV0JB7nF6r3kYnHkZG'),
('Charlie', '$2a$10$XnH32heSv/TzbU9P.IMk7.sZi7pdYFVw4CpPV0JB7nF6r3kYnHkZG'),
('David', '$2a$10$XnH32heSv/TzbU9P.IMk7.sZi7pdYFVw4CpPV0JB7nF6r3kYnHkZG'),
('Eve', '$2a$10$XnH32heSv/TzbU9P.IMk7.sZi7pdYFVw4CpPV0JB7nF6r3kYnHkZG');

-- 貼文
INSERT INTO post (member_id, image_data, description) VALUES
(1, '', '我家狗狗今天的萌照！'),
(1, '', '下午散步的風景。'),
(2, '', '貓咪睡整天都不想理我。'),
(2, '', '終於買了新的貓抓板！'),
(2, '', '等晚餐的小可愛。'),
(3, '', '酷酷小狗！'),
(3, '', '帶毛小孩去海邊玩水囉～'),
(4, '', '快看看我！看看我！'),
(4, '', '剛洗完澡，香噴噴的！'),
(5, '', '領養的橘貓變好胖。'),
(5, '', '睡得好香～');

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
