-- =========================================================
-- POS Lanka Cloud SaaS Master Database Schema
-- Compatible with MySQL 5.7+, MySQL 8.0+, MariaDB 10.3+
-- =========================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 1. Stores & Licenses Table
CREATE TABLE IF NOT EXISTS `stores` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `shop_id` VARCHAR(50) NOT NULL UNIQUE,
    `shop_name` VARCHAR(150) NOT NULL,
    `owner_name` VARCHAR(100) NOT NULL,
    `phone` VARCHAR(30) NOT NULL,
    `city` VARCHAR(100) DEFAULT 'Sri Lanka',
    `business_type` VARCHAR(50) DEFAULT 'RETAIL',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Leads / Inquiries Table
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. App Config Table
CREATE TABLE IF NOT EXISTS `app_config` (
    `id` INT PRIMARY KEY DEFAULT 1,
    `latest_version_code` INT DEFAULT 1,
    `latest_version_name` VARCHAR(20) DEFAULT 'v1.0.0',
    `force_update` TINYINT(1) DEFAULT 0,
    `apk_download_url` VARCHAR(255) DEFAULT 'downloads/possystem.apk',
    `release_notes` TEXT,
    `support_phone` VARCHAR(30) DEFAULT '077 123 4567',
    `support_whatsapp` VARCHAR(30) DEFAULT '+94771234567',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Default Starter Data
INSERT IGNORE INTO `app_config` (`id`, `latest_version_code`, `latest_version_name`, `force_update`, `apk_download_url`, `release_notes`) 
VALUES (1, 1, 'v1.0.0', 0, 'downloads/possystem.apk', 'Initial Release with 5 Multi-Industry Profiles & Offline Billing');

INSERT IGNORE INTO `agents` (`agent_code`, `name`, `phone`, `city`, `commission_percent`, `total_referred_shops`, `total_earned`, `balance_payable`, `bank_details`) VALUES
('AGT-701', 'Kasun Bandara', '0778899001', 'Colombo', 20.00, 2, 1200.00, 1200.00, 'Commercial Bank - Acc: 8001234567 - Kasun B'),
('AGT-702', 'Nimal Perera', '0712233445', 'Kandy', 20.00, 1, 600.00, 600.00, 'BOC Bank - Acc: 7009876543 - Nimal P');
