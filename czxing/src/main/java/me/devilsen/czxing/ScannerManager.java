package me.devilsen.czxing;

import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.devilsen.czxing.code.BarcodeFormat;
import me.devilsen.czxing.view.ScanActivityDelegate;

/**
 * desc : 扫码API管理
 * date : 2019/07/31
 *
 * @author : dongsen
 */
public class ScannerManager {

    private Context context;
    private ScanOption scanOption;

    ScannerManager(Context context) {
        this.context = context;
        scanOption = new ScanOption();
    }

    /**
     * 扫码框角颜色
     *
     * @param cornerColor 色值
     */
    public ScannerManager setCornerColor(int cornerColor) {
        scanOption.cornerColor = cornerColor;
        return this;
    }

    /**
     * 设置扫码框颜色
     *
     * @param borderColor 色值
     */
    public ScannerManager setBorderColor(int borderColor) {
        scanOption.borderColor = borderColor;
        return this;
    }

    /**
     * 设置扫码框四周颜色
     *
     * @param maskColor 色值
     */
    public ScannerManager setMaskColor(int maskColor) {
        scanOption.maskColor = maskColor;
        return this;
    }

    /**
     * 设置边框长度(扫码框大小)
     *
     * @param borderSize px
     */
    public ScannerManager setBorderSize(int borderSize) {
        scanOption.borderSize = borderSize;
        return this;
    }

    /**
     * 扫描模式，提供了三种扫码模式
     * ┏--------------┓
     * ┃              ┃
     * ┃       2      ┃
     * ┃    ┏----┓    ┃
     * ┃    ┃  1 ┃    ┃
     * ┃    ┗----┛    ┃
     * ┃              ┃
     * ┃              ┃
     * ┃              ┃
     * ┃              ┃
     * ┗--------------┛
     * <p>
     * 0：混合扫描模式（默认），扫描4次扫码框里的内容，扫描1次以屏幕宽为边长的内容
     * 1：只扫描扫码框里的内容
     * 2：扫描以屏幕宽为边长的内容
     *
     * @param scanMode 0 / 1 / 2
     */
    public ScannerManager setScanMode(int scanMode) {
        scanOption.scanMode = scanMode;
        return this;
    }

    /**
     * 设置扫码界面标题
     *
     * @param title 标题内容
     */
    public ScannerManager setTitle(String title) {
        scanOption.title = title;
        return this;
    }

    /**
     * 显示从图库选取图片按钮
     *
     * @param showAlbum true：显示   false：隐藏
     */
    public ScannerManager showAlbum(boolean showAlbum) {
        scanOption.showAlbum = showAlbum;
        return this;
    }

    /**
     * 连续扫码，当扫描出结果后不关闭扫码界面
     */
    public ScannerManager continuousScan() {
        scanOption.continuousScan = true;
        return this;
    }

    /**
     * 设置闪光灯打开时的样式
     *
     * @param flashLightOnDrawable drawable
     */
    public ScannerManager setFlashLightOnDrawable(int flashLightOnDrawable) {
        scanOption.flashLightOnDrawable = flashLightOnDrawable;
        return this;
    }

    /**
     * 设置闪光灯关闭时的样式
     *
     * @param flashLightOffDrawable drawable
     */
    public ScannerManager setFlashLightOffDrawable(int flashLightOffDrawable) {
        scanOption.flashLightOffDrawable = flashLightOffDrawable;
        return this;
    }

    /**
     * 设置闪光灯打开时的提示文字
     *
     * @param lightOnText 提示文字
     */
    public ScannerManager setFlashLightOnText(String lightOnText) {
        scanOption.flashLightOnText = lightOnText;
        return this;
    }

    /**
     * 设置闪光灯关闭时的提示文字
     *
     * @param lightOffText 提示文字
     */
    public ScannerManager setFlashLightOffText(String lightOffText) {
        scanOption.flashLightOffText = lightOffText;
        return this;
    }

    /**
     * 设置扫码框下方的提示文字
     *
     * @param scanNoticeText 提示文字
     */
    public ScannerManager setScanNoticeText(String scanNoticeText) {
        scanOption.scanNoticeText = scanNoticeText;
        return this;
    }

    /**
     * 隐藏闪光灯图标及文字
     */
    public ScannerManager setFlashLightInvisible() {
        scanOption.dropFlashLight = true;
        return this;
    }

    /**
     * 设置扫码线颜色，扫描线最好使用一个渐变颜色
     * 如：
     * --- === #### === ---
     * 我们需要一条线，将它分成5份，拿到1/5、2/5、3/5处的3个颜色就可以画出一条两边细中间粗的扫描线
     *
     * @param scanLineColors 颜色合集
     */
    public ScannerManager setScanLineColors(List<Integer> scanLineColors) {
        scanOption.scanLineColors = scanLineColors;
        return this;
    }

