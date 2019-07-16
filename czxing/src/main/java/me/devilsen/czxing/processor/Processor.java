package me.devilsen.czxing.processor;

/**
 * desc : 处理器
 * date : 2019-06-25
 *
 * @author : dongSen
 */
abstract class Processor {

    boolean cancel;

    public void cancel() {
        cancel = true;
    }
}
