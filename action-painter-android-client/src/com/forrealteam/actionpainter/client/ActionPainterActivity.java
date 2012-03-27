package com.forrealteam.actionpainter.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

public class ActionPainterActivity extends Activity implements SensorEventListener 
{
	private static final int SPLASH_THRESHOLD = 5;
	private static final String TAG = "ACTION_PAINTER";
	private static final String SERVER_IP = "http://action-painter.appspot.com/ip.jsp";
	private static final double RAD = (double) 57.2957795;

	private TextView text1, text2, text3;
	private Button showMotionDataButton;
	private Socket mSocket;
	private OutputStream out; 

	private SensorManager mSensorManager;
	private PowerManager mPowerManager;
	private WakeLock mWakeLock;

	private float[] gravity = new float[3];
	private long cid;

	private DecimalFormat digit3 = new DecimalFormat("0.000");

	private float azimuth = -1, pitch = -1, roll = -1;
	private float accX,accY,accZ;
	boolean splash = false; 
	
	float[] mLastAccl = new float[3];
	float[] mLastMag = new float[3];
	float[] mRotationMatrix = new float[9];
			
	private boolean showMotionData = false;
	private boolean penDown = false;
	
	private long lastSplashTimestamp = System.currentTimeMillis();
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		mPowerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
		cid = Math.abs(telephonyManager.getDeviceId().hashCode());
		
