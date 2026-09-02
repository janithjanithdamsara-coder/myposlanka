<?php
session_start();
require_once '../db.php';

// Auth Guard
if (!isset($_SESSION['admin_logged_in']) || $_SESSION['admin_logged_in'] !== true) {
    header('Location: login.php');
    exit;
}

$alert_msg = null;
$voucher_data = null;

// ==========================================
// ⚙️ ACTION HANDLERS
// ==========================================

// 1. Add New Store
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action']) && $_POST['action'] === 'add_store') {
    $name = trim($_POST['shop_name'] ?? '');
    $owner = trim($_POST['owner_name'] ?? '');
    $phone = trim($_POST['phone'] ?? '');
    $city = trim($_POST['city'] ?? 'Sri Lanka');
    if (empty($city)) $city = 'Sri Lanka';
    $business_type = trim($_POST['business_type'] ?? 'RETAIL');
    $monthly_fee = floatval($_POST['monthly_fee'] ?? 3000);
    $plan_duration = $_POST['plan_duration'] ?? 'TRIAL_14';
    $agent_code = trim($_POST['referred_by_agent'] ?? '');
    if (empty($agent_code)) $agent_code = null;

    $days = 14;
    $plan_type = 'TRIAL';
    $action_key = 'RENEW_TRIAL_14';

    if ($plan_duration === 'TRIAL_7') { $days = 7; $action_key = 'RENEW_TRIAL_7'; }
    if ($plan_duration === 'TRIAL_14') { $days = 14; $action_key = 'RENEW_TRIAL_14'; }
    if ($plan_duration === 'TRIAL_30') { $days = 30; $action_key = 'RENEW_TRIAL_30'; }
    if ($plan_duration === 'PAID_30') { $days = 30; $plan_type = 'PAID'; $action_key = 'RENEW_PAID_30'; }
    if ($plan_duration === 'PAID_90') { $days = 90; $plan_type = 'PAID'; $action_key = 'RENEW_PAID_30'; }
    if ($plan_duration === 'PAID_365') { $days = 365; $plan_type = 'PAID'; $action_key = 'RENEW_PAID_30'; }

    $count = $pdo->query("SELECT COUNT(*) FROM `stores`")->fetchColumn();
    $shop_id = "SHP-" . (101 + $count);
    $pin = generateOfflinePin($shop_id, $action_key);
    $expiry_date = date('Y-m-d H:i:s', strtotime("+$days days"));
    $features_json = json_encode(getDefaultFeatures()); // All 9 features enabled for demo
    $receipt_header = "Welcome to " . $name;
    $receipt_footer = "Thank you for shopping with us! • Exchange within 7 days";

    $stmt = $pdo->prepare("INSERT INTO `stores` (shop_id, shop_name, owner_name, phone, city, business_type, plan_type, monthly_fee, expiry_date, support_pin, referred_by_agent, features_json, receipt_header, receipt_footer, is_active) 
                           VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)");
    $stmt->execute([$shop_id, $name, $owner, $phone, $city, $business_type, $plan_type, $monthly_fee, $expiry_date, $pin, $agent_code, $features_json, $receipt_header, $receipt_footer]);

    // If referred by an Agent, increment their counter
    if (!empty($agent_code)) {
        $pdo->prepare("UPDATE `agents` SET `total_referred_shops` = `total_referred_shops` + 1 WHERE `agent_code` = ?")->execute([$agent_code]);
        if ($plan_type === 'PAID') {
            $agent = $pdo->prepare("SELECT * FROM `agents` WHERE `agent_code` = ?");
            $agent->execute([$agent_code]);
            $ag = $agent->fetch();
            if ($ag) {
                $comm = ($monthly_fee * ($ag['commission_percent'] / 100));
                $pdo->prepare("INSERT INTO `commissions` (agent_code, shop_id, shop_name, amount, description, status) VALUES (?, ?, ?, ?, 'Initial Paid Subscription', 'UNPAID')")
                    ->execute([$agent_code, $shop_id, $name, $comm]);
                $pdo->prepare("UPDATE `agents` SET `total_earned` = `total_earned` + ?, `balance_payable` = `balance_payable` + ? WHERE `agent_code` = ?")
                    ->execute([$comm, $comm, $agent_code]);
            }
        }
    }

    $voucher_data = [
        'shop_id' => $shop_id,
        'shop_name' => $name,
        'pin' => $pin,
        'expiry' => date('d-M-Y', strtotime($expiry_date)),
        'plan' => $plan_type . " ($days Days)",
        'agent' => $agent_code ?: 'Direct',
        'phone' => $phone
    ];
    $alert_msg = "Store '$name' ($shop_id) registered with full demo features!";
}

// 2. Add New Sales Agent
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action']) && $_POST['action'] === 'add_agent') {
    $name = trim($_POST['agent_name'] ?? '');
    $phone = trim($_POST['agent_phone'] ?? '');
    $city = trim($_POST['agent_city'] ?? 'Sri Lanka');
    if (empty($city)) $city = 'Sri Lanka';
    $commission_percent = floatval($_POST['commission_percent'] ?? 20);
    $bank_details = trim($_POST['bank_details'] ?? '');

    $count = $pdo->query("SELECT COUNT(*) FROM `agents`")->fetchColumn();
    $agent_code = "AGT-" . (701 + $count);

    $stmt = $pdo->prepare("INSERT INTO `agents` (agent_code, name, phone, city, commission_percent, bank_details, is_active) VALUES (?, ?, ?, ?, ?, ?, 1)");
    $stmt->execute([$agent_code, $name, $phone, $city, $commission_percent, $bank_details]);

    $alert_msg = "Sales Agent '$name' registered successfully with Code: $agent_code";
}

// 3. Extend Store Subscription & Credit Commission
if (isset($_GET['extend_id']) && isset($_GET['days'])) {
    $shop_id = $_GET['extend_id'];
    $days = intval($_GET['days']);

    $store = $pdo->prepare("SELECT * FROM `stores` WHERE `shop_id` = ?");
    $store->execute([$shop_id]);
    $st = $store->fetch();

    if ($st) {
        $base_time = max(strtotime($st['expiry_date']), time());
        $new_expiry = date('Y-m-d H:i:s', $base_time + ($days * 86400));
        $new_pin = generateOfflinePin($shop_id, 'RENEW_PAID_30');

        $upd = $pdo->prepare("UPDATE `stores` SET `expiry_date` = ?, `plan_type` = 'PAID', `is_active` = 1, `support_pin` = ? WHERE `shop_id` = ?");
        $upd->execute([$new_expiry, $new_pin, $shop_id]);

        // If store has an assigned agent, credit their recurring commission
        if (!empty($st['referred_by_agent'])) {
            $agent_code = $st['referred_by_agent'];
            $agent = $pdo->prepare("SELECT * FROM `agents` WHERE `agent_code` = ?");
            $agent->execute([$agent_code]);
            $ag = $agent->fetch();
            if ($ag) {
                $comm = ($st['monthly_fee'] * ($ag['commission_percent'] / 100));
                $pdo->prepare("INSERT INTO `commissions` (agent_code, shop_id, shop_name, amount, description, status) VALUES (?, ?, ?, ?, 'Monthly Renewal (+30 Days)', 'UNPAID')")
                    ->execute([$agent_code, $shop_id, $st['shop_name'], $comm]);
                $pdo->prepare("UPDATE `agents` SET `total_earned` = `total_earned` + ?, `balance_payable` = `balance_payable` + ? WHERE `agent_code` = ?")
                    ->execute([$comm, $comm, $agent_code]);
            }
        }

        $voucher_data = [
            'shop_id' => $shop_id,
            'shop_name' => $st['shop_name'],
            'pin' => $new_pin,
            'expiry' => date('d-M-Y', strtotime($new_expiry)),
            'plan' => "Paid Plan (+$days Days)",
            'agent' => $st['referred_by_agent'] ?: 'Direct',
            'phone' => $st['phone']
        ];
        $alert_msg = "Store '$shop_id' extended by $days days & Agent commission credited!";
    }
}

// 4. Mark Commission Payout as Settled / Paid
if (isset($_GET['pay_agent_id'])) {
    $agent_code = $_GET['pay_agent_id'];
    $pdo->prepare("UPDATE `commissions` SET `status` = 'PAID' WHERE `agent_code` = ? AND `status` = 'UNPAID'")->execute([$agent_code]);
    $pdo->prepare("UPDATE `agents` SET `balance_payable` = 0.00 WHERE `agent_code` = ?")->execute([$agent_code]);
    $alert_msg = "Payout for Agent $agent_code marked as SETTLED!";
}

// 5. Toggle Status (Enable/Disable Store)
if (isset($_GET['toggle_id'])) {
    $shop_id = $_GET['toggle_id'];
    $pdo->prepare("UPDATE `stores` SET `is_active` = NOT `is_active` WHERE `shop_id` = ?")->execute([$shop_id]);
    header('Location: index.php#stores');
    exit;
}

// 6. Delete Store
if (isset($_GET['delete_id'])) {
    $shop_id = $_GET['delete_id'];
    $pdo->prepare("DELETE FROM `stores` WHERE `shop_id` = ?")->execute([$shop_id]);
    header('Location: index.php#stores');
    exit;
}

// 7. Update In-App Version Config
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action']) && $_POST['action'] === 'save_app_config') {
    $v_code = intval($_POST['version_code']);
    $v_name = trim($_POST['version_name']);
    $apk_url = trim($_POST['apk_url']);
    $notes = trim($_POST['release_notes']);

    $stmt = $pdo->prepare("UPDATE `app_config` SET `latest_version_code` = ?, `latest_version_name` = ?, `apk_download_url` = ?, `release_notes` = ? WHERE `id` = 1");
    $stmt->execute([$v_code, $v_name, $apk_url, $notes]);
    $alert_msg = "App Update configuration saved & broadcasted!";
}

// 8. Update Store Details, Due Date, Receipt Branding & Modular Features (Store Hub)
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action']) && $_POST['action'] === 'save_store_hub') {
    $shop_id = trim($_POST['shop_id']);
    $shop_name = trim($_POST['shop_name']);
    $owner_name = trim($_POST['owner_name']);
    $phone = trim($_POST['phone']);
    $city = trim($_POST['city'] ?? 'Sri Lanka');
    if (empty($city)) $city = 'Sri Lanka';
    $business_type = trim($_POST['business_type'] ?? 'RETAIL');
    $address = trim($_POST['address'] ?? '');
    $receipt_header = trim($_POST['receipt_header'] ?? ('Welcome to ' . $shop_name));
    $receipt_footer = trim($_POST['receipt_footer'] ?? 'Thank you for shopping with us! • Exchange within 7 days');
    $logo_url = trim($_POST['logo_url'] ?? '');
    $monthly_fee = floatval($_POST['monthly_fee'] ?? 3000);
    $plan_type = trim($_POST['plan_type'] ?? 'TRIAL');
    $expiry_date = trim($_POST['expiry_date'] ?? date('Y-m-d H:i:s'));
    $referred_by_agent = trim($_POST['referred_by_agent'] ?? '');
    if (empty($referred_by_agent)) $referred_by_agent = null;
    $is_active = isset($_POST['is_active']) ? 1 : 0;

    // Handle File Upload for Store Logo if provided
    if (isset($_FILES['logo_file']) && $_FILES['logo_file']['error'] === UPLOAD_ERR_OK) {
        $upload_dir = __DIR__ . '/../uploads/logos/';
        if (!is_dir($upload_dir)) {
            @mkdir($upload_dir, 0777, true);
        }
        $ext = strtolower(pathinfo($_FILES['logo_file']['name'], PATHINFO_EXTENSION));
        if (in_array($ext, ['png', 'jpg', 'jpeg', 'svg', 'webp'])) {
            $filename = 'logo_' . preg_replace('/[^a-zA-Z0-9_-]/', '', $shop_id) . '_' . time() . '.' . $ext;
            $destination = $upload_dir . $filename;
            if (move_uploaded_file($_FILES['logo_file']['tmp_name'], $destination)) {
                $logo_url = 'uploads/logos/' . $filename;
            }
        }
    }

    // Feature Toggles
    $features = [
        'pos' => isset($_POST['feat_pos']) ? true : false,
        'inventory' => isset($_POST['feat_inventory']) ? true : false,
        'credit' => isset($_POST['feat_credit']) ? true : false,
        'purchases' => isset($_POST['feat_purchases']) ? true : false,
        'expenses' => isset($_POST['feat_expenses']) ? true : false,
        'reports' => isset($_POST['feat_reports']) ? true : false,
        'barcode' => isset($_POST['feat_barcode']) ? true : false,
        'whatsapp' => isset($_POST['feat_whatsapp']) ? true : false,
        'multi_user' => isset($_POST['feat_multi_user']) ? true : false
    ];
    $features_json = json_encode($features);

    $stmt = $pdo->prepare("UPDATE `stores` SET 
        `shop_name` = ?, 
        `owner_name` = ?, 
        `phone` = ?, 
        `city` = ?, 
        `business_type` = ?,
        `address` = ?,
        `receipt_header` = ?,
        `receipt_footer` = ?,
        `logo_url` = ?,
        `monthly_fee` = ?, 
        `plan_type` = ?, 
        `expiry_date` = ?, 
        `referred_by_agent` = ?, 
        `is_active` = ?,
        `features_json` = ? 
        WHERE `shop_id` = ?");
    $stmt->execute([$shop_name, $owner_name, $phone, $city, $business_type, $address, $receipt_header, $receipt_footer, $logo_url, $monthly_fee, $plan_type, $expiry_date, $referred_by_agent, $is_active, $features_json, $shop_id]);

    $alert_msg = "Store '$shop_name' ($shop_id) business type [$business_type], branding, features & due date saved!";
}

