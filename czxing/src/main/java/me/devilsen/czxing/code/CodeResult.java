package me.devilsen.czxing.code;

/**
 * desc: code result model
 * date: 2019/08/17
 *
 * @author : dongsen
 */
public class CodeResult {

    private final BarcodeFormat format;
    private final String text;
    private int[] points;
    private int scanType;

    CodeResult(BarcodeFormat format, String text) {
        this.format = format;
        this.text = text;
    }

    CodeResult(BarcodeFormat format, String text, int[] points) {
        this.format = format;
        this.text = text;
        this.points = points;
    }

    public CodeResult(String content, int formatIndex, int[] points, int scanType) {
        this.text = content;
        if (formatIndex < 0) {
            this.format = BarcodeFormat.QR_CODE;
        } else {
            this.format = BarcodeFormat.values()[formatIndex];
        }
        this.points = points;
        this.scanType = scanType;
    }

    public void setPoint(int[] lists) {
        points = lists;
    }

    public BarcodeFormat getFormat() {
        return format;
    }

    public String getText() {
        return text;
    }

    public int[] getPoints() {
        return points;
    }

    public int getScanType() {
        return scanType;
    }

    @Override
    public String toString() {
        return "text: " + text +
                "\nformat: " + getFormat() +
                "\nscanType: " + getScanType() +
                "\npoints: " + getPointsString();
    }

    private String getPointsString() {
        if (points == null) return "";
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
