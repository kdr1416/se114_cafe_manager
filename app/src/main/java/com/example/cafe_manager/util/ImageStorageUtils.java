package com.example.cafe_manager.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Copy ảnh từ URI (gallery) vào internal storage,
 * resize xuống max 1024px width để giảm dung lượng.
 */
public final class ImageStorageUtils {

    private static final String DIR_PRODUCTS = "products";
    private static final int MAX_WIDTH = 1024;
    private static final int JPEG_QUALITY = 85;
    private ImageStorageUtils() {}

    /**
     * Copy ảnh từ Uri vào internal storage, return tên file (vd "product_12345.jpg").
     * Trả về null nếu fail.
     */
    public static String saveImageFromUri(Context context, Uri sourceUri) {
        if (sourceUri == null) return null;

        try {
            // Đọc ảnh từ URI
            InputStream input = context.getContentResolver().openInputStream(sourceUri);
            if (input == null) return null;

            Bitmap original = BitmapFactory.decodeStream(input);
            input.close();
            if (original == null) return null;

            // Resize giữ aspect ratio nếu width > MAX_WIDTH
            Bitmap resized = resizeIfNeeded(original);

            // Tạo file đích
            File dir = getProductImageDir(context);
            String fileName = "product_" + System.currentTimeMillis() + ".jpg";
            File dest = new File(dir, fileName);

            // Save dưới dạng JPEG
            FileOutputStream out = new FileOutputStream(dest);
            resized.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out);
            out.flush();
            out.close();

            // Clean up
            if (resized != original) resized.recycle();
            original.recycle();

            return fileName;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /** Lấy File từ tên ảnh (sau khi save xong). Null nếu file không tồn tại. */
    public static File getImageFile(Context context, String fileName) {
        if (fileName == null || fileName.isEmpty()) return null;
        File file = new File(getProductImageDir(context), fileName);
        return file.exists() ? file : null;
    }

    /** Xoá ảnh cũ khi update product. */
    public static void deleteImage(Context context, String fileName) {
        File file = getImageFile(context, fileName);
        if (file != null && file.exists()) {
            file.delete();
        }
    }

    private static File getProductImageDir(Context context) {
        File dir = new File(context.getFilesDir(), DIR_PRODUCTS);
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private static Bitmap resizeIfNeeded(Bitmap source) {
        int w = source.getWidth();
        int h = source.getHeight();

        if (w <= MAX_WIDTH) return source;

        float scale = (float) MAX_WIDTH / w;
        int newH = Math.round(h * scale);

        return Bitmap.createScaledBitmap(source, MAX_WIDTH, newH, true);
    }
}