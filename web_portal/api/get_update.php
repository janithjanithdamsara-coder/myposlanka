<?php
header('Content-Type: application/json; charset=utf-8');
require_once '../db.php';

$config = $pdo->query("SELECT * FROM `app_config` WHERE `id` = 1")->fetch() ?: [
    'latest_version_code' => 1,
    'latest_version_name' => 'v1.0.0',
    'force_update' => 0,
    'apk_download_url' => 'http://localhost/possystem/downloads/possystem.apk',
    'release_notes' => 'Initial Release',
    'support_phone' => '077 123 4567',
    'support_whatsapp' => '+94771234567'
];

echo json_encode([
    'status' => 'success',
    'latest_version_code' => intval($config['latest_version_code']),
    'latest_version_name' => $config['latest_version_name'],
    'force_update' => (bool)$config['force_update'],
    'apk_download_url' => $config['apk_download_url'],
    'release_notes' => $config['release_notes'],
    'support_phone' => $config['support_phone'],
    'support_whatsapp' => $config['support_whatsapp']
]);
?>
