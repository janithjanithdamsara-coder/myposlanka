<?php
require_once 'db.php';

$success_msg = null;
$generated_shop = null;

// Handle 1-Month (30 Days) Free Trial Registration
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action']) && $_POST['action'] === 'request_trial') {
    $shop_name = trim($_POST['shop_name'] ?? '');
    $owner_name = trim($_POST['owner_name'] ?? '');
    $phone = trim($_POST['phone'] ?? '');
    $city = trim($_POST['city'] ?? 'Sri Lanka');
    $business_type = trim($_POST['business_type'] ?? 'Retail');
    $referred_by_agent = trim($_POST['referred_by_agent'] ?? '') ?: null;

    if (!empty($shop_name) && !empty($owner_name) && !empty($phone)) {
        // Generate Unique Shop ID
        $count = $pdo->query("SELECT COUNT(*) FROM `stores`")->fetchColumn();
        $shop_id = "SHP-" . (101 + $count);
        // Generate 30-Day (1 Month) Trial PIN
        $pin = generateOfflinePin($shop_id, 'RENEW_TRIAL_30');
        $expiry_date = date('Y-m-d H:i:s', strtotime('+30 days'));

        // Save to stores table with ALL features enabled for full demo experience
        $features_json = json_encode(getDefaultFeatures());
        $receipt_header = "Welcome to " . $shop_name;
        $receipt_footer = "Thank you for shopping! • Exchange within 7 days";

        $stmt = $pdo->prepare("INSERT INTO `stores` (shop_id, shop_name, owner_name, phone, city, business_type, plan_type, monthly_fee, expiry_date, support_pin, referred_by_agent, features_json, receipt_header, receipt_footer, is_active) 
                               VALUES (?, ?, ?, ?, ?, ?, 'TRIAL', 3000.00, ?, ?, ?, ?, ?, ?, 1)");
        $stmt->execute([$shop_id, $shop_name, $owner_name, $phone, $city, $business_type, $expiry_date, $pin, $referred_by_agent, $features_json, $receipt_header, $receipt_footer]);

        // If referred by an Agent, increment their counter
        if (!empty($referred_by_agent)) {
            $pdo->prepare("UPDATE `agents` SET `total_referred_shops` = `total_referred_shops` + 1 WHERE `agent_code` = ?")->execute([$referred_by_agent]);
        }

        // Save lead record
        $lead_stmt = $pdo->prepare("INSERT INTO `leads` (shop_name, owner_name, phone, city, business_type, referred_by_agent, status) VALUES (?, ?, ?, ?, ?, ?, 'APPROVED')");
        $lead_stmt->execute([$shop_name, $owner_name, $phone, $city, $business_type, $referred_by_agent]);

        $generated_shop = [
            'shop_id' => $shop_id,
            'shop_name' => $shop_name,
            'business_type' => $business_type,
            'pin' => $pin,
            'expiry' => date('d-M-Y', strtotime($expiry_date)),
            'phone' => $phone
        ];
        $success_msg = "🎉 Congratulations! Your 1-Month (30 Days) Free Trial is Ready!";
    }
}

// Fetch Active Sales Agents / Resellers
$active_agents = $pdo->query("SELECT * FROM `agents` WHERE `is_active` = 1 ORDER BY `name` ASC")->fetchAll();
$selected_ref = trim($_GET['ref'] ?? '');

// Fetch App Config
$config = $pdo->query("SELECT * FROM `app_config` WHERE `id` = 1")->fetch() ?: [
    'latest_version_name' => 'v1.0.0',
    'apk_download_url' => 'downloads/possystem.apk',
    'support_phone' => '077 123 4567',
    'support_whatsapp' => '+94771234567'
];