		setContentView(R.layout.main);
		text1 = (TextView)findViewById(R.id.text1);
		text2 = (TextView)findViewById(R.id.text2);
		text3 = (TextView)findViewById(R.id.text3);
		showMotionDataButton = (Button)findViewById(R.id.showMotionData);
		showMotionDataButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(showMotionData) {
					showMotionDataButton.setText(R.string.show_motion_data);
					showMotionData = false;
					text1.setText("");
					text2.setText("");
				}
				else {
					showMotionDataButton.setText(R.string.hide_motion_data);
					showMotionData = true;
				}
			}
		});
		final ImageButton paintToggle = (ImageButton)findViewById(R.id.imageButton1);
		paintToggle.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				penDown = !penDown;
			}
		});
	}


	private void sendSensorData() 
	{
		try
		{
			if (null == mSocket  || mSocket.isConnected() == false) {
				text3.setText("Obtaining local server IP...");
				String ip = getServerIp();
				text3.setText("local server: " + ip);
				mSocket = new Socket(ip, 7890);
				out = mSocket.getOutputStream();
				Log.d(TAG,"reconnecting");
			}
			JSONObject obj = new JSONObject();
			obj.put("id", cid);
			obj.put("acx", digit3.format(threshold(accX,1)));
			obj.put("acy", digit3.format(threshold(accY,1)));
			obj.put("acz", digit3.format(threshold(accZ,1)));
			obj.put("az", digit3.format(azimuth));
			obj.put("pt", digit3.format(pitch));
			obj.put("rl", digit3.format(roll));
			obj.put("sp", splash);
			obj.put("pd", penDown);

			String str = obj.toString();
			out.write(toBytes(str.length()));
			out.write(str.getBytes());
			out.flush();
		}
		catch (JSONException e) {
			Log.e(TAG, "JSON Error: " + e.getMessage(), e);
		}
		catch (IOException io) {
			Log.e(TAG, "I/O Error: " + io.getMessage(), io);
			try {
				if (null != mSocket)
					mSocket.close();	
			}
			catch (IOException e) {
				Log.e(TAG, "I/O Error closing socket: " + e.getMessage(), e);
			}
			mSocket = null;
		}
	}

	private String getServerIp() throws IOException, ClientProtocolException {
		Log.d(TAG, "retrieving server IP from: " + SERVER_IP);
		HttpClient client = new DefaultHttpClient();
		HttpGet req = new HttpGet(SERVER_IP);
		HttpResponse resp = client.execute(req);
		InputStream content = resp.getEntity().getContent();
		BufferedReader reader = new BufferedReader(new InputStreamReader(content));
		StringBuilder buff = new StringBuilder();
		String line = reader.readLine();
		while(line != null) {
			buff.append(line).append('\n');
			line = reader.readLine();
		}
		content.close();
		String result = buff.toString().trim();
		Log.d(TAG, "got server IP: " + result);
		return result;
	}

	private float threshold(float val, float threshold) {
		if (val < threshold && val > (0-threshold)) {
			return 0;
		}
		return val;
	}
 
	private byte[] toBytes(int i)
	{
		byte[] result = new byte[2];
		result[1] = (byte) (i >> 8);
		result[0] = (byte) (i /*>> 0*/);

		return result;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	@Override
	public void onSensorChanged(SensorEvent event)
	{
		if (event.sensor.getType() ==  Sensor.TYPE_MAGNETIC_FIELD) {
			mLastMag = event.values;

			if (null != mLastAccl)
			{
				float[]  vals = new float[3];
				SensorManager.getRotationMatrix(mRotationMatrix, null, mLastAccl, mLastMag);
				SensorManager.getOrientation(mRotationMatrix, vals);

				azimuth = (float)(vals[0] /* RAD*/);
				pitch 	= (float)(vals[1] /* RAD*/);
				roll	= (float)(vals[2] /* RAD*/);
				if(showMotionData) {
					text2.setText("azimuth: "+(int)azimuth + ", pitch: " + (int)pitch + ", roll: " + (int)roll);
				}
			}
		}
		
//		if (event.sensor.getType() ==  Sensor.TYPE_ORIENTATION) {
//			azimuth = event.values[0];
//			pitch 	= event.values[1];
//			roll	= event.values[2];
//			if(debug) {
//				text2.setText("Azimuth: "+(int)azimuth + "\nPitch: " + (int)pitch + "\nRoll: " + (int)roll);
//			}
//		}		
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) 
		{
			mLastAccl = event.values;
			final float alpha = (float) 0.8;

			// Movement
			gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
			gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
			gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];
			
			// Remove the gravity contribution with the high-pass filter.
			accX = event.values[0] - gravity[0];
			accY = event.values[1] - gravity[1];
			accZ = event.values[2] - gravity[2];

			String text = "acc-x: " + digit3.format(accX) + ", acc-y: " + digit3.format(accY) + ", acc-z: " + digit3.format(accZ);
			if (showMotionData) {
				text1.setText(text);
				text1.invalidate();
			}

			//Log.d(TAG, "X " + x + " Y " + y + " Z " +z);
			// float accelationSquareRoot = (accX * accX + accY * accY + accZ * accZ) / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);
			
			splash =  (System.currentTimeMillis() - lastSplashTimestamp > 1000) && ((Math.abs(accZ) > SPLASH_THRESHOLD) && (Math.abs(accY) < SPLASH_THRESHOLD) && (Math.abs(accX) < SPLASH_THRESHOLD)); // (accelationSquareRoot >= 2); // twice the accelation of earth
			if(splash) {
				lastSplashTimestamp = System.currentTimeMillis(); 
				accX = 0;
				accY = 0;
				accZ = 0;
			}
			// Toast.makeText(this, "Device was shuffed", Toast.LENGTH_SHORT).show();
			if (mLastMag != null) { // orientation was read at least once
				sendSensorData();
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();	
		registerSensors();
		mWakeLock = mPowerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "ActivityWakeLock"); 
		mWakeLock.acquire();		
	}

	@Override
	protected void onPause() {		
		mSensorManager.unregisterListener(this);
		//mWakeLock.release();
		try {
			mSocket.close(); 
		}
		catch (IOException e) {
			Log.e(TAG, "failed closing socket: " + e.getMessage(), e);
		}
		mSocket = null;
		super.onPause();
	}

	private void registerSensors()
	{
		Sensor snr;
		List<Sensor> list = mSensorManager.getSensorList(Sensor.TYPE_ALL);
		for (int i=0; i<list.size(); i++)
		{
			snr = (Sensor)list.get(i);
			Log.d(TAG, "SENSOR: " + snr.getName());
		}

//		mSensorManager.registerListener(this,
//				mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
//				SensorManager.SENSOR_DELAY_UI);

		mSensorManager.registerListener(this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
				SensorManager.SENSOR_DELAY_UI);

		mSensorManager.registerListener(this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_UI);
	} 
}