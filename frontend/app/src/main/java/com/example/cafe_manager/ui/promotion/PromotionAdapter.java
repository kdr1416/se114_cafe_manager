package com.example.cafe_manager.ui.promotion;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.data.local.entity.PromotionEntity;
import com.example.cafe_manager.util.Constants;
import com.example.cafe_manager.util.CurrencyUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PromotionAdapter extends ListAdapter<PromotionEntity, PromotionAdapter.ViewHolder> {

    public interface OnActionListener {
        void onEdit(PromotionEntity promotion);
        void onToggleActive(PromotionEntity promotion);
        void onDelete(PromotionEntity promotion);
    }

    private final OnActionListener listener;

    public PromotionAdapter(OnActionListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<PromotionEntity> DIFF =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull PromotionEntity a, @NonNull PromotionEntity b) {
                    return a.getPromotionId() == b.getPromotionId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull PromotionEntity a, @NonNull PromotionEntity b) {
                    return a.getCode().equals(b.getCode())
                            && a.getType().equals(b.getType())
                            && a.getValue() == b.getValue()
                            && a.isActive() == b.isActive()
                            && a.getExpiresAt() == b.getExpiresAt();
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_promotion, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        PromotionEntity p = getItem(position);
        h.tvCode.setText(p.getCode());

        if (Constants.PROMO_PERCENT.equals(p.getType())) {
            h.tvTypeValue.setText("Giảm " + (int) p.getValue() + "%");
        } else {
            h.tvTypeValue.setText("Giảm " + CurrencyUtils.formatVnd(p.getValue()));
        }

        if (p.getExpiresAt() == 0) {
            h.tvExpires.setText("Không hết hạn");
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            h.tvExpires.setText("Hết hạn: " + sdf.format(new Date(p.getExpiresAt())));
        }

        if (p.isActive()) {
            h.tvStatus.setText("Hoạt động");
            h.tvStatus.setBackgroundResource(R.drawable.bg_badge_success);
            h.tvStatus.setTextColor(h.itemView.getContext().getColor(R.color.success));
            h.btnToggle.setText("Tắt");
        } else {
            h.tvStatus.setText("Đã tắt");
            h.tvStatus.setBackgroundResource(R.drawable.bg_badge_warning);
            h.tvStatus.setTextColor(h.itemView.getContext().getColor(R.color.warning));
            h.btnToggle.setText("Bật");
        }

        h.btnToggle.setOnClickListener(v -> listener.onToggleActive(p));
        h.btnEdit.setOnClickListener(v -> listener.onEdit(p));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(p));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvCode, tvTypeValue, tvExpires, tvStatus;
        final TextView btnToggle, btnEdit, btnDelete;

        ViewHolder(@NonNull View v) {
            super(v);
            tvCode = v.findViewById(R.id.tv_code);
            tvTypeValue = v.findViewById(R.id.tv_type_value);
            tvExpires = v.findViewById(R.id.tv_expires);
            tvStatus = v.findViewById(R.id.tv_status);
            btnToggle = v.findViewById(R.id.btn_toggle);
            btnEdit = v.findViewById(R.id.btn_edit);
            btnDelete = v.findViewById(R.id.btn_delete);
        }
    }
}
