package com.example.nearbuy.discounts;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nearbuy.R;

import java.util.List;

/**
 * DealPromoAdapter – RecyclerView adapter for the DealsAndPromoActivity.
 * Renders both deal and promotion items using item_deal_promo layout.
 */
public class DealPromoAdapter extends RecyclerView.Adapter<DealPromoAdapter.ViewHolder> {

    private final Context             context;
    private final List<DealPromoItem> items;

    public DealPromoAdapter(Context context, List<DealPromoItem> items) {
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
        DealPromoItem item = items.get(position);

        h.tvEmoji.setText(item.getEmoji());
        h.tvTitle.setText(item.getTitle());
        h.tvShop.setText(item.getShopName());
        h.tvDescription.setText(item.getDescription());
        h.tvBadge.setText(item.getBadge());
        h.tvValidity.setText(item.getValidity());

        // Colour the emoji circle differently for promos vs deals
        if (item.getType() == DealPromoItem.TYPE_PROMO) {
            h.emojiCircle.setBackgroundResource(R.drawable.bg_icon_circle_orange);
        } else {
            h.emojiCircle.setBackgroundResource(R.drawable.bg_icon_circle_teal);
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView    tvEmoji, tvTitle, tvShop, tvDescription, tvBadge, tvValidity;
        LinearLayout emojiCircle;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmoji      = itemView.findViewById(R.id.tvItemEmoji);
            tvTitle      = itemView.findViewById(R.id.tvItemTitle);
            tvShop       = itemView.findViewById(R.id.tvItemShop);
            tvDescription= itemView.findViewById(R.id.tvItemDescription);
            tvBadge      = itemView.findViewById(R.id.tvItemBadge);
            tvValidity   = itemView.findViewById(R.id.tvItemValidity);
            emojiCircle  = itemView.findViewById(R.id.ivEmojiCircle);
        }
    }
}