// Logout
if (isset($_GET['logout'])) {
    session_destroy();
    header('Location: login.php');
    exit;
}

// ==========================================
// 📊 FETCH DATA FROM MYSQL
// ==========================================
$stores = $pdo->query("SELECT * FROM `stores` ORDER BY `id` DESC")->fetchAll();
$agents = $pdo->query("SELECT * FROM `agents` ORDER BY `id` DESC")->fetchAll();
$commissions = $pdo->query("SELECT * FROM `commissions` ORDER BY `id` DESC LIMIT 20")->fetchAll();
$leads = $pdo->query("SELECT * FROM `leads` ORDER BY `id` DESC LIMIT 10")->fetchAll();
$config = $pdo->query("SELECT * FROM `app_config` WHERE `id` = 1")->fetch();

// KPI Calculations
$total_stores = count($stores);
$total_agents = count($agents);
$now = time();
$active_trials = 0;
$active_paid = 0;
$expired_stores = 0;
$mrr = 0;
$total_unpaid_commissions = 0;

foreach ($stores as $s) {
    $is_exp = strtotime($s['expiry_date']) < $now || !$s['is_active'];
    if ($is_exp) {
        $expired_stores++;
    } else {
        if ($s['plan_type'] === 'TRIAL') $active_trials++;
        else {
            $active_paid++;
            $mrr += floatval($s['monthly_fee']);
        }
    }
}

foreach ($agents as $a) {
    $total_unpaid_commissions += floatval($a['balance_payable']);
}
?>
<!DOCTYPE html>
<html lang="en" class="h-full bg-slate-50">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Admin Dashboard | POS SaaS Master Portal</title>
  <!-- Tailwind CSS CDN -->
  <script src="https://cdn.tailwindcss.com"></script>
  <!-- Google Fonts -->
  <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet" />
  <!-- FontAwesome Icons -->
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" />
  
  <script>
    tailwind.config = {
      theme: {
        extend: {
          fontFamily: { sans: ['Plus Jakarta Sans', 'sans-serif'] },
        }
      }
    }
  </script>
  <style>
    .tab-content { display: none; }
    .tab-content.active { display: block; }
    .nav-link.active {
      background-color: #064E3B;
      color: #FFFFFF;
      font-weight: 700;
    }
  </style>
