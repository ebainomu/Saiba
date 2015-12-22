package com.prim.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.location.GpsStatus.Listener;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.prim.MainActivity;
import com.prim.Others;
import com.prim.IssueList;
import com.prim.actions.GetAccelerometerValues;
import com.prim.actions.NameRoute;
import com.prim.custom.CustomFragment;
import com.prim.db.Prim.Labels;
import com.prim.logger.GPSLoggerService;
import com.prim.logger.GPSLoggerServiceManager;
import com.prim.logger.IGPSLoggerServiceRemote;
import com.prim.model.Data;
import com.prim.streaming.StreamUtils;
import com.prim.utils.Constants;

import dev.baalmart.prim.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

@SuppressWarnings("deprecation")
public class MainFragment extends CustomFragment  implements 
LocationListener , SensorEventListener, SensorListener
{
	

	   private static final float FINE_DISTANCE = 5F;
	   private static final long  FINE_INTERVAL = 1000l;
	   private static final float FINE_ACCURACY = 20f;
	   
	   private static final float NORMAL_DISTANCE = 10F;
	   private static final long  NORMAL_INTERVAL = 15000l;
	   private static final float NORMAL_ACCURACY = 30f;

	   private static final float COARSE_DISTANCE = 25F;
	   private static final long  COARSE_INTERVAL = 30000l;
	   private static final float COARSE_ACCURACY = 75f;
	   
	   private static final float GLOBAL_DISTANCE = 500F;
	   private static final long  GLOBAL_INTERVAL = 300000l;
	   private static final float GLOBAL_ACCURACY = 1000f;

	   /**
	    * <code>MAX_REASONABLE_SPEED</code> is about 324 kilometer per hour or 201
	    * mile per hour.
	    */
	   private static final int MAX_REASONABLE_SPEED = 90;

	   /**
	    * <code>MAX_REASONABLE_ALTITUDECHANGE</code> between the last few waypoints
	    * and a new one the difference should be less then 200 meter.
	    */
	   private static final int MAX_REASONABLE_ALTITUDECHANGE = 200;

	   private static final boolean VERBOSE = false;


	   private static final String SERVICESTATE_DISTANCE = "SERVICESTATE_DISTANCE";
	   private static final String SERVICESTATE_STATE = "SERVICESTATE_STATE";
	   private static final String SERVICESTATE_PRECISION = "SERVICESTATE_PRECISION";
	   private static final String SERVICESTATE_SEGMENTID = "SERVICESTATE_SEGMENTID";
	   private static final String SERVICESTATE_TRACKID = "SERVICESTATE_TRACKID";

	   private static final int ADDGPSSTATUSLISTENER = 0;
	   private static final int REQUEST_FINEGPS_LOCATIONUPDATES = 1;
	   private static final int REQUEST_NORMALGPS_LOCATIONUPDATES = 2;
	   private static final int REQUEST_COARSEGPS_LOCATIONUPDATES = 3;
	   private static final int REQUEST_GLOBALNETWORK_LOCATIONUPDATES = 4;
	   private static final int REQUEST_CUSTOMGPS_LOCATIONUPDATES = 5;
	   private static final int STOPLOOPER = 6;
	   private static final int GPSPROBLEM = 7;


	   /**
	    * DUP from android.app.Service.START_STICKY
	    */
	   private static final int START_STICKY = 1;

	   public static final String COMMAND = "com.prim.extra.COMMAND";
	   public static final int EXTRA_COMMAND_START = 0;
	   public static final int EXTRA_COMMAND_PAUSE = 1;
	   public static final int EXTRA_COMMAND_RESUME = 2;
	   public static final int EXTRA_COMMAND_STOP = 3;



	   /**
	    * If broadcasts of location about should be sent to stream location
	    */
	   private boolean mStreamBroadcast;

	  
	   private long mLabel = -1;
	   private long mSegmentId = -1;
	   private long mWaypointId = -1;
	   private long mLocationId = -1;
	   private long mCheckPeriod;

	   private float mBroadcastDistance;

	   private long mLastTimeBroadcast;
	

	   private String mSources;

	   private Location mPreviousLocation;
	   private float mDistance;
	

	   private Vector<Location> mWeakLocations;
	   private Queue<Double> mAltitudes;


	
//declarations	
private Sensor sensor;
protected static final String TAG = null;
private ArrayList<Data> iList;
private static final Boolean DEBUG = false;
private int mPrecision;
private boolean mShowingGpsDisabled;
private boolean mStartNextSegment;
private float mMaxAcceptableAccuracy = 20;


private LocationManager mLocationManager;
private PowerManager.WakeLock mWakeLock;
private Handler mHandler;

/**
 * If speeds should be checked to sane values
 */
private boolean mSpeedSanityCheck;


/**
 * Time thread to runs tasks that check whether the GPS listener has received
 * enough to consider the GPS system alive.
 */
private Timer mHeartbeatTimer;
private NotificationManager mNoticationManager;
private static final int LOGGING_UNAVAILABLE = R.string.service_connectiondisabled;
private GPSLoggerServiceManager mLoggerServiceManager;
private IGPSLoggerServiceRemote mGPSLoggerRemote;
private GPSLoggerService mLoggerService;
private NameRoute nameRoute;
GetAccelerometerValues getAccelerometerValues;
//private static final Boolean GPSenabled = false; 
private int mLoggingState = Constants.STOPPED;

String labelName = null;
Context context = null;
Long labelTime = null;
private long mLabelId = -1;
private Notification mNotification;
private int mSatellites = 0;
/**
 * Should the GPS Status monitor update the notification bar
 */
private boolean mStatusMonitor;

private SensorManager sensorManager;
long lastTime;

Uri mLabelUri;
private Activity mActivity;

@Override
public void onAttach(Activity activity) {
    super.onAttach(activity);
    mActivity = activity;
}

  private void loadDummyData()
  {
    iList = new ArrayList<Data>();
    
    //potholes
    iList.add(new Data(new String[] { "potholes" }, 
    		new int[] { R.drawable.potholes, 
    		R.drawable.potholes, R.drawable.potholes }));
    
    //road humps
    iList.add(new Data(new String[] { "road humps" }, 
    		new int[] { R.drawable.road_humps, 
    		R.drawable.road_humps, 
    		R.drawable. road_humps }));
    
    //uneven roads
    iList.add(new Data(new String[] { "uneven roads" }, 
    		new int[] { R.drawable.uneven_roads, 
    		R.drawable.uneven_roads, 
    		R.drawable.uneven_roads }));
    
    //pavements
    iList.add(new Data(new String[] { "pavements" }, 
    		new int[] {R.drawable.pavements, 
    		R.drawable.pavements, 
    		R.drawable.pavements }));    
    
    //road markings
    //ArrayList localArrayList1 = iList;    
    String[] arrayOfString1 = { "road markings" };   
    int[] arrayOfInt1 = new int[3];
    arrayOfInt1[0] = R.drawable.road_markings;
    arrayOfInt1[2] = R.drawable.road_markings;
    iList.add(new Data(arrayOfString1, arrayOfInt1));    
    
    //sudden breaking
    iList.add(new Data(new String[] { "sudden breaking" }, 
    		new int[] { R.drawable.sudden_breaking, 
    		R.drawable.sudden_breaking, 
    		R.drawable.sudden_breaking }));   
    
   //gravel
   // ArrayList localArrayList2 = iList;   
    String[] arrayOfString2 = { "gravel" };
    int[] arrayOfInt2 = new int[3];
    arrayOfInt2[0] = R.drawable.gravel;
    arrayOfInt2[2] = R.drawable.gravel;
    iList.add(new Data(arrayOfString2, arrayOfInt2));
    
    //others
    iList.add(new Data(new String[] { "others" }, 
    		new int[] { R.drawable.others, 
    		R.drawable.others, 
    		R.drawable.others }));
  }

  @NonNull
  @Nullable
  private void setupView(View paramView)
  {
    setTouchNClick(paramView.findViewById(R.id.nearby));
    loadDummyData();
    GridView localGridView = (GridView)paramView.findViewById(R.id.grid);
    localGridView.setAdapter(new GridAdapter());   
    lastTime = System.currentTimeMillis();
    
  /*  mNoticationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
    stopNotification();*/
    
    
    ///handling item click events
    localGridView.setOnItemClickListener(new AdapterView.OnItemClickListener()
    {
      private Location location;

	@SuppressWarnings("null")
	public void onItemClick(AdapterView<?> paramAnonymousAdapterView, View paramAnonymousView, 
    		  int paramAnonymousInt, long paramAnonymousLong)
      {
		  //mLoggerService.storeLocation(location);
		 /* Intent intent = new Intent();
		  intent.setClass(getActivity(), IssueList.class);
		  intent.putExtra("title", ((Data)iList.get(paramAnonymousInt)).getTexts()[0]).
		  putExtra("icon", ((Data)iList.get(paramAnonymousInt)).getResources()[1]).
		  putExtra("icon1", ((Data)iList.get(paramAnonymousInt)).getResources()[2]);*/		  
		     	  
	    
	   /*     startActivity(new Intent(getActivity(), IssueList.class).putExtra
	        		("title", ((Data)iList.get(paramAnonymousInt)).getTexts()[0]).
	        		putExtra("icon", ((Data)iList.get(paramAnonymousInt)).getResources()[1]).
	        		putExtra("icon1", ((Data)iList.get(paramAnonymousInt)).getResources()[2]));}         		
	        		
	   */	  
		
		/*  String labelName = null;
		  Context context = null;
		  Long labelTime = null;*/
		  
		  //used the final modifier to enforce good initializer;
		SensorEvent event =  null;	  
		  
			//You need context object in your view.
			sensorManager = (SensorManager)getActivity().getSystemService(getActivity().SENSOR_SERVICE);						
			sensorManager.registerListener(MainFragment.this, Sensor.TYPE_ACCELEROMETER, 
					SensorManager.SENSOR_DELAY_NORMAL);
			
		try
		{	
	     	 /* in this section
	     	  * the logging has to momentarily stop
	     	  * then the x,y,z,longitude and latitude values are all stored with respect to the time
	     	  * that data is changed to XML using the XML creator class
	     	  * And then stored in a file on the SD card     
	     	  * 
	     	  * store the label name, 	
	     	  * 
	     	  * */ 	
		
			//if(event != null){
			
			/**
			 * I have used this assertion below to prevent the null pointer exception.
			 * 
			 * More about NullPointerExceptions:
			 * 
			 * Use final modifier to enforce good initialization.
               Avoid returning null in methods, for example returning empty collections 
               when applicable.
               Use annotations @NotNull and @Nullable
               Fail fast and use asserts to avoid propagation of null objects trough 
               the whole application when they shouldn't be null.
               Use equals with known object first: if("knownObject".equals(unknownObject)
               Prefer valueOf() over toString().
               Use null safe StringUtils methods StringUtils.isEmpty(null).
			 * 
			 * 
			 * */		
			
			    assert (event != null);
			    if (event instanceof SensorEvent)
			{ 	  
			    float[] value = event.values; 			    
				float xVal = value[0];
				float yVal = value[1];
				float zVal = value[2]; 
			
				float accelationSquareRoot = (xVal*xVal + yVal*yVal + zVal*zVal) 
						/ (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);
				
				long actualTime = System.currentTimeMillis();				
				
			if(accelationSquareRoot >= 1.2) 
		    {			
			if(actualTime-lastTime < 2000000000) 
			{		
				 if (!isLogging())
			      {
			         Log.e(TAG, String.format("Not logging but storing location %s, prepare to fail", location.toString()));
			      }
				 
				//labelName = mTrackNameView.getText().toString();   
				labelName = iList.get(paramAnonymousInt).getTexts().toString();
				labelTime = Long.valueOf(System.currentTimeMillis());
	            ContentValues values = new ContentValues();
	            values.put( Labels.NAME, labelName );          
				values.put(Labels.DETECTION_TIME, labelTime);			
				values.put(Labels.LATITUDE, Double.valueOf(location.getLatitude()));
				values.put(Labels.LONGITUDE, Double.valueOf(location.getLongitude()));
				values.put(Labels.X, xVal);
				values.put(Labels.Y, yVal);
				values.put(Labels.Z, zVal);
				//inserting the values in the database.
	            context.getContentResolver().insert( mLabelUri, values ); 
	           // context.getContentResolver().i
	            MainFragment.this.clearNotification();	            
	        	/*mLoggerService.StoreLatLongTimeSpeed(location); 
	        	getAccelerometerValues.getAccelerometer(event);*/				
			}
			lastTime = actualTime;			
		    }
			    }
		}
		   		
    	catch (IllegalArgumentException e)
        {
           Log.e(TAG, "Could not start GPSLoggerService.", e);
       	Intent intent = new Intent();
		intent.setClass(getActivity(), MainActivity.class);
        }
        catch (SecurityException e)
        {
           Log.e(TAG, "Could not start GPSLoggerService.", e);
       	Intent intent = new Intent();
		intent.setClass(getActivity(), MainActivity.class);
        }
        catch (IllegalStateException e)
        {
           Log.e(TAG, "Could not start GPSLoggerService.", e);
       	Intent intent = new Intent();
		intent.setClass(getActivity(), MainActivity.class);
        }
    	
    	catch (NullPointerException e)
        {
           Log.e(TAG, "Could not start GPSLoggerService.", e);
       	Intent intent = new Intent();
		intent.setClass(getActivity(), MainActivity.class);
        }			
   
      }
      });   
    
  }
  
  /**
   * the getSystemService() method that provides access to system 
   * services comes from Context. An Activity extends Context, 
   * a Fragment does not. Hence, you first need to get a reference to the 
   * Activity in which the Fragment is contained and then magically retrieve
   * the system service you want.   
   *  
   * 
   * **/
  
  private void clearNotification() //changed the visibility of this method
  {
     NotificationManager noticationManager = (NotificationManager ) getActivity().getSystemService(Context.NOTIFICATION_SERVICE); 
     noticationManager.cancel( R.layout.namedialog );
  }
  
  private void startDelayNotification()
  {
     int resId = R.string.dialog_routename_title;
     int icon = R.drawable.ic_maps_indicator_current_position;
     CharSequence tickerText = getResources().getString( resId );
     long when = System.currentTimeMillis();
     
     Notification nameNotification = new Notification( icon, tickerText, when );
     nameNotification.flags |= Notification.FLAG_AUTO_CANCEL;
     
     CharSequence contentTitle = getResources().getString( R.string.app_name );
     CharSequence contentText = getResources().getString( resId );     
    
     Intent notificationintent = new Intent();
     notificationintent.setClass(getActivity(), MainFragment.class);
     notificationintent.setData( mLabelUri );
         
	 PendingIntent contentIntent = PendingIntent.getActivity( context, 0, notificationintent, Intent.FLAG_ACTIVITY_NEW_TASK );
     nameNotification.setLatestEventInfo( context, contentTitle, contentText, contentIntent );
     
     //NotificationManager noticationManager = (NotificationManager) this.getSystemService( Context.NOTIFICATION_SERVICE );
     NotificationManager noticationManager = (NotificationManager ) getActivity().getSystemService(Context.NOTIFICATION_SERVICE); 
     noticationManager.notify( R.layout.namedialog, nameNotification );
  }
  
  
  

  protected boolean isLogging()
  {
     return this.mLoggingState == Constants.LOGGING;
  }



//the click listener for the upper section of the main fragment....the sign for location image....
  @Override
  public void onClick(View paramView)
  {
    super.onClick(paramView);
    if (paramView.getId() == R.id.nearby) 
    {
    	//using this one to start and stop the logging
      	
         Message msg = null;
	
    	try 
    	{
    	 //mLoggerService._handleMessage(msg);    		
    	mLoggerService. soundGpsSignalAlarm();	
    	mLoggerService.startLogging();
    	this.notifyOnEnabledProviderNotification(R.string.service_gpsenabled);
    	} 
    	
    	catch (IllegalArgumentException e)
        {
           Log.e(TAG, "Could not start GPSLoggerService.", e);
       	Intent intent = new Intent();
		intent.setClass(getActivity(), MainActivity.class);
        }
        catch (SecurityException e)
        {
           Log.e(TAG, "Could not start GPSLoggerService.", e);
       	Intent intent = new Intent();
		intent.setClass(getActivity(), MainActivity.class);
        }
        catch (IllegalStateException e)
        {
           Log.e(TAG, "Could not start GPSLoggerService.", e);
       	Intent intent = new Intent();
		intent.setClass(getActivity(), MainActivity.class);
        }
    	
    	catch (NullPointerException e)
        {
           Log.e(TAG, "Could not start GPSLoggerService.", e);
       	Intent intent = new Intent();
		intent.setClass(getActivity(), MainActivity.class);
        }
    
    }
      //startActivity(new Intent(getActivity(), IssueList.class).putExtra("title", getString(R.string.nearby)));
  }

  @SuppressLint({"InflateParams"})
  public View onCreateView(LayoutInflater paramLayoutInflater, ViewGroup paramViewGroup, Bundle paramBundle)
  {
    View localView = paramLayoutInflater.inflate(R.layout.main_container, null);
    setupView(localView);
    return localView;
  }

  private class GridAdapter extends BaseAdapter
  {
    private GridAdapter()
    {
    }

    public int getCount()
    {
      return iList.size();
    }

    public Data getItem(int paramInt)
    {
      return (Data)iList.get(paramInt);
    }

    public long getItemId(int paramInt)
    {
      return paramInt;
    }

    @SuppressLint({"InflateParams"})
    public View getView(int paramInt, View paramView, ViewGroup paramViewGroup)
    {
      if (paramView == null)
        paramView = getActivity().getLayoutInflater().inflate(R.layout.grid_item, null);
      Data localData = getItem(paramInt);
      TextView localTextView = (TextView)paramView;
      localTextView.setText(localData.getTexts()[0]);
      localTextView.setCompoundDrawablesWithIntrinsicBounds(0, localData.getResources()[0], 0, 0);
      return paramView;
    }
  }

@Override
public void onLocationChanged(Location location) 
{
	// TODO Auto-generated method stub
	
}

@Override
public void onStatusChanged(String provider, int status, Bundle extras) 
{
	// TODO Auto-generated method stub
	
    if (DEBUG)
    {
       Log.d(TAG, "onStatusChanged( String " + provider + ", int " + status + ", Bundle " + extras + " )");
    }
    ;
    if (status == LocationProvider.OUT_OF_SERVICE)
    {
       Log.e(TAG, String.format("Provider %s changed to status %d", provider, status));
    }
	
}

@Override
public void onProviderEnabled(String provider) {
	// TODO Auto-generated method stub
	if (DEBUG)
    {
       Log.d(TAG, "onProviderEnabled( String " + provider + " )");
    }
    ;
    if (mPrecision != Constants.LOGGING_GLOBAL && provider.equals(LocationManager.GPS_PROVIDER))
    {
       notifyOnEnabledProviderNotification(R.string.service_gpsenabled);
       mStartNextSegment = true;
    }
    else if (mPrecision == Constants.LOGGING_GLOBAL && provider.equals(LocationManager.NETWORK_PROVIDER))
    {
       notifyOnEnabledProviderNotification(R.string.service_dataenabled);
    }
	
}

private void notifyOnEnabledProviderNotification(int resId)
{
   mNoticationManager.cancel(LOGGING_UNAVAILABLE);
   mShowingGpsDisabled = false;
   CharSequence text = this.getString(resId);
   Toast toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
   toast.show();
}

private void notifyOnPoorSignal(int resId)
{
   int icon = R.drawable.ic_maps_indicator_current_position;
   CharSequence tickerText = getResources().getString(resId);
   long when = System.currentTimeMillis();
   Notification signalNotification = new Notification(icon, tickerText, when);
   CharSequence contentTitle = getResources().getString(R.string.app_name);
  // Intent notificationIntent = new Intent(this, CommonLoggerMap.class);
 
   Intent notificationintent = new Intent();
   notificationintent.setClass(getActivity(), MainActivity.class);    
   
   PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationintent, Intent.FLAG_ACTIVITY_NEW_TASK);
   signalNotification.setLatestEventInfo(context, contentTitle, tickerText, contentIntent);
   signalNotification.flags |= Notification.FLAG_AUTO_CANCEL;

   mNoticationManager.notify(resId, signalNotification);
}

