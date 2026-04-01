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
 * DashboardPromoAdapter – vertical RecyclerView adapter for the
 * "Active Promotions" section on the Dashboard.
 */
public class DashboardPromoAdapter
        extends RecyclerView.Adapter<DashboardPromoAdapter.ViewHolder> {

    // ── Random promo icons (assigned per item, stable within session) ──────────
    private static final String[] PROMO_EMOJIS = {
        "🎁", "🏷️", "💳", "🎊", "🥳", "💰", "🎉", "✨", "🌟", "🎀", "🛍️", "🎯",
        "🏅", "🎪", "🪄", "🎶", "🍀", "🌺", "💝", "🪙"
    };

    // ── Random background colors to rotate through ─────────────────────────────
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

        holder.tvTitle.setText(promo.getTitle() != null ? promo.getTitle() : "");
        holder.tvDesc.setText(promo.getDescription() != null ? promo.getDescription() : "");

        // Assign a stable random emoji icon based on title hash
        String title = promo.getTitle() != null ? promo.getTitle() : "";
        int emojiIndex = Math.abs((title + position).hashCode()) % PROMO_EMOJIS.length;
        int bgIndex    = Math.abs((title + position).hashCode()) % ICON_BACKGROUNDS.length;
        if (holder.tvIcon != null) {
            holder.tvIcon.setText(PROMO_EMOJIS[emojiIndex]);
            holder.tvIcon.setBackgroundResource(ICON_BACKGROUNDS[bgIndex]);
        }

        // Human-readable expiry label
        int days = promo.daysUntilExpiry();
        if (days < 0) {
            holder.tvExpiry.setText("Expired");
        } else if (days == 0) {
            holder.tvExpiry.setText("Today only");
        } else if (days == 1) {
            holder.tvExpiry.setText("1 day left");
        } else {
            holder.tvExpiry.setText(days + " days left");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onPromoClick(promo);
        });
    }

    @Override
    public int getItemCount() {
        return promos.size();
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvIcon;
        final TextView tvTitle;
        final TextView tvDesc;
        final TextView tvExpiry;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIcon   = itemView.findViewById(R.id.tvPromoIcon);
            tvTitle  = itemView.findViewById(R.id.tvPromoTitle);
            tvDesc   = itemView.findViewById(R.id.tvPromoDesc);
            tvExpiry = itemView.findViewById(R.id.tvPromoExpiry);
        }
    }
}

