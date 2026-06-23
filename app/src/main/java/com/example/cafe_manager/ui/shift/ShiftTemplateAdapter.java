package com.example.cafe_manager.ui.shift;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.data.local.entity.ShiftTemplateEntity;

import java.util.ArrayList;
import java.util.List;

public class ShiftTemplateAdapter extends RecyclerView.Adapter<ShiftTemplateAdapter.ViewHolder> {

    public interface OnTemplateActionListener {
        void onEdit(ShiftTemplateEntity template);
        void onDeactivate(ShiftTemplateEntity template);
    }

    private List<ShiftTemplateEntity> items = new ArrayList<>();
    private OnTemplateActionListener listener;

    public ShiftTemplateAdapter(OnTemplateActionListener listener) {
        this.listener = listener;
    }

    public void setItems(List<ShiftTemplateEntity> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shift_template, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShiftTemplateEntity item = items.get(position);

        holder.tvName.setText(item.getTemplateName());
        holder.tvTime.setText(item.getStartTime() + " - " + item.getEndTime());
        holder.tvMinStaff.setText("Tối thiểu " + item.getMinStaff() + " nhân viên");

        if (item.isActive()) {
            holder.tvBadge.setText("Hoạt động");
            holder.tvBadge.setBackgroundResource(R.drawable.bg_badge_success);
            holder.tvBadge.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.success));
        } else {
            holder.tvBadge.setText("Ngưng");
            holder.tvBadge.setBackgroundResource(R.drawable.bg_badge_warning);
            holder.tvBadge.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.warning));
        }

        holder.btnEdit.setOnClickListener(v -> listener.onEdit(item));
        holder.btnDeactivate.setOnClickListener(v -> listener.onDeactivate(item));

        // Ẩn nút xóa nếu đã ngưng
        holder.btnDeactivate.setVisibility(item.isActive() ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvTime, tvMinStaff, tvBadge;
        Button btnEdit, btnDeactivate;

        ViewHolder(View v) {
            super(v);
            tvName = v.findViewById(R.id.tv_template_name);
            tvTime = v.findViewById(R.id.tv_template_time);
            tvMinStaff = v.findViewById(R.id.tv_min_staff);
            tvBadge = v.findViewById(R.id.tv_status_badge);
            btnEdit = v.findViewById(R.id.btn_edit);
            btnDeactivate = v.findViewById(R.id.btn_deactivate);
        }
    }
}