    /**
     * 设置扫码格式{@link BarcodeFormat}
     *
     * @param barcodeFormats 扫码格式
     */
    public ScannerManager setBarcodeFormat(BarcodeFormat... barcodeFormats) {
        scanOption.barcodeFormats = Arrays.asList(barcodeFormats);
        return this;
    }

    /**
     * 设置扫码代理，获取扫码结果
     *
     * @param delegate 扫码代理
     */
    public ScannerManager setOnScanResultDelegate(ScanActivityDelegate.OnScanDelegate delegate) {
        ScanActivityDelegate.getInstance().setScanResultDelegate(delegate);
        return this;
    }

    /**
     * 设置点击图库代理，可以通过自定义的图片选择框架，打开图库
     *
     * @param onClickAlbumDelegate 点击图库代理
     */
    public ScannerManager setOnClickAlbumDelegate(ScanActivityDelegate.OnClickAlbumDelegate onClickAlbumDelegate) {
        ScanActivityDelegate.getInstance().setOnClickAlbumDelegate(onClickAlbumDelegate);
        return this;
    }

    /**
     * 开启界面
     */
    public void start() {
        Intent intent = new Intent(context, ScanActivity.class);
        intent.putExtra("option", scanOption);
        context.startActivity(intent);
    }

    public static class ScanOption implements Parcelable {

        private int cornerColor;
        private int borderColor;
        private int maskColor;
        private int borderSize;
        private int scanMode;
        private String title;
        private boolean showAlbum = true;
        private boolean continuousScan;

        private int flashLightOnDrawable;
        private int flashLightOffDrawable;
        private String flashLightOnText;
        private String flashLightOffText;
        private boolean dropFlashLight;
        private String scanNoticeText;

        private List<BarcodeFormat> barcodeFormats;
        private List<Integer> scanLineColors;

        public int getCornerColor() {
            return cornerColor;
        }

        public int getBorderColor() {
            return borderColor;
        }

        public int getMaskColor() {
            return maskColor;
        }

        public int getBorderSize() {
            return borderSize;
        }

        public int getScanMode() {
            return scanMode;
        }

        public String getTitle() {
            return title;
        }

        public boolean isShowAlbum() {
            return showAlbum;
        }

        public boolean isContinuousScan() {
            return continuousScan;
        }

        public int getFlashLightOnDrawable() {
            return flashLightOnDrawable;
        }

        public int getFlashLightOffDrawable() {
            return flashLightOffDrawable;
        }

        public String getFlashLightOnText() {
            return flashLightOnText;
        }

        public String getFlashLightOffText() {
            return flashLightOffText;
        }

        public boolean isDropFlashLight() {
            return dropFlashLight;
        }

        public String getScanNoticeText() {
            return scanNoticeText;
        }

        public List<Integer> getScanLineColors() {
            return scanLineColors;
        }

        public BarcodeFormat[] getBarcodeFormat() {
            return barcodeFormats.toArray(new BarcodeFormat[0]);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.cornerColor);
            dest.writeInt(this.borderColor);
            dest.writeInt(this.maskColor);
            dest.writeInt(this.borderSize);
            dest.writeInt(this.scanMode);
            dest.writeString(this.title);
            dest.writeByte(this.showAlbum ? (byte) 1 : (byte) 0);
            dest.writeByte(this.continuousScan ? (byte) 1 : (byte) 0);
            dest.writeInt(this.flashLightOnDrawable);
            dest.writeInt(this.flashLightOffDrawable);
            dest.writeString(this.flashLightOnText);
            dest.writeString(this.flashLightOffText);
            dest.writeByte(this.dropFlashLight ? (byte) 1 : (byte) 0);
            dest.writeString(this.scanNoticeText);
            dest.writeList(this.barcodeFormats);
            dest.writeList(this.scanLineColors);
        }

        public ScanOption() {
        }

        protected ScanOption(Parcel in) {
            this.cornerColor = in.readInt();
            this.borderColor = in.readInt();
            this.maskColor = in.readInt();
            this.borderSize = in.readInt();
            this.scanMode = in.readInt();
            this.title = in.readString();
            this.showAlbum = in.readByte() != 0;
            this.continuousScan = in.readByte() != 0;
            this.flashLightOnDrawable = in.readInt();
            this.flashLightOffDrawable = in.readInt();
            this.flashLightOnText = in.readString();
            this.flashLightOffText = in.readString();
            this.dropFlashLight = in.readByte() != 0;
            this.scanNoticeText = in.readString();
            this.barcodeFormats = new ArrayList<>();
            in.readList(this.barcodeFormats, BarcodeFormat.class.getClassLoader());
            this.scanLineColors = new ArrayList<>();
            in.readList(this.scanLineColors, Integer.class.getClassLoader());
        }

        public static final Creator<ScanOption> CREATOR = new Creator<ScanOption>() {
            @Override
            public ScanOption createFromParcel(Parcel source) {
                return new ScanOption(source);
            }

            @Override
            public ScanOption[] newArray(int size) {
                return new ScanOption[size];
            }
        };

    }
}
