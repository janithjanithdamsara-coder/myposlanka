package com.example.possystem.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.possystem.R;
import com.example.possystem.data.entity.ProductEntity;

import java.util.ArrayList;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private List<ProductEntity> productList = new ArrayList<>();
    private OnProductClickListener clickListener;
    private OnProductDeleteListener deleteListener;

    public interface OnProductClickListener {
        void onProductClick(ProductEntity product);
    }

    public interface OnProductDeleteListener {
        void onProductDelete(ProductEntity product);
    }

    public ProductAdapter(OnProductClickListener clickListener, OnProductDeleteListener deleteListener) {
        this.clickListener = clickListener;
        this.deleteListener = deleteListener;
    }

    public ProductAdapter(OnProductClickListener clickListener) {
        this(clickListener, null);
    }

    public void setProductList(List<ProductEntity> list) {
        this.productList = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        ProductEntity product = productList.get(position);

        holder.tvName.setText(product.name);
        holder.tvBarcode.setText("Barcode: " + (product.barcode != null ? product.barcode : "N/A"));
        holder.tvCategory.setText("Unit: " + (product.unit != null ? product.unit : "Pcs") + (product.isWeightBased ? " (Weight-Based)" : ""));
        holder.tvPrice.setText(String.format("Rs. %.2f", product.sellingPrice));

        // Stock Pill Status
        if (product.stockQuantity <= product.minimumStock) {
            holder.tvStockPill.setText(String.format("Low Stock: %.1f", product.stockQuantity));
            holder.tvStockPill.setBackgroundResource(R.drawable.bg_badge_low_stock);
        } else {
            holder.tvStockPill.setText(String.format("Stock: %.1f", product.stockQuantity));
            holder.tvStockPill.setBackgroundResource(R.drawable.bg_badge_in_stock);
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onProductClick(product);
            }
        });

        if (holder.btnDelete != null) {
            if (deleteListener != null) {
                holder.btnDelete.setVisibility(View.VISIBLE);
                holder.btnDelete.setOnClickListener(v -> deleteListener.onProductDelete(product));
            } else {
                holder.btnDelete.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return productList != null ? productList.size() : 0;
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvBarcode, tvCategory, tvPrice, tvStockPill;
        ImageView btnDelete;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvProductName);
            tvBarcode = itemView.findViewById(R.id.tvProductBarcode);
            tvCategory = itemView.findViewById(R.id.tvProductCategory);
            tvPrice = itemView.findViewById(R.id.tvProductSellingPrice);
            tvStockPill = itemView.findViewById(R.id.tvProductStockPill);
            btnDelete = itemView.findViewById(R.id.btnDeleteProduct);
        }
    }
}
