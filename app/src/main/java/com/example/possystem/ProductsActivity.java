package com.example.possystem;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class ProductsActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private RecyclerView rvProducts;
    private EditText etSearch;
    private ImageView btnBack;
    private ExtendedFloatingActionButton fabAddProduct;

    private ProductAdapter adapter;
    private List<ProductEntity> fullProductList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_products_drawer);

        // Bind Views
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        rvProducts = findViewById(R.id.rvProducts);
        etSearch = findViewById(R.id.etSearchProducts);
        btnBack = findViewById(R.id.btnBackProducts);
        fabAddProduct = findViewById(R.id.fabAddProduct);

        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (fabAddProduct != null) {
            fabAddProduct.setOnClickListener(v -> {
                startActivity(new Intent(ProductsActivity.this, AddProductActivity.class));
            });
        }

        // Setup RecyclerView with Click and Delete listeners
        if (rvProducts != null) {
            rvProducts.setLayoutManager(new LinearLayoutManager(this));
            adapter = new ProductAdapter(
                    product -> {
                        // Open AddProductActivity in edit mode if needed
                    },
                    product -> showDeleteConfirmationDialog(product)
            );
            rvProducts.setAdapter(adapter);
        }

        // Live Search Text Change Handler
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterProducts(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void showDeleteConfirmationDialog(ProductEntity product) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Product / භාණ්ඩය ඉවත් කරන්න")
                .setMessage("Are you sure you want to delete '" + product.name + "' from shop catalog?")
                .setPositiveButton("Delete", (dialog, which) -> deleteProductFromDb(product))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteProductFromDb(ProductEntity product) {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            db.productDao().delete(product);

            runOnUiThread(() -> {
                Toast.makeText(ProductsActivity.this, "Product deleted! / භාණ්ඩය ඉවත් කරන ලදී", Toast.LENGTH_SHORT).show();
                loadProductsFromDatabase();
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProductsFromDatabase();
    }

    private void loadProductsFromDatabase() {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            fullProductList = db.productDao().getAllProducts();

            runOnUiThread(() -> {
                if (adapter != null) {
                    adapter.setProductList(fullProductList);
                }
            });
        });
    }

    private void filterProducts(String query) {
        if (query == null || query.trim().isEmpty()) {
            adapter.setProductList(fullProductList);
            return;
        }

        String lowerQuery = query.toLowerCase().trim();
        List<ProductEntity> filteredList = new ArrayList<>();

        for (ProductEntity p : fullProductList) {
            if ((p.name != null && p.name.toLowerCase().contains(lowerQuery)) ||
                (p.barcode != null && p.barcode.contains(lowerQuery))) {
                filteredList.add(p);
            }
        }
        adapter.setProductList(filteredList);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_dashboard) {
            startActivity(new Intent(ProductsActivity.this, MainActivity.class));
            finish();
        } else if (id == R.id.nav_pos) {
            startActivity(new Intent(ProductsActivity.this, PosActivity.class));
            finish();
        } else if (id == R.id.nav_products) {
            // Already on Products
        } else if (id == R.id.nav_inventory) {
            startActivity(new Intent(ProductsActivity.this, InventoryActivity.class));
            finish();
        } else if (id == R.id.nav_credit) {
            startActivity(new Intent(ProductsActivity.this, CreditActivity.class));
            finish();
        } else if (id == R.id.nav_logout) {
            startActivity(new Intent(ProductsActivity.this, LoginActivity.class));
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
