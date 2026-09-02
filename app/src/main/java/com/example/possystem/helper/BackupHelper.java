package com.example.possystem.helper;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.example.possystem.data.AppDatabase;
import com.example.possystem.data.entity.ProductEntity;
import com.example.possystem.data.entity.SaleEntity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BackupHelper {

    public static File exportDatabaseToJson(Context context) {
        try {
            AppDatabase db = AppDatabase.getInstance(context);
            List<ProductEntity> products = db.productDao().getAllProducts();
            List<SaleEntity> sales = db.saleDao().getAllSales();

            JSONObject backupRoot = new JSONObject();
            backupRoot.put("app", "Antigravity POS System");
            backupRoot.put("version", "1.0.0");
            backupRoot.put("timestamp", System.currentTimeMillis());

            // Product Array
            JSONArray productArray = new JSONArray();
            for (ProductEntity p : products) {
                JSONObject obj = new JSONObject();
                obj.put("id", p.id);
                obj.put("barcode", p.barcode);
                obj.put("name", p.name);
                obj.put("purchasePrice", p.purchasePrice);
                obj.put("sellingPrice", p.sellingPrice);
                obj.put("unit", p.unit);
                obj.put("isWeightBased", p.isWeightBased);
                obj.put("stockQuantity", p.stockQuantity);
                productArray.put(obj);
            }
            backupRoot.put("products", productArray);

            // Sales Array
            JSONArray salesArray = new JSONArray();
            for (SaleEntity s : sales) {
                JSONObject obj = new JSONObject();
                obj.put("invoiceNumber", s.invoiceNumber);
                obj.put("total", s.total);
                obj.put("discount", s.discount);
                obj.put("paymentMethod", s.paymentMethod);
                obj.put("timestamp", s.timestamp);
                salesArray.put(obj);
            }
            backupRoot.put("sales", salesArray);

            // Save to File
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String fileName = "retail_pos_backup_" + sdf.format(new Date()) + ".json";

            File backupDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (backupDir == null) backupDir = context.getFilesDir();
            if (!backupDir.exists()) backupDir.mkdirs();

            File backupFile = new File(backupDir, fileName);
            FileWriter writer = new FileWriter(backupFile);
            writer.write(backupRoot.toString(2));
            writer.flush();
            writer.close();

            Log.d("POS_BACKUP", "Backup exported to: " + backupFile.getAbsolutePath());
            Toast.makeText(context, "💾 Database Backup Exported:\n" + fileName, Toast.LENGTH_LONG).show();
            return backupFile;

        } catch (Exception e) {
            Log.e("POS_BACKUP", "Backup export failed", e);
            Toast.makeText(context, "❌ Backup Export Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }
}
