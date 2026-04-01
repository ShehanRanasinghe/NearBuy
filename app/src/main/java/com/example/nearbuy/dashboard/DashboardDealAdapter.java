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

    public interface OnDealClickListener {
        void onDealClick(DealItem deal);
    }

    private final List<DealItem>      deals;
    private final OnDealClickListener listener;

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

        holder.tvDiscount.setText(
                deal.getDiscountLabel() != null ? deal.getDiscountLabel() : "SALE");
        holder.tvTitle.setText(deal.getTitle() != null ? deal.getTitle() : "");

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

    static final class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvDiscount;
        final TextView tvTitle;
        final TextView tvShopDist;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDiscount = itemView.findViewById(R.id.tvDealDiscount);
            tvTitle    = itemView.findViewById(R.id.tvDealTitle);
            tvShopDist = itemView.findViewById(R.id.tvDealShopDist);
        }
    }
}

