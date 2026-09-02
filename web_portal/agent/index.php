<?php
session_start();
require_once '../db.php';

if (!isset($_SESSION['agent_code'])) {
    header('Location: login.php');
    exit;
}

$agent_code = $_SESSION['agent_code'];

// Fetch Agent Profile
$stmt = $pdo->prepare("SELECT * FROM `agents` WHERE `agent_code` = ?");
$stmt->execute([$agent_code]);
$agent = $stmt->fetch();

if (!$agent) {
    session_destroy();
    header('Location: login.php');
    exit;
}

// Fetch stores referred by this agent
$stores_stmt = $pdo->prepare("SELECT * FROM `stores` WHERE `referred_by_agent` = ? ORDER BY `id` DESC");
$stores_stmt->execute([$agent_code]);
$my_stores = $stores_stmt->fetchAll();

// Fetch commissions earned by this agent
$comm_stmt = $pdo->prepare("SELECT * FROM `commissions` WHERE `agent_code` = ? ORDER BY `id` DESC");
$comm_stmt->execute([$agent_code]);
$my_commissions = $comm_stmt->fetchAll();

// Logout
if (isset($_GET['logout'])) {
    session_destroy();
    header('Location: login.php');
    exit;
}
?>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Agent Dashboard | <?= htmlspecialchars($agent['name']) ?></title>
  <script src="https://cdn.tailwindcss.com"></script>
  <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet" />
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" />
  <script>
    tailwind.config = { theme: { extend: { fontFamily: { sans: ['Plus Jakarta Sans', 'sans-serif'] } } } }
  </script>
