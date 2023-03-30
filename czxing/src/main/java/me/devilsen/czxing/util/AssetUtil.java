package me.devilsen.czxing.util;

import android.content.Context;
import android.content.res.AssetManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

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
        File file;
        if (dir == null) {
            file = new File(cacheDir.getAbsolutePath() + File.separator + "Assets" + File.separator + name);
        } else {
            file = new File(cacheDir.getAbsolutePath() + File.separator + "Assets" + File.separator + dir + File.separator + name);
        }
        if (file.exists()) {
            Log.d("CZXing", "Asset file absolute path: " + file.getAbsolutePath());
            return file.getAbsolutePath();
        } else {
            Log.w("CZXing", "Asset file absolute path: 请检查路径");
            return null;
        }
    }

    public static void copyAssetsToCacheAssets(final Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    copyAssets(context, "", cacheAssets(context).getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static File cacheAssets(Context context) {
        return new File(context.getExternalCacheDir().getAbsolutePath(), "Assets");
    }

    public static void copyAssets(Context context, String assetPath, String outputPath) throws IOException {
        AssetManager assetManager = context.getAssets();
        String[] list = assetManager.list(assetPath);
        if (list == null || list.length == 0) {
            try {
                InputStream inputStream = assetManager.open(assetPath);
                File file = new File(outputPath, assetPath);
                file.getParentFile().mkdirs();
                FileOutputStream outputStream = new FileOutputStream(file);
                byte[] buffer = new byte[1024 * 10];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            for (String path : list) {
                copyAssets(context, TextUtils.isEmpty(assetPath) ? path : assetPath + "/" + path, outputPath);
            }
        }
    }
}
