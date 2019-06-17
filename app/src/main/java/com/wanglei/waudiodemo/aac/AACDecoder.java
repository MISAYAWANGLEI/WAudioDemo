/*
 *  COPYRIGHT NOTICE
 *  Copyright (C) 2016, Jhuster <lujun.hust@gmail.com>
 *  https://github.com/Jhuster/AudioDemo
 *
 *  @license under the Apache License, Version 2.0
 *
 *  @file    AACDecoder.java
 *
 *  @version 1.0
 *  @author  Jhuster
 *  @date    2016/04/02
 */
package com.wanglei.waudiodemo.aac;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;

import java.io.IOException;
import java.nio.ByteBuffer;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class AACDecoder {

    private static final String DEFAULT_MIME_TYPE = "audio/mp4a-latm";
    private static final int DEFAULT_CHANNEL_NUM = 1;
    private static final int DEFAULT_SAMPLE_RATE = 44100;
    private static final int DEFAULT_MAX_BUFFER_SIZE = 16384;

    private MediaCodec mMediaCodec;
    private OnAudioDecodedListener mAudioDecodedListener;
    private boolean isStart = false;
    private boolean mIsFirstFrame = true;

    public interface OnAudioDecodedListener {
        void onFrameDecoded(byte[] decoded, long presentationTimeUs);
    }

    public boolean start() {
        if (isStart) {
            return true;
        }
        try {
            mMediaCodec = MediaCodec.createDecoderByType(DEFAULT_MIME_TYPE);
            MediaFormat format = new MediaFormat();
            format.setString(MediaFormat.KEY_MIME, DEFAULT_MIME_TYPE);
            format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, DEFAULT_CHANNEL_NUM);
            format.setInteger(MediaFormat.KEY_SAMPLE_RATE, DEFAULT_SAMPLE_RATE);
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, DEFAULT_MAX_BUFFER_SIZE);
            mMediaCodec.configure(format, null, null, 0);
            mMediaCodec.start();
            isStart = true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void close() {
        if (!isStart) {
            return;
        }
        mMediaCodec.stop();
        mMediaCodec.release();
        mMediaCodec = null;
        isStart = false;
    }

    public void setOnAccDecodedListener(OnAudioDecodedListener listener) {
        mAudioDecodedListener = listener;
    }

    public synchronized void decodeData(byte[] input) {
        if (!isStart) {
            return;
        }
        try {
            ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
            int inputBufferIndex = mMediaCodec.dequeueInputBuffer(1000);
            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();
                inputBuffer.put(input);
                if (mIsFirstFrame) {
                    /**
                     * Some formats, notably AAC audio and MPEG4, H.264 and H.265 video formats
                     * require the actual data to be prefixed by a number of buffers containing
                     * setup data, or codec specific data. When processing such compressed formats,
                     * this data must be submitted to the codec after start() and before any frame data.
                     * Such data must be marked using the flag BUFFER_FLAG_CODEC_CONFIG in a call to queueInputBuffer.
                     */
                    mMediaCodec.queueInputBuffer(inputBufferIndex, 0,
                            input.length, System.nanoTime() / 1000L, MediaCodec.BUFFER_FLAG_CODEC_CONFIG);
                    mIsFirstFrame = false;
                } else {
                    mMediaCodec.queueInputBuffer(inputBufferIndex, 0,
                            input.length, System.nanoTime() / 1000L, 0);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public synchronized void retrieveData() {
        if (!isStart) {
            return;
        }
        try {
            ByteBuffer[] outputBuffers = mMediaCodec.getOutputBuffers();
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 1000);
            if (outputBufferIndex >= 0) {
                ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                byte[] outData = new byte[bufferInfo.size];
                outputBuffer.get(outData);
                if (mAudioDecodedListener != null) {
                    mAudioDecodedListener.onFrameDecoded(outData, bufferInfo.presentationTimeUs);
                }
                mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
