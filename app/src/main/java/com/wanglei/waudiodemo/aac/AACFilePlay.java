package com.wanglei.waudiodemo.aac;

import android.media.MediaExtractor;
import android.media.MediaFormat;

import com.wanglei.waudiodemo.basic.AudioCapture;
import com.wanglei.waudiodemo.basic.AudioPlayer;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.nio.ByteBuffer;

/**
 *MediaCodec解码AAC文件，播放
*@author wanglei
*@time 2019/6/17 16:40
*/
public class AACFilePlay implements AACDecoder.OnAudioDecodedListener{

    private AACDecoder mAudioDecoder;
    private AudioPlayer mAudioPlayer;
    private MediaExtractor mediaExtractor;
    private boolean isStop = false;

    public AACFilePlay(){
        mAudioPlayer = new AudioPlayer();
        mAudioDecoder = new AACDecoder();
    }

    public void start(String filePath) {
        mAudioDecoder.setOnAccDecodedListener(this);
        initMediaExtractor(filePath);
        new Thread(mDecodeRenderRunnable).start();
        new Thread(mRetriveDataRunnable).start();
        mAudioPlayer.startPlayer();
    }

    private void initMediaExtractor(String filePath) {
        try {
            mediaExtractor = new MediaExtractor();
            mediaExtractor.setDataSource(filePath);//设置要解析的数据源
            int videoTrackIndex = -1;
            int audioTrackIndex = -1;
            for(int i = 0; i < mediaExtractor.getTrackCount(); i++) {
                //获取码流的详细格式/配置信息
                MediaFormat format = mediaExtractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if(mime.startsWith("video/")) {
                    videoTrackIndex = i;
                }
                else if(mime.startsWith("audio/")) {
                    audioTrackIndex = i;
                    mAudioDecoder.start(null);
                }
            }
            //设置选定音频，因为这里我们要读出音频数据来解码并播放
            mediaExtractor.selectTrack(audioTrackIndex);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void stop() {
        isStop = true;
        mAudioPlayer.stopPlayer();
    }

    private Runnable mRetriveDataRunnable = new Runnable() {

        ByteBuffer buffer = ByteBuffer.allocate(1024 * 8);
        @Override
        public void run() {
            while (!isStop) {
                buffer.clear();
                int ret = mediaExtractor.readSampleData(buffer,0);
                if (ret < 0){
                    isStop = true;
                    break;
                }
                //读取出的数据送入解码器解码
                mAudioDecoder.decodeData(buffer.array());
                mediaExtractor.advance();//移动到下一帧
            }
            mediaExtractor.release();//读取结束后，要记得释放资源
        }
    };

    private Runnable mDecodeRenderRunnable = new Runnable() {
        @Override
        public void run() {
            while (!isStop) {
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
