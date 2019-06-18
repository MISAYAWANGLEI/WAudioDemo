package com.wanglei.waudiodemo.aac;

import com.wanglei.waudiodemo.basic.AudioCapture;
import com.wanglei.waudiodemo.basic.AudioPlayer;
/**
*录制PCM->AAC->PCM->播放
*@author wanglei
*@time 2019/6/17 16:40
*/
public class AACCodecTester implements AACDecoder.OnAudioDecodedListener,
        AACEncoder.OnAACEncodeListener,
        AudioCapture.OnAudioCaptureListener {

    private AACEncoder mAudioEncoder;
    private AACDecoder mAudioDecoder;
    private AudioCapture mAudioCapturer;
    private AudioPlayer mAudioPlayer;
    private volatile boolean mIsTestingExit = false;

    public boolean start() {
        mAudioCapturer = new AudioCapture();
        mAudioPlayer = new AudioPlayer();
        mAudioEncoder = new AACEncoder();
        mAudioDecoder = new AACDecoder();
        if (!mAudioEncoder.start() || !mAudioDecoder.start(null)) {
            return false;
        }
        mAudioEncoder.setOnAACEncodeListener(this);
        mAudioDecoder.setOnAccDecodedListener(this);
        mAudioCapturer.setOnAudioCaptureListener(this);
        new Thread(mEncodeRenderRunnable).start();
        new Thread(mDecodeRenderRunnable).start();
        if (!mAudioCapturer.start()) {
            return false;
        }
        mAudioPlayer.startPlayer();
        return true;
    }


    public boolean stop() {
        mIsTestingExit = true;
        mAudioCapturer.stop();
        return true;
    }

    private Runnable mEncodeRenderRunnable = new Runnable() {
        @Override
        public void run() {
            while (!mIsTestingExit) {
                mAudioEncoder.retriveData();
            }
            mAudioEncoder.close();
        }
    };

    private Runnable mDecodeRenderRunnable = new Runnable() {
        @Override
        public void run() {
            while (!mIsTestingExit) {
                mAudioDecoder.retrieveData();
            }
            mAudioDecoder.close();
        }
    };

    @Override
    public void onEncodedFrame(byte[] data) {//编码完
        mAudioDecoder.decodeData(data);
    }

    @Override
    public void onAudioFrameCaptured(byte[] bytes) {
        mAudioEncoder.encodeData(bytes);//获取pcm数据进行编码
    }

    @Override
    public void onFrameDecoded(byte[] decodedData, long presentationTimeUs) {//解码完
        mAudioPlayer.play(decodedData,0,decodedData.length);
    }
}