private void notifyOnDisabledProvider(int resId)
{
   int icon = R.drawable.ic_maps_indicator_current_position;
   CharSequence tickerText = getResources().getString(resId);
   long when = System.currentTimeMillis();
   Notification gpsNotification = new Notification(icon, tickerText, when);
   gpsNotification.flags |= Notification.FLAG_AUTO_CANCEL;

   CharSequence contentTitle = getResources().getString(R.string.app_name);
   CharSequence contentText = getResources().getString(resId);
   //Intent notificationIntent = new Intent(this, CommonLoggerMap.class);
   Intent notificationIntent = new Intent();
   notificationIntent.setClass(getActivity(), MainActivity.class);
   notificationIntent.setData(ContentUris.withAppendedId(Labels.CONTENT_URI, mLabelId));
   PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 
 		  Intent.FLAG_ACTIVITY_NEW_TASK);
   gpsNotification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);

   mNoticationManager.notify(LOGGING_UNAVAILABLE, gpsNotification);
   mShowingGpsDisabled = true;
}

private void updateNotification()
{
   CharSequence contentTitle = getResources().getString(R.string.app_name);

   String precision = getResources().getStringArray(R.array.precision_choices)[mPrecision];
   String state = getResources().getStringArray(R.array.state_choices)[mLoggingState - 1];
   CharSequence contentText;
   switch (mPrecision)
   {
      case (Constants.LOGGING_GLOBAL):
         contentText = getResources().getString(R.string.service_networkstatus, state, precision);
         break;
      default:
         if (mStatusMonitor)
         {
            contentText = getResources().getString(R.string.service_gpsstatus, state, precision, mSatellites);
         }
         else
         {
            contentText = getResources().getString(R.string.service_gpsnostatus, state, precision);
         }
         break;
   }
 // Intent notificationIntent = new Intent(this, CommonLoggerMap.class);
   Intent notificationIntent = new Intent();
   notificationIntent.setClass(getActivity(), MainActivity.class);
  notificationIntent.setData(ContentUris.withAppendedId(Labels.CONTENT_URI, mLabelId));
  mNotification.contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, Intent.FLAG_ACTIVITY_NEW_TASK);
   mNotification.setLatestEventInfo(context, contentTitle, contentText, mNotification.contentIntent);
   //mNoticationManager.notify(R.layout.map_widgets, mNotification);
}



