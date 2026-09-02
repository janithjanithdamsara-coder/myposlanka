<?php
// db.php - Database Connection & Auto Schema Initializer with Agent & Referral System

// Database Credentials (cPanel Live Server Credentials)
$host = getenv('DB_HOST') ?: 'localhost';
$user = getenv('DB_USER') ?: 'sahashra_sahashra';
$pass = getenv('DB_PASS') ?: '8SpeGzsetfZf5vWOKcbOa6X';
$dbname = getenv('DB_NAME') ?: 'sahashra_possystem';

try {
    // 1. Attempt connecting directly with cPanel Live Credentials
    try {
        $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8mb4", $user, $pass, [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC
        ]);
    } catch (PDOException $e1) {
        // 2. Automatic Local XAMPP Fallback (for local development & testing)
        try {
            $pdo = new PDO("mysql:host=localhost;charset=utf8mb4", "root", "", [
                PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
                PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC
            ]);
            $pdo->exec("CREATE DATABASE IF NOT EXISTS `possystem_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            $pdo->exec("USE `possystem_db`");
        } catch (PDOException $e2) {
            // Re-throw if both live and local fail
            throw $e1;
        }
    }

    // 1. Stores Table
    $pdo->exec("
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
        );
    ");

    // Ensure `business_type`, `referred_by_agent`, `features_json`, `address`, `receipt_header`, `receipt_footer`, `logo_url` exist
    try {
        $pdo->exec("ALTER TABLE `stores` ADD COLUMN `business_type` VARCHAR(50) DEFAULT 'RETAIL' AFTER `city`");
    } catch (Exception $e) { /* already exists */ }

    try {
        $pdo->exec("ALTER TABLE `stores` ADD COLUMN `referred_by_agent` VARCHAR(50) DEFAULT NULL AFTER `support_pin`");
    } catch (Exception $e) { /* already exists */ }

    try {
        $pdo->exec("ALTER TABLE `stores` ADD COLUMN `features_json` TEXT DEFAULT NULL AFTER `device_id`");
    } catch (Exception $e) { /* already exists */ }

    try {
        $pdo->exec("ALTER TABLE `stores` ADD COLUMN `address` TEXT DEFAULT NULL AFTER `city`");
    } catch (Exception $e) { /* already exists */ }

    try {
        $pdo->exec("ALTER TABLE `stores` ADD COLUMN `receipt_header` VARCHAR(255) DEFAULT 'Thank you for shopping with us!' AFTER `address`");
    } catch (Exception $e) { /* already exists */ }

    try {
        $pdo->exec("ALTER TABLE `stores` ADD COLUMN `receipt_footer` VARCHAR(255) DEFAULT 'Exchange accepted within 7 days with original bill' AFTER `receipt_header`");
    } catch (Exception $e) { /* already exists */ }

    try {
        $pdo->exec("ALTER TABLE `stores` ADD COLUMN `logo_url` VARCHAR(255) DEFAULT NULL AFTER `receipt_footer`");
    } catch (Exception $e) { /* already exists */ }

    // 2. Sales Agents Table
    $pdo->exec("
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
    ");

    // 3. Commission Ledger Table
    $pdo->exec("
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
    ");

    // 4. Leads Table
    $pdo->exec("
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
    ");

    try {
        $pdo->exec("ALTER TABLE `leads` ADD COLUMN `referred_by_agent` VARCHAR(50) DEFAULT NULL AFTER `business_type`");
    } catch (Exception $e) { /* already exists */ }

    // 5. App Config Table
    $pdo->exec("
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
    ");

    // Seed Starter Config & Agents if empty
    $chkConfig = $pdo->query("SELECT COUNT(*) FROM `app_config`")->fetchColumn();
    if ($chkConfig == 0) {
        $pdo->exec("INSERT INTO `app_config` (`id`, `latest_version_code`, `latest_version_name`, `force_update`, `apk_download_url`, `release_notes`) 
                    VALUES (1, 1, 'v1.0.0', 0, 'http://localhost/possystem/downloads/possystem.apk', 'Initial Release with Offline POS, Inventory and Reports')");
    }

    $chkAgents = $pdo->query("SELECT COUNT(*) FROM `agents`")->fetchColumn();
    if ($chkAgents == 0) {
        $pdo->exec("INSERT INTO `agents` (`agent_code`, `name`, `phone`, `city`, `commission_percent`, `total_referred_shops`, `total_earned`, `balance_payable`, `bank_details`) VALUES
            ('AGT-701', 'Kasun Bandara', '0778899001', 'Colombo', 20.00, 2, 1200.00, 1200.00, 'Commercial Bank - Acc: 8001234567 - Kasun B'),
            ('AGT-702', 'Nimal Perera', '0712233445', 'Kandy', 20.00, 1, 600.00, 600.00, 'BOC Bank - Acc: 7009876543 - Nimal P')");
    }

} catch (PDOException $e) {
    die("Database Connection Error: " . $e->getMessage() . "<br>Please make sure MySQL is running in your XAMPP Control Panel.");
}

// Global Secret Salt matching Android App LicenseManager.java
define('SECRET_SALT', 'POS_LANKA_SEC_2026');

/**
 * Generate 6-digit offline PIN in PHP matching Android LicenseManager.java
 */
function generateOfflinePin($shopId, $actionKey) {
    $raw = strtoupper(trim($shopId)) . ':' . $actionKey . ':' . SECRET_SALT;
    $hash = hash('sha256', $raw, true); // raw binary output
    
    // Unpack first 4 bytes as unsigned 32-bit integer (Exact match with Android unsigned long)
    $unpacked = unpack('N', substr($hash, 0, 4));
    $number = abs($unpacked[1]);
    
    return str_pad($number % 1000000, 6, '0', STR_PAD_LEFT);
}

/**
 * Returns default feature flags (All 9 features enabled for demo trial)
 */
function getDefaultFeatures() {
    return [
        'pos' => true,
        'inventory' => true,
        'credit' => true,
        'purchases' => true,
        'expenses' => true,
        'reports' => true,
        'barcode' => true,
        'whatsapp' => true,
        'multi_user' => true
    ];
}

/**
 * Parse and merge store features with defaults
 */
function getStoreFeatures($featuresJson) {
    $defaults = getDefaultFeatures();
    if (empty($featuresJson)) {
        return $defaults;
    }
    $decoded = json_decode($featuresJson, true);
    if (!is_array($decoded)) {
        return $defaults;
    }
    return array_merge($defaults, $decoded);
}
?>
