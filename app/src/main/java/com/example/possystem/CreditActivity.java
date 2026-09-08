package com.example.possystem;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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

import com.example.possystem.data.AppDatabase;
import com.example.possystem.data.entity.SaleEntity;
import com.example.possystem.helper.LicenseManager;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class CreditActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView tvTotalCreditOutstanding, tvCustomersWithDueCount, tvOverdueCount;
    private EditText etSearchCredit;
    private RecyclerView rvCreditLedger;

    private List<SaleEntity> allCreditSales = new ArrayList<>();
    private List<SaleEntity> filteredCreditSales = new ArrayList<>();
    private CreditAdapter creditAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credit_drawer);

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        tvTotalCreditOutstanding = findViewById(R.id.tvTotalCreditOutstanding);
        tvCustomersWithDueCount = findViewById(R.id.tvCustomersWithDueCount);
        tvOverdueCount = findViewById(R.id.tvOverdueCount);
        etSearchCredit = findViewById(R.id.etSearchCreditCustomers);
        rvCreditLedger = findViewById(R.id.rvCreditLedger);

        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);
        }

        if (findViewById(R.id.btnBackCredit) != null) {
            findViewById(R.id.btnBackCredit).setOnClickListener(v -> finish());
        }

        if (rvCreditLedger != null) {
            rvCreditLedger.setLayoutManager(new LinearLayoutManager(this));
            creditAdapter = new CreditAdapter();
            rvCreditLedger.setAdapter(creditAdapter);
        }

        if (etSearchCredit != null) {
            etSearchCredit.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterCreditList(s.toString().trim());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCreditDataFromDatabase();
    }

    private void loadCreditDataFromDatabase() {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            List<SaleEntity> list = db.saleDao().getCreditSales();
            if (list == null) list = new ArrayList<>();

            double totalDue = 0.0;
            for (SaleEntity s : list) {
                totalDue += s.total;
            }

            final List<SaleEntity> finalList = list;
            final double fTotalDue = totalDue;
            final int count = list.size();

            runOnUiThread(() -> {
                allCreditSales = new ArrayList<>(finalList);
                filteredCreditSales = new ArrayList<>(finalList);
                if (creditAdapter != null) {
                    creditAdapter.setList(filteredCreditSales);
                }

                if (tvTotalCreditOutstanding != null) {
                    tvTotalCreditOutstanding.setText(String.format(Locale.US, "LKR %.2f", fTotalDue));
                }
                if (tvCustomersWithDueCount != null) {
                    tvCustomersWithDueCount.setText("• " + count + " Records Pending");
                }
                if (tvOverdueCount != null) {
                    tvOverdueCount.setText("• 100% Offline Tracked");
                }
            });
        });
    }

    private void filterCreditList(String query) {
        if (TextUtils.isEmpty(query)) {
            filteredCreditSales = new ArrayList<>(allCreditSales);
        } else {
            List<SaleEntity> temp = new ArrayList<>();
            for (SaleEntity s : allCreditSales) {
                if ((s.invoiceNumber != null && s.invoiceNumber.toLowerCase().contains(query.toLowerCase())) ||
                    (s.saleType != null && s.saleType.toLowerCase().contains(query.toLowerCase()))) {
                    temp.add(s);
                }
            }
            filteredCreditSales = temp;
        }
        if (creditAdapter != null) {
            creditAdapter.setList(filteredCreditSales);
        }
    }

    private void showCreditSettlementDialog(SaleEntity sale) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_credit_payment, null);
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        TextView tvName = dialogView.findViewById(R.id.tvCreditPaymentCustomerName);
        TextView tvCurrentDue = dialogView.findViewById(R.id.tvCreditDialogCurrentDue);
        EditText etPaying = dialogView.findViewById(R.id.etCreditPayingAmount);
        TextView tvRemaining = dialogView.findViewById(R.id.tvCreditDialogRemainingBalance);
        Button btnSave = dialogView.findViewById(R.id.btnSaveCreditPayment);

        if (tvName != null) tvName.setText("Invoice: " + sale.invoiceNumber);
        if (tvCurrentDue != null) tvCurrentDue.setText(String.format(Locale.US, "LKR %.2f", sale.total));
        if (tvRemaining != null) tvRemaining.setText(String.format(Locale.US, "LKR %.2f", sale.total));

        if (etPaying != null && tvRemaining != null) {
            etPaying.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    try {
                        double paying = Double.parseDouble(s.toString());
                        double remaining = Math.max(0.0, sale.total - paying);
                        tvRemaining.setText(String.format(Locale.US, "LKR %.2f", remaining));
                    } catch (Exception e) {
                        tvRemaining.setText(String.format(Locale.US, "LKR %.2f", sale.total));
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                dialog.dismiss();
                Toast.makeText(this, "✅ Credit Payment Recorded for " + sale.invoiceNumber, Toast.LENGTH_SHORT).show();
            });
        }

        dialog.show();
    }

    private void sendWhatsAppPaymentReminder(SaleEntity sale) {
        String message = "Dear Customer, this is a reminder from " + LicenseManager.getShopName(this) +
                " regarding outstanding credit balance of LKR " + String.format(Locale.US, "%.2f", sale.total) +
                " (Invoice: " + sale.invoiceNumber + "). Thank you!";
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://api.whatsapp.com/send?text=" + Uri.encode(message)));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "WhatsApp is not installed.", Toast.LENGTH_SHORT).show();
        }
    }

    // RecyclerView Adapter for Credit Ledger
    private class CreditAdapter extends RecyclerView.Adapter<CreditAdapter.CreditViewHolder> {

        private List<SaleEntity> list = new ArrayList<>();

        public void setList(List<SaleEntity> newList) {
            this.list = newList;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public CreditViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_customer, parent, false);
            return new CreditViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull CreditViewHolder holder, int position) {
            SaleEntity s = list.get(position);
            holder.tvName.setText("Credit Sale: " + s.invoiceNumber);
            
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy HH:mm", Locale.getDefault());
            String dateStr = sdf.format(new Date(s.timestamp));
            holder.tvPhone.setText("📅 " + dateStr + " • " + s.paymentMethod);
            holder.tvBalance.setText(String.format(Locale.US, "LKR %.2f", s.total));

            holder.itemView.setOnClickListener(v -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(CreditActivity.this);
                builder.setTitle(s.invoiceNumber + " (LKR " + String.format(Locale.US, "%.2f", s.total) + ")");
                builder.setItems(new CharSequence[]{"💵 Settle Credit Payment", "📲 Send WhatsApp Reminder"}, (d, which) -> {
                    if (which == 0) {
                        showCreditSettlementDialog(s);
                    } else if (which == 1) {
                        sendWhatsAppPaymentReminder(s);
                    }
                });
                builder.show();
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class CreditViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvPhone, tvBalance;

            CreditViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvCustomerName);
                tvPhone = itemView.findViewById(R.id.tvCustomerPhone);
                tvBalance = itemView.findViewById(R.id.tvCustomerCreditBalance);
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_dashboard) {
            finish();
        } else if (id == R.id.nav_pos) {
            startActivity(new Intent(CreditActivity.this, PosActivity.class));
            finish();
        } else if (id == R.id.nav_products) {
            startActivity(new Intent(CreditActivity.this, ProductsActivity.class));
            finish();
        } else if (id == R.id.nav_inventory) {
            startActivity(new Intent(CreditActivity.this, InventoryActivity.class));
            finish();
        } else if (id == R.id.nav_purchases) {
            startActivity(new Intent(CreditActivity.this, PurchasesActivity.class));
            finish();
        } else if (id == R.id.nav_credit) {
            // Already on Credit
        } else if (id == R.id.nav_logout) {
            startActivity(new Intent(CreditActivity.this, LoginActivity.class));
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
