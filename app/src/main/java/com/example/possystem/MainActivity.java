package com.example.possystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.example.possystem.helper.LicenseManager;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private BottomNavigationView bottomNavigation;
    private TextView tvShopTitle, tvShopSubtitle, tvLicenseStatusBadge;
    private View btnLicenseStatusBadge;
    private TextView tvLabelPOS, tvLabelProducts, tvLabelPurchases, tvLabelCredit, tvLabelExpenses, tvLabelReports;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_drawer);

        drawerLayout = findViewById(R.id.drawerLayout);
        NavigationView navigationView = findViewById(R.id.navigationView);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        ImageView btnMenuDrawer = findViewById(R.id.btnMenuDrawer);
        tvShopTitle = findViewById(R.id.tvShopTitle);
        tvShopSubtitle = findViewById(R.id.tvShopSubtitle);
        tvLicenseStatusBadge = findViewById(R.id.tvLicenseStatusBadge);
        btnLicenseStatusBadge = findViewById(R.id.btnLicenseStatusBadge);

        tvLabelPOS = findViewById(R.id.tvLabelPOS);
        tvLabelProducts = findViewById(R.id.tvLabelProducts);
        tvLabelPurchases = findViewById(R.id.tvLabelPurchases);
        tvLabelCredit = findViewById(R.id.tvLabelCredit);
        tvLabelExpenses = findViewById(R.id.tvLabelExpenses);
        tvLabelReports = findViewById(R.id.tvLabelReports);

        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);
        }

        // Setup Hamburger Menu Drawer Button Click Handler
        if (btnMenuDrawer != null) {
            btnMenuDrawer.setOnClickListener(v -> {
                if (drawerLayout != null) {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }

        // Setup Bottom Navigation Bar Item Click Listener
        setupBottomNavigation();

        // Setup Dashboard Quick Module Cards
        setupDashboardCards();

        // Setup License & Subscription Status
        setupLicenseStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if license is valid locally
        if (!LicenseManager.isLicenseValid(this)) {
            Intent intent = new Intent(MainActivity.this, SubscriptionLockedActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // 🌐 Real-time check with server in background (Instantly detects if Admin marked store as Inactive)
        LicenseManager.syncLicenseWithServer(this, (isBlocked, message) -> {
            if (isBlocked && !isFinishing()) {
                Toast.makeText(MainActivity.this, "🚫 Store access disabled by Administrator.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(MainActivity.this, SubscriptionLockedActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            } else {
                setupLicenseStatus();
                setupDashboardCards();
            }
        });

        setupLicenseStatus();
        setupDashboardCards();
    }

    private void setupLicenseStatus() {
        if (tvShopTitle != null) {
            tvShopTitle.setText(LicenseManager.getShopName(this));
        }

        String btype = LicenseManager.getBusinessType(this);
        if (tvShopSubtitle != null) {
            switch (btype) {
                case "PHARMACY":
                    tvShopSubtitle.setText("💊 Pharmacy & Medi-Care Mode • Rx Counter");
                    break;
                case "RESTAURANT":
                    tvShopSubtitle.setText("🍽️ Restaurant & Dine-In Mode • POS Terminal");
                    break;
                case "FASHION":
                    tvShopSubtitle.setText("👗 Fashion & Boutique Mode • Cashier 01");
                    break;
                case "HARDWARE":
                    tvShopSubtitle.setText("🔨 Hardware & Electrical Mode • POS Terminal");
                    break;
                case "RETAIL":
                default:
                    tvShopSubtitle.setText("🛒 Retail & Supermarket Mode • Main Counter");
                    break;
            }
        }

        if (tvLicenseStatusBadge != null) {
            long daysLeft = LicenseManager.getDaysRemaining(this);
            boolean isTrial = LicenseManager.isTrial(this);
            if (isTrial) {
                tvLicenseStatusBadge.setText("🟢 Trial: " + daysLeft + " Days");
            } else {
                tvLicenseStatusBadge.setText("✅ Paid: " + daysLeft + " Days");
            }
        }

        if (btnLicenseStatusBadge != null) {
            btnLicenseStatusBadge.setOnClickListener(v -> showLicenseDetailsDialog());
        }
    }

    private void showLicenseDetailsDialog() {
        String shopName = LicenseManager.getShopName(this);
        String shopId = LicenseManager.getShopId(this);
        String planType = LicenseManager.getPlanType(this);
        String btype = LicenseManager.getBusinessType(this);
        String expiry = LicenseManager.getFormattedExpiryDate(this);
        long daysLeft = LicenseManager.getDaysRemaining(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Store License & Subscription");
        builder.setMessage("🏪 Shop Name: " + shopName +
                "\n🔑 Shop ID: " + shopId +
                "\n🏢 Industry: " + btype +
                "\n📦 Plan: " + planType +
                "\n⏳ Remaining: " + daysLeft + " Days" +
                "\n📅 Expiry Date: " + expiry +
                "\n\n📞 Admin Support: " + LicenseManager.SUPPORT_PHONE);

        builder.setPositiveButton("Enter Renewal PIN", (dialog, which) -> showEnterPinDialog());
        builder.setNegativeButton("Close", null);
        builder.show();
    }

    private void showEnterPinDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter 6-Digit Renewal PIN");

        final EditText input = new EditText(this);
        input.setHint("e.g. 849201");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setPadding(40, 30, 40, 30);
        builder.setView(input);

        builder.setPositiveButton("Apply PIN", (dialog, which) -> {
            String pin = input.getText().toString().trim();
            if (LicenseManager.applyOfflineRenewalPin(MainActivity.this, pin)) {
                Toast.makeText(MainActivity.this, "🎉 License Extended! Expiry: " + LicenseManager.getFormattedExpiryDate(MainActivity.this), Toast.LENGTH_LONG).show();
                setupLicenseStatus();
                setupDashboardCards();
            } else {
                Toast.makeText(MainActivity.this, "❌ Invalid Renewal PIN", Toast.LENGTH_LONG).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void setupBottomNavigation() {
        if (bottomNavigation != null) {
            bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    return true;
                } else if (itemId == R.id.nav_pos) {
                    startActivity(new Intent(MainActivity.this, PosActivity.class));
                    return true;
                } else if (itemId == R.id.nav_inventory) {
                    startActivity(new Intent(MainActivity.this, InventoryActivity.class));
                    return true;
                } else if (itemId == R.id.nav_more) {
                    if (drawerLayout != null) {
                        drawerLayout.openDrawer(GravityCompat.START);
                    }
                    return true;
                }
                return false;
            });
        }
    }

    private void setupDashboardCards() {
        MaterialCardView cardPOS = findViewById(R.id.cardModulePOS);
        MaterialCardView cardProducts = findViewById(R.id.cardModuleProducts);
        MaterialCardView cardPurchases = findViewById(R.id.cardModulePurchases);
        MaterialCardView cardCredit = findViewById(R.id.cardModuleCredit);
        MaterialCardView cardExpenses = findViewById(R.id.cardModuleExpenses);
        MaterialCardView cardReports = findViewById(R.id.cardModuleReports);

        String btype = LicenseManager.getBusinessType(this);

        // Customize Card Titles dynamically based on Industry Profile
        if (tvLabelPOS != null) {
            switch (btype) {
                case "PHARMACY": tvLabelPOS.setText("💊 Rx Dispense"); break;
                case "RESTAURANT": tvLabelPOS.setText("🍽️ Table / KOT"); break;
                case "FASHION": tvLabelPOS.setText("👗 Tag & Size POS"); break;
                case "HARDWARE": tvLabelPOS.setText("🔨 Yard POS"); break;
                default: tvLabelPOS.setText("🛒 Fast POS"); break;
            }
        }

        if (tvLabelProducts != null) {
            switch (btype) {
                case "PHARMACY": tvLabelProducts.setText("💊 Drugs & Batch"); break;
                case "RESTAURANT": tvLabelProducts.setText("🍔 Food Menu"); break;
                case "FASHION": tvLabelProducts.setText("👗 Apparel & Sizes"); break;
                case "HARDWARE": tvLabelProducts.setText("🔨 Tools & Units"); break;
                default: tvLabelProducts.setText("📦 Products"); break;
            }
        }

        if (tvLabelPurchases != null) {
            switch (btype) {
                case "PHARMACY": tvLabelPurchases.setText("📦 Pharma GRN"); break;
                case "RESTAURANT": tvLabelPurchases.setText("🥬 Kitchen Stock"); break;
                case "FASHION": tvLabelPurchases.setText("🚚 Garment GRN"); break;
                case "HARDWARE": tvLabelPurchases.setText("🚚 Bulk Supplies"); break;
                default: tvLabelPurchases.setText("🚚 Purchases"); break;
            }
        }

        if (tvLabelCredit != null) {
            switch (btype) {
                case "PHARMACY": tvLabelCredit.setText("👥 Patient Credit"); break;
                case "RESTAURANT": tvLabelCredit.setText("👥 Account Bills"); break;
                case "FASHION": tvLabelCredit.setText("👥 VIP Credit"); break;
                case "HARDWARE": tvLabelCredit.setText("👥 Mason / Contractor ණය"); break;
                default: tvLabelCredit.setText("👥 Credit / ණය"); break;
            }
        }

        // Modular Feature Visibility Toggles
        if (cardPOS != null) {
            boolean posEnabled = LicenseManager.isFeatureEnabled(this, "pos");
            cardPOS.setVisibility(posEnabled ? View.VISIBLE : View.GONE);
            cardPOS.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, PosActivity.class)));
        }

        if (cardProducts != null) {
            boolean invEnabled = LicenseManager.isFeatureEnabled(this, "inventory");
            cardProducts.setVisibility(invEnabled ? View.VISIBLE : View.GONE);
            cardProducts.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ProductsActivity.class)));
        }

        if (cardPurchases != null) {
            boolean purEnabled = LicenseManager.isFeatureEnabled(this, "purchases");
            cardPurchases.setVisibility(purEnabled ? View.VISIBLE : View.GONE);
            cardPurchases.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, PurchasesActivity.class)));
        }

        if (cardCredit != null) {
            boolean creditEnabled = LicenseManager.isFeatureEnabled(this, "credit");
            cardCredit.setVisibility(creditEnabled ? View.VISIBLE : View.GONE);
            cardCredit.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, CreditActivity.class)));
        }

        if (cardExpenses != null) {
            boolean expEnabled = LicenseManager.isFeatureEnabled(this, "expenses");
            cardExpenses.setVisibility(expEnabled ? View.VISIBLE : View.GONE);
            cardExpenses.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ExpensesActivity.class)));
        }

        if (cardReports != null) {
            boolean repEnabled = LicenseManager.isFeatureEnabled(this, "reports");
            cardReports.setVisibility(repEnabled ? View.VISIBLE : View.GONE);
            cardReports.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ReportsActivity.class)));
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_dashboard) {
            // Already on Dashboard
        } else if (id == R.id.nav_pos) {
            startActivity(new Intent(MainActivity.this, PosActivity.class));
        } else if (id == R.id.nav_products) {
            startActivity(new Intent(MainActivity.this, ProductsActivity.class));
        } else if (id == R.id.nav_inventory) {
            startActivity(new Intent(MainActivity.this, InventoryActivity.class));
        } else if (id == R.id.nav_purchases) {
            startActivity(new Intent(MainActivity.this, PurchasesActivity.class));
        } else if (id == R.id.nav_credit) {
            startActivity(new Intent(MainActivity.this, CreditActivity.class));
        } else if (id == R.id.nav_customers) {
            startActivity(new Intent(MainActivity.this, CustomersActivity.class));
        } else if (id == R.id.nav_expenses) {
            startActivity(new Intent(MainActivity.this, ExpensesActivity.class));
        } else if (id == R.id.nav_reports) {
            startActivity(new Intent(MainActivity.this, ReportsActivity.class));
        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        } else if (id == R.id.nav_logout) {
            Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }

        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}