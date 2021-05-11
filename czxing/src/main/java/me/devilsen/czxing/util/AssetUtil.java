package me.devilsen.czxing.util;

import android.content.Context;
import android.util.Log;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;

/**
 * @author : dongSen
 */
public class AssetUtil {

    /**
     * 获取文件的绝对路径
     *
     * @param name 文件名
     *
     * @return 文件的绝对路径
     */
    @Nullable
    @CheckResult
    public static String getAbsolutePath(@NonNull Context context, @Nullable String dir, @NonNull String name) {
        File cacheDir = context.getExternalCacheDir();
        File file = new File(cacheDir.getAbsolutePath() + File.separator + "Assets" + File.separator + dir + File.separator + name);
        if (file.exists()) {
            Log.d("CZXing", "Asset file absolute path: " + file.getAbsolutePath());
            return file.getAbsolutePath();
        } else {
            Log.w("CZXing", "Asset file absolute path: 请检查路径");
            return null;
        }
    }
}
