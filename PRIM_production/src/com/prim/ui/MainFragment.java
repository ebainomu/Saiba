package com.prim.ui;



/**
 * 
 * @author Martin Bbaale
 * 
 * 
 * ***/

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.sqlite.SQLiteDatabase;
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
import com.prim.custom.CustomFragment;
import com.prim.model.Data;

import dev.baalmart.gps.R;
import dev.ugasoft.android.gps.actions.ControlTracking;
import dev.ugasoft.android.gps.actions.NameTrack;
import dev.ugasoft.android.gps.db.DatabaseHelper;
import dev.ugasoft.android.gps.db.Prim.Labels;
import dev.ugasoft.android.gps.db.Prim.Tracks;
import dev.ugasoft.android.gps.db.Prim.Waypoints;
import dev.ugasoft.android.gps.db.Prim.WaypointsColumns;
import dev.ugasoft.android.gps.db.PrimProvider;
import dev.ugasoft.android.gps.logger.GPSLoggerService;
import dev.ugasoft.android.gps.logger.GPSLoggerServiceManager;
import dev.ugasoft.android.gps.logger.IGPSLoggerServiceRemote;
import dev.ugasoft.android.gps.streaming.StreamUtils;
import dev.ugasoft.android.gps.util.Constants;
import dev.ugasoft.android.gps.viewer.map.LoggerLabelHelper;
import dev.ugasoft.android.gps.viewer.map.LoggerMapHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

@SuppressWarnings("deprecation")
public class MainFragment extends CustomFragment  
{
	
      ;;LoggerLabelHelper mHelper;
      
      DatabaseHelper mDbHelper;
      PrimProvider pProvider;
   
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
private NameTrack nameRoute;
//GetAccelerometerValues getAccelerometerValues;
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

  private void loadDummyData() //from private
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
       
	@Override
   public void onItemClick(AdapterView<?> paramAnonymousAdapterView, View paramAnonymousView, 
    		  int paramAnonymousInt, long paramAnonymousLong)
      {
		  
	    
	   /*     startActivity(new Intent(getActivity(), IssueList.class).putExtra
	        		("title", ((Data)iList.get(paramAnonymousInt)).getTexts()[0]).
	        		putExtra("icon", ((Data)iList.get(paramAnonymousInt)).getResources()[1]).
	        		putExtra("icon1", ((Data)iList.get(paramAnonymousInt)).getResources()[2]));}        		
	   */	  
		
		/*  String labelName = null;
		  Context context = null;
		  Long labelTime = null;*/
		  
		  //used the final modifier to enforce good initializer;
	
		/*	//You need context object in your view.
			sensorManager = (SensorManager)getActivity().getSystemService(getActivity().SENSOR_SERVICE);						
			sensorManager.registerListener(MainFragment.this, Sensor.TYPE_ACCELEROMETER, 
					SensorManager.SENSOR_DELAY_NORMAL);*/
	
	     	 /* in this section
	     	  * the logging has to momentarily stop
	     	  * then the x,y,z,longitude and latitude values are all stored with respect to the time
	     	  * that data is changed to XML using the XML creator class
	     	  * And then stored in a file on the SD card     
	     	  * 
	     	  * store the label name, 	
	     	  * 
	     	  * */ 	
		

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
	         /*startActivity(new Intent(getActivity(), IssueList.class).putExtra
            ("title", ((Data)iList.get(paramAnonymousInt)).getTexts()[0]).
            putExtra("icon", ((Data)iList.get(paramAnonymousInt)).getResources()[1]).
            putExtra("icon1", ((Data)iList.get(paramAnonymousInt)).getResources()[2]));} */
	     
	   
	             
	
				   labelName = iList.get(paramAnonymousInt).getTexts()[0].toString();
				   labelTime = Long.valueOf(System.currentTimeMillis());
	            ContentValues values = new ContentValues();
	            values.put( Labels.NAME, labelName );  
				   values.put(Labels.CREATION_TIME, labelTime);
	            context.getContentResolver().insert( mLabelUri, values );	  
	            
	       /*     ContentValues args = new ContentValues();
	            args.put(WaypointsColumns.SEGMENT, segmentId);
	            args.put(WaypointsColumns.TIME, location.getTime());
	            args.put(WaypointsColumns.LATITUDE, location.getLatitude());
	            args.put(WaypointsColumns.LONGITUDE, location.getLongitude());
	            args.put(WaypointsColumns.SPEED, location.getSpeed());
	            args.put(WaypointsColumns.ACCURACY, location.getAccuracy());
	            args.put(WaypointsColumns.ALTITUDE, location.getAltitude());
	            args.put(WaypointsColumns.BEARING, location.getBearing());

	            long waypointId = sqldb.insert(Waypoints.TABLE, null, args);*/
	            
	            
	            
		   
      }
      });   
    
  }
  
  public void getX(float xval)
  {
	//  xVal = xval;
	  
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
 
//the click listener for the upper section of the main fragment....the sign for location image....
  @Override
  public void onClick(View paramView)
  {
    super.onClick(paramView);
    if (paramView.getId() == R.id.nearby) 
    {
    	//using this one to start and stop the logging
       Intent intent = new Intent();
	
    	try 
    	{

       long loggerTrackId = mLoggerServiceManager.startGPSLogging( null );
       
/*       Intent namingIntent = new Intent();
       namingIntent.setClass(getActivity(), NameTrack.class );
       namingIntent.setData( ContentUris.withAppendedId( Labels.CONTENT_URI, loggerTrackId ) );
       startActivity( namingIntent );*/
       
       
       Intent namingIntent = new Intent(getActivity(), NameTrack.class );
       namingIntent.setData( ContentUris.withAppendedId( Tracks.CONTENT_URI, loggerTrackId ) );
       startActivity( namingIntent );
       
       // Create data for the caller that a new track has been started
       ComponentName caller = ((Activity) context).getCallingActivity();
       if( caller != null )
       {
          intent.setData( ContentUris.withAppendedId( Tracks.CONTENT_URI, loggerTrackId ) );
     
       }    
       
       
      
    	} 
    	
    	catch (Exception e)
        {
           Log.e(TAG, "Could not start GPSLoggerService.", e);
       	Intent intent1 = new Intent();
		intent1.setClass(getActivity(), MainActivity.class);
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
public void onPause() 
{
// TODO Auto-generated method stub
super.onPause();

}


@Override
public void onResume() 
{
// TODO Auto-generated method stub
super.onResume();
}

@Override
public void onDestroy() 
{
  super.onDestroy();
	
}

@Override
public void onStop() 
{
	   super.onStop();
		   
}




}
