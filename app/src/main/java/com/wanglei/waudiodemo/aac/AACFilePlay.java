package com.wanglei.waudiodemo.aac;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import com.wanglei.waudiodemo.basic.AudioPlayer;

import java.nio.ByteBuffer;

/**
 * MediaCodec解码AAC文件，播放
 *
 * @author wanglei
 * @time 2019/6/17 16:40
 */
public class AACFilePlay {

    private static final String DEFAULT_MIME_TYPE = "audio/mp4a-latm";
    private MediaCodec mMediaCodec;
    //private AACDecoder mAudioDecoder;
    private AudioPlayer mAudioPlayer;
    private MediaExtractor mediaExtractor;
    private boolean isStop = false;
    private boolean mIsFirstFrame = true;

    public AACFilePlay() {
        mAudioPlayer = new AudioPlayer();
    }

    public void start(String filePath) {
        try {
            isStop = false;
            mMediaCodec = MediaCodec.createDecoderByType(DEFAULT_MIME_TYPE);
            if (mMediaCodec == null) {
                throw new IllegalStateException("该设备不支持AAC解码器");
            }
            mediaExtractor = new MediaExtractor();
            mediaExtractor.setDataSource(filePath);//设置要解析的数据源
            int videoTrackIndex = -1;
            int audioTrackIndex = -1;
            for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {
                //获取码流的详细格式/配置信息
                MediaFormat format = mediaExtractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("video/")) {
                    videoTrackIndex = i;
                } else if (mime.startsWith("audio/")) {
                    audioTrackIndex = i;
                    mMediaCodec.configure(format, null, null, 0);
                }
            }
            //设置选定音频，因为这里我们要读出音频数据来解码并播放
            mediaExtractor.selectTrack(audioTrackIndex);
            mMediaCodec.start();
            new Thread(mDecodeRenderRunnable).start();
            mAudioPlayer.startPlayer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        isStop = true;
        mediaExtractor.release();
        mAudioPlayer.stopPlayer();
        if (mMediaCodec != null) {
            Log.d("WL", "decoder close");
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;
        }
    }

    private Runnable mDecodeRenderRunnable = new Runnable() {

        @Override
        public void run() {
            while (!isStop) {
                //读取音频文件数据送入解码器
                ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
                int inputBufferIndex = mMediaCodec.dequeueInputBuffer(1000);
                if (inputBufferIndex >= 0) {
                    ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                    inputBuffer.clear();
                    int sampleSize = mediaExtractor.readSampleData(inputBuffer, 0);
                    long presentationTimeUs = mediaExtractor.getSampleTime();
                    Log.d("WL", "sampleSize " + sampleSize);
                    if (sampleSize < 0) {
                        isStop = true;
                        break;
                    }
                    if (mIsFirstFrame) {
                        /**
                         * Some formats, notably AAC audio and MPEG4, H.264 and H.265 video formats
                         * require the actual data to be prefixed by a number of buffers containing
                         * setup data, or codec specific data. When processing such compressed formats,
                         * this data must be submitted to the codec after start() and before any frame data.
                         * Such data must be marked using the flag BUFFER_FLAG_CODEC_CONFIG in a call to queueInputBuffer.
                         */
                        mMediaCodec.queueInputBuffer(inputBufferIndex, 0,
                                sampleSize, presentationTimeUs,
                                MediaCodec.BUFFER_FLAG_CODEC_CONFIG);
                        mIsFirstFrame = false;
                    } else {
                        mMediaCodec.queueInputBuffer(inputBufferIndex, 0,
                                sampleSize, presentationTimeUs, 0);
                    }
                    mediaExtractor.advance();//移动到下一帧
                    //获取解码数据
                    ByteBuffer[] outputBuffers = mMediaCodec.getOutputBuffers();
                    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                    int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 1000);
                    Log.d("WL", "outputBufferIndex " + outputBufferIndex);
                    if (outputBufferIndex >= 0) {
                        ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                        outputBuffer.position(bufferInfo.offset);
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                        byte[] outData = new byte[bufferInfo.size];
                        outputBuffer.get(outData);
                        outputBuffer.clear();
                        //播放
                        mAudioPlayer.play(outData, 0, outData.length);
                        mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                    }
                }
            }
            stop();
        }
    };
}
