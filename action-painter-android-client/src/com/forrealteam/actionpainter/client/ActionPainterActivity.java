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
import android.widget.LinearLayout;
import android.widget.TextView;

public class ActionPainterActivity extends Activity implements SensorEventListener 
{
	private static final String TAG = "ACTION_PAINTER";
	private static final String SERVER_IP = "http://action-painter.appspot.com/ip.jsp";
	// private static final double RAD = (double) 57.2957795;

	private TextView mTextView, mTextView2;
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
	
	private boolean debug = false;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		mPowerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
		cid = Math.abs(telephonyManager.getDeviceId().hashCode());
		
		setContentView(R.layout.main);
		mTextView = (TextView)findViewById(R.id.text);
		mTextView2 = new TextView(this);
		LinearLayout ll = (LinearLayout)findViewById(R.id.ll);
		ll.addView(mTextView2);
	}

	public void btnClick(View v) {
		debug = !debug;
	}
	
	private void sendSensorData() 
	{
		try
		{
			if (null == mSocket  || mSocket.isConnected() == false) {
				String ip = getServerIp();
				mSocket = new Socket(ip, 7890);
				out = mSocket.getOutputStream();
				Log.d(TAG,"reconnecting");
			}
			JSONObject obj = new JSONObject();
			obj.put("id", cid);
			obj.put("ax", digit3.format(threshold(accX,5)));
			obj.put("ay", digit3.format(threshold(accY,5)));
			obj.put("az", digit3.format(threshold(accZ,15)));
			obj.put("az", digit3.format(azimuth));
			obj.put("pt", digit3.format(pitch));
			obj.put("rl", digit3.format(roll));
			obj.put("sp", splash);

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
//		if (event.sensor.getType() ==  Sensor.TYPE_MAGNETIC_FIELD) {
//			mLastMag = event.values;
//
//			if (null != mLastAccl)
//			{
//				float[]  vals = new float[3];
//				SensorManager.getRotationMatrix(mRotationMatrix, null, mLastAccl, mLastMag);
//				SensorManager.getOrientation(mRotationMatrix, vals);
//
//				azimuth = vals[0] * RAD;
//				pitch 	= vals[1] * RAD;
//				roll	= vals[2] * RAD;
//				mTextView2.setText("Azimuth is: "+azimuth + " pitch " + pitch + " roll " + roll);
//				sendSensorData();
//			}
//		}
		if (event.sensor.getType() ==  Sensor.TYPE_ORIENTATION) {
			azimuth = event.values[0];
			pitch 	= event.values[1];
			roll	= event.values[2];
			if(debug) {
				mTextView2.setText("Azimuth: "+(int)azimuth + "\nPitch: " + (int)pitch + "\nRoll: " + (int)roll);
			}
		}		
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) 
		{
			final float alpha = (float) 0.8;

			// Movement
			gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
			gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
			gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];
			
			// Remove the gravity contribution with the high-pass filter.
			accX = event.values[0] - gravity[0];
			accY = event.values[1] - gravity[1];
			accZ = event.values[2] - gravity[2];

			String text = "acceleration-x: " + accX + "\nacceleration-y: " + accY + "\nacceleration-z: " + accZ;
			if (!text.equals("") & debug) {
				mTextView.setText(text);
				mTextView.invalidate();
			}

			//Log.d(TAG, "X " + x + " Y " + y + " Z " +z);
			float accelationSquareRoot = (accX * accX + accY * accY + accZ * accZ) / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);
			splash =  (accelationSquareRoot >= 3); // twice the accelation of earth
			// Toast.makeText(this, "Device was shuffed", Toast.LENGTH_SHORT).show();
			if (azimuth >= 0) { // orientation was read at least once
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

		mSensorManager.registerListener(this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
				SensorManager.SENSOR_DELAY_UI);

		mSensorManager.registerListener(this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
				SensorManager.SENSOR_DELAY_UI);

		mSensorManager.registerListener(this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_UI);
	} 
}