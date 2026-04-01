package com.example.nearbuy.search;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nearbuy.R;
import com.example.nearbuy.product.ProductDetailsActivity;

import java.util.List;

/**
 * SearchListAdapter – RecyclerView adapter for the vertical product list
 * shown in SearchActivity.  Uses item_search_result layout (horizontal row card).
 * Tapping an item opens ProductDetailsActivity with full Firestore IDs so the
 * product can be loaded completely.
 */
public class SearchGridAdapter extends RecyclerView.Adapter<SearchGridAdapter.ViewHolder> {

    private final Context                context;
    private final List<SearchResultItem> items;

    public SearchGridAdapter(Context context, List<SearchResultItem> items) {
        this.context = context;
        this.items   = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_search_result, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        SearchResultItem item = items.get(position);

        h.tvEmoji.setText(item.getEmoji());
        h.tvName.setText(item.getProductName());
        h.tvShop.setText(item.getShopName());
        h.tvDistance.setText(item.getDistanceLabel());
        h.tvCategory.setText(item.getCategory());
        h.tvPrice.setText(item.getPrice());

        // Strikethrough original price; hide if no discount
        String origPrice = item.getOriginalPrice();
        if (origPrice != null && !origPrice.isEmpty()) {
            h.tvOriginal.setVisibility(View.VISIBLE);
            h.tvOriginal.setText(origPrice);
            h.tvOriginal.setPaintFlags(h.tvOriginal.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            h.tvOriginal.setVisibility(View.GONE);
        }

        h.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProductDetailsActivity.class);
            // Pass Firestore IDs so ProductDetailsActivity loads full data
            intent.putExtra(ProductDetailsActivity.EXTRA_SHOP_ID,       item.getShopId());
            intent.putExtra(ProductDetailsActivity.EXTRA_PRODUCT_ID,    item.getProductId());
            // Fallback extras shown immediately while Firestore loads
            intent.putExtra(ProductDetailsActivity.EXTRA_EMOJI,          item.getEmoji());
            intent.putExtra(ProductDetailsActivity.EXTRA_NAME,            item.getProductName());
            intent.putExtra(ProductDetailsActivity.EXTRA_SHOP_NAME,       item.getShopName());
            intent.putExtra(ProductDetailsActivity.EXTRA_PRICE,           item.getPrice());
            intent.putExtra(ProductDetailsActivity.EXTRA_ORIGINAL_PRICE,  item.getOriginalPrice());
            intent.putExtra(ProductDetailsActivity.EXTRA_DISTANCE,        item.getDistanceLabel());
            intent.putExtra(ProductDetailsActivity.EXTRA_CATEGORY,        item.getCategory());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() { return items == null ? 0 : items.size(); }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvEmoji, tvName, tvShop, tvDistance, tvCategory, tvPrice, tvOriginal;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmoji    = itemView.findViewById(R.id.tvProductEmoji);
            tvName     = itemView.findViewById(R.id.tvProductName);
            tvShop     = itemView.findViewById(R.id.tvShopName);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvPrice    = itemView.findViewById(R.id.tvPrice);
            tvOriginal = itemView.findViewById(R.id.tvOriginalPrice);
        }
    }
}
