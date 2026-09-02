package com.example.possystem;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.possystem.adapter.CartAdapter;
import com.example.possystem.data.AppDatabase;
import com.example.possystem.data.entity.ProductEntity;
import com.example.possystem.data.entity.SaleEntity;
import com.example.possystem.data.entity.SaleItemEntity;
import com.example.possystem.model.CartItemModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.journeyapps.barcodescanner.CaptureActivity;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

public class PosActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private RecyclerView rvCartItems;
    private AutoCompleteTextView etPOSSearch;
    private TextView tvSubtotal, tvDiscount, tvGrandTotal, tvCartCount, tvScanBadge;
    private Button btnPay, btnHold, btnClear, btnCheckoutTopPill;
    private ImageView btnBack, btnScanBarcode;
    private DecoratedBarcodeView barcodeScannerView;

    private CartAdapter cartAdapter;
    private List<CartItemModel> cartList = new ArrayList<>();
    private Map<String, ProductEntity> searchSuggestionMap = new HashMap<>();
    private double currentDiscount = 0.0;
    private boolean isPercentDiscountMode = false;
    private String selectedPaymentMethod = "Cash";
    private long lastScanTimestamp = 0;

    // ZXing Barcode Scanner Activity Result Launcher (Camera Fallback)
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(), result -> {
        if (result.getContents() != null) {
            searchAndAddProductToCart(result.getContents().trim());
            showScanAcceptedBadge();
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pos_drawer);

        // Bind Views
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        rvCartItems = findViewById(R.id.rvCartItems);
        etPOSSearch = findViewById(R.id.etPOSSearch);
        btnScanBarcode = findViewById(R.id.btnScanBarcodePOS);
        tvSubtotal = findViewById(R.id.tvPOSSubtotal);
        tvDiscount = findViewById(R.id.tvPOSDiscount);
        tvGrandTotal = findViewById(R.id.tvPOSGrandTotal);
        tvCartCount = findViewById(R.id.tvCartItemCount);
        btnPay = findViewById(R.id.btnPayPOS);
        btnHold = findViewById(R.id.btnHoldBill);
        btnClear = findViewById(R.id.btnClearCart);
        btnBack = findViewById(R.id.btnBack);
        btnCheckoutTopPill = findViewById(R.id.btnCheckoutTopPill);
        barcodeScannerView = findViewById(R.id.barcodeScannerView);
        tvScanBadge = findViewById(R.id.tvScanAcceptedBadge);

        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnScanBarcode != null) {
            btnScanBarcode.setOnClickListener(v -> launchCameraBarcodeScanner());
        }

        // Top Checkout Fast Access Pill
        if (btnCheckoutTopPill != null) {
            btnCheckoutTopPill.setOnClickListener(v -> triggerCheckoutAction());
        }

        // Setup Cart RecyclerView
        if (rvCartItems != null) {
            rvCartItems.setLayoutManager(new LinearLayoutManager(this));
            cartAdapter = new CartAdapter(this::recalculateCartTotals);
            rvCartItems.setAdapter(cartAdapter);
        }

        // Setup Live AutoComplete Search Dropdown from DB
        setupAutoCompleteSearch();

        // Continuous Embedded Top Camera Scanning
        if (barcodeScannerView != null) {
            barcodeScannerView.decodeContinuous(result -> {
                if (result.getText() != null && !result.getText().isEmpty()) {
                    long now = System.currentTimeMillis();
                    if (now - lastScanTimestamp > 2000) { // 2s debounce
                        lastScanTimestamp = now;
                        runOnUiThread(() -> {
                            searchAndAddProductToCart(result.getText().trim());
                            showScanAcceptedBadge();
                        });
                    }
                }
            });
        }

        // Handle Keyboard IME Search / Enter Action
        if (etPOSSearch != null) {
            etPOSSearch.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE ||
                        (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                    String query = etPOSSearch.getText().toString().trim();
                    if (!TextUtils.isEmpty(query)) {
                        searchAndAddProductToCart(query);
                    }
                    return true;
                }
                return false;
            });
        }

        // Clear Cart
        if (btnClear != null) {
            btnClear.setOnClickListener(v -> {
                cartList.clear();
                cartAdapter.setCartList(cartList);
                recalculateCartTotals();
                Toast.makeText(this, "Cart Cleared!", Toast.LENGTH_SHORT).show();
            });
        }

        // Pay / Checkout Button
        if (btnPay != null) {
            btnPay.setOnClickListener(v -> triggerCheckoutAction());
        }

        // Start POS with clean empty cart
        recalculateCartTotals();
    }

    private void setupAutoCompleteSearch() {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            List<ProductEntity> products = db.productDao().getAllProducts();
            if (products != null && !products.isEmpty()) {
                List<String> suggestionList = new ArrayList<>();
                searchSuggestionMap.clear();

                for (ProductEntity p : products) {
                    String displayKey = p.name;
                    suggestionList.add(displayKey);
                    searchSuggestionMap.put(displayKey, p);
                }

                runOnUiThread(() -> {
                    if (etPOSSearch != null) {
                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(PosActivity.this, R.layout.item_search_suggestion, R.id.tvSuggestionName, suggestionList) {
                            @NonNull
                            @Override
                            public View getView(int position, View convertView, @NonNull android.view.ViewGroup parent) {
                                View view = super.getView(position, convertView, parent);
                                TextView tvName = view.findViewById(R.id.tvSuggestionName);
                                TextView tvSub = view.findViewById(R.id.tvSuggestionSub);
                                TextView tvPrice = view.findViewById(R.id.tvSuggestionPrice);

                                String itemStr = getItem(position);
                                ProductEntity p = searchSuggestionMap.get(itemStr);
                                if (p != null) {
                                    if (tvName != null) tvName.setText(p.name);
                                    if (tvSub != null) tvSub.setText("Barcode: " + p.barcode + " • Stock: " + String.format(Locale.getDefault(), "%.0f", p.stockQuantity));
                                    if (tvPrice != null) tvPrice.setText("LKR " + String.format(Locale.getDefault(), "%.2f", p.sellingPrice));
                                }
                                return view;
                            }
                        };

                        etPOSSearch.setAdapter(adapter);
                        etPOSSearch.setThreshold(1);
                        int dropdownWidth = (int) (getResources().getDisplayMetrics().widthPixels * 0.85);
                        etPOSSearch.setDropDownWidth(dropdownWidth);

                        etPOSSearch.setOnItemClickListener((parent, view, position, id) -> {
                            String selectedString = (String) parent.getItemAtPosition(position);
                            ProductEntity matchedProduct = searchSuggestionMap.get(selectedString);
                            if (matchedProduct != null) {
                                addProductToCartList(matchedProduct);
                                etPOSSearch.setText("");
                                Toast.makeText(PosActivity.this, "✓ Added: " + matchedProduct.name, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        });
    }

    private void triggerCheckoutAction() {
        if (cartList.isEmpty()) {
            Toast.makeText(this, "🛒 Cart is empty! Please add items first.", Toast.LENGTH_SHORT).show();
            return;
        }
        showPaymentCheckoutDialog();
    }

    private void showScanAcceptedBadge() {
        if (tvScanBadge != null) {
            tvScanBadge.setVisibility(View.VISIBLE);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (tvScanBadge != null) tvScanBadge.setVisibility(View.GONE);
            }, 1500);
        }
    }

    private void launchCameraBarcodeScanner() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Scan Barcode for POS Cart / බාර්කෝඩ් එක Scan කරන්න");
        options.setBeepEnabled(true);
        options.setOrientationLocked(false);
        options.setCaptureActivity(CaptureActivity.class);
        barcodeLauncher.launch(options);
    }

    private void searchAndAddProductToCart(String query) {
        if (TextUtils.isEmpty(query)) return;

        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            List<ProductEntity> allProducts = db.productDao().getAllProducts();
            ProductEntity matchedProduct = null;

            if (allProducts != null) {
                String cleanQuery = query.toLowerCase().trim();
                // 1. Exact Barcode match first
                for (ProductEntity p : allProducts) {
                    if (p.barcode != null && p.barcode.equalsIgnoreCase(cleanQuery)) {
                        matchedProduct = p;
                        break;
                    }
                }
                // 2. Product Name contains search query
                if (matchedProduct == null) {
                    for (ProductEntity p : allProducts) {
                        if (p.name != null && p.name.toLowerCase().contains(cleanQuery)) {
                            matchedProduct = p;
                            break;
                        }
                    }
                }
            }

            final ProductEntity productToAdd = matchedProduct;
            if (productToAdd != null) {
                runOnUiThread(() -> {
                    addProductToCartList(productToAdd);
                    if (etPOSSearch != null) etPOSSearch.setText("");
                    Toast.makeText(PosActivity.this, "✓ Added: " + productToAdd.name, Toast.LENGTH_SHORT).show();
                });
            } else {
                runOnUiThread(() -> {
                    Toast.makeText(PosActivity.this, "⚠️ No product found matching '" + query + "'", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void addProductToCartList(ProductEntity product) {
        for (CartItemModel item : cartList) {
            if (item.product.id == product.id) {
                item.quantity++;
                item.recalculate();
                cartAdapter.notifyDataSetChanged();
                recalculateCartTotals();
                return;
            }
        }

        cartList.add(new CartItemModel(product, 1.0));
        cartAdapter.setCartList(cartList);
        recalculateCartTotals();
    }

    private void recalculateCartTotals() {
        double subtotal = 0.0;
        int itemCount = 0;

        for (CartItemModel item : cartList) {
            subtotal += item.total;
            itemCount += (int) item.quantity;
        }

        double grandTotal = Math.max(0.0, subtotal - currentDiscount);

        if (tvSubtotal != null) tvSubtotal.setText(String.format("LKR %.2f", subtotal));
        if (tvDiscount != null) tvDiscount.setText(String.format("- LKR %.2f", currentDiscount));
        if (tvGrandTotal != null) tvGrandTotal.setText(String.format("LKR %.2f", grandTotal));
        if (tvCartCount != null) tvCartCount.setText(itemCount + " Items");
        if (btnCheckoutTopPill != null) btnCheckoutTopPill.setText(String.format("LKR %.0f Checkout", grandTotal));
    }

    private void showPaymentCheckoutDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_payment, null);
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        TextView tvDialogTotal = dialogView.findViewById(R.id.tvPaymentDialogTotal);
        EditText etDiscountVal = dialogView.findViewById(R.id.etDiscountValue);
        Button btnToggleLKR = dialogView.findViewById(R.id.btnToggleDiscountLKR);
        Button btnTogglePercent = dialogView.findViewById(R.id.btnToggleDiscountPercent);
        Button btnPayCash = dialogView.findViewById(R.id.btnPayCash);
        Button btnPayCard = dialogView.findViewById(R.id.btnPayCard);
        Button btnPayCredit = dialogView.findViewById(R.id.btnPayCredit);
        EditText etCashReceived = dialogView.findViewById(R.id.etCashReceived);
        TextView tvBalance = dialogView.findViewById(R.id.tvChangeBalance);
        Button btnFinish = dialogView.findViewById(R.id.btnCompleteSaleAction);

        double subtotal = 0.0;
        for (CartItemModel item : cartList) subtotal += item.total;
        final double cartSubtotal = subtotal;

        Runnable updateDialogTotals = () -> {
            double discountVal = 0.0;
            if (etDiscountVal != null && !TextUtils.isEmpty(etDiscountVal.getText())) {
                try {
                    discountVal = Double.parseDouble(etDiscountVal.getText().toString());
                } catch (Exception ignored) {}
            }

            double calculatedDiscount = isPercentDiscountMode ? (cartSubtotal * (discountVal / 100.0)) : discountVal;
            currentDiscount = calculatedDiscount;
            double payableTotal = Math.max(0.0, cartSubtotal - currentDiscount);

            if (tvDialogTotal != null) tvDialogTotal.setText(String.format("LKR %.2f", payableTotal));
            if (btnFinish != null) btnFinish.setText(String.format("CHECKOUT LKR %.2f", payableTotal));

            if (etCashReceived != null && tvBalance != null && !TextUtils.isEmpty(etCashReceived.getText())) {
                try {
                    double cash = Double.parseDouble(etCashReceived.getText().toString());
                    tvBalance.setText(String.format("LKR %.2f", Math.max(0.0, cash - payableTotal)));
                } catch (Exception ignored) {}
            }
            recalculateCartTotals();
        };

        // Discount Toggles (LKR vs %)
        if (btnToggleLKR != null && btnTogglePercent != null) {
            btnToggleLKR.setOnClickListener(v -> {
                isPercentDiscountMode = false;
                btnToggleLKR.setBackgroundTintList(getColorStateList(R.color.green_primary));
                btnToggleLKR.setTextColor(getColor(R.color.white));
                btnTogglePercent.setBackgroundTintList(getColorStateList(android.R.color.transparent));
                btnTogglePercent.setTextColor(getColor(R.color.green_primary));
                updateDialogTotals.run();
            });

            btnTogglePercent.setOnClickListener(v -> {
                isPercentDiscountMode = true;
                btnTogglePercent.setBackgroundTintList(getColorStateList(R.color.green_primary));
                btnTogglePercent.setTextColor(getColor(R.color.white));
                btnToggleLKR.setBackgroundTintList(getColorStateList(android.R.color.transparent));
                btnToggleLKR.setTextColor(getColor(R.color.green_primary));
                updateDialogTotals.run();
            });
        }

        if (etDiscountVal != null) {
            etDiscountVal.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    updateDialogTotals.run();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        // Payment Method Selectors
        if (btnPayCash != null && btnPayCard != null && btnPayCredit != null) {
            btnPayCash.setOnClickListener(v -> {
                selectedPaymentMethod = "Cash";
                btnPayCash.setBackgroundTintList(getColorStateList(R.color.green_primary));
                btnPayCash.setTextColor(getColor(R.color.white));
                btnPayCard.setBackgroundTintList(getColorStateList(android.R.color.transparent));
                btnPayCard.setTextColor(getColor(R.color.text_secondary));
                btnPayCredit.setBackgroundTintList(getColorStateList(android.R.color.transparent));
                btnPayCredit.setTextColor(getColor(R.color.text_secondary));
            });

            btnPayCard.setOnClickListener(v -> {
                selectedPaymentMethod = "Card";
                btnPayCard.setBackgroundTintList(getColorStateList(R.color.green_primary));
                btnPayCard.setTextColor(getColor(R.color.white));
                btnPayCash.setBackgroundTintList(getColorStateList(android.R.color.transparent));
                btnPayCash.setTextColor(getColor(R.color.text_secondary));
                btnPayCredit.setBackgroundTintList(getColorStateList(android.R.color.transparent));
                btnPayCredit.setTextColor(getColor(R.color.text_secondary));
            });

            btnPayCredit.setOnClickListener(v -> {
                selectedPaymentMethod = "Credit";
                btnPayCredit.setBackgroundTintList(getColorStateList(R.color.green_primary));
                btnPayCredit.setTextColor(getColor(R.color.white));
                btnPayCash.setBackgroundTintList(getColorStateList(android.R.color.transparent));
                btnPayCash.setTextColor(getColor(R.color.text_secondary));
                btnPayCard.setBackgroundTintList(getColorStateList(android.R.color.transparent));
                btnPayCard.setTextColor(getColor(R.color.text_secondary));
            });
        }

        if (etCashReceived != null && tvBalance != null) {
            etCashReceived.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    updateDialogTotals.run();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            // Quick Cash Denominations
            Button btn500 = dialogView.findViewById(R.id.btnQuickCash500);
            Button btn1000 = dialogView.findViewById(R.id.btnQuickCash1000);
            Button btn2000 = dialogView.findViewById(R.id.btnQuickCash2000);
            Button btn5000 = dialogView.findViewById(R.id.btnQuickCash5000);

            if (btn500 != null) btn500.setOnClickListener(v -> etCashReceived.setText("500"));
            if (btn1000 != null) btn1000.setOnClickListener(v -> etCashReceived.setText("1000"));
            if (btn2000 != null) btn2000.setOnClickListener(v -> etCashReceived.setText("2000"));
            if (btn5000 != null) btn5000.setOnClickListener(v -> etCashReceived.setText("5000"));
        }

        if (btnFinish != null) {
            btnFinish.setOnClickListener(v -> {
                dialog.dismiss();
                double finalPayable = Math.max(0.0, cartSubtotal - currentDiscount);
                processSaleCheckout(finalPayable, etCashReceived != null ? etCashReceived.getText().toString() : "");
            });
        }

        updateDialogTotals.run();
        dialog.show();
    }

    private void processSaleCheckout(double grandTotal, String cashStr) {
        double paidAmount = TextUtils.isEmpty(cashStr) ? grandTotal : Double.parseDouble(cashStr);
        double changeAmount = Math.max(0.0, paidAmount - grandTotal);
        String invoiceNo = "INV-" + System.currentTimeMillis() % 100000;

        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());

            // 1. Insert Master Sale Entity
            SaleEntity sale = new SaleEntity(
                    invoiceNo,
                    grandTotal + currentDiscount,
                    currentDiscount,
                    grandTotal,
                    paidAmount,
                    changeAmount,
                    selectedPaymentMethod,
                    selectedPaymentMethod
            );
            long saleId = db.saleDao().insertSale(sale);

            // 2. Insert Sale Items & Reduce Stock Quantity
            List<SaleItemEntity> saleItems = new ArrayList<>();
            for (CartItemModel item : cartList) {
                saleItems.add(new SaleItemEntity(
                        (int) saleId,
                        item.product.id,
                        item.product.name,
                        item.quantity,
                        item.product.sellingPrice,
                        item.total
                ));

                // Execute DB Stock Reduction Query
                db.productDao().reduceStock(item.product.id, item.quantity);
            }
            db.saleDao().insertSaleItems(saleItems);

            runOnUiThread(() -> {
                Toast.makeText(PosActivity.this, "Sale Completed via " + selectedPaymentMethod + "!", Toast.LENGTH_SHORT).show();
                cartList.clear();
                cartAdapter.setCartList(cartList);
                recalculateCartTotals();

                // Open Receipt Activity
                Intent intent = new Intent(PosActivity.this, ReceiptActivity.class);
                intent.putExtra("INVOICE_NO", invoiceNo);
                intent.putExtra("GRAND_TOTAL", grandTotal);
                intent.putExtra("PAID_AMOUNT", paidAmount);
                intent.putExtra("CHANGE_AMOUNT", changeAmount);
                startActivity(intent);
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (barcodeScannerView != null) {
            barcodeScannerView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (barcodeScannerView != null) {
            barcodeScannerView.pause();
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_dashboard) {
            finish();
        } else if (id == R.id.nav_products) {
            startActivity(new Intent(PosActivity.this, ProductsActivity.class));
        } else if (id == R.id.nav_inventory) {
            startActivity(new Intent(PosActivity.this, InventoryActivity.class));
        } else if (id == R.id.nav_purchases) {
            startActivity(new Intent(PosActivity.this, PurchasesActivity.class));
        } else if (id == R.id.nav_credit) {
            startActivity(new Intent(PosActivity.this, CreditActivity.class));
        } else if (id == R.id.nav_reports) {
            startActivity(new Intent(PosActivity.this, ReportsActivity.class));
        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(PosActivity.this, SettingsActivity.class));
        }

        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        return true;
    }
}