@Override
public void onProviderDisabled(String provider) {
	// TODO Auto-generated method stub
	
}

@Override
public void onSensorChanged(SensorEvent event) {
	// TODO Auto-generated method stub
	
}

@Override
public void onAccuracyChanged(Sensor sensor, int accuracy) {
	// TODO Auto-generated method stub
	
}




@Override
public void onSensorChanged(int sensor, float[] values) {
	// TODO Auto-generated method stub
	
}




@Override
public void onAccuracyChanged(int sensor, int accuracy) {
	// TODO Auto-generated method stub
	
}


@Override
public void onPause() 
{
// TODO Auto-generated method stub
super.onPause();
/*sensorManager.unregisterListener((SensorListener)context);*/
}


@Override
public void onResume() 
{
// TODO Auto-generated method stub
super.onResume();

/*try{
sensorManager.registerListener(this, sensorManager.getDefaultSensor
	(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
 lastTime = System.currentTimeMillis(); }

catch (NullPointerException e)
{
   Log.e(TAG, "Could not start GPSLoggerService.", e);
	Intent intent = new Intent();
intent.setClass(getActivity(), MainActivity.class);
}*/
}

@Override
public void onDestroy() 
{
	   super.onDestroy();
	   //sensorManager.unregisterListener((SensorListener) context);  
/*	   if (DEBUG)
	      {
	         Log.d(TAG, "onDestroy()");
	      }
	      ;
	      super.onDestroy();

	      if (isLogging())
	      {
	         Log.w(TAG, "Destroyin an activly logging service");
	      }
	      mHeartbeatTimer.cancel();
	      mHeartbeatTimer.purge();
	      if (this.mWakeLock != null)
	      {
	         this.mWakeLock.release();
	         this.mWakeLock = null;
	      }
	      PreferenceManager.getDefaultSharedPreferences(context).
	      unregisterOnSharedPreferenceChangeListener(this.mSharedPreferenceChangeListener);
	      mLocationManager.removeGpsStatusListener(mStatusListener);
	      stopListening();
	      
	      //the widgets which appear in the map...
	     // mNoticationManager.cancel(R.layout.map_widgets);
	      
	      //one of them have ids in the layout file above commented.

	      Message msg = Message.obtain();
	      msg.what = STOPLOOPER;
	      mHandler.sendMessage(msg);*/

}

