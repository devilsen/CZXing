package me.devilsen.czxing.view;

/**
 * @author : dongSen
 * date : 2019/07/21
 * desc : 扫码结果
 */
public class ScanActivityDelegate {

    private OnScanDelegate mOnScanDelegate;

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

    public interface OnScanDelegate {
        void onScanResult(String result);
    }

}
