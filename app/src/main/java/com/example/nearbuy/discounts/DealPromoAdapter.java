package com.example.nearbuy.discounts;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nearbuy.R;
import com.example.nearbuy.data.model.DealItem;

import java.util.List;
import java.util.Locale;

/**
 * DealPromoAdapter – RecyclerView adapter for DealsAndPromoActivity.
 * Works directly with {@link DealItem} so every Firestore field is available
 * (title, description, category, occasion, originalPrice, salePrice,
 *  discountLabel, shopName, distance, expiry / valid-until date).
 *
 * Fields shown per type:
 *   DEAL  → emoji, title, discountBadge, shop+distance, description,
 *            expiryLabel, prices (if > 0)
 *   PROMO → emoji, title, occasion, discountBadge, shop+distance,
 *            categoryChip, validDateLabel, description, prices (if > 0)
 */
public class DealPromoAdapter extends RecyclerView.Adapter<DealPromoAdapter.ViewHolder> {

    // ── Category → emoji mapping ───────────────────────────────────────────────
    private static final String[] DEAL_EMOJIS = {
        "🔥","💥","⚡","🎯","✨","💫","🌟","🏷️","🎁","💰","🎉","🛒",
        "🍎","🥦","🥛","🍞","🧃","🍗","🎀","💎"
    };
    private static final String[] PROMO_EMOJIS = {
        "🎁","🏷️","💳","🎊","🥳","💰","🎉","✨","🌟","🎀","🛍️","🎯",
        "🏅","🎪","🪄","🎶","🍀","🌺","💝","🪙"
    };

    private final Context        context;
    private final List<DealItem> items;

