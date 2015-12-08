package com.prim.accelerometer;

/*import com.example.accelerometer_nextactivity.MainActivity;
import com.example.accelerometer_nextactivity.NextPage;*/

import dev.baalmart.prim.R;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.text.Html;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;
 
public class XYZ extends Activity implements SensorEventListener {
	
	long lastTime;
    private long now = 0;
    private long timeDiff = 0;
    private long lastUpdate = 0;
    private long lastShake = 0;  
    private float lastX = 0;
    private float lastY = 0;
    private float lastZ = 0;
    private float force = 0;
    
	private SensorManager sensorManager;
	
	TextView x;
	TextView y;
	TextView z;
	
	String sx, sy, sz;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.acc_main);
		
		x = (TextView) findViewById (R.id.textView2);
		y = (TextView) findViewById (R.id.textView3);
		z = (TextView) findViewById (R.id.textView4);
		
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		
		sensorManager.registerListener(this, sensorManager.getDefaultSensor
				(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
		 lastTime = System.currentTimeMillis();
		
		// I want to create some notifications in case the accuracy of the entire app changes
		 //I will get back to this later on.....
		           NotificationManager notificationManager = (NotificationManager) 
				   getSystemService(NOTIFICATION_SERVICE);		
		      
	 }
 
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
 
	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) 
	{
		// TODO Auto-generated method stub
		
	}
 
	@Override
	public void onSensorChanged(SensorEvent event) 
	   {
		// TODO Auto-generated method stub
		
		//getting the type of sensor in question....
		if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
		{			
				
			/*float xVal = event.values[0];
			float yVal = event.values[1];
			float zVal = event.values[2];			
			sx = "X Value : <font color = '#800080'> " + xVal + "</font>";
			sy = "Y Value : <font color = '#800080'> " + yVal + "</font>";
			sz = "Z Value : <font color = '#800080'> " + zVal + "</font>";			
			x.setText(Html.fromHtml(sx));
			y.setText(Html.fromHtml(sy));
			z.setText(Html.fromHtml(sz));*/			
			getAccelerometer(event);
		}
		}
	
	//getting the accelerometer readings...
private void getAccelerometer(SensorEvent event) 
     {	
		float[] value = event.values;
		
		float xVal = value[0];
		float yVal = value[1];
		float zVal = value[2];
		
		float accelationSquareRoot = (xVal*xVal + yVal*yVal + zVal*zVal) 
				/ (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);
		
		long actualTime = System.currentTimeMillis();
		//long timeNow = System.nanoTime();
		
		//I have changed the value from 1.2...initiall was at 2.
		//if(accelationSquareRoot > SensorManager.GRAVITY_EARTH)
		if(accelationSquareRoot >= 1.2) 
		    {			
			if(actualTime-lastTime < 2000000000) 
			{				
				//return;
				sx = "X Value : <font color = '#800080'> " + xVal + "</font>";
				sy = "Y Value : <font color = '#800080'> " + yVal + "</font>";
				sz = "Z Value : <font color = '#800080'> " + zVal + "</font>";			
				x.setText(Html.fromHtml(sx));
				y.setText(Html.fromHtml(sy));
				z.setText(Html.fromHtml(sz));
			}			
			lastTime = actualTime;
			
			// just to start another activity from here...
			
			/*Intent i = new Intent(MainActivity.this, NextPage.class);
			startActivity(i);
			*/
			/*Toast.makeText(this, "Your Next Activity is successfully called",
					Toast.LENGTH_SHORT).show();*/
			
			/*Toast.makeText(this, "the values are +x,+y,+z " ,
					Toast.LENGTH_SHORT).show();*/
			
			//finish();
		}
	}
           @Override
           protected void onPause() 
        {
	       // TODO Auto-generated method stub
	       super.onPause();
	       sensorManager.unregisterListener(this);
        }

           
           @Override
          protected void onResume() 
           {
	// TODO Auto-generated method stub
	      super.onResume();
	      sensorManager.registerListener(this, sensorManager.getDefaultSensor
			(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
	        lastTime = System.currentTimeMillis();
           }
     
           @Override
           public void onDestroy() 
           {
        	   super.onDestroy();
        	   sensorManager.unregisterListener(this);   	   
           
           }
           
           @Override
           public void onStop() 
           {
        	   super.onStop();
        	   sensorManager.unregisterListener(this);
        	   
        	   
           }
	
	
}

