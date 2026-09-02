<?php
session_start();

$error = null;

if (isset($_SESSION['admin_logged_in']) && $_SESSION['admin_logged_in'] === true) {
    header('Location: index.php');
    exit;
}

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $username = trim($_POST['username'] ?? '');
    $password = trim($_POST['password'] ?? '');

    // Default credentials (admin / admin123)
    if ($username === 'admin' && $password === 'admin123') {
        $_SESSION['admin_logged_in'] = true;
        $_SESSION['admin_user'] = 'Master Admin';
        header('Location: index.php');
        exit;
    } else {
        $error = 'Invalid username or password. Default is: admin / admin123';
    }
}
?>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Admin Login | POS Master Cloud Portal</title>
  <script src="https://cdn.tailwindcss.com"></script>
  <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet" />
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" />
  <style>body { font-family: 'Plus Jakarta Sans', sans-serif; background-color: #0F172A; }</style>
</head>
<body class="min-h-screen flex items-center justify-center p-4">

  <div class="max-w-md w-full bg-slate-900 border border-slate-800 rounded-3xl p-8 shadow-2xl space-y-6 text-white">
    <div class="text-center space-y-2">
      <div class="w-14 h-14 rounded-2xl bg-emerald-600 mx-auto flex items-center justify-center text-2xl shadow-lg shadow-emerald-500/20">
        <i class="fa-solid fa-lock"></i>
      </div>
      <h1 class="text-2xl font-extrabold tracking-tight">Admin Portal Login</h1>
      <p class="text-xs text-slate-400">Enter your credentials to manage POS licenses</p>
    </div>

    <?php if ($error): ?>
    <div class="p-3 bg-red-500/10 border border-red-500/30 rounded-xl text-red-400 text-xs font-semibold text-center">
      <i class="fa-solid fa-circle-exclamation mr-1"></i> <?= htmlspecialchars($error) ?>
    </div>
    <?php endif; ?>

    <form method="POST" class="space-y-4">
      <div>
        <label class="block text-xs font-bold text-slate-300 mb-1.5 uppercase tracking-wider">Username</label>
        <div class="relative">
          <i class="fa-solid fa-user absolute left-3.5 top-3 text-slate-500 text-sm"></i>
          <input type="text" name="username" required value="admin" placeholder="Username" 
            class="w-full pl-10 pr-4 py-2.5 bg-slate-800 border border-slate-700 rounded-xl text-sm text-white focus:outline-none focus:ring-2 focus:ring-emerald-500" />
        </div>
      </div>

      <div>
        <label class="block text-xs font-bold text-slate-300 mb-1.5 uppercase tracking-wider">Password</label>
        <div class="relative">
          <i class="fa-solid fa-key absolute left-3.5 top-3 text-slate-500 text-sm"></i>
          <input type="password" name="password" required value="admin123" placeholder="Password" 
            class="w-full pl-10 pr-4 py-2.5 bg-slate-800 border border-slate-700 rounded-xl text-sm text-white focus:outline-none focus:ring-2 focus:ring-emerald-500" />
        </div>
      </div>

      <button type="submit" class="w-full py-3 bg-emerald-600 hover:bg-emerald-700 text-white font-extrabold rounded-xl text-sm shadow-lg shadow-emerald-600/30 transition duration-150">
        Sign In to Portal →
      </button>
    </form>

    <div class="pt-4 border-t border-slate-800 text-center">
      <a href="../index.php" class="text-xs text-slate-400 hover:text-emerald-400 transition flex items-center justify-center gap-1.5">
        <i class="fa-solid fa-arrow-left"></i> Back to Public Website
      </a>
    </div>
  </div>

</body>
</html>
