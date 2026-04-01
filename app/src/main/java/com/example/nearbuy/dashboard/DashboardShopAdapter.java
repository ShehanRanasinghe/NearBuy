package com.example.nearbuy.dashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nearbuy.R;
import com.example.nearbuy.data.model.Shop;

import java.util.List;

/**
 * DashboardShopAdapter – vertical RecyclerView adapter for the
 * "Nearby Shops" section on the Dashboard.
 *
 * Replaces five hardcoded static shop cards from activity_dashboard.xml.
 * RecyclerView recycles item views so only the visible ones are in the
 * view hierarchy at any time, keeping performTraversals() cheap.
 */
public class DashboardShopAdapter
        extends RecyclerView.Adapter<DashboardShopAdapter.ViewHolder> {

    public interface OnShopClickListener {
        void onShopClick(Shop shop);
    }

    private final List<Shop>          shops;
    private final OnShopClickListener listener;

    public DashboardShopAdapter(List<Shop> shops, OnShopClickListener listener) {
        this.shops    = shops;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dashboard_shop, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Shop shop = shops.get(position);

        holder.tvName.setText(shop.getName() != null ? shop.getName() : "");

        String distStatus = "📍 " + shop.getDistanceLabel() + "  •  "
                + (shop.getOpeningHours() != null && !shop.getOpeningHours().isEmpty()
                ? shop.getOpeningHours() : "Open");
        holder.tvDistStatus.setText(distStatus);

        // The badge shows the distance until a deal count can be loaded from Firestore
        holder.tvDeals.setText(shop.getDistanceLabel());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onShopClick(shop);
        });
    }

    @Override
    public int getItemCount() {
        return shops.size();
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName;
        final TextView tvDistStatus;
        final TextView tvDeals;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName       = itemView.findViewById(R.id.tvShopName);
            tvDistStatus = itemView.findViewById(R.id.tvShopDistStatus);
            tvDeals      = itemView.findViewById(R.id.tvShopDeals);
        }
    }
}

