package com.example.test;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.Permission;

public class MainActivity extends AppCompatActivity {
    final String TAG = "myLogs";
    final String FILE_NAME = "file2.txt";
    final String FILENAME = "file.txt";
    int myBufferSize = 8192;
    AudioRecord audioRecord;
    boolean isReading = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


    }

    public void saveText(String message){

        FileOutputStream fos = null;
        try {
            fos = openFileOutput(FILE_NAME, MODE_APPEND);
            fos.write(message.getBytes());
        }
        catch(IOException ex) {

        }
        finally{
            try{
                if(fos!=null)
                    fos.close();
            }
            catch(IOException ex){
            }
        }
    }

    public void onClick(View v) throws IOException {
        TextView textView = (TextView)findViewById(R.id.textView);
        TextView textViewTick = (TextView)findViewById(R.id.textViewTick);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                openFileOutput(FILENAME, MODE_APPEND)));
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        int sampleRate = 48000;
        int channelConfig = AudioFormat.CHANNEL_OUT_DEFAULT;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

        int minInternalBufferSize = AudioRecord.getMinBufferSize(sampleRate,
                channelConfig, audioFormat);
        int internalBufferSize = minInternalBufferSize;
        Log.d(TAG, "minInternalBufferSize = " + minInternalBufferSize
                + ", internalBufferSize = " + internalBufferSize
                + ", myBufferSize = " + myBufferSize);

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                sampleRate, channelConfig, audioFormat, internalBufferSize);
        Log.d(TAG, "record start");
        audioRecord.startRecording();
        int recordingState = audioRecord.getRecordingState();
        Log.d(TAG, "recordingState = " + recordingState);
        Log.d(TAG, "read start");
        isReading = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (audioRecord == null){
                    Log.d(TAG, "empty");
                    return;
                }
                short[] myBuffer = new short[internalBufferSize];
                int readCount = 0;
                int countTik = 0;
                int count = 0;
                boolean flag = false;
                int startTime = (int)System.currentTimeMillis();
                int startDoTime = (int)System.currentTimeMillis();;
                int tiks = 5;
                float currentRadiation;
                float currentTime;
                int filter_order = 7;
                int measurmunt_count = 0;
                float[] radiation = new float[filter_order];
                float average_radiation;
                saveText("Новый замер\n");
                while (isReading) {
                    readCount = audioRecord.read(myBuffer, 0, internalBufferSize);
                    for(int i = 0; i < internalBufferSize; i++){
                        short buffer = (short) Math.abs(myBuffer[i]);
                        if(buffer > 1900){
                            if(!flag){
                                flag = true;
                            }
                        }
                        else{
                            if (flag){
                                flag = false;
                                countTik = countTik + 1;
                                count = count + 1;
                                Log.d(TAG, "Tik №" + countTik);
                                int finalCount = count;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        textViewTick.setText("Count: " + Integer.toString(finalCount));
                                        }
                                });
                                if (countTik == tiks) {
                                    currentRadiation =(float)  ((60000 / ((int)System.currentTimeMillis() - startTime) * tiks * 0.006645));
                                    float finalCurrentRadiation = currentRadiation;
                                    currentTime = ((int)System.currentTimeMillis()- startDoTime);
                                    saveText(Float.toString(currentTime) + "\t" +  currentRadiation + "\n");
                                    countTik = 0;
                                    measurmunt_count++;
                                    startTime = (int)System.currentTimeMillis();
                                    if (measurmunt_count <= filter_order){
                                        radiation[measurmunt_count - 1] = currentRadiation;
                                        float sum = 0;
                                        for(int j = 0; j < measurmunt_count; j++){
                                            sum += radiation[j];
                                        }
                                        average_radiation = sum / measurmunt_count;
                                    }
                                    else{
                                        float sum = 0;
                                        for (int j = 0; j < filter_order - 1; j++){
                                            radiation[j] = radiation[j+1];
                                            sum += radiation[j];
                                        }
                                        radiation[filter_order - 1] = currentRadiation;
                                        sum += currentRadiation;
                                        average_radiation = sum / filter_order;
                                    }
                                    Log.d(TAG, currentTime + "\t" + currentRadiation + "\t" + average_radiation);
                                    float finalAverage_radiation = average_radiation;
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            textView.setText(Float.toString(finalAverage_radiation) +"uSv/h");
                                        }
                                    });
                                }

                            }
                        }
                        try {
                            bw.write(Short.toString(myBuffer[i]));
                            bw.write("\n");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }
    public void stop(View v){
        Log.d(TAG, "record stop");
        audioRecord.stop();
        Log.d(TAG, "read stop");
        isReading = false;
    }
    protected void onDestroy() {
        super.onDestroy();

        isReading = false;
        if (audioRecord != null) {
            audioRecord.release();
        }
    }
}



