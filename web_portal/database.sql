-- MySQL Database Schema for POS SaaS Portal & License Management
CREATE DATABASE IF NOT EXISTS `possystem_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `possystem_db`;

-- 1. Stores / Clients Table
CREATE TABLE IF NOT EXISTS `stores` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `shop_id` VARCHAR(50) NOT NULL UNIQUE,
    `shop_name` VARCHAR(150) NOT NULL,
    `owner_name` VARCHAR(100) NOT NULL,
    `phone` VARCHAR(30) NOT NULL,
    `city` VARCHAR(100) DEFAULT 'Sri Lanka',
    `plan_type` ENUM('TRIAL', 'PAID') DEFAULT 'TRIAL',
    `monthly_fee` DECIMAL(10, 2) DEFAULT 3000.00,
    `expiry_date` DATETIME NOT NULL,
    `support_pin` VARCHAR(20) NOT NULL,
    `referred_by_agent` VARCHAR(50) DEFAULT NULL,
    `device_id` VARCHAR(100) DEFAULT NULL,
    `features_json` TEXT DEFAULT NULL,
    `address` TEXT DEFAULT NULL,
    `receipt_header` VARCHAR(255) DEFAULT 'Thank you for shopping with us!',
    `receipt_footer` VARCHAR(255) DEFAULT 'Exchange accepted within 7 days with original bill',
    `logo_url` VARCHAR(255) DEFAULT NULL,
    `is_active` TINYINT(1) DEFAULT 1,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 2. Sales Agents Table
CREATE TABLE IF NOT EXISTS `agents` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `agent_code` VARCHAR(50) NOT NULL UNIQUE,
    `name` VARCHAR(100) NOT NULL,
    `phone` VARCHAR(30) NOT NULL,
    `city` VARCHAR(100) DEFAULT 'Sri Lanka',
    `commission_percent` DECIMAL(5, 2) DEFAULT 20.00,
    `total_referred_shops` INT DEFAULT 0,
    `total_earned` DECIMAL(10, 2) DEFAULT 0.00,
    `balance_payable` DECIMAL(10, 2) DEFAULT 0.00,
    `bank_details` TEXT DEFAULT NULL,
    `is_active` TINYINT(1) DEFAULT 1,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Commission Ledger Table
CREATE TABLE IF NOT EXISTS `commissions` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `agent_code` VARCHAR(50) NOT NULL,
    `shop_id` VARCHAR(50) NOT NULL,
    `shop_name` VARCHAR(150) NOT NULL,
    `amount` DECIMAL(10, 2) NOT NULL,
    `description` VARCHAR(255) DEFAULT 'Monthly Subscription Commission',
    `status` ENUM('UNPAID', 'PAID') DEFAULT 'UNPAID',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. Customer Free Trial Leads / Inquiries Table
CREATE TABLE IF NOT EXISTS `leads` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `shop_name` VARCHAR(150) NOT NULL,
    `owner_name` VARCHAR(100) NOT NULL,
    `phone` VARCHAR(30) NOT NULL,
    `city` VARCHAR(100) DEFAULT NULL,
    `business_type` VARCHAR(50) DEFAULT 'Retail',
    `referred_by_agent` VARCHAR(50) DEFAULT NULL,
    `status` ENUM('PENDING', 'APPROVED', 'CONTACTED') DEFAULT 'PENDING',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 5. App In-App Updates Config Table
CREATE TABLE IF NOT EXISTS `app_config` (
    `id` INT PRIMARY KEY DEFAULT 1,
    `latest_version_code` INT DEFAULT 1,
    `latest_version_name` VARCHAR(20) DEFAULT 'v1.0.0',
    `force_update` TINYINT(1) DEFAULT 0,
    `apk_download_url` VARCHAR(255) DEFAULT 'http://localhost/possystem/downloads/possystem.apk',
    `release_notes` TEXT,
    `support_phone` VARCHAR(30) DEFAULT '077 123 4567',
    `support_whatsapp` VARCHAR(30) DEFAULT '+94771234567',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Seed Initial App Config
INSERT INTO `app_config` (`id`, `latest_version_code`, `latest_version_name`, `force_update`, `apk_download_url`, `release_notes`)
VALUES (1, 1, 'v1.0.0', 0, 'http://localhost/possystem/downloads/possystem.apk', 'Initial Release with Offline POS, Inventory and Reports')
ON DUPLICATE KEY UPDATE `latest_version_name` = 'v1.0.0';

-- Seed Starter Demo Stores (All 9 Features Unlocked for Demo)
INSERT INTO `stores` (`shop_id`, `shop_name`, `owner_name`, `phone`, `city`, `address`, `receipt_header`, `receipt_footer`, `plan_type`, `monthly_fee`, `expiry_date`, `support_pin`, `referred_by_agent`, `features_json`, `is_active`)
VALUES 
('SHP-101', 'Saman Super Mart', 'Saman Kumara', '0771234567', 'Galle', 'No. 45, Main Street, Galle', 'Welcome to Saman Super Mart', 'Thank you for shopping with us! • Exchange within 7 days', 'TRIAL', 3000.00, DATE_ADD(NOW(), INTERVAL 14 DAY), '849201', 'AGT-701', '{"pos":true,"inventory":true,"credit":true,"purchases":true,"expenses":true,"reports":true,"barcode":true,"whatsapp":true,"multi_user":true}', 1),
('SHP-102', 'Perera Grocery & Fresh', 'K. Perera', '0719876543', 'Colombo 05', 'No. 12, Havelock Road, Colombo 05', 'Welcome to Perera Grocery', 'Goods return accepted with original bill', 'PAID', 3500.00, DATE_ADD(NOW(), INTERVAL 30 DAY), '123456', NULL, '{"pos":true,"inventory":true,"credit":true,"purchases":true,"expenses":true,"reports":true,"barcode":true,"whatsapp":true,"multi_user":true}', 1)
ON DUPLICATE KEY UPDATE `shop_name` = VALUES(`shop_name`);

-- Seed Starter Agents
INSERT INTO `agents` (`agent_code`, `name`, `phone`, `city`, `commission_percent`, `total_referred_shops`, `total_earned`, `balance_payable`, `bank_details`) VALUES
('AGT-701', 'Kasun Bandara', '0778899001', 'Colombo', 20.00, 1, 600.00, 600.00, 'Commercial Bank - Acc: 8001234567 - Kasun B'),
('AGT-702', 'Nimal Perera', '0712233445', 'Kandy', 20.00, 0, 0.00, 0.00, 'BOC Bank - Acc: 7009876543 - Nimal P')
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

