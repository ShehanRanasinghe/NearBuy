package com.example.nearbuy.discounts;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nearbuy.R;

import java.util.List;

/**
 * SavedDealsAdapter – RecyclerView adapter that displays the customer's bookmarked deals.
 * Each row shows the deal emoji, name, shop, discount badge, prices, expiry, and distance.
 * Tapping a row fires onItemClick; tapping the remove button fires onRemoveClick.
 */
public class SavedDealsAdapter extends RecyclerView.Adapter<SavedDealsAdapter.ViewHolder> {

    /** Callback interface for row-level user actions. */
    public interface OnItemClickListener {
        /** Called when the customer taps a deal row to view its details. */
        void onItemClick(SavedDealItem item);
        /** Called when the customer taps the remove button on a deal row. */
        void onRemoveClick(SavedDealItem item, int position);
    }

    private final List<SavedDealItem> items;
    private final Context context;
    private OnItemClickListener listener;

    /** Creates the adapter with a context and the list of saved deals to display. */
    public SavedDealsAdapter(Context context, List<SavedDealItem> items) {
        this.context = context;
        this.items   = items;
    }

    /** Registers the listener that receives item and remove click events. */
    public void setOnItemClickListener(OnItemClickListener l) { this.listener = l; }

    /** Removes the item at the given position and notifies the RecyclerView to animate it out. */
    public void removeItem(int position) {
        items.remove(position);
        notifyItemRemoved(position);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the saved-deal row card layout
        View v = LayoutInflater.from(context).inflate(R.layout.item_saved_deal, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        SavedDealItem item = items.get(position);

        // Populate all visible fields for this deal row
        h.tvEmoji.setText(item.getEmoji());
        h.tvDealName.setText(item.getDealName());
        h.tvShopName.setText(item.getShopName());
        h.tvDiscountBadge.setText(item.getDiscountLabel());
        h.tvDealPrice.setText(item.getDealPrice());
        h.tvOriginalPrice.setText(item.getOriginalPrice());
        // Strike through the original price to highlight the saving
        h.tvOriginalPrice.setPaintFlags(h.tvOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        h.tvExpiry.setText(item.getExpiryLabel());
        h.tvDistance.setText(item.getDistanceLabel());

        // Row tap opens deal details; remove button unsaves the deal
        h.itemView.setOnClickListener(v -> { if (listener != null) listener.onItemClick(item); });
        h.btnRemove.setOnClickListener(v -> {
            if (listener != null) listener.onRemoveClick(item, h.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    /** ViewHolder that caches view references to avoid repeated findViewById calls. */
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmoji, tvDealName, tvShopName, tvDiscountBadge;
        TextView tvDealPrice, tvOriginalPrice, tvExpiry, tvDistance;
        View btnRemove;

        ViewHolder(View v) {
            super(v);
            tvEmoji         = v.findViewById(R.id.tv_deal_emoji);
            tvDealName      = v.findViewById(R.id.tv_deal_name);
            tvShopName      = v.findViewById(R.id.tv_shop_name);
            tvDiscountBadge = v.findViewById(R.id.tv_discount_badge);
            tvDealPrice     = v.findViewById(R.id.tv_deal_price);
            tvOriginalPrice = v.findViewById(R.id.tv_original_price);
            tvExpiry        = v.findViewById(R.id.tv_expiry);
            tvDistance      = v.findViewById(R.id.tv_distance);
            btnRemove       = v.findViewById(R.id.btn_remove);
        }
    }
}
