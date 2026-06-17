package com.example.cafe_manager.ui.table;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.data.local.entity.TableEntity;
import com.example.cafe_manager.util.Constants;

public class TableManageAdapter extends ListAdapter<TableEntity, TableManageAdapter.ViewHolder> {

    public interface OnActionListener {
        void onEdit(TableEntity table);
        void onDelete(TableEntity table);
    }

    private final OnActionListener listener;

    public TableManageAdapter(OnActionListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<TableEntity> DIFF =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull TableEntity a, @NonNull TableEntity b) {
                    return a.getTableId() == b.getTableId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull TableEntity a, @NonNull TableEntity b) {
                    return a.getTableName().equals(b.getTableName())
                            && a.getCapacity() == b.getCapacity()
                            && a.getStatus().equals(b.getStatus())
                            && ((a.getArea() == null && b.getArea() == null)
                            || (a.getArea() != null && a.getArea().equals(b.getArea())));
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_table_manage, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        TableEntity t = getItem(position);
        h.tvName.setText(t.getTableName());

        String meta = t.getCapacity() + " người";
        if (t.getArea() != null && !t.getArea().isEmpty()) {
            meta += " · " + t.getArea();
        }
        h.tvCapacity.setText(meta);

        boolean empty = Constants.TABLE_EMPTY.equals(t.getStatus());
        h.tvStatus.setText(empty ? "Trống" : "Có khách");
        h.tvStatus.setBackgroundResource(empty
                ? R.drawable.bg_badge_success : R.drawable.bg_badge_warning);
        h.tvStatus.setTextColor(h.itemView.getContext().getColor(
                empty ? R.color.success : R.color.warning));

        h.btnEdit.setOnClickListener(v -> listener.onEdit(t));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(t));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName, tvCapacity, tvStatus;
        final ImageButton btnEdit, btnDelete;

        ViewHolder(@NonNull View v) {
            super(v);
            tvName = v.findViewById(R.id.tv_table_name);
            tvCapacity = v.findViewById(R.id.tv_capacity);
            tvStatus = v.findViewById(R.id.tv_status);
            btnEdit = v.findViewById(R.id.btn_edit);
            btnDelete = v.findViewById(R.id.btn_delete);
        }
    }
}
