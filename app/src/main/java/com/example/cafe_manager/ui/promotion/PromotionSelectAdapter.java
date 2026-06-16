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

public class PromotionSelectAdapter extends ListAdapter<PromotionEntity, PromotionSelectAdapter.ViewHolder> {

    public interface OnPromoClickListener {
        void onPromoClick(PromotionEntity promotion);
    }

    private final OnPromoClickListener listener;

    public PromotionSelectAdapter(OnPromoClickListener listener) {
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
                            && a.getExpiresAt() == b.getExpiresAt();
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_promotion_select, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        PromotionEntity p = getItem(position);
        h.tvCode.setText(p.getCode());

        if (Constants.PROMO_PERCENT.equals(p.getType())) {
            h.tvValue.setText("Giảm " + (int) p.getValue() + "%");
        } else {
            h.tvValue.setText("Giảm " + CurrencyUtils.formatVnd(p.getValue()));
        }

        if (p.getExpiresAt() == 0) {
            h.tvExpires.setText("Không hết hạn");
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            h.tvExpires.setText("HSD: " + sdf.format(new Date(p.getExpiresAt())));
        }

        h.itemView.setOnClickListener(v -> listener.onPromoClick(p));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvCode, tvValue, tvExpires;

        ViewHolder(@NonNull View v) {
            super(v);
            tvCode = v.findViewById(R.id.tv_code);
            tvValue = v.findViewById(R.id.tv_value);
            tvExpires = v.findViewById(R.id.tv_expires);
        }
    }
}
