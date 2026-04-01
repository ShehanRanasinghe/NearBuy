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
 */
public class DashboardShopAdapter
        extends RecyclerView.Adapter<DashboardShopAdapter.ViewHolder> {

    // ── Random shop icons (stable within session, assigned per shop) ───────────
    private static final String[] SHOP_EMOJIS = {
        "🏪", "🛒", "🏬", "🍎", "🥖", "🥛", "🍗", "🧴", "🍜", "🎂",
        "🌿", "🏢", "🍦", "🧁", "🛍️", "🥩", "🐟", "🍕", "☕", "🧃"
    };

    private static final int[] ICON_BACKGROUNDS = {
        R.drawable.bg_icon_circle_teal,
        R.drawable.bg_icon_circle_orange,
        R.drawable.bg_icon_circle_blue,
        R.drawable.bg_icon_circle_light
    };

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

        // Assign a stable random emoji icon based on shop name hash
        String name = shop.getName() != null ? shop.getName() : "";
        int emojiIndex = Math.abs((name + position).hashCode()) % SHOP_EMOJIS.length;
        int bgIndex    = Math.abs((name + position).hashCode()) % ICON_BACKGROUNDS.length;
        if (holder.tvIcon != null) {
            holder.tvIcon.setText(SHOP_EMOJIS[emojiIndex]);
            holder.tvIcon.setBackgroundResource(ICON_BACKGROUNDS[bgIndex]);
        }

        // The badge shows the distance
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
        final TextView tvIcon;
        final TextView tvName;
        final TextView tvDistStatus;
        final TextView tvDeals;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIcon       = itemView.findViewById(R.id.tvShopIcon);
            tvName       = itemView.findViewById(R.id.tvShopName);
            tvDistStatus = itemView.findViewById(R.id.tvShopDistStatus);
            tvDeals      = itemView.findViewById(R.id.tvShopDeals);
        }
    }
}
