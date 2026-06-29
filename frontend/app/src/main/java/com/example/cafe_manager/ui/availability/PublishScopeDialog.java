package com.example.cafe_manager.ui.availability;

import android.app.DatePickerDialog;
import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import java.util.Calendar;
import java.util.TimeZone;

public class PublishScopeDialog {

    public interface OnPublishScopeSelectedListener {
        void onScopeSelected(String scope, Long untilDate);
    }

    public static void show(Context context, OnPublishScopeSelectedListener listener) {
        String[] options = {"Tuần này", "Đến ngày..."};

        new AlertDialog.Builder(context)
                .setTitle("Phát hành lịch rảnh")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        listener.onScopeSelected("THIS_WEEK", null);
                    } else {
                        showDatePicker(context, listener);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private static void showDatePicker(Context context, OnPublishScopeSelectedListener listener) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        cal.add(Calendar.DAY_OF_YEAR, 7); // min = today + 7 days

        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog picker = new DatePickerDialog(context, (view, y, m, d) -> {
            Calendar selected = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
            selected.set(y, m, d, 0, 0, 0);
            selected.set(Calendar.MILLISECOND, 0);
            listener.onScopeSelected("UNTIL_DATE", selected.getTimeInMillis());
        }, year, month, day);

        picker.getDatePicker().setMinDate(cal.getTimeInMillis());
        picker.show();
    }
}
