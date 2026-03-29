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
 * SearchGridAdapter – RecyclerView adapter for the 2-column product grid
 * shown in SearchActivity. Tapping an item opens ProductDetailsActivity.
 */
public class SearchGridAdapter extends RecyclerView.Adapter<SearchGridAdapter.ViewHolder> {

    private final Context               context;
    private final List<SearchResultItem> items;

    public SearchGridAdapter(Context context, List<SearchResultItem> items) {
        this.context = context;
        this.items   = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_search_grid, parent, false);
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
        h.tvOriginal.setText(item.getOriginalPrice());
        h.tvOriginal.setPaintFlags(h.tvOriginal.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        h.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProductDetailsActivity.class);
            intent.putExtra(ProductDetailsActivity.EXTRA_EMOJI,           item.getEmoji());
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
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmoji, tvName, tvShop, tvDistance, tvCategory, tvPrice, tvOriginal;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmoji    = itemView.findViewById(R.id.tvGridEmoji);
            tvName     = itemView.findViewById(R.id.tvGridName);
            tvShop     = itemView.findViewById(R.id.tvGridShop);
            tvDistance = itemView.findViewById(R.id.tvGridDistance);
            tvCategory = itemView.findViewById(R.id.tvGridCategory);
            tvPrice    = itemView.findViewById(R.id.tvGridPrice);
            tvOriginal = itemView.findViewById(R.id.tvGridOriginalPrice);
        }
    }
}