</head>
<body class="h-full antialiased text-slate-800 flex overflow-hidden">

  <!-- LEFT COLLAPSIBLE SIDEBAR -->
  <aside class="w-64 bg-emerald-950 text-white flex-shrink-0 flex flex-col justify-between transition-all duration-300 z-30 shadow-xl">
    <div>
      <!-- Brand Logo & Header -->
      <div class="h-16 px-6 flex items-center gap-3 border-b border-emerald-900 bg-emerald-900/60">
        <div class="w-9 h-9 rounded-xl bg-emerald-500 flex items-center justify-center text-white text-lg font-bold shadow">
          <i class="fa-solid fa-cash-register"></i>
        </div>
        <div>
          <h1 class="text-sm font-extrabold tracking-tight">POS Master SaaS</h1>
          <p class="text-[10px] text-emerald-300 font-semibold uppercase tracking-wider">Cloud Admin v2.0</p>
        </div>
      </div>

      <!-- Navigation Menu Links -->
      <nav class="p-4 space-y-1.5 text-xs font-semibold text-emerald-100">
        <button onclick="switchTab('dashboard')" id="nav-dashboard" class="nav-link active w-full flex items-center gap-3 px-3.5 py-2.5 rounded-xl hover:bg-emerald-900 transition">
          <i class="fa-solid fa-chart-pie text-sm w-4"></i> Dashboard Overview
        </button>

        <button onclick="switchTab('stores')" id="nav-stores" class="nav-link w-full flex items-center justify-between px-3.5 py-2.5 rounded-xl hover:bg-emerald-900 transition">
          <span class="flex items-center gap-3"><i class="fa-solid fa-store text-sm w-4"></i> Stores &amp; Licenses</span>
          <span class="px-2 py-0.5 rounded-full bg-emerald-800 text-[10px] text-emerald-200"><?= $total_stores ?></span>
        </button>

        <button onclick="switchTab('agents')" id="nav-agents" class="nav-link w-full flex items-center justify-between px-3.5 py-2.5 rounded-xl hover:bg-emerald-900 transition">
          <span class="flex items-center gap-3"><i class="fa-solid fa-users-gear text-sm w-4"></i> Sales Agents &amp; Team</span>
          <span class="px-2 py-0.5 rounded-full bg-emerald-800 text-[10px] text-amber-300 font-bold"><?= $total_agents ?></span>
        </button>

        <button onclick="switchTab('commissions')" id="nav-commissions" class="nav-link w-full flex items-center justify-between px-3.5 py-2.5 rounded-xl hover:bg-emerald-900 transition">
          <span class="flex items-center gap-3"><i class="fa-solid fa-hand-holding-dollar text-sm w-4"></i> Commission Ledger</span>
          <?php if ($total_unpaid_commissions > 0): ?>
          <span class="px-2 py-0.5 rounded-full bg-amber-500 text-slate-900 font-extrabold text-[10px]">Due</span>
          <?php endif; ?>
        </button>

        <button onclick="switchTab('leads')" id="nav-leads" class="nav-link w-full flex items-center justify-between px-3.5 py-2.5 rounded-xl hover:bg-emerald-900 transition">
          <span class="flex items-center gap-3"><i class="fa-solid fa-inbox text-sm w-4"></i> Website Leads</span>
          <span class="px-2 py-0.5 rounded-full bg-emerald-800 text-[10px] text-emerald-300"><?= count($leads) ?></span>
        </button>

        <button onclick="switchTab('updates')" id="nav-updates" class="nav-link w-full flex items-center gap-3 px-3.5 py-2.5 rounded-xl hover:bg-emerald-900 transition">
          <i class="fa-solid fa-cloud-arrow-up text-sm w-4 text-amber-400"></i> In-App Updates &amp; APK
        </button>
      </nav>
    </div>

    <!-- User / Agent Portal Link & Logout Footer -->
    <div class="p-4 border-t border-emerald-900 space-y-2">
      <a href="../agent/login.php" target="_blank" class="block w-full py-2 px-3 rounded-xl bg-emerald-900 hover:bg-emerald-800 text-[11px] text-emerald-200 font-bold text-center transition">
        <i class="fa-solid fa-arrow-up-right-from-square mr-1"></i> Open Agent Portal
      </a>
      <div class="flex items-center justify-between pt-2">
        <div class="flex items-center gap-2">
          <div class="w-8 h-8 rounded-full bg-emerald-700 flex items-center justify-center text-xs font-bold">
            <i class="fa-solid fa-user-shield"></i>
          </div>
          <div>
            <p class="text-xs font-bold text-white leading-tight">Master Admin</p>
            <p class="text-[10px] text-emerald-400">Super Admin</p>
          </div>
        </div>
        <a href="index.php?logout=1" class="text-emerald-400 hover:text-white text-sm" title="Logout">
          <i class="fa-solid fa-right-from-bracket"></i>
        </a>
      </div>
    </div>
  </aside>

  <!-- MAIN CONTENT AREA -->
  <div class="flex-1 flex flex-col h-full overflow-hidden">
    
    <!-- TOP HEADER -->
    <header class="h-16 bg-white border-b border-slate-200 px-6 flex items-center justify-between flex-shrink-0">
      <div>
        <h2 id="pageTitle" class="text-base font-extrabold text-slate-900">Dashboard Overview</h2>
        <p class="text-xs text-slate-400">Manage stores, agents, and island-wide POS licensing</p>
      </div>

      <div class="flex items-center gap-3">
        <button onclick="openAddAgentModal()" class="px-3.5 py-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold transition flex items-center gap-1.5">
          <i class="fa-solid fa-user-plus text-emerald-600"></i> + Register Agent
        </button>
        <button onclick="openAddStoreModal()" class="px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold shadow-sm transition flex items-center gap-1.5">
          <i class="fa-solid fa-plus"></i> + Add New Store
        </button>
      </div>
    </header>

    <!-- SCROLLABLE TAB CONTENT -->
    <main class="flex-1 overflow-y-auto p-6 space-y-6">

      <?php if ($alert_msg): ?>
      <div class="p-4 bg-emerald-50 border border-emerald-300 rounded-2xl text-emerald-900 text-xs font-bold flex items-center justify-between shadow-sm animate-in fade-in">
        <div class="flex items-center gap-2">
          <i class="fa-solid fa-circle-check text-emerald-600 text-base"></i>
          <span><?= htmlspecialchars($alert_msg) ?></span>
        </div>
        <button onclick="this.parentElement.remove()" class="text-emerald-700 hover:text-emerald-900"><i class="fa-solid fa-xmark"></i></button>
      </div>
      <?php endif; ?>

      <!-- ======================================================== -->
      <!-- 📊 TAB 1: DASHBOARD OVERVIEW -->
      <!-- ======================================================== -->
      <section id="tab-dashboard" class="tab-content active space-y-6">
        <!-- KPI METRICS -->
        <div class="grid grid-cols-2 lg:grid-cols-5 gap-4">
          <div class="bg-white p-4 rounded-2xl shadow-sm border border-slate-200 flex items-center gap-4">
            <div class="w-12 h-12 rounded-xl bg-slate-100 flex items-center justify-center text-slate-700 text-xl"><i class="fa-solid fa-store"></i></div>
            <div><p class="text-xs font-semibold text-slate-500 uppercase">Total Stores</p><p class="text-2xl font-extrabold text-slate-900"><?= $total_stores ?></p></div>
          </div>

          <div class="bg-white p-4 rounded-2xl shadow-sm border border-slate-200 border-l-4 border-l-amber-500 flex items-center gap-4">
            <div class="w-12 h-12 rounded-xl bg-amber-50 flex items-center justify-center text-amber-600 text-xl"><i class="fa-solid fa-clock"></i></div>
            <div><p class="text-xs font-semibold text-slate-500 uppercase">Active Trials</p><p class="text-2xl font-extrabold text-amber-600"><?= $active_trials ?></p></div>
          </div>

          <div class="bg-white p-4 rounded-2xl shadow-sm border border-slate-200 border-l-4 border-l-emerald-600 flex items-center gap-4">
            <div class="w-12 h-12 rounded-xl bg-emerald-50 flex items-center justify-center text-emerald-600 text-xl"><i class="fa-solid fa-circle-check"></i></div>
            <div><p class="text-xs font-semibold text-slate-500 uppercase">Paid Stores</p><p class="text-2xl font-extrabold text-emerald-600"><?= $active_paid ?></p></div>
          </div>

          <div class="bg-white p-4 rounded-2xl shadow-sm border border-slate-200 border-l-4 border-l-purple-600 flex items-center gap-4">
            <div class="w-12 h-12 rounded-xl bg-purple-50 flex items-center justify-center text-purple-600 text-xl"><i class="fa-solid fa-users-gear"></i></div>
            <div><p class="text-xs font-semibold text-slate-500 uppercase">Active Agents</p><p class="text-2xl font-extrabold text-purple-600"><?= $total_agents ?></p></div>
          </div>

          <div class="bg-white p-4 rounded-2xl shadow-sm border border-slate-200 border-l-4 border-l-blue-600 col-span-2 lg:col-span-1 flex items-center gap-4">
            <div class="w-12 h-12 rounded-xl bg-blue-50 flex items-center justify-center text-blue-600 text-xl"><i class="fa-solid fa-money-bill-wave"></i></div>
            <div><p class="text-xs font-semibold text-slate-500 uppercase">Monthly MRR</p><p class="text-xl font-extrabold text-blue-700">LKR <?= number_format($mrr) ?></p></div>
          </div>
        </div>

        <!-- QUICK SHORTCUTS & RECENT REVENUE GRID -->
        <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <!-- Top Performing Agents Card -->
          <div class="bg-white p-5 rounded-3xl border border-slate-200 shadow-sm space-y-4">
            <div class="flex items-center justify-between">
              <h3 class="text-sm font-extrabold text-slate-900 flex items-center gap-2">
                <i class="fa-solid fa-trophy text-amber-500"></i> Top Sales Agents
              </h3>
              <button onclick="switchTab('agents')" class="text-xs font-bold text-emerald-600 hover:underline">View All →</button>
            </div>
            <div class="space-y-3">
              <?php foreach (array_slice($agents, 0, 3) as $ag): ?>
              <div class="p-3 bg-slate-50 rounded-2xl border border-slate-100 flex items-center justify-between">
                <div>
                  <p class="font-bold text-xs text-slate-900"><?= htmlspecialchars($ag['name']) ?> <span class="text-[10px] text-slate-400 font-mono">(<?= $ag['agent_code'] ?>)</span></p>
                  <p class="text-[11px] text-slate-500"><i class="fa-solid fa-store text-emerald-600"></i> <?= $ag['total_referred_shops'] ?> Stores Onboarded</p>
                </div>
                <div class="text-right">
                  <p class="text-xs font-extrabold text-emerald-700">LKR <?= number_format($ag['total_earned']) ?></p>
                  <p class="text-[10px] text-slate-400">Total Commission</p>
                </div>
              </div>
              <?php endforeach; ?>
            </div>
          </div>

          <!-- Unpaid Commission Payouts Card -->
          <div class="bg-white p-5 rounded-3xl border border-slate-200 shadow-sm space-y-4">
            <div class="flex items-center justify-between">
              <h3 class="text-sm font-extrabold text-slate-900 flex items-center gap-2">
                <i class="fa-solid fa-wallet text-emerald-600"></i> Pending Agent Payouts
              </h3>
              <button onclick="switchTab('commissions')" class="text-xs font-bold text-emerald-600 hover:underline">Manage →</button>
            </div>
            <div class="p-4 rounded-2xl bg-amber-50 border border-amber-200">
              <p class="text-xs text-amber-800 font-semibold">Total Unpaid Commission Balance:</p>
              <p class="text-2xl font-black text-amber-900 mt-1">LKR <?= number_format($total_unpaid_commissions) ?></p>
              <p class="text-[11px] text-amber-700 mt-1">Distribute to agents after monthly client subscription collection.</p>
            </div>
          </div>

          <!-- Fast Quick Actions -->
          <div class="bg-white p-5 rounded-3xl border border-slate-200 shadow-sm space-y-3">
            <h3 class="text-sm font-extrabold text-slate-900 flex items-center gap-2">
              <i class="fa-solid fa-bolt text-amber-500"></i> Quick Actions
            </h3>
            <button onclick="openAddStoreModal()" class="w-full py-2.5 px-4 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs flex items-center justify-between transition">
              <span>+ Register New Client Store</span>
              <i class="fa-solid fa-arrow-right"></i>
            </button>
            <button onclick="openAddAgentModal()" class="w-full py-2.5 px-4 rounded-xl bg-slate-900 hover:bg-slate-800 text-white font-bold text-xs flex items-center justify-between transition">
              <span>+ Onboard New Sales Agent</span>
              <i class="fa-solid fa-arrow-right"></i>
            </button>
            <button onclick="switchTab('updates')" class="w-full py-2.5 px-4 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-800 font-bold text-xs flex items-center justify-between transition">
              <span>🚀 Broadcast New APK Update</span>
              <i class="fa-solid fa-arrow-right"></i>
            </button>
          </div>
        </div>
      </section>

      <!-- ======================================================== -->
      <!-- 🏪 TAB 2: STORES & LICENSES -->
      <!-- ======================================================== -->
      <section id="tab-stores" class="tab-content space-y-4">
        <div class="bg-white rounded-3xl shadow-sm border border-slate-200 overflow-hidden">
          <div class="p-5 border-b border-slate-200 flex flex-col md:flex-row gap-4 items-center justify-between">
            <div class="flex items-center gap-3">
              <h3 class="text-sm font-extrabold text-slate-900">All Client Stores &amp; Remote Hubs</h3>
              <span class="px-3 py-1 rounded-full bg-slate-100 text-slate-700 text-xs font-bold"><?= count($stores) ?> Total</span>
            </div>
            <button onclick="openAddStoreModal()" class="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold rounded-xl shadow transition flex items-center gap-1.5">
              <i class="fa-solid fa-plus"></i> + Add New Store
            </button>
          </div>

          <div class="overflow-x-auto">
            <table class="w-full text-left text-xs">
              <thead class="bg-slate-50 text-slate-500 uppercase font-bold border-b border-slate-200">
                <tr>
                  <th class="px-6 py-4">Store Details</th>
                  <th class="px-6 py-4">Owner &amp; Phone</th>
                  <th class="px-6 py-4">Reseller / Agent</th>
                  <th class="px-6 py-4">Plan &amp; Fee</th>
                  <th class="px-6 py-4">Due Date (අවසන් දිනය)</th>
                  <th class="px-6 py-4">Status</th>
                  <th class="px-6 py-4 text-right">Store Hub &amp; Actions</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-200">
                <?php foreach ($stores as $s): 
                  $exp = strtotime($s['expiry_date']);
                  $days_left = max(0, ceil(($exp - $now) / 86400));
                  $is_expired = $exp < $now || !$s['is_active'];
                  $s_features = getStoreFeatures($s['features_json'] ?? null);
                ?>
                <tr class="hover:bg-slate-50/80 transition">
                  <td class="px-6 py-4">
                    <div class="flex items-center gap-3">
                      <div class="px-2.5 py-1.5 min-w-[76px] text-center rounded-xl <?= $s['plan_type'] === 'PAID' ? 'bg-emerald-100 text-emerald-900 border border-emerald-300' : 'bg-amber-100 text-amber-900 border border-amber-300' ?> font-extrabold text-xs font-mono whitespace-nowrap shadow-sm">
                        <?= htmlspecialchars($s['shop_id']) ?>
                      </div>
                      <div class="space-y-1">
                        <p class="font-extrabold text-slate-900 text-xs"><?= htmlspecialchars($s['shop_name']) ?></p>
                        <div class="flex items-center gap-2 flex-wrap">
                          <span class="text-[11px] text-slate-500 whitespace-nowrap"><i class="fa-solid fa-location-dot text-slate-400 mr-0.5"></i><?= htmlspecialchars($s['city']) ?></span>
                          <?php
                            $btype = $s['business_type'] ?? 'RETAIL';
                            if ($btype === 'PHARMACY') {
                              echo '<span class="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-emerald-50 text-emerald-800 font-bold text-[10px] border border-emerald-200"><i class="fa-solid fa-prescription-bottle-medical text-[9px]"></i> Pharmacy</span>';
                            } elseif ($btype === 'RESTAURANT') {
                              echo '<span class="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-amber-50 text-amber-800 font-bold text-[10px] border border-amber-200"><i class="fa-solid fa-utensils text-[9px]"></i> Dining</span>';
                            } elseif ($btype === 'FASHION') {
                              echo '<span class="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-pink-50 text-pink-800 font-bold text-[10px] border border-pink-200"><i class="fa-solid fa-shirt text-[9px]"></i> Fashion</span>';
                            } elseif ($btype === 'HARDWARE') {
                              echo '<span class="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-orange-50 text-orange-800 font-bold text-[10px] border border-orange-200"><i class="fa-solid fa-screwdriver-wrench text-[9px]"></i> Hardware</span>';
                            } else {
                              echo '<span class="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-blue-50 text-blue-800 font-bold text-[10px] border border-blue-200"><i class="fa-solid fa-cart-shopping text-[9px]"></i> Retail</span>';
                            }
                          ?>
                        </div>
                      </div>
                    </div>
                  </td>

                  <td class="px-6 py-4">
                    <p class="font-bold text-slate-900 text-xs whitespace-nowrap"><?= htmlspecialchars($s['owner_name']) ?></p>
                    <p class="text-[11px] text-slate-500 font-mono whitespace-nowrap mt-0.5"><i class="fa-solid fa-phone text-slate-400 mr-1"></i><?= htmlspecialchars($s['phone']) ?></p>
                  </td>

                  <td class="px-6 py-4">
                    <?php if (!empty($s['referred_by_agent'])): ?>
                    <span class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-purple-50 text-purple-700 font-bold border border-purple-200 whitespace-nowrap">
                      <i class="fa-solid fa-user-tag text-[10px]"></i> <?= htmlspecialchars($s['referred_by_agent']) ?>
                    </span>
                    <?php else: ?>
                    <span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-slate-100 text-slate-500 font-semibold text-[11px] whitespace-nowrap">
                      <i class="fa-solid fa-user-slash text-[9px]"></i> Direct
                    </span>
                    <?php endif; ?>
                  </td>

                  <td class="px-6 py-4">
                    <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-[10px] font-black <?= $s['plan_type'] === 'PAID' ? 'bg-emerald-50 text-emerald-700 border border-emerald-200' : 'bg-amber-50 text-amber-700 border border-amber-200' ?> whitespace-nowrap">
                      <?= $s['plan_type'] === 'PAID' ? '🔵 Paid' : '🟢 Trial' ?>
                    </span>
                    <p class="text-[11px] text-slate-600 font-bold mt-1 whitespace-nowrap">LKR <?= number_format($s['monthly_fee']) ?>/mo</p>
                  </td>

                  <td class="px-6 py-4">
                    <p class="font-extrabold text-xs whitespace-nowrap <?= $is_expired ? 'text-red-600' : ($days_left <= 5 ? 'text-amber-600' : 'text-slate-900') ?>">
                      <?= date('d-M-Y', $exp) ?>
                    </p>
                    <?php if ($is_expired): ?>
                    <span class="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-red-100 text-red-700 font-extrabold text-[10px] mt-1 whitespace-nowrap">
                      <i class="fa-solid fa-lock text-[9px]"></i> Expired / Locked
                    </span>
                    <?php elseif ($days_left <= 5): ?>
                    <span class="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-amber-100 text-amber-900 font-bold text-[10px] mt-1 whitespace-nowrap">
                      <i class="fa-solid fa-triangle-exclamation text-[9px]"></i> <?= $days_left ?> days left
                    </span>
                    <?php else: ?>
                    <span class="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-emerald-50 text-emerald-700 font-bold text-[10px] mt-1 whitespace-nowrap border border-emerald-200">
                      <i class="fa-solid fa-check text-[9px]"></i> <?= $days_left ?> days left
                    </span>
                    <?php endif; ?>
                  </td>

                  <td class="px-6 py-4">
                    <span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[10px] font-black <?= $is_expired ? 'bg-red-100 text-red-700' : 'bg-emerald-100 text-emerald-800' ?> whitespace-nowrap">
                      <span class="w-1.5 h-1.5 rounded-full <?= $is_expired ? 'bg-red-500' : 'bg-emerald-500 animate-pulse' ?>"></span>
                      <?= $is_expired ? 'Inactive' : 'Active' ?>
                    </span>
                  </td>

                  <td class="px-6 py-4 text-right space-x-2 whitespace-nowrap">
                    <?php
                      $clean_p = preg_replace('/[^0-9]/', '', $s['phone']);
                      if (strpos($clean_p, '0') === 0) $clean_p = '94' . substr($clean_p, 1);
                      $apk_link = !empty($config['apk_download_url']) ? $config['apk_download_url'] : ("http://" . ($_SERVER['HTTP_HOST'] ?? 'localhost') . "/possystem/downloads/possystem.apk");
                      $quick_msg = urlencode("🎉 *Store License & APK Download - POS Lanka*\n\n"
                        . "🏪 *Store:* {$s['shop_name']}\n"
                        . "🔑 *Store ID:* {$s['shop_id']}\n"
                        . "🏢 *Mode:* {$s['business_type']}\n"
                        . "🔢 *Activation PIN:* {$s['support_pin']}\n"
                        . "📅 *Valid Until:* " . date('d-M-Y', $exp) . " ({$s['plan_type']})\n\n"
                        . "📥 *Download POS APK:*\n{$apk_link}\n\n"
                        . "🚀 *පියවර 3කින් ඇරඹුම:*\n"
                        . "1️⃣ ඉහත Link එකෙන් App එක Download කර Install කරන්න.\n"
                        . "2️⃣ Store ID ({$s['shop_id']}) හා PIN ({$s['support_pin']}) ඇතුළත් කරන්න.\n"
                        . "3️⃣ 100% නොමිලේ බිල් කිරීම අරඹන්න!\n\n"
                        . "📞 Helpline: 077 123 4567");
                    ?>
                    <a href="https://wa.me/<?= $clean_p ?>?text=<?= $quick_msg ?>" target="_blank" title="Send APK & License Voucher via WhatsApp" class="px-2.5 py-1.5 rounded-xl bg-emerald-500 hover:bg-emerald-600 text-white font-bold text-xs shadow-sm transition inline-flex items-center gap-1">
                      <i class="fa-brands fa-whatsapp text-sm"></i> Send APK
                    </a>
                    <button type="button" onclick='openStoreHub(<?= json_encode($s, JSON_HEX_TAG | JSON_HEX_APOS | JSON_HEX_QUOT | JSON_HEX_AMP) ?>, <?= json_encode($s_features) ?>)' 
                            class="px-3.5 py-1.5 rounded-xl bg-slate-900 hover:bg-slate-800 text-white font-extrabold text-xs inline-flex items-center gap-1.5 shadow-sm transition">
                      <i class="fa-solid fa-sliders text-emerald-400"></i> Manage Hub
                    </button>
                    <a href="index.php?extend_id=<?= $s['shop_id'] ?>&days=30" class="px-2.5 py-1.5 rounded-xl bg-emerald-600 text-white hover:bg-emerald-700 font-bold text-xs shadow-sm transition inline-block">
                      +30D
                    </a>
                    <a href="index.php?toggle_id=<?= $s['shop_id'] ?>" title="Toggle Remote Access" class="p-1.5 text-slate-400 hover:text-emerald-700 inline-block align-middle">
                      <i class="fa-solid <?= $s['is_active'] ? 'fa-toggle-on text-emerald-600 text-xl' : 'fa-toggle-off text-slate-400 text-xl' ?>"></i>
                    </a>
                    <a href="index.php?delete_id=<?= $s['shop_id'] ?>" onclick="return confirm('Delete store license for <?= htmlspecialchars($s['shop_name']) ?>?');" class="p-1.5 text-slate-400 hover:text-red-600 text-sm inline-block align-middle">
                      <i class="fa-solid fa-trash"></i>
                    </a>
                  </td>
                </tr>
                <?php endforeach; ?>
              </tbody>
            </table>
          </div>
        </div>
      </section>

      <!-- ======================================================== -->
      <!-- 🧑‍💼 TAB 3: SALES AGENTS & REFERRAL TEAM -->
      <!-- ======================================================== -->
      <section id="tab-agents" class="tab-content space-y-4">
        <div class="bg-white rounded-3xl shadow-sm border border-slate-200 overflow-hidden">
          <div class="p-5 border-b border-slate-200 flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
            <div>
              <h3 class="text-sm font-extrabold text-slate-900">Island-wide Sales Agents &amp; Franchise Network</h3>
              <p class="text-xs text-slate-500">Track stores onboarded by agents, recurring commissions, and payouts</p>
            </div>
            <button onclick="openAddAgentModal()" class="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold rounded-xl shadow transition flex items-center gap-1.5">
              <i class="fa-solid fa-user-plus"></i> + Onboard New Agent
            </button>
          </div>

          <div class="overflow-x-auto">
            <table class="w-full text-left text-xs">
              <thead class="bg-slate-50 text-slate-500 uppercase font-bold border-b border-slate-200">
                <tr>
                  <th class="px-6 py-4">Agent Code &amp; Name</th>
                  <th class="px-6 py-4">Phone &amp; Region</th>
                  <th class="px-6 py-4">Commission Rate</th>
                  <th class="px-6 py-4">Stores Onboarded</th>
                  <th class="px-6 py-4">Total Earned</th>
                  <th class="px-6 py-4">Unpaid Balance</th>
                  <th class="px-6 py-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-200">
                <?php foreach ($agents as $ag): ?>
                <tr class="hover:bg-slate-50/80 transition">
                  <td class="px-6 py-4">
                    <div class="flex items-center gap-3">
                      <div class="px-2.5 py-1.5 min-w-[76px] text-center rounded-xl bg-purple-100 text-purple-900 border border-purple-300 font-extrabold text-xs font-mono whitespace-nowrap shadow-sm">
                        <?= htmlspecialchars($ag['agent_code']) ?>
                      </div>
                      <div>
                        <p class="font-extrabold text-slate-900 text-xs"><?= htmlspecialchars($ag['name']) ?></p>
                        <p class="text-[11px] text-slate-400 font-mono whitespace-nowrap mt-0.5"><?= htmlspecialchars($ag['bank_details'] ?: 'No Bank Added') ?></p>
                      </div>
                    </div>
                  </td>

                  <td class="px-6 py-4">
                    <p class="font-bold text-slate-900 text-xs font-mono whitespace-nowrap"><?= htmlspecialchars($ag['phone']) ?></p>
                    <p class="text-[11px] text-slate-500 whitespace-nowrap mt-0.5"><i class="fa-solid fa-location-dot text-slate-400 mr-1"></i><?= htmlspecialchars($ag['city']) ?></p>
                  </td>

                  <td class="px-6 py-4">
                    <span class="inline-flex items-center px-2.5 py-1 rounded-lg bg-emerald-50 text-emerald-800 font-extrabold border border-emerald-200 whitespace-nowrap">
                      <?= number_format($ag['commission_percent']) ?>% Monthly
                    </span>
                  </td>

                  <td class="px-6 py-4">
                    <p class="font-extrabold text-slate-900 text-sm whitespace-nowrap"><?= $ag['total_referred_shops'] ?> Stores</p>
                  </td>

                  <td class="px-6 py-4">
                    <p class="font-extrabold text-emerald-700 text-sm whitespace-nowrap">LKR <?= number_format($ag['total_earned']) ?></p>
                  </td>

                  <td class="px-6 py-4">
                    <?php if ($ag['balance_payable'] > 0): ?>
                    <span class="inline-flex items-center px-3 py-1 rounded-full bg-amber-100 text-amber-950 font-black text-xs whitespace-nowrap border border-amber-300">
                      LKR <?= number_format($ag['balance_payable']) ?> Due
                    </span>
                    <?php else: ?>
                    <span class="text-slate-400 font-semibold whitespace-nowrap">Settled</span>
                    <?php endif; ?>
                  </td>

                  <td class="px-6 py-4 text-right space-x-2 whitespace-nowrap">
                    <?php if ($ag['balance_payable'] > 0): ?>
                    <a href="index.php?pay_agent_id=<?= $ag['agent_code'] ?>" onclick="return confirm('Mark LKR <?= number_format($ag['balance_payable']) ?> as Paid to <?= $ag['name'] ?>?');" 
                       class="inline-flex items-center gap-1 px-3 py-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-extrabold text-xs shadow transition">
                      ✓ Mark Paid
                    </a>
                    <?php endif; ?>
                    <a href="https://wa.me/<?= preg_replace('/[^0-9]/', '', $ag['phone']) ?>" target="_blank" class="inline-flex items-center justify-center p-2 rounded-xl bg-emerald-500 hover:bg-emerald-600 text-white text-xs shadow transition">
                      <i class="fa-brands fa-whatsapp text-sm"></i>
                    </a>
                  </td>
                </tr>
                <?php endforeach; ?>
              </tbody>
            </table>
          </div>
        </div>
      </section>

      <!-- ======================================================== -->
      <!-- 💰 TAB 4: COMMISSION LEDGER -->
      <!-- ======================================================== -->
      <section id="tab-commissions" class="tab-content space-y-4">
        <div class="bg-white rounded-3xl shadow-sm border border-slate-200 overflow-hidden">
          <div class="p-5 border-b border-slate-200">
            <h3 class="text-sm font-extrabold text-slate-900">Commission History &amp; Transaction Ledger</h3>
            <p class="text-xs text-slate-500">Automated recurring profit share generated when stores renew subscriptions</p>
          </div>

          <div class="overflow-x-auto">
            <table class="w-full text-left text-xs">
              <thead class="bg-slate-50 text-slate-500 uppercase font-bold border-b border-slate-200">
                <tr>
                  <th class="px-6 py-4">Agent Code</th>
                  <th class="px-6 py-4">Store ID &amp; Name</th>
                  <th class="px-6 py-4">Commission Amount</th>
                  <th class="px-6 py-4">Description</th>
                  <th class="px-6 py-4">Date &amp; Time</th>
                  <th class="px-6 py-4">Status</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-200">
                <?php foreach ($commissions as $c): ?>
                <tr class="hover:bg-slate-50/80 transition">
                  <td class="px-6 py-4 font-mono font-bold text-purple-800 whitespace-nowrap">
                    <span class="px-2 py-1 rounded-lg bg-purple-50 border border-purple-200"><?= htmlspecialchars($c['agent_code']) ?></span>
                  </td>
                  <td class="px-6 py-4">
                    <p class="font-bold text-slate-900"><?= htmlspecialchars($c['shop_name']) ?></p>
                    <p class="text-[11px] text-slate-400 font-mono"><?= htmlspecialchars($c['shop_id']) ?></p>
                  </td>
                  <td class="px-6 py-4 font-extrabold text-emerald-700 text-sm whitespace-nowrap">LKR <?= number_format($c['amount']) ?></td>
                  <td class="px-6 py-4 text-slate-600"><?= htmlspecialchars($c['description']) ?></td>
                  <td class="px-6 py-4 text-slate-500 whitespace-nowrap"><?= date('d-M-Y H:i', strtotime($c['created_at'])) ?></td>
                  <td class="px-6 py-4 whitespace-nowrap">
                    <span class="px-2.5 py-1 rounded-full text-[10px] font-extrabold <?= $c['status'] === 'PAID' ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-900' ?>">
                      <?= $c['status'] ?>
                    </span>
                  </td>
                </tr>
                <?php endforeach; ?>
              </tbody>
            </table>
          </div>
        </div>
      </section>

      <!-- ======================================================== -->
      <!-- 📩 TAB 5: WEBSITE LEADS -->
      <!-- ======================================================== -->
      <section id="tab-leads" class="tab-content space-y-4">
        <div class="bg-white rounded-3xl shadow-sm border border-slate-200 overflow-hidden">
          <div class="p-5 border-b border-slate-200">
            <h3 class="text-sm font-extrabold text-slate-900">Website Free Trial Requests &amp; Inquiries</h3>
            <p class="text-xs text-slate-500">Direct leads submitted by store owners on your public website</p>
          </div>

          <div class="overflow-x-auto">
            <table class="w-full text-left text-xs">
              <thead class="bg-slate-50 text-slate-500 uppercase font-bold border-b border-slate-200">
                <tr>
                  <th class="px-6 py-4">Store Name</th>
                  <th class="px-6 py-4">Owner &amp; Phone</th>
                  <th class="px-6 py-4">Category &amp; City</th>
                  <th class="px-6 py-4">Referred By</th>
                  <th class="px-6 py-4">Date</th>
                  <th class="px-6 py-4 text-right">Action</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-200">
                <?php foreach ($leads as $l): ?>
                <tr class="hover:bg-slate-50/80 transition">
                  <td class="px-6 py-4 font-bold text-slate-900 text-xs"><?= htmlspecialchars($l['shop_name']) ?></td>
                  <td class="px-6 py-4">
                    <p class="font-bold text-slate-900 text-xs whitespace-nowrap"><?= htmlspecialchars($l['owner_name']) ?></p>
                    <p class="text-[11px] text-slate-500 font-mono whitespace-nowrap"><?= htmlspecialchars($l['phone']) ?></p>
                  </td>
                  <td class="px-6 py-4">
                    <p class="font-semibold text-xs"><?= htmlspecialchars($l['business_type']) ?></p>
                    <p class="text-[11px] text-slate-400"><?= htmlspecialchars($l['city']) ?></p>
                  </td>
                  <td class="px-6 py-4 font-mono font-bold text-purple-700 whitespace-nowrap">
                    <?= htmlspecialchars($l['referred_by_agent'] ?: 'Direct') ?>
                  </td>
                  <td class="px-6 py-4 text-slate-500 whitespace-nowrap"><?= date('d-M-Y', strtotime($l['created_at'])) ?></td>
                  <td class="px-6 py-4 text-right whitespace-nowrap">
                    <a href="https://wa.me/<?= preg_replace('/[^0-9]/', '', $l['phone']) ?>" target="_blank" class="px-3.5 py-1.5 bg-emerald-500 hover:bg-emerald-600 text-white font-bold rounded-xl inline-flex items-center gap-1.5 shadow-sm transition">
                      <i class="fa-brands fa-whatsapp"></i> Chat Lead
                    </a>
                  </td>
                </tr>
                <?php endforeach; ?>
              </tbody>
            </table>
          </div>
        </div>
      </section>

      <!-- ======================================================== -->
      <!-- 🚀 TAB 6: IN-APP UPDATES -->
      <!-- ======================================================== -->
      <section id="tab-updates" class="tab-content space-y-4">
        <div class="bg-white rounded-3xl p-6 shadow-sm border border-slate-200 max-w-2xl space-y-5">
          <div>
            <h3 class="text-base font-extrabold text-slate-900 flex items-center gap-2">
              <i class="fa-solid fa-cloud-arrow-up text-amber-500"></i> In-App Update Configuration
            </h3>
            <p class="text-xs text-slate-500">When you release a new APK, update these details to notify all Android devices.</p>
          </div>

          <form method="POST" class="space-y-4">
            <input type="hidden" name="action" value="save_app_config" />
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="block text-xs font-bold text-slate-700 mb-1">Version Code</label>
                <input type="number" name="version_code" value="<?= $config['latest_version_code'] ?>" class="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs font-bold focus:ring-2 focus:ring-emerald-500 focus:outline-none" />
              </div>
              <div>
                <label class="block text-xs font-bold text-slate-700 mb-1">Version Name</label>
                <input type="text" name="version_name" value="<?= htmlspecialchars($config['latest_version_name']) ?>" class="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs font-bold focus:ring-2 focus:ring-emerald-500 focus:outline-none" />
              </div>
            </div>

            <div>
              <label class="block text-xs font-bold text-slate-700 mb-1">Direct APK Download Link</label>
              <input type="text" name="apk_url" value="<?= htmlspecialchars($config['apk_download_url']) ?>" class="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-emerald-500 focus:outline-none" />
            </div>

            <div>
              <label class="block text-xs font-bold text-slate-700 mb-1">Release Announcement / Notes</label>
              <textarea name="release_notes" rows="3" class="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-emerald-500 focus:outline-none"><?= htmlspecialchars($config['release_notes']) ?></textarea>
            </div>

            <button type="submit" class="px-6 py-3 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold rounded-xl shadow transition">
              Save &amp; Broadcast Update to All POS Apps
            </button>
          </form>
        </div>
      </section>

    </main>
  </div>

  <!-- ======================================================== -->
  <!-- 🎛️ MODAL: STORE HUB & MODULAR FEATURE MANAGEMENT -->
  <!-- ======================================================== -->
  <div id="storeHubModal" class="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm hidden flex items-center justify-center p-3 sm:p-6 overflow-y-auto">
    <div class="bg-white rounded-3xl max-w-4xl w-full shadow-2xl overflow-hidden animate-in fade-in my-auto border border-slate-200">
      
      <!-- Modal Header -->
      <div class="bg-slate-900 text-white p-5 flex items-center justify-between">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-2xl bg-emerald-600 flex items-center justify-center text-white text-lg font-bold shadow-md">
            <i class="fa-solid fa-store"></i>
          </div>
          <div>
            <div class="flex items-center gap-2">
              <h2 id="hub_header_title" class="text-base font-black">Store Hub Management</h2>
              <span id="hub_header_id" class="px-2 py-0.5 rounded-md bg-slate-800 text-emerald-400 font-mono text-xs font-bold">SHP-101</span>
            </div>
            <p class="text-xs text-slate-400">Configure due date, licensing, reseller, bill branding, and modular features</p>
          </div>
        </div>
        <button type="button" onclick="closeStoreHub()" class="text-slate-400 hover:text-white text-lg"><i class="fa-solid fa-xmark"></i></button>
      </div>

      <form method="POST" id="storeHubForm" enctype="multipart/form-data" class="p-6 space-y-6 max-h-[82vh] overflow-y-auto">
        <input type="hidden" name="action" value="save_store_hub" />
        <input type="hidden" name="shop_id" id="hub_shop_id" />
        <input type="hidden" name="logo_url" id="hub_logo_url" />

        <!-- SECTION 1: SUBSCRIPTION DUE DATE & LICENSING -->
        <div class="bg-emerald-50/70 p-4 rounded-2xl border border-emerald-200 space-y-3">
          <div class="flex items-center justify-between">
            <h3 class="text-xs font-black text-emerald-950 uppercase tracking-wider flex items-center gap-2">
              <i class="fa-solid fa-calendar-check text-emerald-600"></i> Subscription Due Date &amp; Status
            </h3>
            <span id="hub_days_badge" class="px-2.5 py-1 rounded-full text-xs font-black bg-emerald-200 text-emerald-900">Active</span>
          </div>

          <div class="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <div>
              <label class="block text-[11px] font-bold text-slate-700 mb-1">Due Date / Expiry Date *</label>
              <input type="datetime-local" name="expiry_date" id="hub_expiry_date" required class="w-full px-3 py-2 bg-white border border-slate-300 rounded-xl text-xs font-bold text-slate-900 focus:ring-2 focus:ring-emerald-500 focus:outline-none" />
            </div>
            <div>
              <label class="block text-[11px] font-bold text-slate-700 mb-1">Plan Type</label>
              <select name="plan_type" id="hub_plan_type" class="w-full px-3 py-2 bg-white border border-slate-300 rounded-xl text-xs font-bold text-slate-900 focus:ring-2 focus:ring-emerald-500 focus:outline-none">
                <option value="PAID">🔵 Paid Subscription</option>
                <option value="TRIAL">🟢 Free Trial (Demo)</option>
              </select>
            </div>
            <div>
              <label class="block text-[11px] font-bold text-slate-700 mb-1">Monthly Fee (LKR)</label>
              <input type="number" name="monthly_fee" id="hub_monthly_fee" value="3000" class="w-full px-3 py-2 bg-white border border-slate-300 rounded-xl text-xs font-bold text-slate-900 focus:ring-2 focus:ring-emerald-500 focus:outline-none" />
            </div>
          </div>

          <!-- Quick Extend Helper & Offline PIN -->
          <div class="pt-2 border-t border-emerald-200/60 flex flex-wrap items-center justify-between gap-2">
            <div class="flex items-center gap-2">
              <span class="text-[11px] font-bold text-emerald-900">Quick Extend:</span>
              <button type="button" onclick="hubExtendDays(30, 'PAID')" class="px-2 py-1 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white text-[11px] font-bold shadow-sm transition">+30D Paid</button>
              <button type="button" onclick="hubExtendDays(14, 'TRIAL')" class="px-2 py-1 rounded-lg bg-amber-600 hover:bg-amber-700 text-white text-[11px] font-bold shadow-sm transition">+14D Trial</button>
              <button type="button" onclick="hubExtendDays(7, 'TRIAL')" class="px-2 py-1 rounded-lg bg-slate-700 hover:bg-slate-800 text-white text-[11px] font-bold shadow-sm transition">+7D Trial</button>
            </div>
            <div class="flex items-center gap-2">
              <span class="text-[11px] font-bold text-slate-600">Offline Unlock PIN:</span>
              <span id="hub_support_pin" class="font-mono font-black text-sm text-emerald-800 bg-white px-2.5 py-0.5 rounded-lg border border-emerald-300">000000</span>
              <button type="button" onclick="copySupportPin()" class="p-1 rounded-lg text-slate-500 hover:text-emerald-700" title="Copy PIN"><i class="fa-solid fa-copy"></i></button>
            </div>
          </div>
        </div>

        <!-- SECTION 2: STORE INFO & INDUSTRY BUSINESS PROFILE -->
        <div class="bg-slate-50 p-4 rounded-2xl border border-slate-200 space-y-3">
          <h3 class="text-xs font-black text-slate-800 uppercase tracking-wider flex items-center gap-2">
            <i class="fa-solid fa-store text-slate-600"></i> Store Profile &amp; Industry Mode (ව්‍යාපාරික ආකෘතිය)
          </h3>

          <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label class="block text-[11px] font-bold text-slate-700 mb-1">Store Name *</label>
              <input type="text" name="shop_name" id="hub_shop_name" oninput="updateBillPreview()" required class="w-full px-3 py-2 bg-white border border-slate-300 rounded-xl text-xs font-semibold focus:ring-2 focus:ring-emerald-500 focus:outline-none" />
            </div>
            <div>
              <label class="block text-[11px] font-bold text-slate-700 mb-1">Owner Name *</label>
              <input type="text" name="owner_name" id="hub_owner_name" required class="w-full px-3 py-2 bg-white border border-slate-300 rounded-xl text-xs font-semibold focus:ring-2 focus:ring-emerald-500 focus:outline-none" />
            </div>
          </div>

          <div class="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <div>
              <label class="block text-[11px] font-bold text-slate-700 mb-1">Phone / WhatsApp *</label>
              <input type="text" name="phone" id="hub_phone" oninput="updateBillPreview()" required class="w-full px-3 py-2 bg-white border border-slate-300 rounded-xl text-xs font-semibold focus:ring-2 focus:ring-emerald-500 focus:outline-none font-mono" />
            </div>
            <div>
              <label class="block text-[11px] font-bold text-slate-700 mb-1">City / Town</label>
              <input type="text" name="city" id="hub_city" oninput="updateBillPreview()" class="w-full px-3 py-2 bg-white border border-slate-300 rounded-xl text-xs font-semibold focus:ring-2 focus:ring-emerald-500 focus:outline-none" />
            </div>
            <div>
              <label class="block text-[11px] font-bold text-slate-700 mb-1">
                <i class="fa-solid fa-shapes text-emerald-600 mr-1"></i> Industry Mode *
              </label>
              <select name="business_type" id="hub_business_type" class="w-full px-3 py-2 bg-white border border-slate-300 rounded-xl text-xs font-bold text-slate-800 focus:ring-2 focus:ring-emerald-500 focus:outline-none">
                <option value="RETAIL">🛒 Retail &amp; Grocery / Supermarket</option>
                <option value="PHARMACY">💊 Pharmacy &amp; Medi-Care</option>
                <option value="RESTAURANT">🍽️ Restaurant, Cafe &amp; Bakery</option>
                <option value="FASHION">👗 Fashion, Clothing &amp; Boutique</option>
                <option value="HARDWARE">🔨 Hardware, Electrical &amp; Paint</option>
              </select>
            </div>
          </div>

          <div>
            <label class="block text-[11px] font-bold text-slate-700 mb-1">
              <i class="fa-solid fa-user-tag text-purple-600 mr-1"></i> Assigned Sales Reseller / Agent
            </label>
            <select name="referred_by_agent" id="hub_referred_by_agent" class="w-full px-3 py-2 bg-white border border-slate-300 rounded-xl text-xs font-bold text-slate-800 focus:ring-2 focus:ring-purple-500 focus:outline-none">
              <option value="">🚫 No Reseller / Direct Customer (කිසිදු නියෝජිතයෙකු නැත - Direct)</option>
              <?php foreach ($agents as $ag): ?>
              <option value="<?= htmlspecialchars($ag['agent_code']) ?>">
                <?= htmlspecialchars($ag['agent_code']) ?> - <?= htmlspecialchars($ag['name']) ?> (<?= htmlspecialchars($ag['city']) ?>) • <?= $ag['commission_percent'] ?>%
              </option>
              <?php endforeach; ?>
            </select>
          </div>
        </div>

        <!-- SECTION 3: 🧾 RECEIPT BRANDING & LOGO CUSTOMIZER + LIVE PREVIEW -->
        <div class="bg-amber-50/50 p-4 rounded-2xl border border-amber-200 space-y-4">
          <div class="flex items-center justify-between">
            <div>
              <h3 class="text-xs font-black text-amber-950 uppercase tracking-wider flex items-center gap-2">
                <i class="fa-solid fa-receipt text-amber-700"></i> Thermal Receipt &amp; Logo Customizer (බිල්පත සහ Logo සැකසීම)
              </h3>
              <p class="text-[11px] text-amber-800">Customize the shop name, address, logo, header and footer notes printed on customer bills.</p>
            </div>
          </div>

          <div class="grid grid-cols-1 lg:grid-cols-12 gap-5">
            
            <!-- Left Form Fields -->
            <div class="lg:col-span-7 space-y-3">
              <div>
                <label class="block text-[11px] font-bold text-slate-700 mb-1">Shop Address &amp; Contact on Bill</label>
                <input type="text" name="address" id="hub_address" oninput="updateBillPreview()" placeholder="e.g. No. 45, Main Street, Galle • Tel: 077 123 4567" class="w-full px-3 py-2 bg-white border border-slate-300 rounded-xl text-xs font-semibold focus:ring-2 focus:ring-amber-500 focus:outline-none" />
              </div>

              <div>
                <label class="block text-[11px] font-bold text-slate-700 mb-1">Receipt Top Greeting / Header Note</label>
                <input type="text" name="receipt_header" id="hub_receipt_header" oninput="updateBillPreview()" placeholder="e.g. *** WELCOME TO OUR STORE ***" class="w-full px-3 py-2 bg-white border border-slate-300 rounded-xl text-xs font-semibold focus:ring-2 focus:ring-amber-500 focus:outline-none" />
              </div>

              <div>
                <label class="block text-[11px] font-bold text-slate-700 mb-1">Receipt Footer Note / Return Policy</label>
                <input type="text" name="receipt_footer" id="hub_receipt_footer" oninput="updateBillPreview()" placeholder="e.g. Goods return accepted within 7 days with bill. Come again!" class="w-full px-3 py-2 bg-white border border-slate-300 rounded-xl text-xs font-semibold focus:ring-2 focus:ring-amber-500 focus:outline-none" />
              </div>

              <!-- Store Logo Image Upload / Presets -->
              <div class="p-3 bg-white rounded-xl border border-slate-200 space-y-2">
                <label class="block text-[11px] font-bold text-slate-800 flex items-center justify-between">
                  <span><i class="fa-solid fa-image text-amber-600 mr-1"></i> Store Bill Logo / Image</span>
                  <span class="text-[10px] text-slate-400 font-normal">PNG / JPG / Preset</span>
                </label>

                <div class="flex items-center gap-2">
                  <input type="file" name="logo_file" id="hub_logo_file" accept="image/*" onchange="previewUploadedLogo(this)" class="block w-full text-xs text-slate-500 file:mr-2 file:py-1.5 file:px-3 file:rounded-lg file:border-0 file:text-xs file:font-bold file:bg-amber-100 file:text-amber-800 hover:file:bg-amber-200 cursor-pointer" />
                </div>

                <!-- Fast Preset Category Logos -->
                <div class="pt-1 flex items-center gap-1.5 flex-wrap">
                  <span class="text-[10px] font-bold text-slate-500">Presets:</span>
                  <button type="button" onclick="setLogoPreset('🛒 Supermarket', 'fa-cart-shopping')" class="px-2 py-0.5 rounded-lg bg-slate-100 hover:bg-amber-100 text-slate-700 text-[10px] font-bold border border-slate-200">🛒 Retail</button>
                  <button type="button" onclick="setLogoPreset('💊 Pharmacy', 'fa-prescription-bottle-medical')" class="px-2 py-0.5 rounded-lg bg-slate-100 hover:bg-amber-100 text-slate-700 text-[10px] font-bold border border-slate-200">💊 Pharmacy</button>
                  <button type="button" onclick="setLogoPreset('🔨 Hardware', 'fa-screwdriver-wrench')" class="px-2 py-0.5 rounded-lg bg-slate-100 hover:bg-amber-100 text-slate-700 text-[10px] font-bold border border-slate-200">🔨 Hardware</button>
                  <button type="button" onclick="setLogoPreset('🍽️ Restaurant', 'fa-utensils')" class="px-2 py-0.5 rounded-lg bg-slate-100 hover:bg-amber-100 text-slate-700 text-[10px] font-bold border border-slate-200">🍽️ Dining</button>
                  <button type="button" onclick="setLogoPreset('👗 Boutique', 'fa-shirt')" class="px-2 py-0.5 rounded-lg bg-slate-100 hover:bg-amber-100 text-slate-700 text-[10px] font-bold border border-slate-200">👗 Boutique</button>
                </div>
              </div>
            </div>

            <!-- Right: Real-time Live Thermal Paper Receipt Preview -->
            <div class="lg:col-span-5 flex flex-col items-center">
              <span class="text-[10px] font-black uppercase tracking-wider text-slate-400 mb-1 flex items-center gap-1">
                <i class="fa-solid fa-eye text-emerald-600"></i> Live Thermal Bill Preview (58mm)
              </span>

              <!-- Thermal Paper Simulation -->
              <div class="w-full max-w-[260px] bg-white border border-slate-300 shadow-md p-4 rounded-sm font-mono text-[11px] text-slate-800 space-y-2 relative border-dashed">
                
                <!-- Logo & Shop Header -->
                <div class="text-center space-y-0.5 border-b border-dashed border-slate-400 pb-2">
                  <div id="prev_logo_container" class="w-10 h-10 mx-auto mb-1 rounded-full bg-slate-900 text-white flex items-center justify-center text-lg font-bold overflow-hidden">
                    <i id="prev_logo_icon" class="fa-solid fa-store"></i>
                    <img id="prev_logo_img" src="" class="w-full h-full object-contain hidden" alt="Logo" />
                  </div>
                  <p id="prev_shop_name" class="font-black text-xs uppercase tracking-wider text-slate-900">SAMAN SUPER MART</p>
                  <p id="prev_address" class="text-[9px] text-slate-600 leading-tight">No. 45, Main Street, Galle</p>
                  <p id="prev_phone" class="text-[9px] text-slate-600">Tel: 077 123 4567</p>
                  <p id="prev_header_note" class="text-[9px] font-bold text-slate-700 pt-1">*** WELCOME ***</p>
                </div>

                <!-- Sample Line Items -->
                <div class="space-y-1 text-[10px] border-b border-dashed border-slate-400 pb-2">
                  <div class="flex justify-between text-slate-500 font-bold uppercase text-[9px]">
                    <span>Item / Qty</span>
                    <span>Total</span>
                  </div>
                  <div class="flex justify-between">
                    <span>Anchor Milk 400g x 1</span>
                    <span>1,150.00</span>
                  </div>
                  <div class="flex justify-between">
                    <span>Samaposha 200g x 2</span>
                    <span>440.00</span>
                  </div>
                  <div class="flex justify-between">
                    <span>Sunlight Soap x 3</span>
                    <span>390.00</span>
                  </div>
                </div>

                <!-- Totals -->
                <div class="space-y-0.5 text-[10px] border-b border-dashed border-slate-400 pb-2">
                  <div class="flex justify-between font-extrabold text-xs text-slate-900">
                    <span>TOTAL:</span>
                    <span>LKR 1,980.00</span>
                  </div>
                  <div class="flex justify-between text-[9px] text-slate-600">
                    <span>Cash Tendered:</span>
                    <span>2,000.00</span>
                  </div>
                  <div class="flex justify-between text-[9px] text-slate-600">
                    <span>Change Due:</span>
                    <span>20.00</span>
                  </div>
                </div>

                <!-- Custom Footer Policy Note -->
                <div class="text-center pt-1 space-y-0.5">
                  <p id="prev_footer_note" class="text-[9px] text-slate-700 font-semibold leading-tight">
                    Thank you for shopping with us! • Exchange within 7 days
                  </p>
                  <p class="text-[8px] text-slate-400 pt-1">Powered by POS Lanka SaaS</p>
                </div>

              </div>
            </div>

          </div>
        </div>

        <!-- SECTION 4: MODULAR FEATURE SWITCHES & PACKAGES -->
        <div class="bg-indigo-50/60 p-4 rounded-2xl border border-indigo-200 space-y-3">
          <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
            <div>
              <h3 class="text-xs font-black text-indigo-950 uppercase tracking-wider flex items-center gap-2">
                <i class="fa-solid fa-sliders text-indigo-600"></i> App Features &amp; Package Control (Features අඩු/වැඩි කිරීම)
              </h3>
              <p class="text-[11px] text-indigo-800">Toggle which modules are enabled or locked for this store's Android app.</p>
            </div>

            <!-- 1-Click Preset Buttons -->
            <div class="flex items-center gap-1.5 flex-wrap">
              <span class="text-[10px] font-bold text-indigo-900 uppercase">Presets:</span>
              <button type="button" onclick="applyFeaturePreset('STARTER')" class="px-2.5 py-1 rounded-lg bg-emerald-100 hover:bg-emerald-200 text-emerald-900 font-extrabold text-[10px] border border-emerald-300 transition">Starter</button>
              <button type="button" onclick="applyFeaturePreset('STANDARD')" class="px-2.5 py-1 rounded-lg bg-blue-100 hover:bg-blue-200 text-blue-900 font-extrabold text-[10px] border border-blue-300 transition">Standard</button>
              <button type="button" onclick="applyFeaturePreset('PRO')" class="px-2.5 py-1 rounded-lg bg-purple-100 hover:bg-purple-200 text-purple-900 font-extrabold text-[10px] border border-purple-300 transition">Full Pro (Demo)</button>
            </div>
          </div>

          <!-- Feature Checkboxes Grid -->
          <div class="grid grid-cols-1 sm:grid-cols-3 gap-2.5 pt-2">
            
            <!-- 1. POS Billing -->
            <label class="p-3 bg-white rounded-xl border border-slate-200 hover:border-indigo-400 flex items-start gap-2.5 cursor-pointer shadow-sm transition">
              <input type="checkbox" name="feat_pos" id="feat_pos" class="mt-0.5 rounded text-emerald-600 focus:ring-emerald-500 w-4 h-4" checked />
              <div>
                <p class="text-xs font-bold text-slate-900"><i class="fa-solid fa-cash-register text-emerald-600 mr-1"></i> POS Billing</p>
                <p class="text-[10px] text-slate-400">Cash/Card Checkout</p>
              </div>
            </label>

            <!-- 2. Inventory -->
            <label class="p-3 bg-white rounded-xl border border-slate-200 hover:border-indigo-400 flex items-start gap-2.5 cursor-pointer shadow-sm transition">
              <input type="checkbox" name="feat_inventory" id="feat_inventory" class="mt-0.5 rounded text-emerald-600 focus:ring-emerald-500 w-4 h-4" checked />
              <div>
                <p class="text-xs font-bold text-slate-900"><i class="fa-solid fa-boxes-stacked text-blue-600 mr-1"></i> Stock / Inventory</p>
                <p class="text-[10px] text-slate-400">Stock &amp; Low Alert</p>
              </div>
            </label>

            <!-- 3. Customer Credit / ණය පොත -->
            <label class="p-3 bg-white rounded-xl border border-slate-200 hover:border-indigo-400 flex items-start gap-2.5 cursor-pointer shadow-sm transition">
              <input type="checkbox" name="feat_credit" id="feat_credit" class="mt-0.5 rounded text-emerald-600 focus:ring-emerald-500 w-4 h-4" checked />
              <div>
                <p class="text-xs font-bold text-slate-900"><i class="fa-solid fa-book-bookmark text-amber-600 mr-1"></i> ණය පොත (Credit)</p>
                <p class="text-[10px] text-slate-400">Customer Balances</p>
              </div>
            </label>

            <!-- 4. Purchases / GRN -->
            <label class="p-3 bg-white rounded-xl border border-slate-200 hover:border-indigo-400 flex items-start gap-2.5 cursor-pointer shadow-sm transition">
              <input type="checkbox" name="feat_purchases" id="feat_purchases" class="mt-0.5 rounded text-emerald-600 focus:ring-emerald-500 w-4 h-4" checked />
              <div>
                <p class="text-xs font-bold text-slate-900"><i class="fa-solid fa-truck-ramp-box text-purple-600 mr-1"></i> Purchases (GRN)</p>
                <p class="text-[10px] text-slate-400">Supplier Purchases</p>
              </div>
            </label>

            <!-- 5. Daily Expenses -->
            <label class="p-3 bg-white rounded-xl border border-slate-200 hover:border-indigo-400 flex items-start gap-2.5 cursor-pointer shadow-sm transition">
              <input type="checkbox" name="feat_expenses" id="feat_expenses" class="mt-0.5 rounded text-emerald-600 focus:ring-emerald-500 w-4 h-4" checked />
              <div>
                <p class="text-xs font-bold text-slate-900"><i class="fa-solid fa-receipt text-rose-600 mr-1"></i> Daily Expenses</p>
                <p class="text-[10px] text-slate-400">Petty Cash Tracking</p>
              </div>
            </label>

            <!-- 6. Sales Reports -->
            <label class="p-3 bg-white rounded-xl border border-slate-200 hover:border-indigo-400 flex items-start gap-2.5 cursor-pointer shadow-sm transition">
              <input type="checkbox" name="feat_reports" id="feat_reports" class="mt-0.5 rounded text-emerald-600 focus:ring-emerald-500 w-4 h-4" checked />
              <div>
                <p class="text-xs font-bold text-slate-900"><i class="fa-solid fa-chart-pie text-emerald-600 mr-1"></i> Profit &amp; Reports</p>
                <p class="text-[10px] text-slate-400">Sales &amp; Net Profit</p>
              </div>
            </label>

            <!-- 7. Barcode Scanner -->
            <label class="p-3 bg-white rounded-xl border border-slate-200 hover:border-indigo-400 flex items-start gap-2.5 cursor-pointer shadow-sm transition">
              <input type="checkbox" name="feat_barcode" id="feat_barcode" class="mt-0.5 rounded text-emerald-600 focus:ring-emerald-500 w-4 h-4" checked />
              <div>
                <p class="text-xs font-bold text-slate-900"><i class="fa-solid fa-barcode text-cyan-600 mr-1"></i> Barcode Scanner</p>
                <p class="text-[10px] text-slate-400">Camera / Laser Scan</p>
              </div>
            </label>

            <!-- 8. WhatsApp Digital Receipts -->
            <label class="p-3 bg-white rounded-xl border border-slate-200 hover:border-indigo-400 flex items-start gap-2.5 cursor-pointer shadow-sm transition">
              <input type="checkbox" name="feat_whatsapp" id="feat_whatsapp" class="mt-0.5 rounded text-emerald-600 focus:ring-emerald-500 w-4 h-4" checked />
              <div>
                <p class="text-xs font-bold text-slate-900"><i class="fa-brands fa-whatsapp text-emerald-600 mr-1"></i> WhatsApp Bill</p>
                <p class="text-[10px] text-slate-400">Send Digital Receipt</p>
              </div>
            </label>

            <!-- 9. Multi-User & Cashier Roles -->
            <label class="p-3 bg-white rounded-xl border border-slate-200 hover:border-indigo-400 flex items-start gap-2.5 cursor-pointer shadow-sm transition">
              <input type="checkbox" name="feat_multi_user" id="feat_multi_user" class="mt-0.5 rounded text-emerald-600 focus:ring-emerald-500 w-4 h-4" checked />
              <div>
                <p class="text-xs font-bold text-slate-900"><i class="fa-solid fa-users text-indigo-600 mr-1"></i> Multi-User / Cashier</p>
                <p class="text-[10px] text-slate-400">Role Permissions</p>
              </div>
            </label>

          </div>
        </div>

        <!-- SECTION 5: ACCESS SWITCH & ACTIONS -->
        <div class="flex flex-col sm:flex-row items-center justify-between gap-4 pt-2">
          <label class="flex items-center gap-2 cursor-pointer">
            <input type="checkbox" name="is_active" id="hub_is_active" class="rounded text-emerald-600 focus:ring-emerald-500 w-4 h-4" checked />
            <span class="text-xs font-bold text-slate-800">Store Active (Uncheck to remote-disable / kill switch)</span>
          </label>

          <div class="flex items-center gap-2 w-full sm:w-auto">
            <button type="button" id="btnHubWhatsApp" class="flex-1 sm:flex-none px-4 py-2.5 bg-emerald-500 hover:bg-emerald-600 text-white text-xs font-bold rounded-xl shadow transition flex items-center justify-center gap-1.5">
              <i class="fa-brands fa-whatsapp text-base"></i> Send WhatsApp Reminder
            </button>
            <button type="submit" class="flex-1 sm:flex-none px-6 py-2.5 bg-slate-900 hover:bg-slate-800 text-white text-xs font-bold rounded-xl shadow transition">
              💾 Save Store, Branding &amp; Features
            </button>
          </div>
        </div>

      </form>
    </div>
  </div>

  <!-- MODAL: ADD STORE -->
  <div id="addStoreModal" class="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm hidden flex items-center justify-center p-4">
    <div class="bg-white rounded-3xl max-w-lg w-full shadow-2xl overflow-hidden animate-in fade-in">
      <div class="bg-emerald-800 text-white p-5 flex items-center justify-between">
        <h2 class="text-sm font-bold flex items-center gap-2"><i class="fa-solid fa-store"></i> Register New Client Store</h2>
        <button onclick="closeAddStoreModal()" class="text-emerald-200 hover:text-white"><i class="fa-solid fa-xmark text-base"></i></button>
      </div>

      <form method="POST" class="p-6 space-y-3.5">
        <input type="hidden" name="action" value="add_store" />
        <div>
          <label class="block text-xs font-bold text-slate-700 mb-1">Store Name (කඩේ නම) *</label>
          <input type="text" name="shop_name" required placeholder="e.g. Saman Super Mart" class="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-emerald-500 focus:outline-none" />
        </div>

        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="block text-xs font-bold text-slate-700 mb-1">Owner Name *</label>
            <input type="text" name="owner_name" required placeholder="Saman Kumara" class="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-emerald-500 focus:outline-none" />
          </div>
          <div>
            <label class="block text-xs font-bold text-slate-700 mb-1">Phone / WhatsApp *</label>
            <input type="text" name="phone" required placeholder="0771234567" class="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-emerald-500 focus:outline-none" />
          </div>
        </div>

        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="block text-xs font-bold text-slate-700 mb-1">City / Town</label>
            <input type="text" name="city" placeholder="Galle / Colombo" class="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-emerald-500 focus:outline-none" />
          </div>
          <div>
            <label class="block text-xs font-bold text-slate-700 mb-1">
              <i class="fa-solid fa-shapes text-emerald-600 mr-1"></i> Industry Mode *
            </label>
            <select name="business_type" class="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs font-bold text-slate-800 focus:ring-2 focus:ring-emerald-500 focus:outline-none">
              <option value="RETAIL">🛒 Retail &amp; Grocery / Supermarket</option>
              <option value="PHARMACY">💊 Pharmacy &amp; Medi-Care</option>
              <option value="RESTAURANT">🍽️ Restaurant, Cafe &amp; Bakery</option>
              <option value="FASHION">👗 Fashion, Clothing &amp; Boutique</option>
              <option value="HARDWARE">🔨 Hardware, Electrical &amp; Paint</option>
            </select>
          </div>
        </div>

        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="block text-xs font-bold text-slate-700 mb-1">Referred By Agent (Code)</label>
            <select name="referred_by_agent" class="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-emerald-500 focus:outline-none font-semibold">
              <option value="">🚫 No Reseller / Direct Customer (Direct)</option>
              <?php foreach ($agents as $ag): ?>
              <option value="<?= $ag['agent_code'] ?>"><?= $ag['agent_code'] ?> - <?= htmlspecialchars($ag['name']) ?> (<?= htmlspecialchars($ag['city']) ?>)</option>
              <?php endforeach; ?>
            </select>
          </div>
          <div>
            <label class="block text-xs font-bold text-slate-700 mb-1">Monthly Fee (LKR)</label>
            <input type="number" name="monthly_fee" value="3000" class="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-emerald-500 focus:outline-none" />
          </div>
        </div>

        <div>
          <label class="block text-xs font-bold text-slate-700 mb-1">Initial Plan / Duration *</label>
          <select name="plan_duration" class="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-emerald-500 focus:outline-none">
            <option value="TRIAL_14">🟢 14 Days Free Trial (Full Demo)</option>
            <option value="TRIAL_7">🟢 7 Days Free Trial (Full Demo)</option>
            <option value="TRIAL_30">🟢 30 Days Free Trial (Full Demo)</option>
            <option value="PAID_30">🔵 1 Month Paid (30 Days)</option>
            <option value="PAID_90">🔵 3 Months Paid (90 Days)</option>
            <option value="PAID_365">🔵 1 Year Paid (365 Days)</option>
          </select>
        </div>

        <div class="pt-3 border-t border-slate-200 flex justify-end gap-2">
          <button type="button" onclick="closeAddStoreModal()" class="px-4 py-2 rounded-xl text-xs font-bold text-slate-600 hover:bg-slate-100">Cancel</button>
          <button type="submit" class="px-5 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold shadow">Save &amp; Generate PIN</button>
        </div>
      </form>
    </div>
  </div>

  <!-- MODAL: ADD AGENT -->
  <div id="addAgentModal" class="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm hidden flex items-center justify-center p-4">
    <div class="bg-white rounded-3xl max-w-lg w-full shadow-2xl overflow-hidden animate-in fade-in">
      <div class="bg-slate-900 text-white p-5 flex items-center justify-between">
        <h2 class="text-sm font-bold flex items-center gap-2"><i class="fa-solid fa-user-plus text-emerald-400"></i> Onboard New Sales Agent</h2>
        <button onclick="closeAddAgentModal()" class="text-slate-400 hover:text-white"><i class="fa-solid fa-xmark text-base"></i></button>
      </div>

      <form method="POST" class="p-6 space-y-3.5">
        <input type="hidden" name="action" value="add_agent" />
        <div>
          <label class="block text-xs font-bold text-slate-700 mb-1">Agent Full Name *</label>
          <input type="text" name="agent_name" required placeholder="e.g. Kasun Bandara" class="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-slate-900 focus:outline-none" />
        </div>

        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="block text-xs font-bold text-slate-700 mb-1">Phone / WhatsApp *</label>
            <input type="text" name="agent_phone" required placeholder="0778899001" class="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-slate-900 focus:outline-none" />
          </div>
          <div>
            <label class="block text-xs font-bold text-slate-700 mb-1">City / Region *</label>
            <input type="text" name="agent_city" required placeholder="Colombo / Galle" class="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-slate-900 focus:outline-none" />
          </div>
        </div>

        <div>
          <label class="block text-xs font-bold text-slate-700 mb-1">Commission Share (%)</label>
          <input type="number" name="commission_percent" value="20" class="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-slate-900 focus:outline-none" />
          <p class="text-[10px] text-slate-400 mt-0.5">20% = Rs. 600 per month for every Rs. 3000 paid store.</p>
        </div>

        <div>
          <label class="block text-xs font-bold text-slate-700 mb-1">Bank Account &amp; Branch Details</label>
          <textarea name="bank_details" rows="2" placeholder="Bank Name, Account Number, Account Name, Branch" class="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-slate-900 focus:outline-none"></textarea>
        </div>

        <div class="pt-3 border-t border-slate-200 flex justify-end gap-2">
          <button type="button" onclick="closeAddAgentModal()" class="px-4 py-2 rounded-xl text-xs font-bold text-slate-600 hover:bg-slate-100">Cancel</button>
          <button type="submit" class="px-5 py-2 rounded-xl bg-slate-900 hover:bg-slate-800 text-white text-xs font-bold shadow">Register Agent</button>
        </div>
      </form>
    </div>
  </div>

  <!-- VOUCHER POPUP IF GENERATED -->
  <?php if ($voucher_data): 
    $apk_download = !empty($config['apk_download_url']) ? $config['apk_download_url'] : ("http://" . ($_SERVER['HTTP_HOST'] ?? 'localhost') . "/possystem/downloads/possystem.apk");
    $clean_phone = preg_replace('/[^0-9]/', '', $voucher_data['phone'] ?? '');
    if (strpos($clean_phone, '0') === 0) $clean_phone = '94' . substr($clean_phone, 1);

    $wa_msg = urlencode("🎉 *Store License & APK Download - POS Lanka*\n\n"
      . "🏪 *Store Name:* {$voucher_data['shop_name']}\n"
      . "🔑 *Store ID:* {$voucher_data['shop_id']}\n"
      . "🔢 *Activation PIN:* {$voucher_data['pin']}\n"
      . "📅 *Valid Until:* {$voucher_data['expiry']} ({$voucher_data['plan']})\n\n"
      . "📥 *Download POS Android App (APK):*\n{$apk_download}\n\n"
      . "🚀 *පියවර 3කින් ඇරඹුම:*\n"
      . "1️⃣ ඉහත Link එකෙන් App එක Download කර Phone එකට Install කරගන්න.\n"
      . "2️⃣ App එක Open කර Store ID ({$voucher_data['shop_id']}) හා PIN ({$voucher_data['pin']}) ඇතුළත් කරන්න.\n"
      . "3️⃣ 100% නොමිලේ බිල් කිරීම අරඹන්න!\n\n"
      . "📞 Admin Helpline: 077 123 4567");
  ?>
  <div id="voucherPopup" class="fixed inset-0 z-50 bg-slate-900/80 backdrop-blur-sm flex items-center justify-center p-4">
    <div class="bg-white rounded-3xl max-w-lg w-full shadow-2xl overflow-hidden animate-in fade-in zoom-in-95 duration-200 border border-slate-200">
      
      <!-- Voucher Header -->
      <div class="bg-emerald-800 text-white p-6 text-center space-y-2 relative">
        <div class="w-14 h-14 rounded-2xl bg-emerald-600 mx-auto flex items-center justify-center text-2xl shadow-lg border border-emerald-400/40">
          <i class="fa-solid fa-key text-amber-300"></i>
        </div>
        <h3 class="text-lg font-black"><?= htmlspecialchars($voucher_data['shop_name']) ?></h3>
        <p class="text-xs text-emerald-200">Store License &amp; Android APK Activation Voucher</p>
      </div>

      <div class="p-6 space-y-4">
        <!-- License Key Details -->
        <div class="bg-slate-50 p-4 rounded-2xl border border-slate-200 grid grid-cols-2 gap-3 text-center">
          <div class="p-2 bg-white rounded-xl border border-slate-200 shadow-sm">
            <p class="text-[10px] text-slate-500 font-bold uppercase">Store ID</p>
            <p class="text-xl font-mono font-black text-slate-900 mt-0.5"><?= $voucher_data['shop_id'] ?></p>
          </div>
          <div class="p-2 bg-emerald-50 rounded-xl border border-emerald-300 shadow-sm">
            <p class="text-[10px] text-emerald-800 font-bold uppercase">Activation PIN</p>
            <p class="text-2xl font-mono font-black text-emerald-700 tracking-wider mt-0.5"><?= $voucher_data['pin'] ?></p>
          </div>
        </div>

        <div class="text-center bg-amber-50 p-2.5 rounded-xl border border-amber-200 text-xs font-bold text-amber-900">
          📅 Valid: <?= $voucher_data['expiry'] ?> <span class="text-[11px] text-amber-700 font-semibold">(<?= $voucher_data['plan'] ?>)</span>
        </div>

        <!-- Direct APK Download Link Display -->
        <div class="p-3 bg-slate-100 rounded-xl text-xs space-y-1">
          <div class="flex items-center justify-between text-[11px] font-bold text-slate-700">
            <span><i class="fa-brands fa-android text-emerald-600 mr-1"></i> APK Download Link:</span>
            <span class="text-emerald-700 font-semibold">Latest Build</span>
          </div>
          <p class="text-[11px] font-mono text-slate-600 truncate select-all bg-white p-2 rounded-lg border border-slate-200">
            <?= htmlspecialchars($apk_download) ?>
          </p>
        </div>

        <!-- Action Buttons -->
        <div class="space-y-2 pt-1">
          <a href="https://wa.me/<?= $clean_phone ?>?text=<?= $wa_msg ?>" target="_blank" class="w-full py-3 bg-emerald-500 hover:bg-emerald-600 text-white font-extrabold text-xs rounded-xl flex items-center justify-center gap-2 shadow-md transition">
            <i class="fa-brands fa-whatsapp text-lg"></i> Send APK &amp; Voucher via WhatsApp
          </a>
          <div class="flex gap-2">
            <a href="<?= htmlspecialchars($apk_download) ?>" download class="flex-1 py-2.5 bg-slate-900 hover:bg-slate-800 text-white font-bold text-xs rounded-xl flex items-center justify-center gap-2 shadow transition">
              <i class="fa-solid fa-download"></i> Download APK
            </a>
            <button onclick="document.getElementById('voucherPopup').remove()" class="px-5 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold text-xs rounded-xl transition">
              Done
            </button>
          </div>
        </div>

      </div>
    </div>
  </div>
  <?php endif; ?>

  <!-- JAVASCRIPT TAB & MODAL CONTROL -->
  <script>
    function switchTab(tabId) {
      document.querySelectorAll('.tab-content').forEach(el => el.classList.remove('active'));
      document.querySelectorAll('.nav-link').forEach(el => el.classList.remove('active'));

      const targetTab = document.getElementById('tab-' + tabId);
      const targetNav = document.getElementById('nav-' + tabId);

      if (targetTab) targetTab.classList.add('active');
      if (targetNav) targetNav.classList.add('active');

      const titles = {
        'dashboard': 'Dashboard Overview',
        'stores': 'Client Stores & Licenses',
        'agents': 'Sales Agents & Referrals',
        'commissions': 'Commission & Payout Ledger',
        'leads': 'Website Inquiries & Leads',
        'updates': 'In-App Update Releases'
      };
      document.getElementById('pageTitle').innerText = titles[tabId] || 'Dashboard';
      window.location.hash = tabId;
    }

    // Modal functions
    function openAddStoreModal() { document.getElementById('addStoreModal').classList.remove('hidden'); }
    function closeAddStoreModal() { document.getElementById('addStoreModal').classList.add('hidden'); }
    function openAddAgentModal() { document.getElementById('addAgentModal').classList.remove('hidden'); }
    function closeAddAgentModal() { document.getElementById('addAgentModal').classList.add('hidden'); }

    // Store Hub Controller
    let currentHubStore = null;

    function openStoreHub(store, features) {
      currentHubStore = store;
      document.getElementById('hub_shop_id').value = store.shop_id || '';
      document.getElementById('hub_header_title').innerText = store.shop_name || 'Store Hub';
      document.getElementById('hub_header_id').innerText = store.shop_id || '';
      document.getElementById('hub_shop_name').value = store.shop_name || '';
      document.getElementById('hub_owner_name').value = store.owner_name || '';
      document.getElementById('hub_phone').value = store.phone || '';
      document.getElementById('hub_city').value = store.city || '';
      document.getElementById('hub_business_type').value = store.business_type || 'RETAIL';
      document.getElementById('hub_address').value = store.address || '';
      document.getElementById('hub_receipt_header').value = store.receipt_header || ('Welcome to ' + (store.shop_name || 'Store'));
      document.getElementById('hub_receipt_footer').value = store.receipt_footer || 'Thank you for shopping with us! • Exchange within 7 days';
      document.getElementById('hub_logo_url').value = store.logo_url || '';
      document.getElementById('hub_monthly_fee').value = store.monthly_fee || 3000;
      document.getElementById('hub_plan_type').value = store.plan_type || 'TRIAL';
      document.getElementById('hub_support_pin').innerText = store.support_pin || '000000';
      document.getElementById('hub_referred_by_agent').value = store.referred_by_agent || '';
      document.getElementById('hub_is_active').checked = parseInt(store.is_active) === 1;

      // Handle Logo Display in live preview
      const logoImg = document.getElementById('prev_logo_img');
      const logoIcon = document.getElementById('prev_logo_icon');
      if (store.logo_url && (store.logo_url.startsWith('http') || store.logo_url.startsWith('uploads/'))) {
        logoImg.src = '../' + store.logo_url.replace(/^\.\.\//, '');
        logoImg.classList.remove('hidden');
        logoIcon.classList.add('hidden');
      } else {
        logoImg.classList.add('hidden');
        logoIcon.classList.remove('hidden');
      }

      // Format Expiry date for datetime-local input (YYYY-MM-DDTHH:MM)
      if (store.expiry_date) {
        const d = new Date(store.expiry_date.replace(' ', 'T'));
        if (!isNaN(d.getTime())) {
          const pad = n => n < 10 ? '0' + n : n;
          const localIso = d.getFullYear() + '-' + pad(d.getMonth()+1) + '-' + pad(d.getDate()) + 'T' + pad(d.getHours()) + ':' + pad(d.getMinutes());
          document.getElementById('hub_expiry_date').value = localIso;

          // Calculate remaining days badge
          const nowMs = new Date().getTime();
          const expMs = d.getTime();
          const daysLeft = Math.ceil((expMs - nowMs) / (1000 * 60 * 60 * 24));
          const badge = document.getElementById('hub_days_badge');
          if (expMs < nowMs || parseInt(store.is_active) !== 1) {
            badge.className = 'px-2.5 py-1 rounded-full text-xs font-black bg-red-100 text-red-700';
            badge.innerText = '🔒 Expired / Locked';
          } else if (daysLeft <= 5) {
            badge.className = 'px-2.5 py-1 rounded-full text-xs font-black bg-amber-100 text-amber-900';
            badge.innerText = '⚠️ ' + daysLeft + ' Days Left';
          } else {
            badge.className = 'px-2.5 py-1 rounded-full text-xs font-black bg-emerald-100 text-emerald-900';
            badge.innerText = '🟢 ' + daysLeft + ' Days Remaining';
          }
        }
      }

      // Feature Checkboxes
      const featList = ['pos', 'inventory', 'credit', 'purchases', 'expenses', 'reports', 'barcode', 'whatsapp', 'multi_user'];
      featList.forEach(k => {
        const el = document.getElementById('feat_' + k);
        if (el) {
          el.checked = features && (features[k] === true || features[k] === 1 || features[k] === '1');
        }
      });

      // Update bill live preview
      updateBillPreview();

      // Setup WhatsApp reminder & voucher link
      const btnWa = document.getElementById('btnHubWhatsApp');
      if (btnWa) {
        btnWa.onclick = () => {
          let cleanPhone = (store.phone || '').replace(/[^0-9]/g, '');
          if (cleanPhone.startsWith('0')) cleanPhone = '94' + cleanPhone.substring(1);
          const expFormatted = store.expiry_date ? store.expiry_date.substring(0, 10) : 'N/A';
          const apkDownload = '<?= htmlspecialchars($config['apk_download_url'] ?? '') ?>' || (window.location.origin + '/possystem/downloads/possystem.apk');
          const msg = encodeURIComponent(
            `🎉 *POS Store License & APK Download - POS Lanka*\n\n` +
            `🏪 *Store:* ${store.shop_name}\n` +
            `🔑 *Store ID:* ${store.shop_id}\n` +
            `🏢 *Mode:* ${store.business_type || 'RETAIL'}\n` +
            `🔢 *Offline Unlock PIN:* ${store.support_pin}\n` +
            `📅 *Due Date:* ${expFormatted} (${store.plan_type || 'TRIAL'})\n` +
            `💵 *Monthly Fee:* LKR ${Number(store.monthly_fee).toLocaleString()}\n\n` +
            `📥 *Download POS App (APK):*\n${apkDownload}\n\n` +
            `🚀 *පියවර 3කින් ඇරඹුම:*\n` +
            `1️⃣ ඉහත Link එකෙන් App එක Download කර Install කරන්න.\n` +
            `2️⃣ Store ID (${store.shop_id}) සහ PIN (${store.support_pin}) ඇතුළත් කරන්න.\n` +
            `3️⃣ 100% නොමිලේ බිල් කිරීම අරඹන්න!\n\n` +
            `📞 Support: 077 123 4567`
          );
          window.open(`https://wa.me/${cleanPhone}?text=${msg}`, '_blank');
        };
      }

      document.getElementById('storeHubModal').classList.remove('hidden');
    }

    function closeStoreHub() {
      document.getElementById('storeHubModal').classList.add('hidden');
    }

    function hubExtendDays(days, newPlan) {
      const input = document.getElementById('hub_expiry_date');
      let currentVal = input.value ? new Date(input.value) : new Date();
      if (isNaN(currentVal.getTime()) || currentVal.getTime() < new Date().getTime()) {
        currentVal = new Date();
      }
      currentVal.setDate(currentVal.getDate() + days);
      const pad = n => n < 10 ? '0' + n : n;
      const localIso = currentVal.getFullYear() + '-' + pad(currentVal.getMonth()+1) + '-' + pad(currentVal.getDate()) + 'T' + pad(currentVal.getHours()) + ':' + pad(currentVal.getMinutes());
      input.value = localIso;
      if (newPlan) {
        document.getElementById('hub_plan_type').value = newPlan;
      }
    }

    function copySupportPin() {
      const pin = document.getElementById('hub_support_pin').innerText;
      navigator.clipboard.writeText(pin).then(() => {
        alert('PIN ' + pin + ' copied to clipboard!');
      });
    }

    function updateBillPreview() {
      const name = document.getElementById('hub_shop_name').value || 'STORE NAME';
      const address = document.getElementById('hub_address').value || document.getElementById('hub_city').value || 'No. 45, Main Street, Sri Lanka';
      const phone = document.getElementById('hub_phone').value || '077 123 4567';
      const header = document.getElementById('hub_receipt_header').value || '*** WELCOME ***';
      const footer = document.getElementById('hub_receipt_footer').value || 'Thank you for shopping! • Exchange within 7 days';

      document.getElementById('prev_shop_name').innerText = name.toUpperCase();
      document.getElementById('prev_address').innerText = address;
      document.getElementById('prev_phone').innerText = 'Tel: ' + phone;
      document.getElementById('prev_header_note').innerText = header;
      document.getElementById('prev_footer_note').innerText = footer;
    }

    function setLogoPreset(title, iconClass) {
      document.getElementById('hub_logo_url').value = 'preset:' + iconClass;
      const logoImg = document.getElementById('prev_logo_img');
      const logoIcon = document.getElementById('prev_logo_icon');
      logoImg.classList.add('hidden');
      logoIcon.className = 'fa-solid ' + iconClass;
      logoIcon.classList.remove('hidden');
    }

    function previewUploadedLogo(input) {
      if (input.files && input.files[0]) {
        const reader = new FileReader();
        reader.onload = function(e) {
          const logoImg = document.getElementById('prev_logo_img');
          const logoIcon = document.getElementById('prev_logo_icon');
          logoImg.src = e.target.result;
          logoImg.classList.remove('hidden');
          logoIcon.classList.add('hidden');
        };
        reader.readAsDataURL(input.files[0]);
      }
    }

    function applyFeaturePreset(preset) {
      const setFeat = (k, v) => {
        const el = document.getElementById('feat_' + k);
        if (el) el.checked = v;
      };

      if (preset === 'STARTER') {
        // Starter Plan: POS + Inventory + Barcode only
        setFeat('pos', true);
        setFeat('inventory', true);
        setFeat('credit', false);
        setFeat('purchases', false);
        setFeat('expenses', false);
        setFeat('reports', false);
        setFeat('barcode', true);
        setFeat('whatsapp', false);
        setFeat('multi_user', false);
      } else if (preset === 'STANDARD') {
        // Standard Plan: POS + Inventory + Credit Book + Expenses + Barcode
        setFeat('pos', true);
        setFeat('inventory', true);
        setFeat('credit', true);
        setFeat('purchases', true);
        setFeat('expenses', true);
        setFeat('reports', false);
        setFeat('barcode', true);
        setFeat('whatsapp', false);
        setFeat('multi_user', false);
      } else if (preset === 'PRO') {
        // Full Pro Demo: Everything enabled
        ['pos', 'inventory', 'credit', 'purchases', 'expenses', 'reports', 'barcode', 'whatsapp', 'multi_user'].forEach(k => setFeat(k, true));
      }
    }

    // Init tab from URL hash if available
    window.addEventListener('DOMContentLoaded', () => {
      const hash = window.location.hash.replace('#', '');
      if (hash && document.getElementById('tab-' + hash)) {
        switchTab(hash);
      }
    });
  </script>
</body>
</html>