$apk_url = !empty($config['apk_download_url']) ? $config['apk_download_url'] : 'downloads/possystem.apk';
?>
<!DOCTYPE html>
<html lang="en" class="scroll-smooth">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Smart Retail POS System | 1-Month Free Trial • 100% Offline</title>
  <!-- Tailwind CSS CDN -->
  <script src="https://cdn.tailwindcss.com"></script>
  <!-- Google Fonts: Plus Jakarta Sans -->
  <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800;900&display=swap" rel="stylesheet" />
  <!-- FontAwesome Icons -->
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" />

  <script>
    tailwind.config = {
      theme: {
        extend: {
          fontFamily: { sans: ['Plus Jakarta Sans', 'sans-serif'] },
          colors: {
            brand: {
              50: '#F0FDF4',
              100: '#DCFCE7',
              500: '#22C55E',
              600: '#16A34A',
              700: '#15803D',
              800: '#166534',
              900: '#14532D',
              950: '#064E3B'
            }
          }
        }
      }
    }
  </script>
  <style>
    .gradient-hero {
      background: radial-gradient(circle at 80% 20%, #166534 0%, #14532D 40%, #064E3B 100%);
    }
    .feature-card:hover {
      transform: translateY(-4px);
      transition: all 0.2s ease-in-out;
    }
    .mockup-shadow {
      box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5), 0 0 40px rgba(34, 197, 94, 0.2);
    }
  </style>
