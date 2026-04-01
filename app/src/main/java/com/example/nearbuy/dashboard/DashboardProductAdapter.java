package com.example.nearbuy.dashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nearbuy.R;
import com.example.nearbuy.data.model.Product;

import java.util.List;

/**
 * DashboardProductAdapter – horizontal RecyclerView adapter for the
 * "Products" section on the Dashboard.
 *
 * Displays products fetched from NearBuyHQ admin app shops.
 * Tapping a product card navigates to ProductDetailsActivity.
 */
public class DashboardProductAdapter
        extends RecyclerView.Adapter<DashboardProductAdapter.ViewHolder> {

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    private final List<Product>           products;
    private final OnProductClickListener  listener;

    public DashboardProductAdapter(List<Product> products, OnProductClickListener listener) {
        this.products = products;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dashboard_product, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product p = products.get(position);

        // Pick an emoji based on category
        holder.tvEmoji.setText(emojiForCategory(p.getCategory()));
        holder.tvName.setText(p.getName() != null ? p.getName() : "");
        holder.tvPrice.setText(p.getPrice() > 0
                ? String.format("Rs. %.0f", p.getPrice()) : "");

        String shopLabel = p.getShopName() != null ? p.getShopName() : "";
        if (p.hasDistance()) shopLabel += " · " + p.getDistanceLabel();
        holder.tvShop.setText(shopLabel);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onProductClick(p);
        });
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    /** Returns a suitable emoji for a product category. */
    private String emojiForCategory(String cat) {
        if (cat == null || cat.isEmpty()) return "🛒";
        switch (cat.toLowerCase()) {
            case "fruits":      return "🍎";
            case "vegetables":  return "🥦";
            case "dairy":       return "🥛";
            case "bakery":      return "🍞";
            case "meat":        return "🥩";
            case "seafood":     return "🐟";
            case "beverages":   return "🥤";
            case "snacks":      return "🍪";
            case "grains":
            case "rice":        return "🌾";
            case "spices":      return "🌶️";
            default:            return "🛒";
        }
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvEmoji;
        final TextView tvName;
        final TextView tvPrice;
        final TextView tvShop;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmoji = itemView.findViewById(R.id.tvProductEmoji);
            tvName  = itemView.findViewById(R.id.tvProductName);
            tvPrice = itemView.findViewById(R.id.tvProductPrice);
            tvShop  = itemView.findViewById(R.id.tvProductShop);
        }
    }
}