private void sendRequestStatusUpdateMessage()
{
   mStatusMonitor = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Constants.STATUS_MONITOR, false);
   Message msg = Message.obtain();
   msg.what = ADDGPSSTATUSLISTENER;
   mHandler.sendMessage(msg);
}

private void broadCastLoggingState()
{
   Intent broadcast = new Intent(Constants.LOGGING_STATE_CHANGED_ACTION);
   broadcast.putExtra(Constants.EXTRA_LOGGING_PRECISION, mPrecision);
   broadcast.putExtra(Constants.EXTRA_LOGGING_STATE, mLoggingState);
   
   // I had to add the getActivity()
   this.getActivity().getApplicationContext().sendBroadcast(broadcast);
   if( isLogging()  )
   {
      StreamUtils.initStreams(context);
   }
   else
   {
      StreamUtils.shutdownStreams(context);
   }
}


public void startNotification()
{
   //mNoticationManager.cancel(R.layout.map_widgets);

  int icon = R.drawable.ic_maps_indicator_current_position;
   CharSequence tickerText = getResources().getString(R.string.service_start);
   long when = System.currentTimeMillis();

  mNotification = new Notification(icon, tickerText, when);
   mNotification.flags |= Notification.FLAG_ONGOING_EVENT;

   updateNotification();

   if (Build.VERSION.SDK_INT >= 5)
   {
      //startForegroundReflected(R.layout.map_widgets, mNotification);
   }
   else
   {
      //mNoticationManager.notify(R.layout.map_widgets, mNotification);
   }
}