<body class="bg-slate-50 text-slate-800 antialiased">

  <!-- HEADER / TOP NAV (No Direct APK Download, Directs to Trial) -->
  <header class="bg-white/95 backdrop-blur-md sticky top-0 z-40 border-b border-slate-200 shadow-sm">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-18 py-3.5 flex items-center justify-between">
      <a href="index.php" class="flex items-center gap-3">
        <div class="w-10 h-10 rounded-xl bg-emerald-700 flex items-center justify-center text-white text-xl shadow-md font-bold">
          <i class="fa-solid fa-cash-register"></i>
        </div>
        <div>
          <span class="text-xl font-extrabold tracking-tight text-slate-900">POS Lanka</span>
          <span class="text-[11px] block text-emerald-600 font-bold -mt-1">Smart Retail &amp; POS Systems</span>
        </div>
      </a>

      <nav class="hidden md:flex items-center gap-8 text-sm font-bold text-slate-600">
        <a href="#features" class="hover:text-emerald-700 transition">Features</a>
        <a href="#pricing" class="hover:text-emerald-700 transition">Pricing</a>
        <a href="#trial-section" class="hover:text-emerald-700 transition">1-Month Free Trial</a>
        <a href="#faq" class="hover:text-emerald-700 transition">FAQ</a>
      </nav>

      <div class="flex items-center gap-3">
        <a href="#trial-section" class="px-5 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs sm:text-sm font-extrabold shadow-md transition flex items-center gap-2">
          <i class="fa-solid fa-gift text-amber-300"></i> Get 1-Month Free Trial
        </a>
      </div>
    </div>
  </header>

  <!-- SUCCESS ACTIVATION MODAL (Shows after submitting inquiry form) -->
  <?php if ($generated_shop): 
    $clean_phone = preg_replace('/[^0-9]/', '', $generated_shop['phone'] ?? '');
    if (strpos($clean_phone, '0') === 0) $clean_phone = '94' . substr($clean_phone, 1);
    
    $is_https = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') || (isset($_SERVER['SERVER_PORT']) && $_SERVER['SERVER_PORT'] == 443);
    $protocol = $is_https ? "https://" : "http://";
    $host_url = $protocol . ($_SERVER['HTTP_HOST'] ?? 'localhost');
    $script_dir = rtrim(dirname($_SERVER['SCRIPT_NAME']), '/\\');
    if ($script_dir === '/' || $script_dir === '\\') $script_dir = '';
    
    $user_apk_link = (strpos($apk_url, 'http') === 0) ? $apk_url : ($host_url . $script_dir . '/' . ltrim($apk_url, '/'));
    $user_wa_msg = urlencode("🎉 *My POS Free Trial License - POS Lanka*\n\n"
      . "🏪 *Store:* {$generated_shop['shop_name']}\n"
      . "🔑 *Store ID:* {$generated_shop['shop_id']}\n"
      . "🔢 *Activation PIN:* {$generated_shop['pin']}\n"
      . "📅 *Valid:* {$generated_shop['expiry']} (30 Days Free)\n\n"
      . "📥 *Download App (APK):*\n{$user_apk_link}\n\n"
      . "Install app, open & enter credentials to start billing!");
  ?>
  <div id="successPopup" class="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
    <div class="bg-white rounded-3xl max-w-lg w-full shadow-2xl overflow-hidden animate-in fade-in zoom-in-95 duration-200 border border-slate-200">
      <div class="bg-emerald-800 text-white p-6 text-center space-y-2">
        <div class="w-14 h-14 rounded-2xl bg-emerald-600 mx-auto flex items-center justify-center text-2xl shadow-lg border border-emerald-400/40">
          <i class="fa-solid fa-check text-amber-300"></i>
        </div>
        <h2 class="text-xl font-black"><?= htmlspecialchars($success_msg) ?></h2>
        <p class="text-xs text-emerald-200">Your store license is ready. Download the APK below and enter your credentials to activate!</p>
      </div>

      <div class="p-6 space-y-4">
        <div class="grid grid-cols-2 gap-3 bg-slate-50 p-4 rounded-2xl border border-slate-200 text-center">
          <div class="p-2 bg-white rounded-xl border border-slate-200 shadow-sm">
            <p class="text-[10px] text-slate-500 font-bold uppercase">Shop ID</p>
            <p class="text-xl font-mono font-black text-slate-900 mt-0.5"><?= $generated_shop['shop_id'] ?></p>
          </div>
          <div class="p-2 bg-emerald-50 rounded-xl border border-emerald-300 shadow-sm">
            <p class="text-[10px] text-emerald-800 font-bold uppercase">Activation PIN</p>
            <p class="text-2xl font-mono font-black text-emerald-700 tracking-wider mt-0.5"><?= $generated_shop['pin'] ?></p>
          </div>
        </div>

        <div class="text-center bg-emerald-50 p-2.5 rounded-xl border border-emerald-200 text-xs font-bold text-emerald-900">
          📅 Trial Valid: <?= $generated_shop['expiry'] ?> <span class="text-emerald-700">(30 Days 100% Free Full Demo)</span>
        </div>

        <!-- Step by Step Instructions -->
        <div class="p-3.5 bg-slate-100 rounded-xl text-xs space-y-1.5 text-slate-700">
          <p class="font-bold text-slate-900"><i class="fa-solid fa-circle-play text-emerald-600 mr-1"></i> පියවර 3කින් ඇරඹුම:</p>
          <p class="text-[11px] leading-relaxed">1️⃣ පහත <strong>Download POS App APK</strong> බොත්තමෙන් App එක Phone එකට Download කරගන්න.</p>
          <p class="text-[11px] leading-relaxed">2️⃣ App එක Open කර Store ID (<strong><?= $generated_shop['shop_id'] ?></strong>) හා PIN (<strong><?= $generated_shop['pin'] ?></strong>) ඇතුළත් කරන්න.</p>
          <p class="text-[11px] leading-relaxed">3️⃣ 100% Offline නොමිලේ බිල් කිරීම අරඹන්න!</p>
        </div>

        <div class="space-y-2.5 pt-1">
          <a href="<?= htmlspecialchars($user_apk_link) ?>" download class="w-full py-3.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-black text-sm shadow-lg transition flex items-center justify-center gap-2">
            <i class="fa-brands fa-android text-xl"></i> Download POS App APK (<?= htmlspecialchars($config['latest_version_name']) ?>)
          </a>
          <a href="https://wa.me/<?= $clean_phone ?>?text=<?= $user_wa_msg ?>" target="_blank" class="w-full py-3 rounded-xl bg-emerald-500 hover:bg-emerald-600 text-white font-bold text-xs shadow transition flex items-center justify-center gap-2">
            <i class="fa-brands fa-whatsapp text-base"></i> Send Credentials to My WhatsApp
          </a>
          <button onclick="document.getElementById('successPopup').remove()" class="w-full py-2.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-600 text-xs font-bold transition">
            Close
          </button>
        </div>
      </div>
    </div>
  </div>
  <?php endif; ?>

  <!-- HERO SECTION WITH BEAUTIFUL REALISTIC POS MOCKUP IMAGE -->
  <section class="gradient-hero text-white py-16 sm:py-24 relative overflow-hidden">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10 grid grid-cols-1 lg:grid-cols-12 gap-12 items-center">
      
      <!-- Hero Left Text -->
      <div class="lg:col-span-7 space-y-6 text-center lg:text-left">
        <div class="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-emerald-900/90 border border-emerald-400/40 text-emerald-300 text-xs font-black uppercase tracking-wider shadow">
          <span class="w-2 h-2 rounded-full bg-emerald-400 animate-ping"></span>
          ⚡ 100% Offline-First POS System
        </div>

        <h1 class="text-3xl sm:text-5xl font-black tracking-tight leading-tight">
          Smart Retail POS Software for Modern Sri Lankan Stores
        </h1>

        <p class="text-base sm:text-lg text-emerald-100 font-medium max-w-2xl leading-relaxed">
          Keep billing non-stop during power cuts and internet downtime. Manage Barcode inventory, Customer Credit (ණය පොත), and Daily Profit from any Android phone, tab, or POS machine.
        </p>

        <!-- Bullet Highlights -->
        <div class="grid grid-cols-2 sm:grid-cols-3 gap-3 pt-2 text-xs sm:text-sm font-semibold text-emerald-100">
          <div class="flex items-center gap-2"><i class="fa-solid fa-circle-check text-emerald-400"></i> 100% Offline Billing</div>
          <div class="flex items-center gap-2"><i class="fa-solid fa-circle-check text-emerald-400"></i> Fast Barcode Scanner</div>
          <div class="flex items-center gap-2"><i class="fa-solid fa-circle-check text-emerald-400"></i> Bluetooth Receipt Print</div>
          <div class="flex items-center gap-2"><i class="fa-solid fa-circle-check text-emerald-400"></i> Expiry Date Alerts</div>
          <div class="flex items-center gap-2"><i class="fa-solid fa-circle-check text-emerald-400"></i> Customer Credit Ledger</div>
          <div class="flex items-center gap-2"><i class="fa-solid fa-circle-check text-emerald-400"></i> Daily Profit Reports</div>
        </div>

        <div class="flex flex-wrap items-center justify-center lg:justify-start gap-4 pt-4">
          <a href="#trial-section" class="px-8 py-4 rounded-2xl bg-white text-emerald-950 hover:bg-emerald-50 font-black text-sm sm:text-base shadow-2xl hover:scale-105 transition duration-200 flex items-center gap-2">
            <i class="fa-solid fa-gift text-emerald-600 text-lg"></i> Get 1-Month Free Trial
          </a>
          <a href="#features" class="px-6 py-4 rounded-2xl bg-emerald-900/80 hover:bg-emerald-900 border border-emerald-400/40 text-white font-bold text-sm sm:text-base transition flex items-center gap-2">
            <i class="fa-solid fa-layer-group text-emerald-400"></i> Explore Features
          </a>
        </div>
      </div>

      <!-- Hero Right: High Quality Realistic POS Tablet Mockup -->
      <div class="lg:col-span-5 flex justify-center">
        <div class="relative group">
          <!-- Ambient Neon Glow Behind Image -->
          <div class="absolute -inset-4 bg-emerald-500/20 rounded-3xl blur-2xl group-hover:bg-emerald-500/30 transition duration-500"></div>
          
          <!-- Mockup Container -->
          <div class="relative rounded-3xl overflow-hidden border-2 border-emerald-400/30 mockup-shadow bg-slate-900">
            <img src="assets/pos_hero.jpg" alt="POS Tablet Terminal & Thermal Printer Mockup" 
                 class="w-full max-w-lg h-auto object-cover transform group-hover:scale-[1.02] transition duration-500" />
            
            <!-- Floating Feature Pills on Image -->
            <div class="absolute top-4 left-4 bg-slate-950/80 backdrop-blur-md px-3 py-1.5 rounded-full border border-emerald-400/40 flex items-center gap-2 text-xs font-bold text-white shadow-lg">
              <span class="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
              100% Offline POS
            </div>

            <div class="absolute bottom-4 right-4 bg-slate-950/80 backdrop-blur-md px-3 py-1.5 rounded-full border border-emerald-400/40 flex items-center gap-2 text-xs font-bold text-emerald-300 shadow-lg">
              <i class="fa-solid fa-print"></i> Thermal Print Ready
            </div>
          </div>
        </div>
      </div>

    </div>
  </section>

  <!-- KEY FEATURES SECTION -->
  <section id="features" class="py-20 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
    <div class="text-center max-w-3xl mx-auto mb-16 space-y-3">
      <span class="px-3 py-1 rounded-full bg-emerald-100 text-emerald-800 text-xs font-bold uppercase tracking-wider">Features &amp; Capabilities</span>
      <h2 class="text-3xl font-extrabold text-slate-900 sm:text-4xl">Everything your store needs to sell, manage and grow</h2>
      <p class="text-base text-slate-600">Built specifically for Sri Lankan supermarkets, grocery shops, pharmacies, hardware, and retail counters.</p>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
      <!-- 1. Offline Billing -->
      <div class="feature-card bg-white p-8 rounded-3xl border border-slate-200 shadow-sm space-y-4">
        <div class="w-12 h-12 rounded-2xl bg-emerald-100 text-emerald-700 flex items-center justify-center text-xl">
          <i class="fa-solid fa-wifi-slash"></i>
        </div>
        <h3 class="text-xl font-bold text-slate-900">100% Offline-First Billing</h3>
        <p class="text-sm text-slate-600 leading-relaxed">
          No internet needed at the counter. Even with power cuts or weak mobile signals, bill items instantly without any lag or downtime.
        </p>
      </div>

      <!-- 2. Barcode Scanning -->
      <div class="feature-card bg-white p-8 rounded-3xl border border-slate-200 shadow-sm space-y-4">
        <div class="w-12 h-12 rounded-2xl bg-blue-100 text-blue-700 flex items-center justify-center text-xl">
          <i class="fa-solid fa-barcode"></i>
        </div>
        <h3 class="text-xl font-bold text-slate-900">Fast Camera &amp; Laser Scanning</h3>
        <p class="text-sm text-slate-600 leading-relaxed">
          Scan barcodes using your phone's camera (ZXing Embedded) or connect external USB / Bluetooth Barcode Scanners for blazing speed.
        </p>
      </div>

      <!-- 3. Customer Credit / ණය පොත -->
      <div class="feature-card bg-white p-8 rounded-3xl border border-slate-200 shadow-sm space-y-4">
        <div class="w-12 h-12 rounded-2xl bg-amber-100 text-amber-700 flex items-center justify-center text-xl">
          <i class="fa-solid fa-book-bookmark"></i>
        </div>
        <h3 class="text-xl font-bold text-slate-900">Customer Credit Ledger (ණය පොත)</h3>
        <p class="text-sm text-slate-600 leading-relaxed">
          Keep track of customers buying on credit. Record settlements, partial cash payments, and view outstanding credit balances anytime.
        </p>
      </div>

      <!-- 4. Stock & Expiry Tracking -->
      <div class="feature-card bg-white p-8 rounded-3xl border border-slate-200 shadow-sm space-y-4">
        <div class="w-12 h-12 rounded-2xl bg-red-100 text-red-700 flex items-center justify-center text-xl">
          <i class="fa-solid fa-triangle-exclamation"></i>
        </div>
        <h3 class="text-xl font-bold text-slate-900">Low Stock &amp; Expiry Alerts</h3>
        <p class="text-sm text-slate-600 leading-relaxed">
          Get notified automatically before items go out of stock or expire. Save thousands of rupees by preventing damaged stock waste.
        </p>
      </div>

      <!-- 5. Bluetooth Receipt Print -->
      <div class="feature-card bg-white p-8 rounded-3xl border border-slate-200 shadow-sm space-y-4">
        <div class="w-12 h-12 rounded-2xl bg-purple-100 text-purple-700 flex items-center justify-center text-xl">
          <i class="fa-solid fa-print"></i>
        </div>
        <h3 class="text-xl font-bold text-slate-900">Thermal Printer Integration</h3>
        <p class="text-sm text-slate-600 leading-relaxed">
          Print professional 58mm or 80mm receipts via Bluetooth or USB POS printers, with your shop logo, contact, and customizable footer notes.
        </p>
      </div>

      <!-- 6. Real-time Profit Reports -->
      <div class="feature-card bg-white p-8 rounded-3xl border border-slate-200 shadow-sm space-y-4">
        <div class="w-12 h-12 rounded-2xl bg-emerald-100 text-emerald-700 flex items-center justify-center text-xl">
          <i class="fa-solid fa-chart-line"></i>
        </div>
        <h3 class="text-xl font-bold text-slate-900">Daily Sales &amp; Profit Analytics</h3>
        <p class="text-sm text-slate-600 leading-relaxed">
          Know exactly how much profit your store made today, cash in drawer, total discounts, and top-selling products in 1 tap.
        </p>
      </div>
    </div>
  </section>

  <!-- DEDICATED FREE TRIAL INQUIRY SECTION (1 Month / 30 Days Free) -->
  <section id="trial-section" class="py-20 bg-emerald-950 text-white relative overflow-hidden">
    <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
      <div class="text-center mb-10 space-y-3">
        <span class="px-3.5 py-1 rounded-full bg-emerald-800 text-emerald-300 text-xs font-black uppercase tracking-wider">Instant Setup</span>
        <h2 class="text-3xl sm:text-4xl font-black text-white">Start Your 1-Month (30 Days) Free Trial</h2>
        <p class="text-sm text-emerald-200 max-w-xl mx-auto">Fill in your shop details below. You will immediately receive your Store ID and Activation PIN to download the app and start billing!</p>
      </div>

      <div class="bg-white rounded-3xl p-6 sm:p-10 text-slate-800 shadow-2xl">
        <form action="index.php#trial-section" method="POST" class="space-y-4">
          <input type="hidden" name="action" value="request_trial" />

          <div>
            <label class="block text-xs font-bold text-slate-700 mb-1">Store / Shop Name (කඩේ නම) *</label>
            <input type="text" name="shop_name" required placeholder="e.g. Saman Super Mart" 
              class="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 font-semibold" />
          </div>

          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label class="block text-xs font-bold text-slate-700 mb-1">Your Name *</label>
              <input type="text" name="owner_name" required placeholder="Saman Kumara" 
                class="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500" />
            </div>
            <div>
              <label class="block text-xs font-bold text-slate-700 mb-1">Phone / WhatsApp *</label>
              <input type="text" name="phone" required placeholder="0771234567" 
                class="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 font-mono" />
            </div>
          </div>

          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label class="block text-xs font-bold text-slate-700 mb-1">City / Town</label>
              <input type="text" name="city" placeholder="Galle / Colombo" 
                class="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500" />
            </div>
            <div>
              <label class="block text-xs font-bold text-slate-700 mb-1">Business / Store Category *</label>
              <select name="business_type" class="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 font-semibold text-slate-800">
                <option value="RETAIL">🛒 Retail &amp; Grocery / Supermarket</option>
                <option value="PHARMACY">💊 Pharmacy &amp; Healthcare</option>
                <option value="RESTAURANT">🍽️ Restaurant, Cafe &amp; Bakery</option>
                <option value="FASHION">👗 Fashion, Clothing &amp; Boutique</option>
                <option value="HARDWARE">🔨 Hardware, Electrical &amp; Paint</option>
              </select>
            </div>
          </div>

          <div>
            <label class="block text-xs font-bold text-slate-700 mb-1">
              <i class="fa-solid fa-user-tag text-emerald-600 mr-1"></i> Referral / Sales Agent (නියෝජිතයා)
            </label>
            <select name="referred_by_agent" class="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 font-semibold text-slate-800">
              <option value="">🚫 No Reseller / Direct Customer (කිසිදු නියෝජිතයෙකු නැත - Direct)</option>
              <?php foreach ($active_agents as $ag): ?>
              <option value="<?= htmlspecialchars($ag['agent_code']) ?>" <?= ($selected_ref === $ag['agent_code']) ? 'selected' : '' ?>>
                <?= htmlspecialchars($ag['agent_code']) ?> - <?= htmlspecialchars($ag['name']) ?> (<?= htmlspecialchars($ag['city']) ?>)
              </option>
              <?php endforeach; ?>
            </select>
          </div>

          <button type="submit" class="w-full py-4 bg-emerald-600 hover:bg-emerald-700 text-white font-black rounded-xl text-sm sm:text-base shadow-xl transition flex items-center justify-center gap-2 mt-4 hover:scale-[1.01] duration-150">
            <i class="fa-solid fa-gift text-amber-300"></i> Activate 1-Month Free Trial &amp; Unlock Download
          </button>
        </form>

        <p class="text-center text-xs text-slate-400 mt-4">
          <i class="fa-solid fa-shield-halved text-emerald-600 mr-1"></i> 100% Free for 30 Days. Full features unlocked. No credit card needed.
        </p>
      </div>
    </div>
  </section>

  <!-- PRICING PACKAGES -->
  <section id="pricing" class="py-20 bg-slate-100 border-t border-slate-200">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div class="text-center max-w-3xl mx-auto mb-16 space-y-3">
        <span class="px-3 py-1 rounded-full bg-emerald-100 text-emerald-800 text-xs font-bold uppercase tracking-wider">Transparent Pricing</span>
        <h2 class="text-3xl font-extrabold text-slate-900 sm:text-4xl">Simple, affordable monthly subscription</h2>
        <p class="text-base text-slate-600">No hidden setup fees • Cancel or renew anytime</p>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-8 max-w-5xl mx-auto">
        <!-- 1. 1-Month Free Trial -->
        <div class="bg-white p-8 rounded-3xl border border-slate-200 shadow-sm flex flex-col justify-between">
          <div class="space-y-4">
            <span class="inline-block px-3 py-1 rounded-full bg-amber-100 text-amber-800 text-xs font-bold uppercase">Special Offer</span>
            <h3 class="text-2xl font-extrabold text-slate-900">1-Month Free Trial</h3>
            <p class="text-xs text-slate-500">Test all features in your store completely free for 30 days.</p>
            <div class="text-3xl font-black text-slate-900">LKR 0 <span class="text-xs text-slate-400 font-normal">/ 30 Days</span></div>
            
            <ul class="space-y-2.5 text-xs text-slate-600 pt-4 border-t border-slate-100">
              <li class="flex items-center gap-2"><i class="fa-solid fa-check text-emerald-600"></i> Full POS Billing Access</li>
              <li class="flex items-center gap-2"><i class="fa-solid fa-check text-emerald-600"></i> Unlimited Products &amp; Sales</li>
              <li class="flex items-center gap-2"><i class="fa-solid fa-check text-emerald-600"></i> Inventory &amp; Expiry Alerts</li>
              <li class="flex items-center gap-2"><i class="fa-solid fa-check text-emerald-600"></i> 100% Offline Capability</li>
            </ul>
          </div>

          <a href="#trial-section" class="mt-8 w-full py-3 rounded-xl bg-slate-900 text-white font-bold text-xs text-center hover:bg-slate-800 transition block">
            Start 1-Month Trial
          </a>
        </div>

        <!-- 2. Standard Monthly (Highlighted) -->
        <div class="bg-white p-8 rounded-3xl border-2 border-emerald-600 shadow-xl flex flex-col justify-between relative">
          <div class="absolute -top-3.5 left-1/2 -translate-x-1/2 px-3 py-1 rounded-full bg-emerald-600 text-white text-xs font-extrabold uppercase shadow">
            Most Popular
          </div>
          <div class="space-y-4">
            <span class="inline-block px-3 py-1 rounded-full bg-emerald-100 text-emerald-800 text-xs font-bold uppercase">Monthly Plan</span>
            <h3 class="text-2xl font-extrabold text-slate-900">Standard Monthly</h3>
            <p class="text-xs text-slate-500">Perfect for single retail shops &amp; grocery stores.</p>
            <div class="text-3xl font-black text-emerald-600">LKR 3,000 <span class="text-xs text-slate-400 font-normal">/ Month</span></div>
            
            <ul class="space-y-2.5 text-xs text-slate-600 pt-4 border-t border-slate-100">
              <li class="flex items-center gap-2"><i class="fa-solid fa-check text-emerald-600"></i> Everything in Free Trial</li>
              <li class="flex items-center gap-2"><i class="fa-solid fa-check text-emerald-600"></i> Free In-App Feature Updates</li>
              <li class="flex items-center gap-2"><i class="fa-solid fa-check text-emerald-600"></i> Phone &amp; WhatsApp Support</li>
              <li class="flex items-center gap-2"><i class="fa-solid fa-check text-emerald-600"></i> Customer Credit Book (ණය පොත)</li>
              <li class="flex items-center gap-2"><i class="fa-solid fa-check text-emerald-600"></i> Instant Offline PIN Renewal</li>
            </ul>
          </div>

          <a href="#trial-section" class="mt-8 w-full py-3.5 rounded-xl bg-emerald-600 text-white font-extrabold text-xs text-center hover:bg-emerald-700 shadow transition block">
            Choose Monthly Plan
          </a>
        </div>

        <!-- 3. Annual Pro -->
        <div class="bg-white p-8 rounded-3xl border border-slate-200 shadow-sm flex flex-col justify-between">
          <div class="space-y-4">
            <span class="inline-block px-3 py-1 rounded-full bg-blue-100 text-blue-800 text-xs font-bold uppercase">Yearly Saver</span>
            <h3 class="text-2xl font-extrabold text-slate-900">Annual Pro</h3>
            <p class="text-xs text-slate-500">Save LKR 6,000 with 1-year prepaid subscription.</p>
            <div class="text-3xl font-black text-slate-900">LKR 30,000 <span class="text-xs text-slate-400 font-normal">/ Year</span></div>
            
            <ul class="space-y-2.5 text-xs text-slate-600 pt-4 border-t border-slate-100">
              <li class="flex items-center gap-2"><i class="fa-solid fa-check text-emerald-600"></i> 2 Months Free (Save LKR 6,000)</li>
              <li class="flex items-center gap-2"><i class="fa-solid fa-check text-emerald-600"></i> Priority 24/7 Phone Support</li>
              <li class="flex items-center gap-2"><i class="fa-solid fa-check text-emerald-600"></i> Custom Thermal Receipt Branding</li>
              <li class="flex items-center gap-2"><i class="fa-solid fa-check text-emerald-600"></i> 1 Year Non-stop Guarantee</li>
            </ul>
          </div>

          <a href="#trial-section" class="mt-8 w-full py-3 rounded-xl bg-slate-900 text-white font-bold text-xs text-center hover:bg-slate-800 transition block">
            Choose Annual Plan
          </a>
        </div>
      </div>
    </div>
  </section>

  <!-- FAQ SECTION -->
  <section id="faq" class="py-20 max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
    <div class="text-center mb-12">
      <span class="px-3 py-1 rounded-full bg-slate-200 text-slate-700 text-xs font-bold uppercase">Frequently Asked Questions</span>
      <h2 class="text-3xl font-extrabold text-slate-900 mt-2">Questions &amp; Answers</h2>
    </div>

    <div class="space-y-4">
      <div class="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
        <h4 class="text-base font-bold text-slate-900">Do I need an internet connection to bill customers?</h4>
        <p class="text-sm text-slate-600 mt-1.5">No! The app operates 100% offline. All barcode scanning, cart calculations, receipts, and customer credit are stored locally on your device.</p>
      </div>

      <div class="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
        <h4 class="text-base font-bold text-slate-900">What hardware do I need to run this?</h4>
        <p class="text-sm text-slate-600 mt-1.5">Any standard Android phone, tab, or all-in-one POS device running Android 7.0 or higher. You can also connect any standard Bluetooth or USB Thermal Receipt Printer.</p>
      </div>

      <div class="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
        <h4 class="text-base font-bold text-slate-900">How do I renew my monthly subscription?</h4>
        <p class="text-sm text-slate-600 mt-1.5">When your monthly subscription ends, simply transfer the monthly fee to our bank account and send the slip via WhatsApp. We will give you a 6-digit PIN to instantly unlock your app without requiring internet!</p>
      </div>
    </div>
  </section>

  <!-- FOOTER -->
  <footer class="bg-slate-900 text-slate-400 py-12 border-t border-slate-800">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col md:flex-row items-center justify-between gap-6">
      <div class="flex items-center gap-3">
        <div class="w-8 h-8 rounded-lg bg-emerald-600 flex items-center justify-center text-white text-base font-bold">
          <i class="fa-solid fa-cash-register"></i>
        </div>
        <span class="text-base font-bold text-white">POS Lanka Software Solutions</span>
      </div>

      <p class="text-xs text-slate-500 text-center">
        &copy; <?= date('Y') ?> POS Lanka SaaS Systems. All Rights Reserved. Built with ❤️ for Sri Lankan Retailers.
      </p>

      <div class="flex items-center gap-4 text-xs font-semibold">
        <a href="#features" class="hover:text-white transition">Features</a>
        <a href="#pricing" class="hover:text-white transition">Pricing</a>
        <a href="https://wa.me/<?= preg_replace('/[^0-9]/', '', $config['support_whatsapp']) ?>" target="_blank" class="text-emerald-400 hover:text-emerald-300 transition flex items-center gap-1">
          <i class="fa-brands fa-whatsapp text-sm"></i> WhatsApp Support
        </a>
      </div>
    </div>
  </footer>

  <!-- FLOATING WHATSAPP BUTTON -->
  <a href="https://wa.me/<?= preg_replace('/[^0-9]/', '', $config['support_whatsapp']) ?>?text=Hello%20POS%20Lanka,%20I%20am%20interested%20in%20the%20POS%20Software" target="_blank" 
     class="fixed bottom-6 right-6 z-50 w-14 h-14 rounded-full bg-emerald-500 hover:bg-emerald-600 text-white flex items-center justify-center text-3xl shadow-2xl hover:scale-110 transition duration-200">
    <i class="fa-brands fa-whatsapp"></i>
  </a>

</body>
</html>
