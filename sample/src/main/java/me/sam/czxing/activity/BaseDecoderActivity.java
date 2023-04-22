package me.sam.czxing.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import me.devilsen.czxing.code.BarcodeDecoder;
import me.devilsen.czxing.util.AssetUtil;

/**
 * Created by dongSen on 2023/4/1
 */
public class BaseDecoderActivity extends AppCompatActivity {

    private BarcodeDecoder mDecoder;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDecoder != null) {
            mDecoder.destroy();
            mDecoder = null;
        }
    }

    @NonNull
    protected BarcodeDecoder getDecoder() {
        if (mDecoder == null) {
            mDecoder = new BarcodeDecoder();
            String detectorPrototxtPath = AssetUtil.getAbsolutePath(this, "wechat", "detect.prototxt");
            String detectorCaffeModelPath = AssetUtil.getAbsolutePath(this, "wechat", "detect.caffemodel");
            String superResolutionPrototxtPath = AssetUtil.getAbsolutePath(this, "wechat", "sr.prototxt");
            String superResolutionCaffeModelPath = AssetUtil.getAbsolutePath(this, "wechat", "sr.caffemodel");
            mDecoder.setDetectModel(detectorPrototxtPath, detectorCaffeModelPath, superResolutionPrototxtPath, superResolutionCaffeModelPath);
        }
        return mDecoder;
    }


}