</head>
<body class="bg-slate-50 text-slate-800 min-h-screen flex flex-col">

  <!-- TOP NAV -->
  <header class="bg-emerald-950 text-white sticky top-0 z-30 shadow-md">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
      <div class="flex items-center gap-3">
        <div class="w-9 h-9 rounded-xl bg-emerald-500 flex items-center justify-center text-slate-950 font-bold text-base">
          <i class="fa-solid fa-id-badge"></i>
        </div>
        <div>
          <h1 class="text-sm font-bold"><?= htmlspecialchars($agent['name']) ?></h1>
          <p class="text-[10px] text-emerald-300 font-mono font-bold">Agent ID: <?= $agent['agent_code'] ?> • <?= $agent['city'] ?></p>
        </div>
      </div>

      <div class="flex items-center gap-3">
        <a href="index.php?logout=1" class="px-3 py-1.5 rounded-lg bg-emerald-900 hover:bg-emerald-800 text-xs font-bold transition flex items-center gap-1.5">
          <i class="fa-solid fa-right-from-bracket"></i> Logout
        </a>
      </div>
    </div>
  </header>

  <!-- MAIN CONTAINER -->
  <main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 flex-1 w-full space-y-6">

    <!-- REFERRAL BANNER -->
    <div class="bg-gradient-to-r from-emerald-800 to-emerald-950 text-white p-6 rounded-3xl shadow-lg flex flex-col md:flex-row items-center justify-between gap-6">
      <div class="space-y-1 text-center md:text-left">
        <span class="px-2.5 py-0.5 rounded-full bg-emerald-600 text-white text-[10px] font-bold uppercase tracking-wider">Your Official Referral Code</span>
        <h2 class="text-2xl font-black font-mono tracking-wider text-amber-300"><?= $agent['agent_code'] ?></h2>
        <p class="text-xs text-emerald-200">Give this code to stores when activating the POS App to earn <?= $agent['commission_percent'] ?>% recurring monthly commission.</p>
      </div>

      <div class="bg-emerald-900/60 p-3 rounded-2xl border border-emerald-700/60 flex items-center gap-3 text-xs">
        <div>
          <p class="text-[10px] text-emerald-300 font-semibold uppercase">Commission Rate</p>
          <p class="text-lg font-black text-white"><?= $agent['commission_percent'] ?>% Monthly</p>
        </div>
      </div>
    </div>

    <!-- KPI STATS -->
    <div class="grid grid-cols-2 lg:grid-cols-4 gap-4">
      <div class="bg-white p-4 rounded-2xl shadow-sm border border-slate-200">
        <p class="text-xs font-semibold text-slate-500 uppercase">My Onboarded Stores</p>
        <p class="text-2xl font-black text-slate-900 mt-1"><?= count($my_stores) ?></p>
      </div>

      <div class="bg-white p-4 rounded-2xl shadow-sm border border-slate-200 border-l-4 border-l-emerald-600">
        <p class="text-xs font-semibold text-slate-500 uppercase">Total Commission Earned</p>
        <p class="text-2xl font-black text-emerald-600 mt-1">LKR <?= number_format($agent['total_earned']) ?></p>
      </div>

      <div class="bg-white p-4 rounded-2xl shadow-sm border border-slate-200 border-l-4 border-l-amber-500">
        <p class="text-xs font-semibold text-slate-500 uppercase">Pending Payout Balance</p>
        <p class="text-2xl font-black text-amber-600 mt-1">LKR <?= number_format($agent['balance_payable']) ?></p>
      </div>

      <div class="bg-white p-4 rounded-2xl shadow-sm border border-slate-200">
        <p class="text-xs font-semibold text-slate-500 uppercase">My Bank Details</p>
        <p class="text-xs font-bold text-slate-700 mt-1 truncate"><?= htmlspecialchars($agent['bank_details'] ?: 'Contact Admin to add Bank') ?></p>
      </div>
    </div>

    <!-- MY REFERRED STORES TABLE -->
    <div class="bg-white rounded-3xl shadow-sm border border-slate-200 overflow-hidden">
      <div class="p-5 border-b border-slate-200">
        <h3 class="text-sm font-extrabold text-slate-900">Stores Activated with Your Code (<?= count($my_stores) ?>)</h3>
        <p class="text-xs text-slate-500">Your client stores and subscription statuses</p>
      </div>

      <div class="overflow-x-auto">
        <table class="w-full text-left text-xs">
          <thead class="bg-slate-50 text-slate-500 uppercase font-bold border-b border-slate-200">
            <tr>
              <th class="px-6 py-4">Store Name &amp; ID</th>
              <th class="px-6 py-4">Owner &amp; Phone</th>
              <th class="px-6 py-4">Plan</th>
              <th class="px-6 py-4">Monthly Fee</th>
              <th class="px-6 py-4">Your Share (<?= $agent['commission_percent'] ?>%)</th>
              <th class="px-6 py-4">Status</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-200">
            <?php foreach ($my_stores as $s): ?>
            <tr class="hover:bg-slate-50 transition">
              <td class="px-6 py-4">
                <p class="font-extrabold text-slate-900"><?= htmlspecialchars($s['shop_name']) ?></p>
                <p class="text-[11px] text-slate-400 font-mono"><?= htmlspecialchars($s['shop_id']) ?> • <?= htmlspecialchars($s['city']) ?></p>
              </td>
              <td class="px-6 py-4">
                <p class="font-bold text-slate-900"><?= htmlspecialchars($s['owner_name']) ?></p>
                <p class="text-[11px] text-slate-500"><?= htmlspecialchars($s['phone']) ?></p>
              </td>
              <td class="px-6 py-4">
                <span class="px-2 py-0.5 rounded-full text-[10px] font-bold <?= $s['plan_type'] === 'PAID' ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800' ?>">
                  <?= $s['plan_type'] ?>
                </span>
              </td>
              <td class="px-6 py-4 font-semibold">LKR <?= number_format($s['monthly_fee']) ?></td>
              <td class="px-6 py-4 font-black text-emerald-700 text-sm">
                LKR <?= number_format($s['monthly_fee'] * ($agent['commission_percent'] / 100)) ?>/mo
              </td>
              <td class="px-6 py-4">
                <span class="px-2 py-0.5 rounded-full text-[10px] font-bold <?= $s['is_active'] ? 'bg-emerald-100 text-emerald-800' : 'bg-red-100 text-red-800' ?>">
                  <?= $s['is_active'] ? 'Active' : 'Inactive' ?>
                </span>
              </td>
            </tr>
            <?php endforeach; ?>
            <?php if (count($my_stores) === 0): ?>
            <tr><td colspan="6" class="p-8 text-center text-slate-400 font-semibold">No stores registered with your code yet. Start sharing your code!</td></tr>
            <?php endif; ?>
          </tbody>
        </table>
      </div>
    </div>

  </main>
</body>
</html>
