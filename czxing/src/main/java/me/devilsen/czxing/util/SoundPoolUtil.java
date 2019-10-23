package me.devilsen.czxing.util;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

import me.devilsen.czxing.R;

/**
 * desc : 播放提示音工具
 * date : 2019-07-25 20:14
 *
 * @author : dongSen
 */
public class SoundPoolUtil {

    private SoundPool mSoundPool;
    private int mSoundId = -1;

    public SoundPoolUtil() {
        mSoundPool = new SoundPool(1, AudioManager.STREAM_RING, 0);
    }

    public void loadDefault(Context context) {
        load(context, R.raw.voice_correct);
    }

    /**
     * 加载音频资源
     *
     * @param context 上下文
     * @param resId   资源ID
     */
    public void load(Context context, int resId) {
        mSoundId = mSoundPool.load(context, resId, 1);
    }

    /**
     * int play(int soundID, float leftVolume, float rightVolume, int priority, int loop, float rate) ：
     * 1)该方法的第一个参数指定播放哪个声音；
     * 2) leftVolume 、
     * 3) rightVolume 指定左、右的音量：
     * 4) priority 指定播放声音的优先级，数值越大，优先级越高；
     * 5) loop 指定是否循环， 0 为不循环， -1 为循环；
     * 6) rate 指定播放的比率，数值可从 0.5 到 2 ， 1 为正常比率。
     */
    public void play() {
        if (mSoundId == -1) {
            return;
        }
        mSoundPool.play(mSoundId, 1, 1, 1, 0, 1);
    }

    /**
     * 释放资源
     */
    public void release() {
        if (mSoundPool != null) {
            mSoundPool.release();
        }
    }
}
