![GitHub release](https://img.shields.io/github/release/devilsen/czxing.svg)

# CZXing
C++ port of ZXing for Android

加入了OpenCV的图像处理，能在更远的距离进行放大操作，并且能更快的识别出二维码。

### 使用
在gradle中:
``` groovy
implementation 'me.devilsen:CZXing:0.3'
```
建议加入abiFilters
```gradle
    defaultConfig {
        
        // 其他设置...

        ndk {
            // 设置支持的so库架构
            abiFilters "armeabi-v7a","arm64-v8a"
        }
    }
```

#### 1. 直接使用
你可以直接使用已经封装好的ScanActivity作为扫码界面
```java
Intent intent = new Intent(this, ScanActivity.class);
startActivity(intent);
```

使用ScanActivityDelegate来接管扫描返回的数据
```java
ScanActivityDelegate.getInstance().setScanResultDelegate(new ScanActivityDelegate.OnScanDelegate() {
    @Override
    public void onScanResult(String result) {
        Intent intent = new Intent(MainActivity.this, DelegateActivity.class);
        intent.putExtra("result", result);
        startActivity(intent);
    }

    @Override
    public void onClickCard() {
        Intent intent = new Intent(MainActivity.this, MyCardActivity.class);
        startActivity(intent);
    }
});
```

#### 2. 自定义界面
或者使用ScanView来自定义你的界面
```xml
<me.devilsen.czxing.view.ScanView
    android:id="@+id/surface_view_scan"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

在自定义的Activity中你需要接管ScanView的生命周期，具体可以参看ScanActivity界面，同时设定setScanListener()
```java
mScanView.setScanListener(new ScanListener() {
    @Override
    public void onScanSuccess(String result) {
        // 扫码成功
    }

    @Override
    public void onOpenCameraError() {
        // 打开相机出错
    }

    @Override
    public void onClickCard() {
        // 点击我的卡片
    }
});
```

如果不需要展示 我的卡片 可以选择关闭
```java
mScanView.hideCard();
```

#### 3. 生成二维码
调用以下代码，可生成二维码的bitmap，Color为可选参数，默认为黑色。
```java
BarcodeWriter reader = new BarcodeWriter();
Bitmap bitmap = reader.write("Hello World", BarCodeUtil.dp2px(this, 200), BarCodeUtil.dp2px(this, 200), Color.RED);
```


### 效果展示
[点击观看](https://www.bilibili.com/video/av59888116)

[apk下载](https://github.com/devilsen/CZXing/releases)

[设计思路](https://www.jianshu.com/p/e2866af44236)

