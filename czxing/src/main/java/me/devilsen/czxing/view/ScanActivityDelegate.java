package me.devilsen.czxing.view;

import android.app.Activity;
import android.content.Intent;

import me.devilsen.czxing.code.BarcodeFormat;

/**
 * @author : dongSen
 * date : 2019/07/21
 * desc : 扫码结果
 */
public class ScanActivityDelegate {

    private OnScanDelegate mOnScanDelegate;
    private OnClickAlbumDelegate mOnClickAlbumDelegate;

    public static ScanActivityDelegate getInstance() {
        return Holder.instance;
    }

    private static final class Holder {
        private static final ScanActivityDelegate instance = new ScanActivityDelegate();
    }

    public void setScanResultDelegate(OnScanDelegate onScanDelegate) {
        this.mOnScanDelegate = onScanDelegate;
    }

    public OnScanDelegate getScanDelegate() {
        return mOnScanDelegate;
    }

    public OnClickAlbumDelegate getOnClickAlbumDelegate() {
        return mOnClickAlbumDelegate;
    }

    public void setOnClickAlbumDelegate(OnClickAlbumDelegate onClickAlbumDelegate) {
        this.mOnClickAlbumDelegate = onClickAlbumDelegate;
    }

    public interface OnScanDelegate {
        void onScanResult(String result, BarcodeFormat format);
    }

    public interface OnClickAlbumDelegate {
        void onClickAlbum(Activity activity);

        void onSelectData(int requestCode, Intent data);
    }

}