private synchronized void crashRestoreState()
{
   SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
   long previousState = preferences.getInt(SERVICESTATE_STATE, Constants.STOPPED);
   if (previousState == Constants.LOGGING || previousState == Constants.PAUSED)
   {
      Log.w(TAG, "Recovering from a crash or kill and restoring state.");
     startNotification();

      mLabelId = preferences.getLong(SERVICESTATE_TRACKID, -1);
      mSegmentId = preferences.getLong(SERVICESTATE_SEGMENTID, -1);
      mPrecision = preferences.getInt(SERVICESTATE_PRECISION, -1);
      mDistance = preferences.getFloat(SERVICESTATE_DISTANCE, 0F);
      if (previousState == Constants.LOGGING)
      {
         mLoggingState = Constants.PAUSED;
         //resumeLogging();
      }
      else if (previousState == Constants.PAUSED)
      {
         mLoggingState = Constants.LOGGING;
         //pauseLogging();
      }
   }
}


private void crashProtectState()

{
   SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
   Editor editor = preferences.edit();
   editor.putLong(SERVICESTATE_TRACKID, mLabelId);
   editor.putLong(SERVICESTATE_SEGMENTID, mSegmentId);
   editor.putInt(SERVICESTATE_PRECISION, mPrecision);
   editor.putInt(SERVICESTATE_STATE, mLoggingState);
   editor.putFloat(SERVICESTATE_DISTANCE, mDistance);
   editor.commit();
   if (DEBUG)
   {
      Log.d(TAG, "crashProtectState()");
   }
   ;
}


