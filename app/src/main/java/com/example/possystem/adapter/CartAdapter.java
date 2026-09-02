package com.example.possystem.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.possystem.R;
import com.example.possystem.model.CartItemModel;

import java.util.ArrayList;
import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private List<CartItemModel> cartList = new ArrayList<>();
    private OnCartChangeListener listener;

    public interface OnCartChangeListener {
        void onCartUpdated();
    }

    public CartAdapter(OnCartChangeListener listener) {
        this.listener = listener;
    }

    public void setCartList(List<CartItemModel> list) {
        this.cartList = list;
        notifyDataSetChanged();
    }

    public List<CartItemModel> getCartList() {
        return cartList;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItemModel item = cartList.get(position);

        holder.tvName.setText(item.product.name);
        holder.tvUnitPrice.setText(String.format("Rs. %.2f / %s", item.product.sellingPrice, item.product.unit != null ? item.product.unit : "pcs"));
        holder.tvQty.setText(String.valueOf((int) item.quantity));
        holder.tvTotal.setText(String.format("Rs. %.2f", item.total));

        // Plus Button
        holder.btnPlus.setOnClickListener(v -> {
            item.quantity++;
            item.recalculate();
            notifyItemChanged(position);
            if (listener != null) listener.onCartUpdated();
        });

        // Minus Button
        holder.btnMinus.setOnClickListener(v -> {
            if (item.quantity > 1) {
                item.quantity--;
                item.recalculate();
                notifyItemChanged(position);
            } else {
                cartList.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, cartList.size());
            }
            if (listener != null) listener.onCartUpdated();
        });
    }

    @Override
    public int getItemCount() {
        return cartList != null ? cartList.size() : 0;
    }

    static class CartViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvUnitPrice, tvQty, tvTotal;
        ImageView btnPlus, btnMinus;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCartItemName);
            tvUnitPrice = itemView.findViewById(R.id.tvCartItemUnitPrice);
            tvQty = itemView.findViewById(R.id.tvCartItemQty);
            tvTotal = itemView.findViewById(R.id.tvCartItemTotalPrice);
            btnPlus = itemView.findViewById(R.id.btnPlusCartQty);
            btnMinus = itemView.findViewById(R.id.btnMinusCartQty);
        }
    }
}
