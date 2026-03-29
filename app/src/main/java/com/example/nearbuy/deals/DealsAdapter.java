package com.example.nearbuy.deals;

import android.content.Context;
import android.graphics.Paint;
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
 * ArrayAdapter for displaying Deal objects in the DealsActivity ListView.
 */
public class DealsAdapter extends ArrayAdapter<Deal> {

    public DealsAdapter(@NonNull Context context, @NonNull List<Deal> deals) {
        super(context, 0, deals);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_deal_card, parent, false);
        }

        Deal deal = getItem(position);
        if (deal == null) return convertView;

        TextView tvEmoji    = convertView.findViewById(R.id.tvDealEmoji);
        TextView tvTitle    = convertView.findViewById(R.id.tvDealTitle);
        TextView tvShop     = convertView.findViewById(R.id.tvDealShop);
        TextView tvDiscount = convertView.findViewById(R.id.tvDealDiscount);
        TextView tvPrice    = convertView.findViewById(R.id.tvDealPrice);
        TextView tvOriginal = convertView.findViewById(R.id.tvDealOriginalPrice);
        TextView tvDistance = convertView.findViewById(R.id.tvDealDistance);
        TextView tvExpiry   = convertView.findViewById(R.id.tvDealExpiry);
        TextView tvCategory = convertView.findViewById(R.id.tvDealCategory);

        tvEmoji.setText(deal.getEmoji());
        tvTitle.setText(deal.getTitle());
        tvShop.setText(deal.getShopName());
        tvDiscount.setText(deal.getDiscountLabel());
        tvPrice.setText(deal.getSalePrice());
        tvOriginal.setText(deal.getOriginalPrice());
        tvDistance.setText(deal.getDistanceLabel());
        tvExpiry.setText(deal.getExpiryLabel());
        tvCategory.setText(deal.getCategory());

        // Strike-through original price
        tvOriginal.setPaintFlags(tvOriginal.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        // Highlight expiry urgency
        if (deal.getExpiryDays() <= 1) {
            tvExpiry.setTextColor(getContext().getColor(R.color.stat_red));
        } else {
            tvExpiry.setTextColor(getContext().getColor(R.color.text_dark_hint));
        }

        return convertView;
    }
}