/**
 * Listens to changes in preference to precision and sanity checks
 */
private OnSharedPreferenceChangeListener mSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener()
{

   @Override
   public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
   {
      if (key.equals(Constants.PRECISION) || key.equals(Constants.LOGGING_DISTANCE) || key.equals(Constants.LOGGING_INTERVAL))
      {
         sendRequestLocationUpdatesMessage();
         crashProtectState();
         updateNotification();
         broadCastLoggingState();
      }
      else if (key.equals(Constants.SPEEDSANITYCHECK))
      {
         mSpeedSanityCheck = sharedPreferences.getBoolean(Constants.SPEEDSANITYCHECK, true);
      }
      else if (key.equals(Constants.STATUS_MONITOR))
      {
         mLocationManager.removeGpsStatusListener(mStatusListener);
         sendRequestStatusUpdateMessage();
         updateNotification();
      }
      else if(key.equals(Constants.BROADCAST_STREAM) || key.equals("VOICEOVER_ENABLED") || key.equals("CUSTOMUPLOAD_ENABLED") )
      {
         if (key.equals(Constants.BROADCAST_STREAM))
         {
            mStreamBroadcast = sharedPreferences.getBoolean(Constants.BROADCAST_STREAM, false);
         }
         StreamUtils.shutdownStreams(context);
         if( !mStreamBroadcast )
         {
            StreamUtils.initStreams(context);
         }
      }
   }
};

