package com.wanglei.waudiodemo;

import android.Manifest;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.wanglei.waudiodemo.aac.AACFileCapture;
import com.wanglei.waudiodemo.basic.AudioTest;
import com.wanglei.waudiodemo.utils.FileUtils;
import com.wanglei.waudiodemo.wav.WaveDecoder;
import com.wanglei.waudiodemo.wav.WaveEncoder;

import java.io.FileNotFoundException;

import kr.co.namee.permissiongen.PermissionFail;
import kr.co.namee.permissiongen.PermissionGen;
import kr.co.namee.permissiongen.PermissionSuccess;

public class MainActivity extends AppCompatActivity {

    private AudioTest audioTest;//实时录制与播放
    private WaveEncoder waveEncoder;
    private WaveDecoder waveDecoder;
    private AACFileCapture aacFileCapture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        aacFileCapture = new AACFileCapture();
        audioTest = new AudioTest();
        waveEncoder = new WaveEncoder();
        waveDecoder = new WaveDecoder();
        try {
            waveEncoder.prepare(FileUtils.createWavFilePath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        PermissionGen.with(MainActivity.this)
                .addRequestCode(100)
                .permissions(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .request();
    }


    public void start(View view){
        if (audioTest !=null){
            audioTest.start();
        }
    }

    public void stop(View view){
        if (audioTest !=null){
            audioTest.stop();
        }
    }

    public void wavstart(View view){
        if (waveEncoder!=null){
            waveEncoder.start();
        }
    }

    public void wavstop(View view){
        if (waveEncoder!=null){
            waveEncoder.stop();
        }
    }

    public void wavplaystart(View view) {
        waveDecoder.start(FileUtils.wavFilePath);
    }

    public void wavplaystop(View view) {
        waveDecoder.stop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        PermissionGen.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    public void aacstart(View view) {
        aacFileCapture.start(FileUtils.createAACFilePath());
    }

    public void aacstop(View view) {
        aacFileCapture.stop();
    }

    @PermissionSuccess(requestCode = 100)
    public void doSomething(){
        Toast.makeText(this, "PermissionSuccess", Toast.LENGTH_SHORT).show();
    }

    @PermissionFail(requestCode = 100)
    public void doFailSomething(){
        Toast.makeText(this, "PermissionFail", Toast.LENGTH_SHORT).show();
    }
}
