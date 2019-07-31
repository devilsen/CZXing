![GitHub release](https://img.shields.io/github/release/devilsen/czxing.svg)

# CZXing
C++ port of ZXing for Android

底层使用C++来处理图像及解析二维码，并且加入了OpenCV来解析图像，可以在更远的距离识别出二维码。

### 使用
在gradle中:
``` groovy
implementation 'me.devilsen:CZXing:0.5'
```
建议加入abiFilters
```gradle
    defaultConfig {
        
        // 其他设置...

        ndk {
            // 设置支持的so库架构，设置一个可以减小包的大小
            abiFilters "armeabi-v7a","arm64-v8a"
        }
    }
```

#### 1. 直接使用
你可以直接使用已经封装好的ScanActivity作为扫码界面
```java
Resources resources = getResources();
List<Integer> scanColors = Arrays.asList(resources.getColor(R.color.scan_side), resources.getColor(R.color.scan_partial), resources.getColor(R.color.scan_middle));

Scanner.with(this)
        .setBorderColor(resources.getColor(R.color.box_line)) // 扫码框边框颜色
        .setCornerColor(resources.getColor(R.color.corner))   // 扫码框角颜色
        .setScanLineColors(scanColors)                        // 扫描线颜色（这是一个渐变颜色）
        .setDelegate(new ScanActivityDelegate.OnScanDelegate() {
            @Override
            public void onScanResult(String result) {
                Intent intent = new Intent(MainActivity.this, DelegateActivity.class);
                intent.putExtra("result", result);
                startActivity(intent);
            }
        })
        .start();
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
});
```

#### 3. 生成二维码
调用以下代码，可生成二维码的bitmap，Color为可选参数，默认为黑色。
```java
BarcodeWriter writer = new BarcodeWriter();
Bitmap bitmap = writer.write("Hello World", BarCodeUtil.dp2px(this, 200), BarCodeUtil.dp2px(this, 200), Color.RED);
```


### 效果展示
[点击观看](https://www.bilibili.com/video/av59888116)

[apk下载](https://github.com/devilsen/CZXing/releases)

[设计思路](https://www.jianshu.com/p/e2866af44236)

