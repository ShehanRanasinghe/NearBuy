package com.example.nearbuy.orders;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.nearbuy.R;

import java.util.List;

/**
 * ArrayAdapter for displayed the OrderItem objects in the OrdersActivity ListView.
 */
public class OrdersAdapter extends ArrayAdapter<OrderItem> {

    public OrdersAdapter(@NonNull Context context, @NonNull List<OrderItem> items) {
        super(context, 0, items);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_order, parent, false);
        }

        OrderItem item = getItem(position);
        if (item == null) return convertView;

        TextView tvEmoji    = convertView.findViewById(R.id.tvOrderShopEmoji);
        TextView tvShop     = convertView.findViewById(R.id.tvOrderShopName);
        TextView tvDate     = convertView.findViewById(R.id.tvOrderDate);
        TextView tvItems    = convertView.findViewById(R.id.tvOrderItems);
        TextView tvCount    = convertView.findViewById(R.id.tvOrderItemCount);
        TextView tvTotal    = convertView.findViewById(R.id.tvOrderTotal);
        TextView tvStatus   = convertView.findViewById(R.id.tvOrderStatus);
        TextView tvOrderId  = convertView.findViewById(R.id.tvOrderId);

        tvEmoji.setText(item.getShopEmoji());
        tvShop.setText(item.getShopName());
        tvDate.setText(item.getOrderDate());
        tvItems.setText(item.getItemsSummary());
        tvCount.setText(item.getItemCount() + " item" + (item.getItemCount() > 1 ? "s" : ""));
        tvTotal.setText(item.getTotalAmount());
        tvOrderId.setText("#" + item.getOrderId());

        // Status color coding
        String status = item.getStatus();
        tvStatus.setText(status);
        switch (status) {
            case "Delivered":
                tvStatus.setBackgroundResource(R.drawable.bg_status_delivered);
                tvStatus.setTextColor(Color.WHITE);
                break;
            case "Processing":
                tvStatus.setBackgroundResource(R.drawable.bg_badge_teal);
                tvStatus.setTextColor(Color.WHITE);
                break;
            case "Cancelled":
                tvStatus.setBackgroundResource(R.drawable.bg_badge_orange);
                tvStatus.setTextColor(Color.WHITE);
                break;
            default:
                tvStatus.setBackgroundResource(R.drawable.bg_distance_badge);
                tvStatus.setTextColor(getContext().getColor(R.color.nb_primary));
        }

        return convertView;
    }
}

