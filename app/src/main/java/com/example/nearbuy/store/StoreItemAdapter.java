package com.example.nearbuy.store;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nearbuy.R;

import java.util.List;

/**
 * StoreItemAdapter – generic RecyclerView adapter for displaying rows of
 * products, deals, or promotions inside StoreDetailsActivity.
 *
 * Each row shows:  emoji | title + subtitle | badge
 */
public class StoreItemAdapter
        extends RecyclerView.Adapter<StoreItemAdapter.ViewHolder> {

    /** Simple data-only row model used by this adapter. */
    public static class RowItem {
        public final String emoji;
        public final String title;
        public final String subtitle;
        public final String badge;
        public final int    badgeColor; // resolved color int

        public RowItem(String emoji, String title, String subtitle,
                       String badge, int badgeColor) {
            this.emoji      = emoji;
            this.title      = title;
            this.subtitle   = subtitle;
            this.badge      = badge;
            this.badgeColor = badgeColor;
        }
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    private final List<RowItem>      items;
    private final OnItemClickListener listener;

    public StoreItemAdapter(List<RowItem> items, OnItemClickListener listener) {
        this.items    = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_store_row, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RowItem row = items.get(position);
        holder.tvEmoji.setText(row.emoji != null    ? row.emoji    : "🛒");
        holder.tvTitle.setText(row.title != null    ? row.title    : "");
        holder.tvSubtitle.setText(row.subtitle != null ? row.subtitle : "");
        holder.tvBadge.setText(row.badge != null    ? row.badge    : "");
        if (row.badgeColor != 0) holder.tvBadge.setTextColor(row.badgeColor);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(position);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvEmoji;
        final TextView tvTitle;
        final TextView tvSubtitle;
        final TextView tvBadge;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmoji    = itemView.findViewById(R.id.tvRowEmoji);
            tvTitle    = itemView.findViewById(R.id.tvRowTitle);
            tvSubtitle = itemView.findViewById(R.id.tvRowSubtitle);
            tvBadge    = itemView.findViewById(R.id.tvRowBadge);
        }
    }
}

