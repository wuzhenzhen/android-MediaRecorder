
package com.example.android.service;

import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.android.common.media.CameraHelper;

import java.io.IOException;

/**
 * Created by kgd on 2016/04/20.
 * wuzhenzhen@tiamaes.com
 */
public class AudioRecorderService extends Service{
    private MediaRecorder mMediaRecorder;
    private boolean isRecording;
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopRecording();
    }

    @Override
    public void onCreate() {
        isRecording = false;
        new MediaAudioPrepareTask().execute(null,null,null);
        super.onCreate();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Asynchronous task for preparing the {@link android.media.MediaRecorder} since it's a long blocking
     * operation.
     */
    class MediaAudioPrepareTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            // initialize video camera
            if (prepareAudioRecorder()) {
                // MediaRecorder is prepared, now you can start recording
                mMediaRecorder.start();
                isRecording = true;
                timeThread.start();
            } else {
                // prepare didn't work, release the mediarecorder
                stopRecording();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Log.d("AudioRecorderService","AudioRecorderService is recording");
        }
    }

    private boolean prepareAudioRecorder(){
        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mMediaRecorder.setOutputFile(CameraHelper.getOutputMediaFile(3).toString());
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            Log.e("prepareAudioRecorder", "prepareAudioRecorder() failed");
            return false;
        }
//        mMediaRecorder.start();
        return true;
    }

    private void stopRecording() {
        if (mMediaRecorder != null) {
            mMediaRecorder.stop();
//        mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    closeService();
                    break;
            }
        }
    };

    public void closeService(){
        Intent stopServiceIntent = new Intent(this,AudioRecorderService.class);
        stopService(stopServiceIntent);
    }

    Thread timeThread = new Thread(){
        @Override
        public void run() {
            super.run();
            long current = System.currentTimeMillis();
            long endCurrent = current + 10*1000;
            while (current < endCurrent) {
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                current = System.currentTimeMillis();
            }
            mHandler.sendEmptyMessage(1);
        }
    };
}
