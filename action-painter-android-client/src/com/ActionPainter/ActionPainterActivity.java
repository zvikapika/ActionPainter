package com.ActionPainter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import io.socket.*;

public class ActionPainterActivity extends Activity implements SensorEventListener, IOCallback 
{
	public static final double RAD =(double) 57.2957795;

	TextView mTextView, mTextView2;

	private Socket mSocket; 
	private OutputStream out; 
	private SensorManager mSensorManager;
	private float lastX = 0;
	private float lastY = 0;
	private float lastZ = 0;
	private float[] mLastMag;
	private float[] mLastAccl;
	private float[] mRotationMatrix;

	private double azimuth,pitch,roll;
	private float x,y,z;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

		setContentView(R.layout.main);
		mTextView = (TextView)findViewById(R.id.text);
		mTextView2 = new TextView(this);
		LinearLayout ll = (LinearLayout)findViewById(R.id.ll);
		ll.addView(mTextView2);
		mRotationMatrix = new float[9];
	}

	public void btnClick (View v) {


	}



	private void sendSensorData() 
	{
		try
		{
			if (null == mSocket  || mSocket.isConnected() == false) {
				mSocket = new Socket("10.0.0.27",7890);
				out = mSocket.getOutputStream();
				Log.d("BUGBUG","reconnect");
			}
			JSONObject obj = new JSONObject();

			
				
			obj.put("x", threshold(x,5));
			obj.put("y", threshold(y,5));
			obj.put("z", threshold(z,15));
			obj.put("azimuth", azimuth);
			obj.put("pitch", pitch);
			obj.put("roll", roll);

			String str = obj.toString();
			out.write(toBytes(str.length()));
			out.write(str.getBytes());
			out.flush();
//			mSocket.close();
//			mSocket = null;
		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();  
		}
		catch (IOException io) {
			
			
			io.printStackTrace();
			try
			{
				if (null != mSocket)
					mSocket.close();	
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			mSocket=null;
		}
	}
 
	private float threshold(float val, int threshold) {
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
	public void onAccuracyChanged(Sensor sensor, int accuracy)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onSensorChanged(SensorEvent event)
	{
		if (event.sensor.getType() ==  Sensor.TYPE_MAGNETIC_FIELD)
		{

			mLastMag = event.values;

			if (null != mLastAccl)
			{
				float[]  vals = new float[3];
				SensorManager.getRotationMatrix(mRotationMatrix, null,mLastAccl, mLastMag);
				SensorManager.getOrientation(mRotationMatrix, vals);

				azimuth 	= vals[0] * RAD;
				pitch 	= vals[1] * RAD;
				roll		= vals[2] * RAD;
				mTextView2.setText("Azimuth is: "+azimuth + " pitch " + pitch + " roll " + roll);

				sendSensorData();
			}
		}		

		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) 
		{
			mLastAccl = event.values;

			float[] values = event.values;
			// Movement
			x = values[0];
			y = values[1];
			z = values[2];

			//do some stuff here


			String text = "x " + x + " y " + y + " " + z;
			/*if ((lastX - x ) > 0.3)
				text += " moving on X";
			if ((lastY - y) > 0.3)
				text += " moving on Y";
			if ((lastZ - z ) > 0.3)
				text += " moving on z ";
			 */
			if (!text.equals(""))
			{
				mTextView.setText(text);
				mTextView.invalidate();
			}
			lastX = x;
			lastY = y;
			lastZ = z;

			//Log.d("tag","X " + x + " Y " + y + " Z " +z);
			float accelationSquareRoot = (x * x + y * y + z * z)
					/ (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);
			if (accelationSquareRoot >= 4) // twice the accelation of earth
				Toast.makeText(this, "Device was shuffed", Toast.LENGTH_SHORT).show();
		}

	}

	@Override
	protected void onResume() {
		super.onResume();	
		registerSensor();
	}

	@Override
	protected void onPause() {		
		mSensorManager.unregisterListener(this);
		try
		{
			mSocket.close(); 
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		mSocket = null;
		super.onPause();
	}

	private void registerSensor()
	{
		Sensor snr;
		List<Sensor> list = mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
		for (int i=0; i<list.size(); i++)
		{
			snr = (Sensor)list.get(i);
		}

		mSensorManager.registerListener(this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
				SensorManager.SENSOR_DELAY_UI);

		mSensorManager.registerListener(this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_UI);
	} 

	@Override
	public void onDisconnect()
	{
		// TODO Auto-generated method stub
		Log.d("BUGBUG","Disconnect@!");
	}

	@Override
	public void onConnect()
	{
		// TODO Auto-generated method stub
		Log.d("BUGBUG","Connect");
	}

	@Override
	public void onMessage(String data, IOAcknowledge ack)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onMessage(JSONObject json, IOAcknowledge ack)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void on(String event, IOAcknowledge ack, Object... args)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onError(SocketIOException socketIOException)
	{
		Log.d("BUGBUG","On Error! ");
		socketIOException.printStackTrace();

	}
}