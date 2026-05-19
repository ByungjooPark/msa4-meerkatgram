CREATE TABLE `users` (
    `id`             BIGINT UNSIGNED PRIMARY KEY NOT NULL AUTO_INCREMENT
    ,`email`         VARCHAR(100) UNIQUE NOT NULL
    ,`password`      VARCHAR(255) NOT NULL
    ,`nick`          VARCHAR(20) NOT NULL
    ,`provider`      VARCHAR(10) NOT NULL DEFAULT 'NONE'
    ,`role`          VARCHAR(10) NOT NULL DEFAULT 'NORMAL'
    ,`profile`       VARCHAR(100) NOT NULL
    ,`refresh_token` VARCHAR(255) DEFAULT NULL
    ,`created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP()
    ,`updated_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP()
    ,`deleted_at`    DATETIME DEFAULT NULL
);