package com.example.possystem;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.possystem.adapter.ProductAdapter;
import com.example.possystem.data.AppDatabase;
import com.example.possystem.data.entity.ProductEntity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class InventoryActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private RecyclerView rvInventory;
    private ImageView btnBack;
    private TextView tvStockValue, tvLowStockCount, tvExpiringCount;
    private TabLayout tabLayoutInventory;

    private ProductAdapter adapter;
    private int currentTabPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory_drawer);

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        rvInventory = findViewById(R.id.rvInventoryItems);
        btnBack = findViewById(R.id.btnBackInventory);

        tvStockValue = findViewById(R.id.tvTotalStockValue);
        tvLowStockCount = findViewById(R.id.tvInventoryLowStockCount);
        tvExpiringCount = findViewById(R.id.tvInventoryExpiringCount);
        tabLayoutInventory = findViewById(R.id.tabLayoutInventory);

        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Setup RecyclerView with Stock Edit Dialog Listener
        if (rvInventory != null) {
            rvInventory.setLayoutManager(new LinearLayoutManager(this));
            adapter = new ProductAdapter(
                    product -> showStockAdjustmentDialog(product),
                    product -> showStockAdjustmentDialog(product)
            );
            rvInventory.setAdapter(adapter);
        }

        // Tab Listener
        if (tabLayoutInventory != null) {
            tabLayoutInventory.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    currentTabPosition = tab.getPosition();
                    loadInventoryData();
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {}

                @Override
                public void onTabReselected(TabLayout.Tab tab) {}
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadInventoryData();
    }

    private void loadInventoryData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());

            // 1. Calculate live summary metrics
            Double totalValue = db.productDao().getTotalStockValue();
            int lowCount = db.productDao().getLowStockCount();
            int expiringCount = db.productDao().getExpiringCount();

            // 2. Fetch list based on active tab
            List<ProductEntity> productList;
            if (currentTabPosition == 1) { // Low Stock
                productList = db.productDao().getLowStockProducts();
            } else if (currentTabPosition == 2) { // Expiring
                productList = db.productDao().getExpiringProducts();
            } else { // All Items
                productList = db.productDao().getAllProducts();
            }

            final double val = totalValue != null ? totalValue : 0.0;

            runOnUiThread(() -> {
                if (tvStockValue != null) tvStockValue.setText(String.format("Rs. %.2f", val));
                if (tvLowStockCount != null) tvLowStockCount.setText(lowCount + " Items");
                if (tvExpiringCount != null) tvExpiringCount.setText(expiringCount + " Items");

                if (adapter != null) {
                    adapter.setProductList(productList);
                }
            });
        });
    }

    private void showStockAdjustmentDialog(ProductEntity product) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_payment, null);
        
        // Build Quick Stock Edit Dialog
        EditText etNewStock = new EditText(this);
        etNewStock.setHint("Enter new stock count (e.g. 50)");
        etNewStock.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etNewStock.setText(String.valueOf(product.stockQuantity));
        etNewStock.setPadding(32, 24, 32, 24);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Update Stock / තොග ප්‍රමාණය (" + product.name + ")")
                .setMessage("Current Stock: " + product.stockQuantity + " " + (product.unit != null ? product.unit : "Pcs"))
                .setView(etNewStock)
                .setPositiveButton("Save Stock", (dialog, which) -> {
                    String input = etNewStock.getText().toString().trim();
                    if (!TextUtils.isEmpty(input)) {
                        try {
                            double newStock = Double.parseDouble(input);
                            updateProductStockInDb(product.id, newStock);
                        } catch (Exception ignored) {}
                    }
                })
                .setNeutralButton("+10 Quick Add", (dialog, which) -> {
                    updateProductStockInDb(product.id, product.stockQuantity + 10);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateProductStockInDb(int productId, double newStock) {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            db.productDao().updateStockQuantity(productId, Math.max(0.0, newStock));

            runOnUiThread(() -> {
                Toast.makeText(InventoryActivity.this, "Stock Updated Successfully! / තොගය යාවත්කාලීන විය!", Toast.LENGTH_SHORT).show();
                loadInventoryData();
            });
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_dashboard) {
            startActivity(new Intent(InventoryActivity.this, MainActivity.class));
            finish();
        } else if (id == R.id.nav_pos) {
            startActivity(new Intent(InventoryActivity.this, PosActivity.class));
            finish();
        } else if (id == R.id.nav_products) {
            startActivity(new Intent(InventoryActivity.this, ProductsActivity.class));
            finish();
        } else if (id == R.id.nav_inventory) {
            // Already on Inventory
        } else if (id == R.id.nav_credit) {
            startActivity(new Intent(InventoryActivity.this, CreditActivity.class));
            finish();
        } else if (id == R.id.nav_logout) {
            startActivity(new Intent(InventoryActivity.this, LoginActivity.class));
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