    public DealPromoAdapter(Context context, List<DealItem> items) {
        this.context = context;
        this.items   = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_deal_promo, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        DealItem item = items.get(position);
        boolean  isPromo = item.isPromotion();

        // ── Emoji circle colour ────────────────────────────────────────────────
        h.emojiCircle.setBackgroundResource(isPromo
                ? R.drawable.bg_icon_circle_orange
                : R.drawable.bg_icon_circle_teal);

        // ── Emoji glyph ───────────────────────────────────────────────────────
        String[] pool = isPromo ? PROMO_EMOJIS : DEAL_EMOJIS;
        String   title = item.getTitle() != null ? item.getTitle() : "";
        int emojiIdx = Math.abs((title + position).hashCode()) % pool.length;
        // Prefer category-based emoji if we have a recognised category
        String catEmoji = categoryToEmoji(item.getCategory());
        h.tvEmoji.setText(!catEmoji.equals("🏷️") ? catEmoji : pool[emojiIdx]);

        // ── Title ─────────────────────────────────────────────────────────────
        h.tvTitle.setText(title);

        // ── Occasion (promos only) ────────────────────────────────────────────
        String occasion = item.getOccasion() != null ? item.getOccasion().trim() : "";
        if (occasion.isEmpty() && isPromo) {
            // fall back to description snippet when no occasion set
            occasion = item.getDescription() != null
                    ? item.getDescription().trim() : "";
        }
        if (h.tvOccasion != null) {
            if (!occasion.isEmpty() && isPromo) {
                h.tvOccasion.setVisibility(View.VISIBLE);
                h.tvOccasion.setText(occasion);
            } else {
                h.tvOccasion.setVisibility(View.GONE);
            }
        }

        // ── Discount / promo badge ────────────────────────────────────────────
        String badge = item.getDiscountLabel() != null ? item.getDiscountLabel().trim() : "";
        h.tvBadge.setText(badge.isEmpty() ? "SALE" : badge);

        // ── Shop name + distance ──────────────────────────────────────────────
        String shopLabel = item.getShopName() != null ? item.getShopName() : "";
        if (item.hasDistance()) shopLabel += " · " + item.getDistanceLabel();
        h.tvShop.setText(shopLabel);

        // ── Category chip (promos) ────────────────────────────────────────────
        String category = item.getCategory() != null ? item.getCategory().trim() : "";
        if (h.tvCategory != null) {
            if (!category.isEmpty() && isPromo) {
                h.tvCategory.setVisibility(View.VISIBLE);
                h.tvCategory.setText(categoryToEmoji(category) + " " + category);
            } else {
                h.tvCategory.setVisibility(View.GONE);
            }
        }

        // ── Validity / expiry ─────────────────────────────────────────────────
        if (isPromo) {
            // Promotions: show "Valid until dd MMM yyyy" or "No expiry"
            h.tvValidity.setText(item.getValidDateLabel());
        } else {
            // Deals: show friendly countdown or "No expiry"
            if (!item.hasExpiry()) {
                h.tvValidity.setText("No expiry");
            } else {
                int days = item.daysUntilExpiry();
                if (days < 0)       h.tvValidity.setText("Expired");
                else if (days == 0) h.tvValidity.setText("Today only!");
                else if (days == 1) h.tvValidity.setText("1 day left");
                else if (days <= 7) h.tvValidity.setText(days + " days left");
                else                h.tvValidity.setText(days / 7 + " weeks left");
            }
        }

        // ── Description (full text) ───────────────────────────────────────────
        String desc = item.getDescription() != null ? item.getDescription().trim() : "";
        h.tvDescription.setVisibility(desc.isEmpty() ? View.GONE : View.VISIBLE);
        h.tvDescription.setText(desc);

        // ── Prices (strikethrough on original, no arrow separator) ───────────
        double origPrice = item.getOriginalPrice();
        double salePrice = item.getSalePrice();
        if (origPrice > 0 || salePrice > 0) {
            if (h.layoutPrices != null) h.layoutPrices.setVisibility(View.VISIBLE);
            if (h.tvOriginalPrice != null) {
                if (origPrice > 0) {
                    h.tvOriginalPrice.setVisibility(View.VISIBLE);
                    h.tvOriginalPrice.setText(String.format(Locale.getDefault(), "Rs.%.0f", origPrice));
                    h.tvOriginalPrice.setPaintFlags(
                            h.tvOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                } else {
                    h.tvOriginalPrice.setVisibility(View.GONE);
                }
            }
            if (h.tvSalePrice != null) {
                if (salePrice > 0) {
                    h.tvSalePrice.setVisibility(View.VISIBLE);
                    h.tvSalePrice.setText(String.format(Locale.getDefault(), "Rs.%.0f", salePrice));
                } else {
                    h.tvSalePrice.setVisibility(View.GONE);
                }
            }
        } else {
            if (h.layoutPrices != null) h.layoutPrices.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private static String categoryToEmoji(String category) {
        if (category == null || category.isEmpty()) return "🏷️";
        switch (category.toLowerCase(Locale.ROOT)) {
            case "fruits":     return "🍎";
            case "vegetables": return "🥦";
            case "food":       return "🍽️";
            case "dairy":      return "🥛";
            case "bakery":     return "🍞";
            case "beverages":  return "🧃";
            case "snacks":     return "🍟";
            case "meat":       return "🍗";
            case "seafood":    return "🐟";
            case "household":  return "🧹";
            case "groceries":  return "🛒";
            case "promo":      return "🎁";
            default:           return "🏷️";
        }
    }

    // ── ViewHolder ─────────────────────────────────────────────────────────────
    static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout emojiCircle;
        LinearLayout layoutPrices;
        TextView     tvEmoji, tvTitle, tvOccasion, tvBadge;
        TextView     tvShop, tvCategory, tvValidity;
        TextView     tvDescription;
        TextView     tvOriginalPrice, tvSalePrice;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            emojiCircle      = itemView.findViewById(R.id.ivEmojiCircle);
            layoutPrices     = itemView.findViewById(R.id.layoutItemPrices);
            tvEmoji          = itemView.findViewById(R.id.tvItemEmoji);
            tvTitle          = itemView.findViewById(R.id.tvItemTitle);
            tvOccasion       = itemView.findViewById(R.id.tvItemOccasion);
            tvBadge          = itemView.findViewById(R.id.tvItemBadge);
            tvShop           = itemView.findViewById(R.id.tvItemShop);
            tvCategory       = itemView.findViewById(R.id.tvItemCategory);
            tvValidity       = itemView.findViewById(R.id.tvItemValidity);
            tvDescription    = itemView.findViewById(R.id.tvItemDescription);
            tvOriginalPrice  = itemView.findViewById(R.id.tvItemOriginalPrice);
            tvSalePrice      = itemView.findViewById(R.id.tvItemSalePrice);
        }
    }
}

