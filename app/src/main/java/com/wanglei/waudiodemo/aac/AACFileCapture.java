package com.wanglei.waudiodemo.aac;


import android.os.Environment;

import com.wanglei.waudiodemo.basic.AudioCapture;
import com.wanglei.waudiodemo.basic.AudioPlayer;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
*生成AAC文件
*@author wanglei
*@time 2019/6/17 16:28
*/
public class AACFileCapture implements AACEncoder.OnAACEncodeListener,
        AudioCapture.OnAudioCaptureListener {

    private AACEncoder mAudioEncoder;
    private AudioCapture mAudioCapturer;
    private volatile boolean isStop = false;
    private FileOutputStream mFileOutputStream;
    private BufferedOutputStream mAudioBos;
    private File mAudioFile;

    public boolean start(String mFilePath) {
        createFile(mFilePath);
        isStop = false;
        mAudioCapturer = new AudioCapture();
        mAudioEncoder = new AACEncoder();
        if (!mAudioEncoder.start()) {
            return false;
        }
        mAudioEncoder.setOnAACEncodeListener(this);
        mAudioCapturer.setOnAudioCaptureListener(this);
        new Thread(mEncodeRenderRunnable).start();
        if (!mAudioCapturer.start()) {
            return false;
        }
        return true;
    }

    private void createFile(String mFilePath) {
        try {
            mAudioFile = new File(mFilePath);
            mAudioFile.createNewFile();
            mFileOutputStream = new FileOutputStream(mAudioFile);
            mAudioBos = new BufferedOutputStream(mFileOutputStream, 20 * 1024);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public boolean stop() {
        try {
            if (mAudioBos != null) {
                mAudioBos.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (mAudioBos != null) {
                try {
                    mAudioBos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    mAudioBos = null;
                }
            }
        }
        isStop = true;
        mAudioCapturer.stop();
        return true;
    }

    private Runnable mEncodeRenderRunnable = new Runnable() {
        @Override
        public void run() {
            while (!isStop) {
                mAudioEncoder.retriveData();
            }
            mAudioEncoder.close();
        }
    };

    @Override
    public void onAudioFrameCaptured(byte[] bytes) {
        if (!isStop){
            mAudioEncoder.encodeData(bytes);//获取pcm数据进行编码
        }
    }

    @Override
    public void onEncodedFrame(byte[] data) {
        writeToFile(data);
    }


    private void writeToFile(byte[] frame) {
        byte[] packetWithADTS = new byte[frame.length + 7];
        System.arraycopy(frame, 0, packetWithADTS, 7, frame.length);
        addADTStoPacket(packetWithADTS, packetWithADTS.length);
        if (mAudioBos != null) {
            try {
                mAudioBos.write(packetWithADTS, 0, packetWithADTS.length);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //ADTS头包含了AAC文件的采样率、通道数、帧数据长度等信息。
    // ADTS头分为固定头信息和可变头信息两个部分，固定头信息在每个帧中的是一样的，
    // 可变头信息在各个帧中并不是固定值。ADTS头一般是7个字节((28+28)/ 8)长度，
    // 如果需要对数据进行CRC校验，则会有2个Byte的校验码，
    // 所以ADTS头的实际长度是7个字节或9个字节。
    private void addADTStoPacket(byte[] packet, int packetLen) {
        //一般情况下ADTS的头信息都是7个字节，分为2部分：
        //固定头信息->28位
        //可变头信息->28位
        //具体可参考https://blog.csdn.net/tantion/article/details/82743942
        int profile = 2;  //AAC LC，MediaCodecInfo.CodecProfileLevel.AACObjectLC;
        int freqIdx = 4;  //44100, 见后面注释avpriv_mpeg4audio_sample_rates中32000对应的数组下标，来自ffmpeg源码
        int chanCfg = 1;  //见后面注释channel_configuration，Stero双声道立体声

        /*int avpriv_mpeg4audio_sample_rates[] = {
            96000, 88200, 64000, 48000, 44100, 32000,
                    24000, 22050, 16000, 12000, 11025, 8000, 7350
        };
        channel_configuration: 表示声道数chanCfg
        0: Defined in AOT Specifc Config
        1: 1 channel: front-center
        2: 2 channels: front-left, front-right
        3: 3 channels: front-center, front-left, front-right
        4: 4 channels: front-center, front-left, front-right, back-center
        5: 5 channels: front-center, front-left, front-right, back-left, back-right
        6: 6 channels: front-center, front-left, front-right, back-left, back-right, LFE-channel
        7: 8 channels: front-center, front-left, front-right, side-left, side-right, back-left, back-right, LFE-channel
        8-15: Reserved
        */
        // fill in ADTS data
        packet[0] = (byte)0xFF;
        packet[1] = (byte)0xF9;
        packet[2] = (byte)(((profile-1)<<6) + (freqIdx<<2) +(chanCfg>>2));
        packet[3] = (byte)(((chanCfg&3)<<6) + (packetLen>>11));
        packet[4] = (byte)((packetLen&0x7FF) >> 3);
        packet[5] = (byte)(((packetLen&7)<<5) + 0x1F);
        packet[6] = (byte)0xFC;
    }

}
