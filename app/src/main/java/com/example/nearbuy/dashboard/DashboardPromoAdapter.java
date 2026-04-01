package com.example.nearbuy.dashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nearbuy.R;
import com.example.nearbuy.data.model.DealItem;

import java.util.List;

/**
 * DashboardPromoAdapter – horizontal RecyclerView adapter for the
 * "Active Promotions" section on the Dashboard.
 * Displays the latest 5 active promotions as "Latest Deals" style cards
 * with random NearBuy theme-coloured gradient backgrounds.
 */
public class DashboardPromoAdapter
        extends RecyclerView.Adapter<DashboardPromoAdapter.ViewHolder> {

    // ── Random promo emojis (stable within session) ───────────────────────────
    private static final String[] PROMO_EMOJIS = {
        "🎁", "🏷️", "💳", "🎊", "🥳", "💰", "🎉", "✨", "🌟", "🎀", "🛍️", "🎯",
        "🏅", "🎪", "🪄", "🎶", "🍀", "🌺", "💝", "🪙"
    };

    // ── Theme-coloured card backgrounds (NearBuy teal + orange palette) ───────
    private static final int[] PROMO_CARD_BACKGROUNDS = {
        R.drawable.bg_promo_deal_card_1,   // dark teal   #006064 → #0097A7
        R.drawable.bg_promo_deal_card_2,   // teal        #0097A7 → #26C6DA
        R.drawable.bg_promo_deal_card_3,   // orange      #E65100 → #FFA726
        R.drawable.bg_promo_deal_card_4,   // deep teal   #004D40 → #00838F
        R.drawable.bg_promo_deal_card_5    // teal-mid    #00838F → #4DB6AC
    };

    public interface OnPromoClickListener {
        void onPromoClick(DealItem promo);
    }

    private final List<DealItem>       promos;
    private final OnPromoClickListener listener;

    public DashboardPromoAdapter(List<DealItem> promos, OnPromoClickListener listener) {
        this.promos   = promos;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Reuse the same deal card layout for a consistent "Latest Deals" look
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dashboard_deal, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DealItem promo = promos.get(position);

        String title = promo.getTitle() != null ? promo.getTitle() : "";

        // Stable random emoji based on title hash
        int emojiIndex = Math.abs((title + position).hashCode()) % PROMO_EMOJIS.length;
        if (holder.tvEmoji != null) holder.tvEmoji.setText(PROMO_EMOJIS[emojiIndex]);

        // Discount badge
        holder.tvDiscount.setText(
                promo.getDiscountLabel() != null ? promo.getDiscountLabel() : "PROMO");

        // Title
        holder.tvTitle.setText(title);

        // Description: use occasion, fall back to description text, then valid date
        String desc = promo.getOccasion() != null ? promo.getOccasion().trim() : "";
        if (desc.isEmpty()) {
            desc = promo.getDescription() != null ? promo.getDescription().trim() : "";
        }
        if (holder.tvDescription != null) {
            holder.tvDescription.setText(desc.isEmpty() ? promo.getValidDateLabel() : desc);
        }

        // Shop + valid date row
        String shopDist = promo.getShopName() != null ? promo.getShopName() : "";
        String validDate = promo.getValidDateLabel();
        if (!validDate.isEmpty()) {
            shopDist = shopDist.isEmpty() ? validDate : shopDist + " · " + validDate;
        }
        holder.tvShopDist.setText(shopDist);

        // Apply a random theme-coloured background (cycles through 5 palette options)
        int bgIndex = Math.abs((title + position).hashCode()) % PROMO_CARD_BACKGROUNDS.length;
        holder.itemView.setBackgroundResource(PROMO_CARD_BACKGROUNDS[bgIndex]);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onPromoClick(promo);
        });
    }

    @Override
    public int getItemCount() {
        return promos.size();
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvEmoji;
        final TextView tvDiscount;
        final TextView tvTitle;
        final TextView tvDescription;
        final TextView tvShopDist;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmoji       = itemView.findViewById(R.id.tvDealEmoji);
            tvDiscount    = itemView.findViewById(R.id.tvDealDiscount);
            tvTitle       = itemView.findViewById(R.id.tvDealTitle);
            tvDescription = itemView.findViewById(R.id.tvDealDescription);
            tvShopDist    = itemView.findViewById(R.id.tvDealShopDist);
        }
    }
}

