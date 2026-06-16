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
import com.example.cafe_manager.data.local.entity.AreaEntity;

public class AreaManageAdapter extends ListAdapter<AreaEntity, AreaManageAdapter.ViewHolder> {

    public interface OnActionListener {
        void onEdit(AreaEntity area);
        void onDelete(AreaEntity area);
    }

    private final OnActionListener listener;

    public AreaManageAdapter(OnActionListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<AreaEntity> DIFF =
            new DiffUtil.ItemCallback<AreaEntity>() {
                @Override
                public boolean areItemsTheSame(@NonNull AreaEntity a, @NonNull AreaEntity b) {
                    return a.getAreaId() == b.getAreaId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull AreaEntity a, @NonNull AreaEntity b) {
                    return a.getAreaName().equals(b.getAreaName())
                            && a.getPrefix().equals(b.getPrefix());
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_area_manage, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        AreaEntity area = getItem(position);
        h.tvName.setText(area.getAreaName());
        h.tvPrefix.setText("Tiền tố đặt tên bàn: " + area.getPrefix());

        h.btnEdit.setOnClickListener(v -> listener.onEdit(area));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(area));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName, tvPrefix;
        final ImageButton btnEdit, btnDelete;

        ViewHolder(@NonNull View v) {
            super(v);
            tvName = v.findViewById(R.id.tv_area_name);
            tvPrefix = v.findViewById(R.id.tv_prefix);
            btnEdit = v.findViewById(R.id.btn_edit);
            btnDelete = v.findViewById(R.id.btn_delete);
        }
    }
}
