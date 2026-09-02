<?php
session_start();
require_once '../db.php';

$error = null;

if (isset($_SESSION['agent_code'])) {
    header('Location: index.php');
    exit;
}

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $code = strtoupper(trim($_POST['agent_code'] ?? ''));
    $phone = trim($_POST['phone'] ?? '');

    $stmt = $pdo->prepare("SELECT * FROM `agents` WHERE `agent_code` = ? AND (`phone` = ? OR `phone` LIKE ?)");
    $stmt->execute([$code, $phone, "%$phone%"]);
    $agent = $stmt->fetch();

    if ($agent) {
        $_SESSION['agent_code'] = $agent['agent_code'];
        $_SESSION['agent_name'] = $agent['name'];
        header('Location: index.php');
        exit;
    } else {
        $error = 'Invalid Agent Code or Phone Number. (Demo: AGT-701 / 0778899001)';
    }
}
?>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Sales Agent Portal Login | POS Master SaaS</title>
  <script src="https://cdn.tailwindcss.com"></script>
  <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet" />
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" />
  <style>body { font-family: 'Plus Jakarta Sans', sans-serif; background-color: #042F2E; }</style>
</head>
<body class="min-h-screen flex items-center justify-center p-4">

  <div class="max-w-md w-full bg-emerald-950 border border-emerald-800 rounded-3xl p-8 shadow-2xl space-y-6 text-white">
    <div class="text-center space-y-2">
      <div class="w-14 h-14 rounded-2xl bg-emerald-600 mx-auto flex items-center justify-center text-2xl shadow-lg">
        <i class="fa-solid fa-users-gear"></i>
      </div>
      <h1 class="text-2xl font-extrabold tracking-tight">Sales Agent Portal</h1>
      <p class="text-xs text-emerald-300">Track your referred client stores &amp; monthly commissions</p>
    </div>

    <?php if ($error): ?>
    <div class="p-3 bg-red-500/20 border border-red-500/40 rounded-xl text-red-300 text-xs font-semibold text-center">
      <i class="fa-solid fa-circle-exclamation mr-1"></i> <?= htmlspecialchars($error) ?>
    </div>
    <?php endif; ?>

    <form method="POST" class="space-y-4">
      <div>
        <label class="block text-xs font-bold text-emerald-200 mb-1.5 uppercase">Your Agent Code</label>
        <div class="relative">
          <i class="fa-solid fa-id-badge absolute left-3.5 top-3 text-emerald-400 text-sm"></i>
          <input type="text" name="agent_code" required value="AGT-701" placeholder="e.g. AGT-701" 
            class="w-full pl-10 pr-4 py-2.5 bg-emerald-900/60 border border-emerald-700 rounded-xl text-sm text-white focus:outline-none focus:ring-2 focus:ring-emerald-400 font-mono font-bold" />
        </div>
      </div>

      <div>
        <label class="block text-xs font-bold text-emerald-200 mb-1.5 uppercase">Registered Phone Number</label>
        <div class="relative">
          <i class="fa-solid fa-phone absolute left-3.5 top-3 text-emerald-400 text-sm"></i>
          <input type="text" name="phone" required value="0778899001" placeholder="e.g. 0778899001" 
            class="w-full pl-10 pr-4 py-2.5 bg-emerald-900/60 border border-emerald-700 rounded-xl text-sm text-white focus:outline-none focus:ring-2 focus:ring-emerald-400" />
        </div>
      </div>

      <button type="submit" class="w-full py-3 bg-emerald-500 hover:bg-emerald-600 text-slate-950 font-black rounded-xl text-sm shadow-lg transition">
        Sign In to Agent Portal →
      </button>
    </form>

    <div class="pt-4 border-t border-emerald-900 text-center">
      <a href="../index.php" class="text-xs text-emerald-400 hover:text-white transition flex items-center justify-center gap-1.5">
        <i class="fa-solid fa-arrow-left"></i> Back to Public Website
      </a>
    </div>
  </div>

</body>
</html>
