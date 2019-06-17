package com.wanglei.waudiodemo.basic;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

/**
 * 实时录制并播放
 */
public class AudioTest implements AudioCapture.OnAudioCaptureListener {

    private static final String TAG = "AudioTest";

    private static final int SIMPLE_RATE = 44100; //采样率
    private static final int FORMAT = AudioFormat.ENCODING_PCM_16BIT; //量化位宽
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO; //单通道
    private static final int PLAY_TYPE = AudioManager.STREAM_MUSIC; //播放模式

    //AudioTrack 提供了两种播放模式，一种是 static 方式，一种是 streaming 方式，
    // 前者需要一次性将所有的数据都写入播放缓冲区，简单高效，
    // 通常用于播放铃声、系统提醒的音频片段;
    // 后者则是按照一定的时间间隔不间断地写入音频数据，
    // 理论上它可用于任何音频播放的场景
    private static final int PLAY_MODE = AudioTrack.MODE_STREAM;
    //使用相对底层的AudioTrack来播放
    private AudioPlayer audioPlayer;
    private AudioCapture mAudioCapture;
    private volatile boolean isStart = false;

    public AudioTest() {
        audioPlayer = new AudioPlayer();
        mAudioCapture = new AudioCapture();
        mAudioCapture.setOnAudioCaptureListener(this);
    }

    public boolean start() {
        boolean audioStart = mAudioCapture.start();
        boolean audioPlayerStart = audioPlayer.startPlayer();
        return audioPlayerStart && audioStart;
    }

    public boolean play(byte[] audioData, int offset, int size) {
        if (!isStart) {
            return false;
        }
        if (audioPlayer.play(audioData, offset, size)) {
            Log.d(TAG, "play: size != write size");
        }
        return true;
    }

    public void stop() {
        if (!isStart) {
            return;
        }
        audioPlayer.stopPlayer();
        audioPlayer = null;
        isStart = false;
        mAudioCapture.stop();
        mAudioCapture = null;
    }

    @Override
    public void onAudioFrameCaptured(byte[] bytes) {
        play(bytes, 0, bytes.length);
    }

}