/**
 * Listens to GPS status changes
 */
private Listener mStatusListener = new GpsStatus.Listener()
{
   @Override
   public synchronized void onGpsStatusChanged(int event)
   {
      switch (event)
      {
         case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
            if (mStatusMonitor)
            {
               GpsStatus status = mLocationManager.getGpsStatus(null);
               mSatellites = 0;
               Iterable<GpsSatellite> list = status.getSatellites();
               for (GpsSatellite satellite : list)
               {
                  if (satellite.usedInFix())
                  {
                     mSatellites++;
                  }
               }
               updateNotification();
            }
            break;
         case GpsStatus.GPS_EVENT_STOPPED:
            break;
         case GpsStatus.GPS_EVENT_STARTED:
            break;
         default:
            break;
      }
   }
};

private void sendRequestLocationUpdatesMessage()
{
   stopListening();
   mPrecision = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(context).getString(Constants.PRECISION, "2")).intValue();
   Message msg = Message.obtain();
   switch (mPrecision)
   {
      case (Constants.LOGGING_FINE): // Fine
         msg.what = REQUEST_FINEGPS_LOCATIONUPDATES;
         mHandler.sendMessage(msg);
         break;
      case (Constants.LOGGING_NORMAL): // Normal
         msg.what = REQUEST_NORMALGPS_LOCATIONUPDATES;
         mHandler.sendMessage(msg);
         break;
      case (Constants.LOGGING_COARSE): // Coarse
         msg.what = REQUEST_COARSEGPS_LOCATIONUPDATES;
         mHandler.sendMessage(msg);
         break;
      case (Constants.LOGGING_GLOBAL): // Global
         msg.what = REQUEST_GLOBALNETWORK_LOCATIONUPDATES;
         mHandler.sendMessage(msg);
         break;
      case (Constants.LOGGING_CUSTOM): // Global
         msg.what = REQUEST_CUSTOMGPS_LOCATIONUPDATES;
         mHandler.sendMessage(msg);
         break;
      default:
         Log.e(TAG, "Unknown precision " + mPrecision);
         break;
   }
}

