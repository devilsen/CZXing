package me.devilsen.czxing.thread;

/**
 * desc : 每一帧的数据
 * date : 2019-07-01 20:17
 *
 * @author : dongSen
 */
public class FrameData {

    public byte[] data;
    public int left;
    public int top;
    public int width;
    public int height;
    public int rowWidth;
    public int rowHeight;

    public FrameData(byte[] data, int left, int top, int width, int height, int rowWidth, int rowHeight) {
        this.data = data;
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
        this.rowWidth = rowWidth;
        this.rowHeight = rowHeight;
    }
}
