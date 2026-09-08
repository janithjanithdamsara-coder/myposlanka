package com.example.possystem;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExpensesActivity extends AppCompatActivity {

    private TextView tvTodayExpenseTotal;
    private RecyclerView rvExpenses;
    private FloatingActionButton fabAddExpense;

    private List<ExpenseModel> expenseList = new ArrayList<>();
    private ExpenseAdapter expenseAdapter;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expenses);

        prefs = getSharedPreferences("pos_expenses_prefs", MODE_PRIVATE);

        tvTodayExpenseTotal = findViewById(R.id.tvTodayExpenseTotal);
        rvExpenses = findViewById(R.id.rvExpenses);
        fabAddExpense = findViewById(R.id.fabAddExpense);

        // Back Button
        if (findViewById(R.id.btnBackExpenses) != null) {
            findViewById(R.id.btnBackExpenses).setOnClickListener(v -> finish());
        }

        if (rvExpenses != null) {
            rvExpenses.setLayoutManager(new LinearLayoutManager(this));
            expenseAdapter = new ExpenseAdapter();
            rvExpenses.setAdapter(expenseAdapter);
        }

        if (fabAddExpense != null) {
            fabAddExpense.setOnClickListener(v -> showAddExpenseDialog());
        }

        loadExpenses();
    }

    private void loadExpenses() {
        expenseList.clear();
        String jsonStr = prefs.getString("expenses_json", "[]");
        double total = 0.0;

        try {
            JSONArray arr = new JSONArray(jsonStr);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                ExpenseModel item = new ExpenseModel(
                        obj.getString("category"),
                        obj.getDouble("amount"),
                        obj.getString("note"),
                        obj.getString("date")
                );
                expenseList.add(item);
                total += item.amount;
            }
        } catch (Exception ignored) {}

        if (expenseAdapter != null) {
            expenseAdapter.notifyDataSetChanged();
        }

        if (tvTodayExpenseTotal != null) {
            tvTodayExpenseTotal.setText(String.format(Locale.US, "LKR %.2f", total));
        }
    }

    private void saveExpenses() {
        try {
            JSONArray arr = new JSONArray();
            for (ExpenseModel item : expenseList) {
                JSONObject obj = new JSONObject();
                obj.put("category", item.category);
                obj.put("amount", item.amount);
                obj.put("note", item.note);
                obj.put("date", item.date);
                arr.put(obj);
            }
            prefs.edit().putString("expenses_json", arr.toString()).apply();
        } catch (Exception ignored) {}

        loadExpenses();
    }

    private void showAddExpenseDialog() {
        View dialogView = LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_1, null);
        
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(40, 30, 40, 20);

        TextView lblCat = new TextView(this);
        lblCat.setText("Expense Category / වියදම් වර්ගය:");
        lblCat.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(lblCat);

        Spinner spnCategory = new Spinner(this);
        String[] categories = {
                "⚡ Electricity / විදුලි බිල",
                "🏢 Shop Rent / කඩ කුලිය",
                "👥 Staff Wages / සේවක වැටුප්",
                "☕ Tea & Food / තේ සහ කෑම",
                "🚗 Transport & Fuel / ප්‍රවාහන",
                "🔨 Maintenance & Repairs / අලුත්වැඩියා",
                "📦 Stationery & Bags / බෑග් සහ වෙනත්"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        spnCategory.setAdapter(adapter);
        spnCategory.setPadding(0, 10, 0, 20);
        layout.addView(spnCategory);

        TextView lblAmt = new TextView(this);
        lblAmt.setText("Amount / මුදල (LKR):");
        lblAmt.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(lblAmt);

        EditText etAmount = new EditText(this);
        etAmount.setHint("e.g. 1500");
        etAmount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(etAmount);

        TextView lblNote = new TextView(this);
        lblNote.setText("Note / විස්තරය (Optional):");
        lblNote.setTypeface(null, android.graphics.Typeface.BOLD);
        lblNote.setPadding(0, 20, 0, 0);
        layout.addView(lblNote);

        EditText etNote = new EditText(this);
        etNote.setHint("e.g. Tea for visitors / Lunch packet");
        layout.addView(etNote);

        new MaterialAlertDialogBuilder(this)
                .setTitle("➕ Add Shop Expense / වියදමක් සටහන් කිරීම")
                .setView(layout)
                .setPositiveButton("Save Expense", (dialog, which) -> {
                    String amtStr = etAmount.getText().toString().trim();
                    if (TextUtils.isEmpty(amtStr)) {
                        Toast.makeText(this, "Please enter an amount!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        double amt = Double.parseDouble(amtStr);
                        String cat = spnCategory.getSelectedItem().toString();
                        String note = etNote.getText().toString().trim();
                        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM HH:mm", Locale.getDefault());
                        String date = sdf.format(new Date());

                        expenseList.add(0, new ExpenseModel(cat, amt, note, date));
                        saveExpenses();
                        Toast.makeText(this, "✅ Expense Saved: LKR " + amt, Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static class ExpenseModel {
        String category;
        double amount;
        String note;
        String date;

        ExpenseModel(String category, double amount, String note, String date) {
            this.category = category;
            this.amount = amount;
            this.note = note;
            this.date = date;
        }
    }

    private class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

        @NonNull
        @Override
        public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_customer, parent, false);
            return new ExpenseViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
            ExpenseModel item = expenseList.get(position);
            holder.tvName.setText(item.category);
            holder.tvPhone.setText("📅 " + item.date + (TextUtils.isEmpty(item.note) ? "" : " • " + item.note));
            holder.tvBalance.setText(String.format(Locale.US, "LKR %.2f", item.amount));

            holder.itemView.setOnLongClickListener(v -> {
                new MaterialAlertDialogBuilder(ExpensesActivity.this)
                        .setTitle("Delete Expense?")
                        .setMessage("Delete " + item.category + " (LKR " + item.amount + ")?")
                        .setPositiveButton("Delete", (d, w) -> {
                            expenseList.remove(position);
                            saveExpenses();
                            Toast.makeText(ExpensesActivity.this, "Deleted.", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return expenseList.size();
        }

        class ExpenseViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvPhone, tvBalance;

            ExpenseViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvCustomerName);
                tvPhone = itemView.findViewById(R.id.tvCustomerPhone);
                tvBalance = itemView.findViewById(R.id.tvCustomerCreditBalance);
            }
        }
    }
}
