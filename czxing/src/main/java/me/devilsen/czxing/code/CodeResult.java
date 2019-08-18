package me.devilsen.czxing.code;

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


    CodeResult(BarcodeFormat format, String text) {
        this.format = format;
        this.text = text;
    }

    public CodeResult(String content, int formatIndex, float[] points) {
        this.text = content;
        if (formatIndex < 0) {
            this.format = BarcodeFormat.QR_CODE;
        } else {
            this.format = BarcodeFormat.values()[formatIndex];
        }
        this.points = points;
    }

    public void setPoint(float[] lists) {
        points = lists;
    }

    public BarcodeFormat getFormat() {
        return format;
    }

    public String getText() {
        return text;
    }

    public float[] getPoints() {
        return points;
    }

    @Override
    public String toString() {
        return "text: " + text + " format: " + getFormat() + " points: " + getPointsString();
    }

    private String getPointsString() {
        StringBuilder stringBuilder = new StringBuilder();
        int i = 0;
        for (float list : points) {
            i++;
            stringBuilder.append(list).append("  ");
            if (i % 2 == 0) {
                stringBuilder.append("\n");
            }
        }
        return stringBuilder.toString();
    }
}