private TimerTask mHeartbeat = null;

/**
 * Task to determine if the GPS is alive
 */
class Heartbeat extends TimerTask
{

   private String mProvider;

   public Heartbeat(String provider)
   {
      mProvider = provider;
   }

   @Override
   public void run()
   {
      if (isLogging())
      {
         // Collect the last location from the last logged location or a more recent from the last weak location
         Location checkLocation = mPreviousLocation;
         synchronized (mWeakLocations)
         {
            if (!mWeakLocations.isEmpty())
            {
               if (checkLocation == null)
               {
                  checkLocation = mWeakLocations.lastElement();
               }
               else
               {
                  Location weakLocation = mWeakLocations.lastElement();
                  checkLocation = weakLocation.getTime() > checkLocation.getTime() ? weakLocation : checkLocation;
               }
            }
         }
         // Is the last known GPS location something nearby we are not told?
         Location managerLocation = mLocationManager.getLastKnownLocation(mProvider);
         if (managerLocation != null && checkLocation != null)
         {
            if (checkLocation.distanceTo(managerLocation) < 2 * mMaxAcceptableAccuracy)
            {
               checkLocation = managerLocation.getTime() > checkLocation.getTime() ? managerLocation : checkLocation;
            }
         }

         if (checkLocation == null || checkLocation.getTime() + mCheckPeriod < new Date().getTime())
         {
            Log.w(TAG, "GPS system failed to produce a location during logging: " + checkLocation);
            mLoggingState = Constants.PAUSED;
            //resumeLogging();

            if (mStatusMonitor)
            {
               soundGpsSignalAlarm();
            }

         }
      }
   }
};

public void soundGpsSignalAlarm() //from private to public....
{
   Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
   if (alert == null)
   {
      alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
      if (alert == null)
      {
         alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
      }
   }
   MediaPlayer mMediaPlayer = new MediaPlayer();
   
   try
   {
      mMediaPlayer.setDataSource(context, alert);
      
      /**
       * Why that one extra method call?
       the getSystemService() method that provides access to system services
        comes from Context. An Activity extends Context, a Fragment does not.
       Hence, you first need to get a reference 
       to the Activity in which the Fragment is contained and then magically 
       retrieve the system service you want.
       
       hence the getActivity();
       * 
       * 
       * 
       * ***/
      
      final AudioManager audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
      
    
      if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0)
      {
         mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
         mMediaPlayer.setLooping(false);
         mMediaPlayer.prepare();
         mMediaPlayer.start();
      }
   }
   catch (IllegalArgumentException e)
   {
      Log.e(TAG, "Problem setting data source for mediaplayer", e);
   }
   catch (SecurityException e)
   {
      Log.e(TAG, "Problem setting data source for mediaplayer", e);
   }
   catch (IllegalStateException e)
   {
      Log.e(TAG, "Problem with mediaplayer", e);
   }
   catch (IOException e)
   {
      Log.e(TAG, "Problem with mediaplayer", e);
   }
   Message msg = Message.obtain();
   msg.what = GPSPROBLEM;
   mHandler.sendMessage(msg);
}


private void stopListening()
{
   if (mHeartbeat != null)
   {
      mHeartbeat.cancel();
      mHeartbeat = null;
   }
   mLocationManager.removeUpdates(this);
}

@Override
public void onStop() 
{
	   super.onStop();
	/*   sensorManager.unregisterListener((SensorListener) context);	*/   
	   
}




}
