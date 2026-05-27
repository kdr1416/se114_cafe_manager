package com.example.cafe_manager.util;

import android.content.Context;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.example.cafe_manager.R;

import java.io.File;

/**
 * Wrapper Glide để load ảnh sản phẩm.
 * Nếu fileName null/empty → load placeholder (icon_coffee).
 */
public final class ImageLoader {

    private ImageLoader() {}

    public static void loadProductImage(
            Context context,
            ImageView imageView,
            String fileName,
            int placeholderRes  // vd R.drawable.ic_coffee
    ) {
        File file = ImageStorageUtils.getImageFile(context, fileName);

        if (file != null) {
            Glide.with(context)
                    .load(file)
                    .placeholder(placeholderRes)
                    .error(placeholderRes)
                    .centerCrop()
                    .into(imageView);
        } else {
            // Không có ảnh → dùng icon mặc định
            imageView.setImageResource(placeholderRes);
        }
    }

    /** Variant cho ic_coffee mặc định. */
    public static void loadProductImage(Context context, ImageView imageView, String fileName) {
        loadProductImage(context, imageView, fileName, R.drawable.ic_coffee);
    }
}
