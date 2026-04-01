package com.example.nearbuy.orders;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nearbuy.R;

import java.util.List;

/**
 * RecyclerView.Adapter for displaying OrderItem objects in the OrdersActivity.
 */
public class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.ViewHolder> {

    public interface OnOrderClickListener {
        void onOrderClick(OrderItem order);
    }

    private List<OrderItem> items;
    private final OnOrderClickListener listener;

    public OrdersAdapter(@NonNull List<OrderItem> items, OnOrderClickListener listener) {
        this.items    = items;
        this.listener = listener;
    }

    /** Replace the data set and refresh the list. */
    public void setItems(@NonNull List<OrderItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OrderItem item = items.get(position);

        holder.tvEmoji.setText(item.getShopEmoji());
        holder.tvShop.setText(item.getShopName());
        holder.tvDate.setText(item.getOrderDate());
        holder.tvItems.setText(item.getItemsSummary());
        holder.tvCount.setText(item.getItemCount() + " item" + (item.getItemCount() > 1 ? "s" : ""));
        holder.tvTotal.setText(item.getTotalAmount());
        holder.tvOrderId.setText("#" + item.getOrderId());

        // Status color coding
        String status = item.getStatus();
        holder.tvStatus.setText(status);
        switch (status) {
            case "Delivered":
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_delivered);
                holder.tvStatus.setTextColor(Color.WHITE);
                break;
            case "Processing":
                holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_teal);
                holder.tvStatus.setTextColor(Color.WHITE);
                break;
            case "Cancelled":
                holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_orange);
                holder.tvStatus.setTextColor(Color.WHITE);
                break;
            default:
                holder.tvStatus.setBackgroundResource(R.drawable.bg_distance_badge);
                holder.tvStatus.setTextColor(
                        holder.itemView.getContext().getColor(R.color.nb_primary));
        }

        // Fulfillment type badge (if present)
        String fType = item.getFulfillmentType();
        if (holder.tvFulfillment != null) {
            if (fType != null && !fType.isEmpty()) {
                holder.tvFulfillment.setVisibility(View.VISIBLE);
                holder.tvFulfillment.setText(fType);
            } else {
                holder.tvFulfillment.setVisibility(View.GONE);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onOrderClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvEmoji, tvShop, tvDate, tvItems, tvCount, tvTotal, tvStatus, tvOrderId;
        final TextView tvFulfillment; // optional – may be null if not in item layout

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmoji       = itemView.findViewById(R.id.tvOrderShopEmoji);
            tvShop        = itemView.findViewById(R.id.tvOrderShopName);
            tvDate        = itemView.findViewById(R.id.tvOrderDate);
            tvItems       = itemView.findViewById(R.id.tvOrderItems);
            tvCount       = itemView.findViewById(R.id.tvOrderItemCount);
            tvTotal       = itemView.findViewById(R.id.tvOrderTotal);
            tvStatus      = itemView.findViewById(R.id.tvOrderStatus);
            tvOrderId     = itemView.findViewById(R.id.tvOrderId);
            tvFulfillment = itemView.findViewById(R.id.tvOrderFulfillment); // optional
        }
    }
}
