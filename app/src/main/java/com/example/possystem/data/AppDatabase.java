package com.example.possystem.data;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.possystem.data.dao.ProductDao;
import com.example.possystem.data.dao.ReturnDao;
import com.example.possystem.data.dao.SaleDao;
import com.example.possystem.data.entity.ProductEntity;
import com.example.possystem.data.entity.ReturnEntity;
import com.example.possystem.data.entity.ReturnItemEntity;
import com.example.possystem.data.entity.SaleEntity;
import com.example.possystem.data.entity.SaleItemEntity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;

@Database(entities = {ProductEntity.class, SaleEntity.class, SaleItemEntity.class, ReturnEntity.class, ReturnItemEntity.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase INSTANCE;

    public abstract ProductDao productDao();
    public abstract SaleDao saleDao();
    public abstract ReturnDao returnDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    "possystem_database"
            ).addCallback(new Callback() {
                @Override
                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                    super.onCreate(db);
                    seedSriLankaProducts(context.getApplicationContext());
                }
            })
            .fallbackToDestructiveMigration()
            .allowMainThreadQueries()
            .build();

            // Perform background seed check safely
            seedSriLankaProducts(context.getApplicationContext());
        }
        return INSTANCE;
    }

    private static void seedSriLankaProducts(Context context) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                if (INSTANCE == null) return;
                List<ProductEntity> currentList = INSTANCE.productDao().getAllProducts();
                if (currentList != null && currentList.size() >= 20) {
                    return; // Catalog already populated
                }

                InputStream is = context.getAssets().open("srilanka_products.json");
                int size = is.available();
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();

                String json = new String(buffer, StandardCharsets.UTF_8);
                JSONArray array = new JSONArray(json);

                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    ProductEntity product = new ProductEntity(
                            obj.getString("barcode"),
                            obj.getString("name"),
                            obj.getDouble("purchasePrice"),
                            obj.getDouble("sellingPrice"),
                            obj.getString("unit"),
                            obj.getBoolean("isWeightBased"),
                            obj.getDouble("stockQuantity"),
                            5.0,
                            ""
                    );
                    INSTANCE.productDao().insert(product);
                }
                Log.d("POS_DB", "Successfully seeded " + array.length() + " Sri Lankan products!");
            } catch (Exception e) {
                Log.e("POS_DB", "Error seeding Sri Lankan product database", e);
            }
        });
    }
}
