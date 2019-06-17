package com.wanglei.waudiodemo.aac;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 利用MediaCodec將pcm数据编码为ACC
 */
public class AACEncoder{

    public static final int DEFAULT_BIT_RATE = 128 * 1024; //128kb //AAC-LC, 64 *1024 for AAC-HE
    public static final int DEFAULT_SIMPLE_RATE = 44100; //44100Hz
    public static final int DEFAULT_CHANNEL_COUNTS = 1;
    public static final int DEFAULT_MAX_INPUT_SIZE = 16384; //16k
    private static final int DEFAULT_PROFILE_LEVEL = MediaCodecInfo.CodecProfileLevel.AACObjectLC;
    private static final String DEFAULT_MIME_TYPE = "audio/mp4a-latm";

    private MediaCodec mediaCodec;

    private OnAACEncodeListener onAACEncodeListener;

    private boolean isStart = false;

    public void setOnAACEncodeListener(OnAACEncodeListener onAACEncodeListener) {
        this.onAACEncodeListener = onAACEncodeListener;
    }

    public boolean start() {
        if (isStart){
            return true;
        }
        try {
            MediaFormat mediaFormat = new MediaFormat();
            mediaFormat.setString(MediaFormat.KEY_MIME, DEFAULT_MIME_TYPE);
            mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, DEFAULT_CHANNEL_COUNTS);
            mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, DEFAULT_SIMPLE_RATE);
            mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE,DEFAULT_PROFILE_LEVEL);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, DEFAULT_BIT_RATE);
            mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, DEFAULT_MAX_INPUT_SIZE);
            mediaCodec = MediaCodec.createEncoderByType(DEFAULT_MIME_TYPE);
            if (mediaCodec == null) {
                throw new IllegalStateException("该设备不支持AAC编码器");
            }
            mediaCodec.configure(mediaFormat, null, null,
                    MediaCodec.CONFIGURE_FLAG_ENCODE);
            mediaCodec.start();
            isStart = true;
        }catch (IOException e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    //采集的PCM数据送入MediaCodec进行编码
    public synchronized void encodeData(byte[] data) {
        if (!isStart) {
            return;
        }
        try {
            if (data != null) {
                ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
                int bufferIndexId = mediaCodec.dequeueInputBuffer(1000);
                if (bufferIndexId >= 0) {
                    ByteBuffer inputBuffer = inputBuffers[bufferIndexId];
                    inputBuffer.clear();
                    inputBuffer.put(data);
                    mediaCodec.queueInputBuffer(bufferIndexId, 0,
                            data.length, System.nanoTime() / 1000L,
                            0);
                }
            }
        }catch (Throwable t){
            t.printStackTrace();
        }
    }

    //从MediaCodec中取出编码后的数据
    public synchronized void retriveData() {
        if (mediaCodec == null) {
            return;
        }
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
        int outputBufferIndexId = mediaCodec.dequeueOutputBuffer(bufferInfo, 1000);
        if (outputBufferIndexId >= 0) {
            ByteBuffer byteBuffer = outputBuffers[outputBufferIndexId];
            byteBuffer.position(bufferInfo.offset);
            byteBuffer.limit(bufferInfo.offset + bufferInfo.size);
            byte[] frame = new byte[bufferInfo.size];
            byteBuffer.get(frame, 0, bufferInfo.size);
            //解码后的数据回调给外部
            if (onAACEncodeListener != null) {
                onAACEncodeListener.onEncodedFrame(frame);
            }
            mediaCodec.releaseOutputBuffer(outputBufferIndexId, false);
        }
    }

    public void close() {
        if (!isStart) {
            return;
        }
        if (mediaCodec != null) {
            mediaCodec.stop();
            mediaCodec.release();
            mediaCodec = null;
        }
        isStart = false;
    }

    public interface OnAACEncodeListener {
        void onEncodedFrame(byte[] data);
    }

}
