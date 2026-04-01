package com.example.nearbuy.dashboard;

import android.graphics.Paint;
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
 * DashboardPromoAdapter – vertical RecyclerView adapter for the
 * "Active Promotions" section on the Dashboard.
 * Shows: promotion name, occasion, food category, valid date, original price,
 * discount label, and discounted sale price.
 */
public class DashboardPromoAdapter
        extends RecyclerView.Adapter<DashboardPromoAdapter.ViewHolder> {

    // ── Random promo icons (assigned per item, stable within session) ──────────
    private static final String[] PROMO_EMOJIS = {
        "🎁", "🏷️", "💳", "🎊", "🥳", "💰", "🎉", "✨", "🌟", "🎀", "🛍️", "🎯",
        "🏅", "🎪", "🪄", "🎶", "🍀", "🌺", "💝", "🪙"
    };

    private static final int[] ICON_BACKGROUNDS = {
        R.drawable.bg_icon_circle_orange,
        R.drawable.bg_icon_circle_teal,
        R.drawable.bg_icon_circle_blue,
        R.drawable.bg_icon_circle_light
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
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dashboard_promo, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DealItem promo = promos.get(position);

        String title = promo.getTitle() != null ? promo.getTitle() : "";
        int emojiIndex = Math.abs((title + position).hashCode()) % PROMO_EMOJIS.length;
        int bgIndex    = Math.abs((title + position).hashCode()) % ICON_BACKGROUNDS.length;

        // ── Icon ──────────────────────────────────────────────────────────────
        if (holder.tvIcon != null) {
            holder.tvIcon.setText(PROMO_EMOJIS[emojiIndex]);
            holder.tvIcon.setBackgroundResource(ICON_BACKGROUNDS[bgIndex]);
        }

        // ── Title ─────────────────────────────────────────────────────────────
        holder.tvTitle.setText(title);

        // ── Occasion (use description as fallback if occasion is empty) ───────
        String occasion = promo.getOccasion() != null ? promo.getOccasion().trim() : "";
        if (occasion.isEmpty()) {
            occasion = promo.getDescription() != null ? promo.getDescription().trim() : "";
        }
        if (holder.tvOccasion != null) {
            holder.tvOccasion.setVisibility(occasion.isEmpty() ? View.GONE : View.VISIBLE);
            holder.tvOccasion.setText(occasion);
        }

        // ── Discount label ────────────────────────────────────────────────────
        String badge = promo.getDiscountLabel() != null ? promo.getDiscountLabel().trim() : "";
        if (holder.tvDiscount != null) {
            holder.tvDiscount.setVisibility(badge.isEmpty() ? View.GONE : View.VISIBLE);
            holder.tvDiscount.setText(badge);
        }

        // ── Category chip ─────────────────────────────────────────────────────
        String category = promo.getCategory() != null ? promo.getCategory().trim() : "";
        if (holder.tvCategory != null) {
            if (category.isEmpty()) {
                holder.tvCategory.setVisibility(View.GONE);
            } else {
                holder.tvCategory.setVisibility(View.VISIBLE);
                holder.tvCategory.setText(categoryEmoji(category) + " " + category);
            }
        }

        // ── Valid date ────────────────────────────────────────────────────────
        if (holder.tvValidDate != null) {
            holder.tvValidDate.setText(promo.getValidDateLabel());
        }

        // ── Prices ────────────────────────────────────────────────────────────
        double origPrice = promo.getOriginalPrice();
        double salePrice = promo.getSalePrice();

        if (holder.tvOriginalPrice != null) {
            if (origPrice > 0) {
                holder.tvOriginalPrice.setVisibility(View.VISIBLE);
                holder.tvOriginalPrice.setText(String.format("Rs.%.0f", origPrice));
                // Strikethrough so it's clear this is the old price (no arrow needed)
                holder.tvOriginalPrice.setPaintFlags(
                        holder.tvOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                holder.tvOriginalPrice.setVisibility(View.GONE);
            }
        }

        if (holder.tvSalePrice != null) {
            if (salePrice > 0) {
                holder.tvSalePrice.setVisibility(View.VISIBLE);
                holder.tvSalePrice.setText(String.format("Rs.%.0f", salePrice));
            } else {
                holder.tvSalePrice.setVisibility(View.GONE);
            }
        }

        // ── Human-readable expiry — hide entirely when no expiry date set ─────
        int days = promo.daysUntilExpiry();
        if (holder.tvExpiry != null) {
            if (!promo.hasExpiry()) {
                // No expiry stored in Firestore – hide the countdown label entirely
                holder.tvExpiry.setVisibility(View.GONE);
            } else {
                holder.tvExpiry.setVisibility(View.VISIBLE);
                if (days < 0)       holder.tvExpiry.setText("Expired");
                else if (days == 0) holder.tvExpiry.setText("Today only!");
                else if (days == 1) holder.tvExpiry.setText("1 day left");
                else if (days <= 7) holder.tvExpiry.setText(days + " days left");
                else                holder.tvExpiry.setText(days / 7 + " weeks left");
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onPromoClick(promo);
        });
    }

    @Override
    public int getItemCount() {
        return promos.size();
    }

    /** Maps a category string to a representative emoji prefix. */
    private static String categoryEmoji(String category) {
        switch (category.toLowerCase(java.util.Locale.ROOT)) {
            case "fruits":      return "🍎";
            case "vegetables":  return "🥦";
            case "food":        return "🍽️";
            case "dairy":       return "🥛";
            case "bakery":      return "🍞";
            case "beverages":   return "🧃";
            case "snacks":      return "🍟";
            case "meat":        return "🍗";
            case "seafood":     return "🐟";
            case "household":   return "🧹";
            case "groceries":   return "🛒";
            default:            return "🏷️";
        }
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvIcon;
        final TextView tvTitle;
        final TextView tvOccasion;
        final TextView tvDiscount;
        final TextView tvCategory;
        final TextView tvValidDate;
        final TextView tvOriginalPrice;
        final TextView tvSalePrice;
        final TextView tvExpiry;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIcon          = itemView.findViewById(R.id.tvPromoIcon);
            tvTitle         = itemView.findViewById(R.id.tvPromoTitle);
            tvOccasion      = itemView.findViewById(R.id.tvPromoOccasion);
            tvDiscount      = itemView.findViewById(R.id.tvPromoDiscount);
            tvCategory      = itemView.findViewById(R.id.tvPromoCategory);
            tvValidDate     = itemView.findViewById(R.id.tvPromoValidDate);
            tvOriginalPrice = itemView.findViewById(R.id.tvPromoOriginalPrice);
            tvSalePrice     = itemView.findViewById(R.id.tvPromoSalePrice);
            tvExpiry        = itemView.findViewById(R.id.tvPromoExpiry);
        }
    }
}

