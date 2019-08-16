package me.devilsen.czxing.code;

import me.devilsen.czxing.util.BarCodeUtil;

/**
 * desc: code result model
 * date: 2019/08/17
 *
 * @author : dongsen
 */
public class CodeResult {

    private BarcodeFormat format;
    private String text;
    private float[] points;

    public BarcodeFormat getFormat() {
        return format;
    }

    public String getText() {
        return text;
    }

    public float[] getPoints() {
        return points;
    }

    CodeResult(BarcodeFormat format, String text) {
        this.format = format;
        this.text = text;
    }

    public void setPoint(float[] lists) {
        points = lists;
        logPoint();
    }

    public void setPoint(int[] lists) {
        points = new float[lists.length];
        for (int i = 0; i < lists.length; i++) {
            points[i] = lists[i];
        }
        logPoint();
    }

    private void logPoint() {
        StringBuilder stringBuilder = new StringBuilder("location points ");
        int i = 0;
        for (float list : points) {
            i++;
            stringBuilder.append(list).append("  ");
            if (i % 2 == 0) {
                stringBuilder.append("\n");
            }
        }
        BarCodeUtil.d(stringBuilder.toString());
    }
}
