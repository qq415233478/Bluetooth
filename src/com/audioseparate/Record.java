package com.audioseparate;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.Toast;

public class Record extends Activity {
	private static final String LOG_TAG = "Record";
	public final String PCM = ".pcm";
	public static final String GP3 = ".3gp";
	private AudioManager mAudioManager;
	private AlertDialog alertDialog;
	private String path;
	private Player player1;
	private AudioTrack player;
	private int audioBufSize;
	private boolean flag;
	private RecordThread mRecordThread;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_record);
		Switch sw = (Switch) findViewById(R.id.sw);
		sw.setChecked(true);
		flag = true;
		sw.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				flag = isChecked;
			}
		});
		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

		audioBufSize = AudioTrack.getMinBufferSize(8000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

		player = new AudioTrack(AudioManager.STREAM_MUSIC, 8000, AudioFormat.CHANNEL_OUT_MONO,
				AudioFormat.ENCODING_PCM_16BIT, audioBufSize, AudioTrack.MODE_STREAM);
	}

	public void onRecord(View view) {

		File dir = Environment.getExternalStorageDirectory();
		long time = System.currentTimeMillis();
		path = dir.getPath() + "/temp/" + time;

		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		if (!flag) {
			mRecordThread = new RecordThread(false, path + GP3);
		}

		// set title
		alertDialogBuilder.setTitle(flag ? "AudioRecord" : "MediaRecord");
		alertDialogBuilder.setMessage("Recording ...");
		// set dialog message
		alertDialogBuilder.setCancelable(false).setNeutralButton("Stop", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				// if this button is clicked, stop recording
				if (flag) {
					AudioRecordManager.getInstance().stopRecord();
				} else {
					mRecordThread.stopRecording();
				}

				mAudioManager.stopBluetoothSco();
				// To do: start separation and play back here
				dialog.dismiss();
			}
		});
		try {
			if (!flag) {
				mRecordThread.prepareRecording();
			}
			mAudioManager.setBluetoothScoOn(true);
			if (flag) {
				AudioRecordManager.getInstance().startRecord(path + PCM);
			} else {
				mRecordThread.start();
			}

		} catch (Exception e) {
			Toast.makeText(this, "Recording failed", Toast.LENGTH_SHORT).show();
			Log.e(LOG_TAG, "Record failed");
			if (flag) {
				AudioRecordManager.getInstance().stopRecord();
			} else {

			}
			// MIC.stopRecording();
			mAudioManager.stopBluetoothSco();
			this.finish();
		}

		// create alert dialog
		alertDialog = alertDialogBuilder.create();

		// show it
		alertDialog.show();

	}

	public void onPlay(View v) {
		player.play();
		player1 = new Player();
		player1.start();
	}

	class Player extends Thread {
		byte[] data1 = new byte[audioBufSize * 2];
		File file = new File(path + PCM);
		int off1;
		FileInputStream fileInputStream;
		int read;

		@Override
		public void run() {
			super.run();
			try {
				do {
					fileInputStream = new FileInputStream(file);
					fileInputStream.skip((long) off1);
					read = fileInputStream.read(data1, 0, audioBufSize * 2);
					player.write(data1, 0, audioBufSize * 2);
					off1 += audioBufSize * 2;
				} while (read > 0);
				player.stop();
			} catch (Exception e) {
				Log.d("1234", "e:" + e.getLocalizedMessage());
			}
		}
	}

	final class RecordThread extends Thread implements Runnable {

		public String mFile;
		protected boolean isBT;

		private MediaRecorder mRecorder;
		private static final String LOG_TAG = "Recording thread";

		public RecordThread(boolean isBT, String file) {
			this.isBT = isBT;
			this.mFile = file;
		}

		@Override
		public void run() throws RuntimeException {

			try {
				this.mRecorder.start();
			} catch (Exception e) {
				Log.e(LOG_TAG, "Run Method Exception");
				RuntimeException re = new RuntimeException();
				throw re;
			}

		}

		public void prepareRecording() throws Exception {
			this.mRecorder = new MediaRecorder();
			this.mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
			this.mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
			this.mRecorder.setOutputFile(mFile);
			this.mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

			try {
				this.mRecorder.prepare();
			} catch (IOException e) {
				Log.e(LOG_TAG, "prepare() failed");
				throw e;
			}
		}

		public void stopRecording() {
			try {
				mRecorder.stop();
				mRecorder.release();
				mRecorder = null;
			} catch (Exception e) {
				mRecorder = null;
			}
		}

	}
}