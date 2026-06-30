package com.example.cafe_manager.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "promotions")
public class PromotionEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "promotion_id")
    private int promotionId;

    /** Mã unique, ví dụ "CAFE10K" */
    @ColumnInfo(name = "code")
    private String code;

    /** Loại: "CASH" (giảm VND) hoặc "PERCENT" (giảm %) — xem Constants.PROMO_*  */
    @ColumnInfo(name = "type")
    private String type;

    /** Giá trị: với CASH là số VND; với PERCENT là số % (1-100) */
    @ColumnInfo(name = "value")
    private double value;

    @ColumnInfo(name = "is_active")
    private boolean isActive;

    /** 0 = không hết hạn; ngược lại = epoch millis */
    @ColumnInfo(name = "expires_at")
    private long expiresAt;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    public PromotionEntity() {
    }

    public int getPromotionId() { return promotionId; }
    public void setPromotionId(int promotionId) { this.promotionId = promotionId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
