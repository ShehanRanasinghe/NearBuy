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
 * DashboardDealAdapter – horizontal RecyclerView adapter for the
 * "Latest Deals" section on the Dashboard.
 *
 * Replaces the five hardcoded static deal cards that were baked into
 * activity_dashboard.xml.  RecyclerView only inflates views that are
 * actually visible on screen, drastically reducing the view tree size
 * and eliminating the JIT over-allocation warning:
 *   "Compiler allocated 5087KB to compile ViewRootImpl.performTraversals()"
 */
public class DashboardDealAdapter
        extends RecyclerView.Adapter<DashboardDealAdapter.ViewHolder> {

    // ── Random deal emojis (assigned per deal, stable within session) ──────────
    private static final String[] DEAL_EMOJIS = {
        "🔥", "💥", "⚡", "🎯", "✨", "💫", "🌟", "🏷️", "🎁", "💰", "🎉", "🛒",
        "🍎", "🥦", "🥛", "🍞", "🧃", "🍗", "🎀", "💎", "🌈", "🎪"
    };

    /** Callback fired when the customer taps a deal card. */
    public interface OnDealClickListener {
        void onDealClick(DealItem deal);
    }

    private final List<DealItem>      deals;
    private final OnDealClickListener listener;

    /** Creates the adapter with the list of deals and a tap callback. */
    public DashboardDealAdapter(List<DealItem> deals, OnDealClickListener listener) {
        this.deals    = deals;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dashboard_deal, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DealItem deal = deals.get(position);

        // Pick a stable random emoji based on the deal title hash
        String title = deal.getTitle() != null ? deal.getTitle() : "";
        int emojiIndex = Math.abs((title + position).hashCode()) % DEAL_EMOJIS.length;
        if (holder.tvEmoji != null) holder.tvEmoji.setText(DEAL_EMOJIS[emojiIndex]);

        holder.tvDiscount.setText(
                deal.getDiscountLabel() != null ? deal.getDiscountLabel() : "SALE");
        holder.tvTitle.setText(deal.getTitle() != null ? deal.getTitle() : "");

        // Show description snippet; fall back to "Valid until dd MMM yyyy" (or "No expiry")
        String desc = deal.getDescription() != null ? deal.getDescription().trim() : "";
        if (holder.tvDescription != null) {
            holder.tvDescription.setText(desc.isEmpty() ? deal.getValidDateLabel() : desc);
        }

        // Build "ShopName · distance" label
        String shopDist = deal.getShopName() != null ? deal.getShopName() : "";
        if (deal.hasDistance()) shopDist += " · " + deal.getDistanceLabel();
        holder.tvShopDist.setText(shopDist);

        // Alternate card background colours for visual variety
        holder.itemView.setBackgroundResource(
                position % 2 == 0
                        ? R.drawable.bg_deal_card
                        : R.drawable.bg_promo_card_teal);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onDealClick(deal);
        });
    }

    @Override
    public int getItemCount() {
        return deals.size();
    }

    /** ViewHolder caches all view references for a single deal card. */
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

