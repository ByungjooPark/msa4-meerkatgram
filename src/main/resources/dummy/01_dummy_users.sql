-- --------------------------------------------------------
-- 호스트:                          112.222.157.156
-- 서버 버전:                        8.4.5 - MySQL Community Server - GPL
-- 서버 OS:                        Linux
-- HeidiSQL 버전:                  12.17.0.7270
-- --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8 */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

-- 테이블 데이터 meerkatgram.users:~6 rows (대략적) 내보내기
DELETE FROM `users`;
INSERT INTO `users` (`id`, `email`, `password`, `nick`, `provider`, `role`, `profile`, `refresh_token`, `created_at`, `updated_at`, `deleted_at`) VALUES
  (3, 'admin@admin.com', '$2b$10$QRDQTEuoyN7MuAjVbXxybOX3syX2Bob.uxjAGWM6sTEfPBJsFEnTC', '미어캣관리자', 'NONE', 'SUPER', 'http://localhost:3000/api/files/profiles/20251130_15731d1d-001e-4ed2-99fa-733265367040.png', 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOjMsImlhdCI6MTc2NTEwNjEyOCwiZXhwIjozMDYxMTA2MTI4LCJpc3MiOiJtZWVya2F0QGdyZWVuLW1lZXJrYXQua3JvLmtyIn0.p2F-2ST6GUlXtGsKuq72SJ5ltzBtdpHGHWpQoyMkQX0', '2025-11-29 10:51:44', '2025-12-07 20:15:28', NULL),
  (4, 'admin2@admin.com', '$2b$10$.TG4EmgL.24cAY5F3v0sFevRIdAD3ouyrOpbIESiCfXRyXsdgczI.', '미어캣관리자2', 'KAKAO', 'NORMAL', 'http://localhost:3000/api/files/profiles/20251129_a28e83bd-023a-46e5-bbc3-6dde6c264901.jpg', 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOjQsImlhdCI6MTc2NDg1OTI2MywiZXhwIjozMDYwODU5MjYzLCJpc3MiOiJtZWVya2F0QGdyZWVuLW1lZXJrYXQua3JvLmtyIn0.LoSnNPOcCQuVbXVU1KZmyfoQ0yzhzou0R6WOUsm2RO8', '2025-11-29 10:51:44', '2025-12-04 23:41:03', NULL),
  (5, 'milkpbj1@naver.com', '$2b$10$a7h0ELNNO1D/o1O5R3vKF.tYZSZV3MUPYYRIvraeRTIH0l8FUchrG', '박병주', 'KAKAO', 'NORMAL', 'https://k.kakaocdn.net/dn/Lh0rn/dJMb99SiEGh/EsumxMqRkjlfRaIrt6ndk0/img_110x110.jpg', 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOjUsImlhdCI6MTc2NDc2NjEyNywiZXhwIjozMDYwNzY2MTI3LCJpc3MiOiJtZWVya2F0QGdyZWVuLW1lZXJrYXQua3JvLmtyIn0.noj2mcFGu0PXSdc5DE9jmjlDunKCzEIUzVXFvPjKT3U', '2025-12-02 22:00:39', '2025-12-03 21:48:47', NULL),
  (16, 'test@test.com', '$2a$10$I06qxLyBGGhhK0dY046lg.z5FCqYrxXCJmMGs/HSpc9v1mST3eVQO', '테스트', 'NONE', 'NORMAL', 'http://localhost:8080/images/profiles/b8c689f2-1a64-44d5-9782-3ea533761d0c.png', 'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzM4NCJ9.eyJzdWIiOiIxNiIsImlzcyI6Im1lZXJrYXRAbWVlcmthdC5rciIsImlhdCI6MTc3OTY5OTAyMCwiZXhwIjoxNzgwOTk1MDIwLCJyb2xlIjoiTk9STUFMIn0.Ahd_JVQhvdoSMD8_jKIoeckXZOl5evUu72sVYaHCTwIiP4AB2ICgdSdiAsYRcd0I', '2026-05-17 13:18:52', '2026-05-25 17:50:19', NULL),
  (17, 'test2@test2.com', '$2a$10$/OUiI.AQKq1/6T2Jjj6v.OVHMwcUPNbTBmeWIAvK4JZ9u78VGVqUa', 'test2', 'NONE', 'NORMAL', 'http://localhost:8080\\images\\profiles\\4a51eb9c-60f9-4ff2-87ca-267aefbcc462.png', 'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzM4NCJ9.eyJzdWIiOiIxNyIsImlzcyI6Im1lZXJrYXRAbWVlcmthdC5rciIsImlhdCI6MTc3OTc4NTA2NSwiZXhwIjoxNzgxMDgxMDY1LCJyb2xlIjoiTk9STUFMIn0.4LSyHB3BayB1wwQLqRu2JFY9hIwq9G47PT_HlMsviXua8CnkIriSkGmd9HgKkqbp', '2026-05-25 17:57:32', '2026-05-26 17:44:24', NULL),
  (18, 'test4@test.com', '$2a$10$Yk4av0jswHW690BNLq56qea5OU38n.Rq9uHN2tiDnB9ZV4kEn8FFu', 'test4', 'NONE', 'NORMAL', 'http://localhost:8080/images/profiles/20260526_5cfe528d-e8d4-47ef-b64c-17239ce8ecda.png', 'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzM4NCJ9.eyJzdWIiOiIxOCIsImlzcyI6Im1lZXJrYXRAbWVlcmthdC5rciIsImlhdCI6MTc3OTc4NTc0MiwiZXhwIjoxNzgxMDgxNzQyLCJyb2xlIjoiTk9STUFMIn0.KgXibY6g12PVbey_8QFg-Pm3tCcMP-usno1qTZeNO1ftRtS1WfY7nSMvntBFYHrU', '2026-05-26 16:12:40', '2026-05-26 17:55:42', NULL);

/*!40103 SET TIME_ZONE=IFNULL(@OLD_TIME_ZONE, 'system') */;
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IFNULL(@OLD_FOREIGN_KEY_CHECKS, 1) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40111 SET SQL_NOTES=IFNULL(@OLD_SQL_NOTES, 1) */;
