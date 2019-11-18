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
 * desc :
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

    public ScannerManager setCornerColor(int cornerColor) {
        scanOption.cornerColor = cornerColor;
        return this;
    }

    public ScannerManager setBorderColor(int borderColor) {
        scanOption.borderColor = borderColor;
        return this;
    }

    public ScannerManager setBorderSize(int borderSize) {
        scanOption.borderSize = borderSize;
        return this;
    }

    public ScannerManager setScanMode(int scanMode) {
        scanOption.scanMode = scanMode;
        return this;
    }

    public ScannerManager setTitle(String title) {
        scanOption.title = title;
        return this;
    }

    public ScannerManager showAlbum(boolean showAlbum) {
        scanOption.showAlbum = showAlbum;
        return this;
    }

    public ScannerManager continuousScan() {
        scanOption.continuousScan = true;
        return this;
    }

    public ScannerManager setFlashLightOnDrawable(int flashLightOnDrawable) {
        scanOption.flashLightOnDrawable = flashLightOnDrawable;
        return this;
    }

    public ScannerManager setFlashLightOffDrawable(int flashLightOffDrawable) {
        scanOption.flashLightOffDrawable = flashLightOffDrawable;
        return this;
    }

    public ScannerManager setFlashLightOnText(String lightOnText) {
        scanOption.flashLightOnText = lightOnText;
        return this;
    }

    public ScannerManager setFlashLightOffText(String lightOffText) {
        scanOption.flashLightOffText = lightOffText;
        return this;
    }

    public ScannerManager setScanNoticeText(String scanNoticeText) {
        scanOption.scanNoticeText = scanNoticeText;
        return this;
    }

    public ScannerManager setFlashLightInvisible() {
        scanOption.dropFlashLight = true;
        return this;
    }

    public ScannerManager setScanLineColors(List<Integer> scanLineColors) {
        scanOption.scanLineColors = scanLineColors;
        return this;
    }

    public ScannerManager setBarcodeFormat(BarcodeFormat... barcodeFormats) {
        scanOption.barcodeFormats = Arrays.asList(barcodeFormats);
        return this;
    }

    public ScannerManager setOnScanResultDelegate(ScanActivityDelegate.OnScanDelegate delegate) {
        ScanActivityDelegate.getInstance().setScanResultDelegate(delegate);
        return this;
    }

    public void start() {
        Intent intent = new Intent(context, ScanActivity.class);
        intent.putExtra("option", scanOption);
        context.startActivity(intent);
    }

    public ScannerManager setOnClickAlbumDelegate(ScanActivityDelegate.OnClickAlbumDelegate onClickAlbumDelegate) {
        ScanActivityDelegate.getInstance().setOnClickAlbumDelegate(onClickAlbumDelegate);
        return this;
    }

    public static class ScanOption implements Parcelable {

        private int cornerColor;
        private int borderColor;
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
