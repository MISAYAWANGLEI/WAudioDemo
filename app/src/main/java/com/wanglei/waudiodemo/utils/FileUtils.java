package com.wanglei.waudiodemo.utils;

import android.os.Environment;

import java.io.File;
import java.io.IOException;

/**
 * Created by wanglei55 on 2018/10/9.
 */
public class FileUtils {
    public static String aacFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() +
            File.separator + "wl.aac";
    public static String wavFilePath = Environment.getExternalStorageDirectory().getAbsolutePath()
            + File.separator +"wl.wav";

    public static String createAACFilePath() {

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

    public static String createWavFilePath() {

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

}
