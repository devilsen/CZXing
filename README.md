![GitHub release](https://img.shields.io/github/release/devilsen/czxing.svg)
![Bintray](https://img.shields.io/bintray/v/devilsen/Android/czxing?color=1E88E5&label=version)

# CZXing
C++ port of ZXing for Android

底层使用C++来处理图像及解析二维码，并且加入了OpenCV来解析图像，可以在更远的距离识别出更加复杂的二维码。

![App展示](https://github.com/devilsen/CZXing/blob/master/screenshots/scan_code.gif)

### 使用
在gradle中:
``` groovy
// 改为小写了，这样显得更整齐一些
implementation 'me.devilsen:czxing:1.0.11'
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
如果下载失败，可以在根目录加入阿里云的镜像
```gradle
maven { url 'https://maven.aliyun.com/repository/jcenter' }
```

#### 1. 直接使用
你可以直接使用已经封装好的ScanActivity作为扫码界面
```java
Resources resources = getResources();
List<Integer> scanColors = Arrays.asList(resources.getColor(R.color.scan_side), resources.getColor(R.color.scan_partial), resources.getColor(R.color.scan_middle));

Scanner.with(this)
        .setMaskColor(resources.getColor(R.color.mask_color))   // 设置设置扫码框四周颜色
        .setBorderColor(resources.getColor(R.color.box_line))   // 扫码框边框颜色
        .setBorderSize(BarCodeUtil.dp2px(this, 200))            // 设置扫码框大小
//        .setBorderSize(BarCodeUtil.dp2px(this, 200), BarCodeUtil.dp2px(this, 100))     // 设置扫码框长宽（如果同时调用了两个setBorderSize方法优先使用上一个）
        .setCornerColor(resources.getColor(R.color.corner))     // 扫码框角颜色
        .setScanLineColors(scanColors)                          // 扫描线颜色（这是一个渐变颜色）
//        .setHorizontalScanLine()                              // 设置扫码线为水平方向（从左到右）
        .setScanMode(ScanView.SCAN_MODE_TINY)                   // 扫描区域 0：混合 1：只扫描框内 2：只扫描整个屏幕
//        .setBarcodeFormat(BarcodeFormat.EAN_13)                 // 设置扫码格式
        .setTitle("My Scan View")                               // 扫码界面标题
        .showAlbum(true)                                        // 显示相册(默认为true)
        .setScanNoticeText("扫描二维码")                         // 设置扫码文字提示
        .setFlashLightOnText("打开闪光灯")                       // 打开闪光灯提示
        .setFlashLightOffText("关闭闪光灯")                      // 关闭闪光灯提示
        .setFlashLightOnDrawable(R.drawable.ic_highlight_blue_open_24dp)       // 闪光灯打开时的样式
        .setFlashLightOffDrawable(R.drawable.ic_highlight_white_close_24dp)    // 闪光灯关闭时的样式
        .setFlashLightInvisible()                               // 不使用闪光灯图标及提示
        .continuousScan()                                       // 连续扫码，不关闭扫码界面
        .setOnClickAlbumDelegate(new ScanActivityDelegate.OnClickAlbumDelegate() {
            @Override
            public void onClickAlbum(Activity activity) {       // 点击右上角的相册按钮
                Intent albumIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                activity.startActivityForResult(albumIntent, CODE_SELECT_IMAGE);
            }

            @Override
            public void onSelectData(int requestCode, Intent data) { // 选择图片返回的数据
                if (requestCode == CODE_SELECT_IMAGE) {
                    selectPic(data);
                }
            }
        })
        .setOnScanResultDelegate(new ScanActivityDelegate.OnScanDelegate() { // 接管扫码成功的数据
            @Override
            public void onScanResult(Activity activity, String result, BarcodeFormat format) {
                Intent intent = new Intent(MainActivity.this, DelegateActivity.class);
                intent.putExtra("result", result);
                startActivity(intent);
            }
        })
        .start();
```

混淆配置
```
-keep class me.devilsen.czxing.**
-keep class me.devilsen.czxing.** { *; }
```

#### 2. 自定义界面
![自定义界面展示](https://github.com/devilsen/CZXing/blob/master/screenshots/customize_scan_view.jpg)

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
    public void onScanSuccess(String result, BarcodeFormat format) {
        // 扫码成功
    }

    @Override
    public void onOpenCameraError() {
        // 打开相机出错
    }
});
```

更多API请参考[CustomizeActivity](https://github.com/devilsen/CZXing/blob/master/sample/src/main/java/me/sam/czxing/CustomizeActivity.java)

#### 3. 生成二维码
![生成二维码](https://github.com/devilsen/CZXing/blob/master/screenshots/write_code.gif)

调用以下代码，可生成二维码的bitmap，Color为可选参数，默认为黑色。

简易调用

```java
BarcodeWriter writer = new BarcodeWriter();
Bitmap bitmap = writer.write("Hello World", BarCodeUtil.dp2px(this, 200), BarCodeUtil.dp2px(this, 200), Color.RED);
```

完整调用

```java
/**
* 生成图片
*
* @param text   要生成的文本
* @param width  图片宽
* @param height 图片高
* @param color  要生成的二维码颜色
* @param format 要生成的条码格式
* @param logo   放在中间的logo
* @return 条码bitmap
*/
private Bitmap write(String text, int width, int height, int color, BarcodeFormat format, Bitmap logo)

```

### 4. 识别图片中的二维码 

```java
// 适当压缩图片
Bitmap bitmap = BitmapUtil.getDecodeAbleBitmap(picturePath);
// 这个方法因为要做bitmap的变换，所以比较耗时，推荐放到子线程执行
CodeResult result = BarcodeReader.getInstance().read(bitmap);
if (result == null) {
  Log.d("Scan >>> ", "no code");
  return;
} else {
  Log.d("Scan >>> ", result.getText());
}
```



### 5. 测试Case

| | | |
:--:|:-:|:--:
![](https://github.com/devilsen/CZXing/blob/master/screenshots/case/test_bar_code.png)|![](https://github.com/devilsen/CZXing/blob/master/screenshots/case/test_black_boder.png)|![](https://github.com/devilsen/CZXing/blob/master/screenshots/case/test_color.png)
![](https://github.com/devilsen/CZXing/blob/master/screenshots/case/test_gray.png)|![](https://github.com/devilsen/CZXing/blob/master/screenshots/case/test_oblique.png)|![](https://github.com/devilsen/CZXing/blob/master/screenshots/case/test_oblique_2.png)

### 效果展示
[远距离扫码演示](https://www.bilibili.com/video/av59888116)

[apk下载](https://github.com/devilsen/CZXing/releases)

[设计思路](https://www.jianshu.com/p/e2866af44236)

## License

    Copyright 2019 Devilsen
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
       http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
