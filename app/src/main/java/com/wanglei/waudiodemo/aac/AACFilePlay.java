package com.wanglei.waudiodemo.aac;

import com.wanglei.waudiodemo.basic.AudioCapture;
import com.wanglei.waudiodemo.basic.AudioPlayer;

/**
 *MediaCodec解码AAC文件，播放
*@author wanglei
*@time 2019/6/17 16:40
*/
public class AACFilePlay implements AACDecoder.OnAudioDecodedListener{

    private AACDecoder mAudioDecoder;
    private AudioPlayer mAudioPlayer;
    private volatile boolean mIsTestingExit = false;

    public boolean start() {
        mAudioPlayer = new AudioPlayer();
        mAudioDecoder = new AACDecoder();
        if (!mAudioDecoder.start()) {
            return false;
        }
        mAudioDecoder.setOnAccDecodedListener(this);
        new Thread(mDecodeRenderRunnable).start();
        mAudioPlayer.startPlayer();
        return true;
    }


    public boolean stop() {
        mIsTestingExit = true;
        return true;
    }

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
    public void onFrameDecoded(byte[] decodedData, long presentationTimeUs) {//解码完
        mAudioPlayer.play(decodedData,0,decodedData.length);
    }
}
