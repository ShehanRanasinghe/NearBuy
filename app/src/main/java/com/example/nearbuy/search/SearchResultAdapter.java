package com.example.nearbuy.search;
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
public class SearchResultAdapter extends ArrayAdapter<SearchResultItem> {
    public SearchResultAdapter(@NonNull Context context, @NonNull List<SearchResultItem> items) {
        super(context, 0, items);
    }
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_search_result, parent, false);
        }
        SearchResultItem item = getItem(position);
        if (item == null) return convertView;
        TextView tvEmoji    = convertView.findViewById(R.id.tvProductEmoji);
        TextView tvName     = convertView.findViewById(R.id.tvProductName);
        TextView tvShop     = convertView.findViewById(R.id.tvShopName);
        TextView tvDistance = convertView.findViewById(R.id.tvDistance);
        TextView tvCategory = convertView.findViewById(R.id.tvCategory);
        TextView tvPrice    = convertView.findViewById(R.id.tvPrice);
        TextView tvOriginal = convertView.findViewById(R.id.tvOriginalPrice);
        tvEmoji.setText(item.getEmoji());
        tvName.setText(item.getProductName());
        tvShop.setText(item.getShopName());
        tvDistance.setText(item.getDistanceLabel());
        tvCategory.setText(item.getCategory());
        tvPrice.setText(item.getPrice());
        tvOriginal.setText(item.getOriginalPrice());
        tvOriginal.setPaintFlags(tvOriginal.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        return convertView;
    }
}
