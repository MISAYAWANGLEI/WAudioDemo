package com.wanglei.waudiodemo.extractor;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.util.Log;

import com.wanglei.waudiodemo.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class Muxer {

    private static final String SDCARD_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() +
            File.separator;
    //输入文件路径 需要在手机SD卡提前有一个1.mp4文件
    private static final String INPUT_FILEPATH = SDCARD_PATH + "1.mp4";
    //输出音频文件路径
    private static final String OUTPUT_AUDIO_FILEPATH = SDCARD_PATH + "waudio.mp4";
    //输出视频文件路径
    private static final String OUTPUT_VIDEO_FILEPATH = SDCARD_PATH + "wvideo.mp4";

    //抽取并合成视频
    public boolean startMuxerVideo(){
        File file = new File(OUTPUT_VIDEO_FILEPATH);
        if (file.exists())file.delete();
            try {
                MediaMuxer videoMuxer = null;
                MediaExtractor extractor = new MediaExtractor();
                extractor.setDataSource(INPUT_FILEPATH);
                int framerate = -1;
                int videoTrackIndex = -1;
                for (int i = 0; i < extractor.getTrackCount(); i++) {
                    MediaFormat format = extractor.getTrackFormat(i);
                    String mime = format.getString(MediaFormat.KEY_MIME);
                    if (mime.startsWith("video")) {
                        extractor.selectTrack(i);
                        //获取帧率
                        framerate = format.getInteger(MediaFormat.KEY_FRAME_RATE);
                        Log.i("WL","framerate ->"+framerate);
                        videoMuxer = new MediaMuxer(OUTPUT_VIDEO_FILEPATH,
                                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                        /*这里有一个比较大的坑，就是，如果手动创建MediaFormat对象的话，一定要记得设置"csd-0"和"csd-1"这两个参数：
                        byte[] csd0 = {x,x,x,x,x,x,x...}
                        byte[] csd1 = {x,x,x,x,x,x,x...}
                        format.setByteBuffer("csd-0",ByteBuffer.wrap(csd0));
                        format.setByteBuffer("csd-1",ByteBuffer.wrap(csd1));
                        至于"csd-0"和"csd-1"是什么，对于H264视频的话，它对应的是sps和pps，
                        对于AAC音频的话，对应的是ADTS，做音视频开发的人应该都知道，
                        它一般存在于编码器生成的IDR帧之中。*/
                        videoTrackIndex = videoMuxer.addTrack(format);
                        videoMuxer.start();
                    }
                }

                if (videoMuxer == null) {
                    return false;
                }
                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                info.presentationTimeUs = 0;
                ByteBuffer buffer = ByteBuffer.allocate(1024 *1024* 2);
                while (true) {
                    int sampleSize = extractor.readSampleData(buffer, 0);
                    if (sampleSize < 0) {
                        break;
                    }
                    info.offset = 0;
                    info.size = sampleSize;
                    info.flags = extractor.getSampleFlags();
                    //时间戳间隔，也就是每一帧的间隔，单位是 us，
                    info.presentationTimeUs += 1000 * 1000 / framerate;
                    //是否为关键帧
                    boolean keyframe = (info.flags & MediaCodec.BUFFER_FLAG_SYNC_FRAME) > 0;
                    videoMuxer.writeSampleData(videoTrackIndex, buffer, info);
                    extractor.advance();
                }
                extractor.release();
                videoMuxer.stop();
                videoMuxer.release();
                Log.i("WL","video finished");
                return true;
            }catch (Exception e){
                e.printStackTrace();
                return false;
            }
    }

    //抽取并合成音频
    public boolean startMuxerAudio(){
        File file = new File(OUTPUT_AUDIO_FILEPATH);
        if (file.exists())file.delete();
        try {
            MediaMuxer audioMuxer = null;
            MediaExtractor extractor = new MediaExtractor();
            extractor.setDataSource(INPUT_FILEPATH);
            int aideoTrackIndex = -1;
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("audio")) {
                    extractor.selectTrack(i);
                    audioMuxer = new MediaMuxer(OUTPUT_AUDIO_FILEPATH,
                            MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                    aideoTrackIndex = audioMuxer.addTrack(format);
                    audioMuxer.start();
                }
            }
            if (audioMuxer == null) {
                return false;
            }
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            info.presentationTimeUs = 0;
            ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024 * 2);
            while (true) {
                int sampleSize = extractor.readSampleData(buffer, 0);
                if (sampleSize < 0) {
                    break;
                }
                info.offset = 0;
                info.size = sampleSize;
                info.flags = extractor.getSampleFlags();
                info.presentationTimeUs = extractor.getSampleTime();
                //是否为关键帧
                boolean keyframe = (info.flags & MediaCodec.BUFFER_FLAG_SYNC_FRAME) > 0;
                audioMuxer.writeSampleData(aideoTrackIndex, buffer, info);
                extractor.advance();
            }
            extractor.release();
            audioMuxer.stop();
            audioMuxer.release();
            Log.i("WL","audio finished");
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

}
