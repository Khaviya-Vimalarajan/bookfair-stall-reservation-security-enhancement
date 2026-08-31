USE `bookfair`;
INSERT INTO `users` (`sub`, `email`, `name`, `phone`, `role`, `active`, `created_at`, `updated_at`) 
VALUES ('auth0|6a9506a01932aa1457e29411', 'vendor@gmail.com', 'vendor@gmail.com', '', 'STALL_VENDOR', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE `sub` = VALUES(`sub`), `role` = VALUES(`role`), `active` = VALUES(`active`);
