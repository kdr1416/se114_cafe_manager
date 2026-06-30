package com.example.cafe_manager.ui.table;

import android.view.LayoutInflater;

import android.view.View;

import android.view.ViewGroup;

import android.widget.TextView;

import androidx.annotation.NonNull;

import androidx.recyclerview.widget.DiffUtil;

import androidx.recyclerview.widget.ListAdapter;

import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;

import com.example.cafe_manager.data.local.entity.TableEntity;

import com.example.cafe_manager.util.Constants;

import com.example.cafe_manager.util.StatusUtils;

public class TableAdapter extends ListAdapter<TableEntity, TableAdapter.TableVH> {

    public interface OnTableClickListener {

        void onTableClick(TableEntity table);

    }

    private final OnTableClickListener listener;

    public TableAdapter(OnTableClickListener listener) {

        super(DIFF);

        this.listener = listener;

    }

    @NonNull

    @Override

    public TableVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())

                .inflate(R.layout.item_table, parent, false);

        return new TableVH(view);

    }

    @Override

    public void onBindViewHolder(@NonNull TableVH holder, int position) {

        holder.bind(getItem(position), listener);

    }

    static class TableVH extends RecyclerView.ViewHolder {

        private final TextView tvName;

        private final TextView tvMeta;

        private final TextView tvHint;

        private final TextView tvBadge;

        TableVH(View itemView) {

            super(itemView);

            tvName = itemView.findViewById(R.id.tv_table_name);

            tvMeta = itemView.findViewById(R.id.tv_table_meta);

            tvHint = itemView.findViewById(R.id.tv_table_hint);

            tvBadge = itemView.findViewById(R.id.tv_status_badge);

        }

        void bind(final TableEntity table, final OnTableClickListener listener) {

            tvName.setText(table.getTableName());

            tvBadge.setText(StatusUtils.getDisplayName(table.getStatus()));

            boolean isEmpty = Constants.TABLE_EMPTY.equals(table.getStatus());

            int badgeBg = isEmpty ? R.drawable.bg_badge_success : R.drawable.bg_badge_warning;

            int badgeColor = isEmpty ? R.color.success : R.color.warning;

            tvBadge.setBackgroundResource(badgeBg);

            tvBadge.setTextColor(itemView.getContext().getColor(badgeColor));

            String areaName = table.getArea();
            if (areaName == null || areaName.trim().isEmpty()) {
                areaName = "Chưa xếp khu vực";
            }
            String statusDesc = isEmpty ? "Trống" : "Có khách";
            tvMeta.setText(areaName + " · " + statusDesc + " · " + table.getCapacity() + " người");

            tvHint.setText(isEmpty

                    ? itemView.getContext().getString(R.string.hint_tap_to_order)

                    : itemView.getContext().getString(R.string.hint_tap_to_add));

            itemView.setOnClickListener(v -> {

                if (listener != null) listener.onTableClick(table);

            });

        }

    }

    private static final DiffUtil.ItemCallback<TableEntity> DIFF =

            new DiffUtil.ItemCallback<TableEntity>() {

                @Override

                public boolean areItemsTheSame(@NonNull TableEntity o, @NonNull TableEntity n) {

                    return o.getTableId() == n.getTableId();

                }

                @Override

                public boolean areContentsTheSame(@NonNull TableEntity o, @NonNull TableEntity n) {

                    return o.getStatus().equals(n.getStatus())

                            && o.getTableName().equals(n.getTableName());

                }

            };

}
