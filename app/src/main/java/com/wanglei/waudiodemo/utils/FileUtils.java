package com.wanglei.waudiodemo.utils;

import android.os.Environment;

import java.io.File;
import java.io.IOException;

/**
 * Created by wanglei55 on 2018/10/9.
 */
public class FileUtils {
    private static String aacFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() +
            File.separator + "wl.aac";
    private static String wavFilePath = Environment.getExternalStorageDirectory().getAbsolutePath()
            + File.separator +"wl.wav";

    public static String getAACFilePath() {

        File file = new File(aacFilePath);
            try {
                if (file.exists())
                    file.delete();
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        return aacFilePath;
    }

    public static String getAACFilePlayPath() {

        File file = new File(aacFilePath);
        if (file.length() <= 0){
            throw new IllegalArgumentException("aac文件size <= 0");
        }
        return aacFilePath;
    }

    public static String getWavFilePath() {

        File file = new File(wavFilePath);
            try {
                if (file.exists())
                    file.delete();
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        return wavFilePath;
    }

    public static String getWavFilePlayPath() {

        File file = new File(wavFilePath);
        if (file.length() <= 0){
            throw new IllegalArgumentException("aac文件size <= 0");
        }
        return wavFilePath;
    }

}
