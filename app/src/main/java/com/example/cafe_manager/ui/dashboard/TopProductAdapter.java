package com.example.cafe_manager.ui.dashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.model.TopProductRow;
import com.example.cafe_manager.util.CurrencyUtils;

import java.util.ArrayList;
import java.util.List;

public class TopProductAdapter extends RecyclerView.Adapter<TopProductAdapter.RankVH> {

    private final List<TopProductRow> items = new ArrayList<>();

    public void submitList(List<TopProductRow> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RankVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_top_product, parent, false);
        return new RankVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RankVH holder, int position) {
        holder.bind(position + 1, items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class RankVH extends RecyclerView.ViewHolder {
        private final TextView tvRank;
        private final TextView tvName;
        private final TextView tvQty;
        private final TextView tvRevenue;

        RankVH(View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tv_rank);
            tvName = itemView.findViewById(R.id.tv_product_name);
            tvQty = itemView.findViewById(R.id.tv_qty);
            tvRevenue = itemView.findViewById(R.id.tv_revenue);
        }

        void bind(int rank, TopProductRow row) {
            tvRank.setText(String.valueOf(rank));
            tvName.setText(row.productName != null ? row.productName : "—");
            tvQty.setText("Bán: " + row.totalQuantity);
            tvRevenue.setText(CurrencyUtils.formatVnd(row.totalRevenue));
        }
    }
}
