package com.example.cafe_manager.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "shift_cash_sessions")
public class ShiftCashSessionEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "session_id")
    private int sessionId;

    @ColumnInfo(name = "shift_id")
    private int shiftId;

    @ColumnInfo(name = "opening_cash")
    private double openingCash;

    @ColumnInfo(name = "closing_cash")
    private double closingCash;

    @ColumnInfo(name = "expected_cash")
    private double expectedCash;

    @ColumnInfo(name = "actual_cash")
    private double actualCash;

    @ColumnInfo(name = "cash_difference")
    private double cashDifference;

    @ColumnInfo(name = "opened_by")
    private int openedBy;

    @ColumnInfo(name = "opened_at")
    private long openedAt;

    @ColumnInfo(name = "closed_by")
    private int closedBy;

    @ColumnInfo(name = "closed_at")
    private long closedAt;

    @ColumnInfo(name = "status")
    private String status; // "OPEN" hoặc "CLOSED"

    public ShiftCashSessionEntity() {}

    // ── Getters & Setters ──
    public int getSessionId() { return sessionId; }
    public void setSessionId(int sessionId) { this.sessionId = sessionId; }

    public int getShiftId() { return shiftId; }
    public void setShiftId(int shiftId) { this.shiftId = shiftId; }

    public double getOpeningCash() { return openingCash; }
    public void setOpeningCash(double openingCash) { this.openingCash = openingCash; }

    public double getClosingCash() { return closingCash; }
    public void setClosingCash(double closingCash) { this.closingCash = closingCash; }

    public double getExpectedCash() { return expectedCash; }
    public void setExpectedCash(double expectedCash) { this.expectedCash = expectedCash; }

    public double getActualCash() { return actualCash; }
    public void setActualCash(double actualCash) { this.actualCash = actualCash; }

    public double getCashDifference() { return cashDifference; }
    public void setCashDifference(double cashDifference) { this.cashDifference = cashDifference; }

    public int getOpenedBy() { return openedBy; }
    public void setOpenedBy(int openedBy) { this.openedBy = openedBy; }

    public long getOpenedAt() { return openedAt; }
    public void setOpenedAt(long openedAt) { this.openedAt = openedAt; }

    public int getClosedBy() { return closedBy; }
    public void setClosedBy(int closedBy) { this.closedBy = closedBy; }

    public long getClosedAt() { return closedAt; }
    public void setClosedAt(long closedAt) { this.closedAt = closedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
