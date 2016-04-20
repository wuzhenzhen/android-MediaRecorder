
package com.example.android.service;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Intent;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.example.android.common.media.CameraHelper;

import java.io.IOException;
import java.util.List;

public class RecorderService extends Service {
	private static final String TAG = "RecorderService";
	private static Camera mCamera;
	private boolean isRecording;
	private MediaRecorder mMediaRecorder;
	
	@Override
	public void onCreate() {
		isRecording = false;
		new MediaPrepareTask().execute(null, null, null);
		super.onCreate();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);

		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		releaseMediaRecorder();
		releaseCamera();
		isRecording = false;
		super.onDestroy();
	}   

	public void closeService(){
		Intent stopServiceIntent = new Intent(getApplicationContext(),RecorderService.class);
		getApplicationContext().stopService(stopServiceIntent);
	}

	/**
	 * Asynchronous task for preparing the {@link android.media.MediaRecorder} since it's a long blocking
	 * operation.
	 */
	class MediaPrepareTask extends AsyncTask<Void, Void, Boolean> {

		@Override
		protected Boolean doInBackground(Void... voids) {
			// initialize video camera
			if (prepareVideoRecorder()) {
				// Camera is available and unlocked, MediaRecorder is prepared,
				// now you can start recording
				System.out.println("MediaRecorder----MediaPrepareTask--start");
				mMediaRecorder.start();
				isRecording = true;
				timeThread.start();
			} else {
				// prepare didn't work, release the camera
				releaseMediaRecorder();
				releaseCamera();
				return false;
			}
			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {

		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private boolean prepareVideoRecorder(){
		System.out.println("MediaRecorder----prepareVideoRecorder");
		// BEGIN_INCLUDE (configure_preview)
		mCamera = CameraHelper.getDefaultCameraInstance();
		mCamera.setDisplayOrientation(90);
		Camera.Parameters parameters = mCamera.getParameters();
		parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);//focusMode, 此处不能少，否则捕获video不清晰
		mCamera.setParameters(parameters);

		// Use the same size for recording profile.
		CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
		profile.fileFormat = 1;
		profile.quality = CamcorderProfile.QUALITY_HIGH;
		profile.videoCodec = 1;
//        profile.videoFrameRate = 14;

		// BEGIN_INCLUDE (configure_media_recorder)
		mMediaRecorder = new MediaRecorder();

		// Step 1: Unlock and set camera to MediaRecorder
		mCamera.unlock();
		mMediaRecorder.setCamera(mCamera);

		// Step 2: Set sources
		mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT );
		mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

		// Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
		mMediaRecorder.setProfile(profile); //profile  there will calling setOutputFormat,

		// Step 4: Set output file
		mMediaRecorder.setOutputFile(CameraHelper.getOutputMediaFile(
				CameraHelper.MEDIA_TYPE_VIDEO).toString());
		// END_INCLUDE (configure_media_recorder)

		// Step 5: Prepare configured MediaRecorder
		try {
			mMediaRecorder.prepare();
		} catch (IllegalStateException e) {
			Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
			releaseMediaRecorder();
			return false;
		} catch (IOException e) {
			Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
			releaseMediaRecorder();
			return false;
		}
		System.out.println("MediaRecorder----prepareVideoRecorder--return true");
		return true;
	}

	private void releaseMediaRecorder(){
		if (mMediaRecorder != null) {
			// clear recorder configuration
			mMediaRecorder.reset();
			// release the recorder object
			mMediaRecorder.release();
			mMediaRecorder = null;
			// Lock camera for later use i.e taking it back from MediaRecorder.
			// MediaRecorder doesn't need it anymore and we will release it if the activity pauses.
			mCamera.lock();
		}
	}

	private void releaseCamera(){
		if (mCamera != null){
			// release the camera for other applications
			mCamera.release();
			mCamera = null;
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