-- Database Creation Script for Bookfair Stall Reservation System
-- Supported Datatypes: MySQL 8.0+

CREATE DATABASE IF NOT EXISTS `bookfair` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `bookfair`;

-- 1. Users Table
CREATE TABLE IF NOT EXISTS `users` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(255) NOT NULL,
    `email` VARCHAR(255) NOT NULL UNIQUE,
    `phone` VARCHAR(255) NOT NULL,
    `role` VARCHAR(50) NOT NULL,
    `business_name` VARCHAR(255) NULL,
    `active` BIT(1) NOT NULL DEFAULT b'1',
    `created_at` DATETIME(6) NULL,
    `updated_at` DATETIME(6) NULL,
    `sub` VARCHAR(255) NULL UNIQUE
) ENGINE=InnoDB;

-- 2. Events Table
CREATE TABLE IF NOT EXISTS `events` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(255) NOT NULL,
    `description` TEXT NULL,
    `location` VARCHAR(255) NOT NULL,
    `event_date` DATETIME(6) NOT NULL,
    `stalls_booked` INT NOT NULL DEFAULT 0,
    `image_url` VARCHAR(500) NULL,
    `created_at` DATETIME(6) NULL,
    `updated_at` DATETIME(6) NULL
) ENGINE=InnoDB;

-- 3. Stalls Table
CREATE TABLE IF NOT EXISTS `stalls` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `stall_code` VARCHAR(50) NOT NULL,
    `stall_size` VARCHAR(50) NOT NULL,
    `price` DECIMAL(12, 2) NOT NULL,
    `blocked` BIT(1) NOT NULL DEFAULT b'0',
    `event_id` BIGINT NOT NULL,
    CONSTRAINT `fk_stalls_event` FOREIGN KEY (`event_id`) REFERENCES `events` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 4. Reservations Table
CREATE TABLE IF NOT EXISTS `reservations` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `booking_id` VARCHAR(50) NOT NULL UNIQUE,
    `event_id` BIGINT NOT NULL,
    `vendor_id` BIGINT NOT NULL,
    `total_amount` DECIMAL(12, 2) NOT NULL,
    `advance_amount` DECIMAL(12, 2) NOT NULL,
    `status` VARCHAR(50) NOT NULL,
    `stall_description` VARCHAR(2000) NULL,
    `qr_code_value` VARCHAR(255) NULL,
    `booking_datetime` DATETIME(6) NULL,
    `updated_at` DATETIME(6) NULL,
    `cancellation_deadline` DATE NULL,
    `payment_method` VARCHAR(50) NULL,
    `account_number` VARCHAR(100) NULL,
    `bank_name` VARCHAR(100) NULL,
    `address` VARCHAR(255) NULL,
    `stall_type` VARCHAR(50) NULL,
    `preferred_stall_size` VARCHAR(50) NULL,
    `number_of_stalls_required` INT NULL,
    `business_category` VARCHAR(50) NULL,
    `reservation_date` DATE NULL,
    `special_requirements` VARCHAR(1000) NULL,
    `admin_ack` BIT(1) NOT NULL DEFAULT b'0',
    CONSTRAINT `fk_reservations_event` FOREIGN KEY (`event_id`) REFERENCES `events` (`id`),
    CONSTRAINT `fk_reservations_vendor` FOREIGN KEY (`vendor_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB;

-- 5. Reservation Stalls Mapping
CREATE TABLE IF NOT EXISTS `reservation_stalls` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `reservation_id` BIGINT NOT NULL,
    CONSTRAINT `uk_reservation_stall`
        UNIQUE (`reservation_id`, `stall_id`),
    `stall_id` BIGINT NOT NULL,
    CONSTRAINT `fk_res_stalls_reservation` FOREIGN KEY (`reservation_id`) REFERENCES `reservations` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_res_stalls_stall` FOREIGN KEY (`stall_id`) REFERENCES `stalls` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 6. Genres Table
CREATE TABLE IF NOT EXISTS `genres` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB;

-- 7. Reservation Genres Mapping
CREATE TABLE IF NOT EXISTS `reservation_genres` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `reservation_id` BIGINT NOT NULL,
        CONSTRAINT `uk_reservation_genre`
        UNIQUE (`reservation_id`, `genre_id`),
    `genre_id` BIGINT NOT NULL,
    CONSTRAINT `fk_res_genres_reservation` FOREIGN KEY (`reservation_id`) REFERENCES `reservations` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_res_genres_genre` FOREIGN KEY (`genre_id`) REFERENCES `genres` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 8. Reservation Logs
CREATE TABLE IF NOT EXISTS `reservation_logs` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `reservation_id` BIGINT NOT NULL,
    `status` VARCHAR(50) NOT NULL,
    `description` VARCHAR(1000) NULL,
    `created_at` DATETIME(6) NULL,
    CONSTRAINT `fk_res_logs_reservation` FOREIGN KEY (`reservation_id`) REFERENCES `reservations` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;
