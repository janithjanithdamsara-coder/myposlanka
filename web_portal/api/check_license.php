<?php
header('Content-Type: application/json; charset=utf-8');
require_once '../db.php';

$shop_id = trim($_GET['shop_id'] ?? $_POST['shop_id'] ?? '');

if (empty($shop_id)) {
    echo json_encode(['status' => 'error', 'message' => 'Missing shop_id parameter']);
    exit;
}

$stmt = $pdo->prepare("SELECT * FROM `stores` WHERE `shop_id` = ?");
$stmt->execute([$shop_id]);
$store = $stmt->fetch();

if (!$store) {
    echo json_encode(['status' => 'not_found', 'message' => 'Store not registered']);
    exit;
}

$now = time();
$exp = strtotime($store['expiry_date']);
$is_expired = $exp < $now || !$store['is_active'];
$days_left = max(0, ceil(($exp - $now) / 86400));
$features = getStoreFeatures($store['features_json'] ?? null);

echo json_encode([
    'status' => 'success',
    'shop_id' => $store['shop_id'],
    'shop_name' => $store['shop_name'],
    'owner_name' => $store['owner_name'],
    'phone' => $store['phone'],
    'city' => $store['city'],
    'business_type' => $store['business_type'] ?? 'RETAIL',
    'address' => $store['address'] ?? ($store['city'] ?? 'Sri Lanka'),
    'receipt_header' => $store['receipt_header'] ?? ('Welcome to ' . $store['shop_name']),
    'receipt_footer' => $store['receipt_footer'] ?? 'Thank you for shopping! • Please visit again',
    'logo_url' => $store['logo_url'] ?? '',
    'is_active' => (bool)$store['is_active'],
    'is_expired' => $is_expired,
    'plan_type' => $store['plan_type'],
    'days_remaining' => $days_left,
    'expiry_date' => date('d-M-Y', $exp),
    'expiry_timestamp' => $exp * 1000, // In milliseconds for Android
    'features' => $features
]);
?>
