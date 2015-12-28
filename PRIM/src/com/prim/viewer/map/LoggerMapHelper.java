package com.prim.viewer.map;

import java.util.concurrent.Semaphore;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Gallery;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.prim.IssueList;
import com.prim.actions.ControlLogging;
import com.prim.actions.SegmentRendering;
import com.prim.actions.ShareLabels;
import com.prim.db.Prim.Labels;
import com.prim.logger.GPSLoggerServiceManager;
import com.prim.utils.Constants;
import com.prim.utils.UnitsI18n;
import com.prim.viewer.ApplicationPreferenceActivity;

import dev.baalmart.prim.R;

public class LoggerMapHelper
{

   public static final String OSM_PROVIDER = "OSM";
   public static final String GOOGLE_PROVIDER = "GOOGLE";
   public static final String MAPQUEST_PROVIDER = "MAPQUEST";

   private static final String INSTANCE_E6LONG = "e6long";
   private static final String INSTANCE_E6LAT = "e6lat";
   private static final String INSTANCE_ZOOM = "zoom";
   private static final String INSTANCE_AVGSPEED = "averagespeed";
   private static final String INSTANCE_HEIGHT = "averageheight";
   private static final String INSTANCE_TRACK = "track";
   private static final String INSTANCE_SPEED = "speed";
   private static final String INSTANCE_ALTITUDE = "altitude";
   private static final String INSTANCE_DISTANCE = "distance";
   
   private static final int ZOOM_LEVEL = 16;
   // MENU'S
   private static final int MENU_SETTINGS = 1;
   private static final int MENU_TRACKING = 2;
   private static final int MENU_TRACKLIST = 3;
   private static final int MENU_NOTE = 7;
   private static final int MENU_SHARE = 13;
   private static final int DIALOG_NOTRACK = 24; 
   private static final int DIALOG_URIS = 34;
   private static final String TAG = "OGT.LoggerMap";

   private double mAverageSpeed = 33.33d / 3d;
   private double mAverageHeight = 33.33d / 3d;
   private long mLabelId = -1;
   private long mLastSegment = -1;
   private UnitsI18n mUnits;
   private WakeLock mWakeLock = null;
   private SharedPreferences mSharedPreferences;
   private GPSLoggerServiceManager mLoggerServiceManager;
   private SegmentRendering mLastSegmentOverlay;
   private BaseAdapter mMediaAdapter;

   private Handler mHandler;

   private ContentObserver mTrackSegmentsObserver;
   private ContentObserver mSegmentWaypointsObserver;
   private ContentObserver mTrackMediasObserver;
   
   
   private DialogInterface.OnClickListener mNoTrackDialogListener;
   private OnItemSelectedListener mGalerySelectListener;
   private Uri mSelected;
   private OnClickListener mNoteSelectDialogListener;
   private OnCheckedChangeListener mCheckedChangeListener;
   private android.widget.RadioGroup.OnCheckedChangeListener mGroupCheckedChangeListener;
   private OnSharedPreferenceChangeListener mSharedPreferenceChangeListener;
   private UnitsI18n.UnitsChangeListener mUnitsChangeListener;

   /**
    * Run after the ServiceManager completes the binding to the remote service
    */
   
   private Runnable mServiceConnected;
   private Runnable speedCalculator;
   private Runnable heightCalculator;
   private LoggerMap mLoggerMap;
   //private BitmapSegmentsOverlay mBitmapSegmentsOverlay;
   private float mSpeed;
   private double mAltitude;
   private float mDistance;

   public LoggerMapHelper(LoggerMap loggerMap)
   {
      mLoggerMap = loggerMap;
   }

   /**
    * Called when the activity is first created.
    */
   protected void onCreate(Bundle load)
   {
      mLoggerMap.setDrawingCacheEnabled(true);
      mUnits = new UnitsI18n(mLoggerMap.getActivity());
      mLoggerServiceManager = new GPSLoggerServiceManager(mLoggerMap.getActivity());

      final Semaphore calulatorSemaphore = new Semaphore(0);
      Thread calulator = new Thread("OverlayCalculator")
      {
         @Override
         public void run()
         {
            Looper.prepare();
            mHandler = new Handler();
            calulatorSemaphore.release();
            Looper.loop();
         }
      };
      
      calulator.start();
      try
      {
         calulatorSemaphore.acquire();
      }
      catch (InterruptedException e)
      {
         Log.e(TAG, "Failed waiting for a semaphore", e);
      }
      mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mLoggerMap.getActivity());

     // mBitmapSegmentsOverlay = new BitmapSegmentsOverlay(mLoggerMap, mHandler);
      createListeners();
      onRestoreInstanceState(load);
      mLoggerMap.updateOverlays();
   }

   protected void onResume()
   {
     // updateMapProvider();

      mLoggerServiceManager.startup(mLoggerMap.getActivity(), mServiceConnected);

      mSharedPreferences.registerOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);
      mUnits.setUnitsChangeListener(mUnitsChangeListener);
      updateTitleBar();
      updateBlankingBehavior();

      if (mLabelId >= 0)
      {
         ContentResolver resolver = mLoggerMap.getActivity().getContentResolver();
         Uri trackUri = Uri.withAppendedPath(Labels.CONTENT_URI, mLabelId + "/segments");
         Uri lastSegmentUri = Uri.withAppendedPath(Labels.CONTENT_URI, mLabelId + "/segments/" + mLastSegment + "/waypoints");
       
         //Uri for the media on a particular segment.
         //  Uri mediaUri = ContentUris.withAppendedId(Media.CONTENT_URI, mTrackId);

         resolver.unregisterContentObserver(this.mTrackSegmentsObserver);
         resolver.unregisterContentObserver(this.mSegmentWaypointsObserver);
         resolver.unregisterContentObserver(this.mTrackMediasObserver);
         
         resolver.registerContentObserver(trackUri, false, this.mTrackSegmentsObserver);
         resolver.registerContentObserver(lastSegmentUri, true, this.mSegmentWaypointsObserver);
         //resolver.registerContentObserver(mediaUri, true, this.mTrackMediasObserver);
      }
      
      //updateDataOverlays();

      /*updateSpeedColoring();
      updateSpeedDisplayVisibility();
      updateAltitudeDisplayVisibility();
      updateDistanceDisplayVisibility();
      updateCompassDisplayVisibility();*/
      updateLocationDisplayVisibility();
      
      updateTrackNumbers();
      
      mLoggerMap.executePostponedActions();
   }

   protected void onPause()
   {
      if (this.mWakeLock != null && this.mWakeLock.isHeld())
      {
         this.mWakeLock.release();
         Log.w(TAG, "onPause(): Released lock to keep screen on!");
      }
      mLoggerMap.clearOverlays();
      //mBitmapSegmentsOverlay.clearSegments();
      mLastSegmentOverlay = null;
      ContentResolver resolver = mLoggerMap.getActivity().getContentResolver();
      resolver.unregisterContentObserver(this.mTrackSegmentsObserver);
      resolver.unregisterContentObserver(this.mSegmentWaypointsObserver);
      resolver.unregisterContentObserver(this.mTrackMediasObserver);
      mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this.mSharedPreferenceChangeListener);
      mUnits.setUnitsChangeListener(null);
      mLoggerMap.disableMyLocation();
      mLoggerMap.disableCompass();
      this.mLoggerServiceManager.shutdown(mLoggerMap.getActivity());
   }

   protected void onDestroy()
   {
      mLoggerMap.clearOverlays();
      mHandler.post(new Runnable()
      {
         @Override
         public void run()
         {
            Looper.myLooper().quit();
         }
      });

      if (mWakeLock != null && mWakeLock.isHeld())
      {
         mWakeLock.release();
         Log.w(TAG, "onDestroy(): Released lock to keep screen on!");
      }
      if (mLoggerServiceManager.getLoggingState() == Constants.STOPPED)
      {
         mLoggerMap.getActivity().stopService(new Intent(Constants.SERVICENAME));
      }
      mUnits = null;
   }

   public void onNewIntent(Intent newIntent)
   {
      Uri data = newIntent.getData();
      if (data != null)
      {
         moveToTrack(Long.parseLong(data.getLastPathSegment()), true);
      }
   }

   protected void onRestoreInstanceState(Bundle load)
   {
      Uri data = mLoggerMap.getActivity().getIntent().getData();
      if (load != null && load.containsKey(INSTANCE_TRACK)) // 1st method: track from a previous instance of this activity
      {
         long loadTrackId = load.getLong(INSTANCE_TRACK);
         moveToTrack(loadTrackId, false);
         if (load.containsKey(INSTANCE_AVGSPEED))
         {
            mAverageSpeed = load.getDouble(INSTANCE_AVGSPEED);
         }
         if (load.containsKey(INSTANCE_HEIGHT))
         {
            mAverageHeight = load.getDouble(INSTANCE_HEIGHT);
         }
         if( load.containsKey(INSTANCE_SPEED))
         {
            mSpeed = load.getFloat(INSTANCE_SPEED);
         }
         if( load.containsKey(INSTANCE_ALTITUDE))
         {
            mAltitude = load.getDouble(INSTANCE_HEIGHT);
         }
         if( load.containsKey(INSTANCE_DISTANCE))
         {
            mDistance = load.getFloat(INSTANCE_DISTANCE);
         }
      }
      else if (data != null) // 2nd method: track ordered to make
      {
         long loadTrackId = Long.parseLong(data.getLastPathSegment());
         moveToTrack(loadTrackId, true);
      }
      else
      // 3rd method: just try the last track
      {
         moveToLastTrack();
      }

      if (load != null && load.containsKey(INSTANCE_ZOOM))
      {
         mLoggerMap.setZoom(load.getInt(INSTANCE_ZOOM));
      }
      else
      {
         mLoggerMap.setZoom(ZOOM_LEVEL);
      }

      if (load != null && load.containsKey(INSTANCE_E6LAT) && load.containsKey(INSTANCE_E6LONG))
      {
         GeoPoint storedPoint = new GeoPoint(load.getInt(INSTANCE_E6LAT), load.getInt(INSTANCE_E6LONG));
         mLoggerMap.animateTo(storedPoint);
      }
      else
      {
         GeoPoint lastPoint = getLastTrackPoint();
         mLoggerMap.animateTo(lastPoint);
      }
   }

   protected void onSaveInstanceState(Bundle save)
   {
      save.putLong(INSTANCE_TRACK, this.mLabelId);
      save.putDouble(INSTANCE_AVGSPEED, mAverageSpeed);
      save.putDouble(INSTANCE_HEIGHT, mAverageHeight);
      save.putInt(INSTANCE_ZOOM, mLoggerMap.getZoomLevel());
      save.putFloat(INSTANCE_SPEED, mSpeed);
      save.putDouble(INSTANCE_ALTITUDE, mAltitude);
      save.putFloat(INSTANCE_DISTANCE, mDistance);
      GeoPoint point = mLoggerMap.getMapCenter();
      save.putInt(INSTANCE_E6LAT, point.getLatitudeE6());
      save.putInt(INSTANCE_E6LONG, point.getLongitudeE6());
   }

   public boolean onKeyDown(int keyCode, KeyEvent event)
   {
      boolean propagate = true;
      switch (keyCode)
      {
         case KeyEvent.KEYCODE_T:
            propagate = mLoggerMap.zoomIn();
            propagate = false;
            break;
         case KeyEvent.KEYCODE_G:
            propagate = mLoggerMap.zoomOut();
            propagate = false;
            break;
         case KeyEvent.KEYCODE_F:
            moveToTrack(this.mLabelId - 1, true);
            propagate = false;
            break;
         case KeyEvent.KEYCODE_H:
            moveToTrack(this.mLabelId + 1, true);
            propagate = false;
            break;
      }
      return propagate;
   }

   private void setSpeedOverlay(boolean b)
   {
      Editor editor = mSharedPreferences.edit();
      editor.putBoolean(Constants.SPEED, b);
      editor.commit();
   }

   private void setAltitudeOverlay(boolean b)
   {
      Editor editor = mSharedPreferences.edit();
      editor.putBoolean(Constants.ALTITUDE, b);
      editor.commit();
   }

   private void setDistanceOverlay(boolean b)
   {
      Editor editor = mSharedPreferences.edit();
      editor.putBoolean(Constants.DISTANCE, b);
      editor.commit();
   }

   private void setCompassOverlay(boolean b)
   {
      Editor editor = mSharedPreferences.edit();
      editor.putBoolean(Constants.COMPASS, b);
      editor.commit();
   }

   private void setLocationOverlay(boolean b)
   {
      Editor editor = mSharedPreferences.edit();
      editor.putBoolean(Constants.LOCATION, b);
      editor.commit();
   }

   private void setOsmBaseOverlay(int b)
   {
      Editor editor = mSharedPreferences.edit();
      editor.putInt(Constants.OSMBASEOVERLAY, b);
      editor.commit();
   }

   private void createListeners()
   {
      /*******************************************************
       * 8 Runnable listener actions
       */
      speedCalculator = new Runnable()
      {
         @Override
         public void run()
         {
            double avgspeed = 0.0;
            ContentResolver resolver = mLoggerMap.getActivity().getContentResolver();
            Cursor waypointsCursor = null;
            try
            {
               waypointsCursor = resolver.query(Uri.withAppendedPath(Labels.CONTENT_URI, 
            		   LoggerMapHelper.this.mLabelId + "/labels"), new String[] {
                     "avg(" + Labels.SPEED + ")", "max(" + Labels.SPEED + ")" }, null, null, null);

               if (waypointsCursor != null && waypointsCursor.moveToLast())
               {
                  double average = waypointsCursor.getDouble(0);
                  double maxBasedAverage = waypointsCursor.getDouble(1) / 2;
                  avgspeed = Math.min(average, maxBasedAverage);
               }
               if (avgspeed < 2)
               {
                  avgspeed = 5.55d / 2;
               }
            }
            finally
            {
               if (waypointsCursor != null)
               {
                  waypointsCursor.close();
               }
            }
            mAverageSpeed = avgspeed;
            mLoggerMap.getActivity().runOnUiThread(new Runnable()
            {
               @Override
               public void run()
               {
                  //updateSpeedColoring();
               }
            });
         }
      };
      
      //a thread for calculating the height
     /* heightCalculator = new Runnable()
      {
         @Override
         public void run()
         {
            double avgHeight = 0.0;
            ContentResolver resolver = mLoggerMap.getActivity().getContentResolver();
            Cursor waypointsCursor = null;
            try
            {
               waypointsCursor = resolver.query(Uri.withAppendedPath(Labels.CONTENT_URI, LoggerMapHelper.this.mTrackId + "/waypoints"), new String[] {
                     "avg(" + Waypoints.ALTITUDE + ")", "max(" + Waypoints.ALTITUDE + ")" }, null, null, null);

               if (waypointsCursor != null && waypointsCursor.moveToLast())
               {
                  double average = waypointsCursor.getDouble(0);
                  double maxBasedAverage = waypointsCursor.getDouble(1) / 2;
                  avgHeight = Math.min(average, maxBasedAverage);
               }
            }
            finally
            {
               if (waypointsCursor != null)
               {
                  waypointsCursor.close();
               }
            }
            mAverageHeight = avgHeight;
            mLoggerMap.getActivity().runOnUiThread(new Runnable()
            {
               @Override
               public void run()
               {
                  updateSpeedColoring();
               }
            });
         }
      };
      */
      
      mServiceConnected = new Runnable()
      {
         @Override
         public void run()
         {
            updateBlankingBehavior();
         }
      };
      
      
      
      /*******************************************************
       * 8 Various dialog listeners
       */
      
      
      //gallery select listener
      mGalerySelectListener = new AdapterView.OnItemSelectedListener()
      {
         @Override
         public void onItemSelected(AdapterView< ? > parent, View view, int pos, long id)
         {
            mSelected = (Uri) parent.getSelectedItem();
         }

         @Override
         public void onNothingSelected(AdapterView< ? > arg0)
         {
            mSelected = null;
         }
      };
      
      // note selection
      mNoteSelectDialogListener = new DialogInterface.OnClickListener()
      {

         @Override
         public void onClick(DialogInterface dialog, int which)
         {
            SegmentRendering.handleMedia(mLoggerMap.getActivity(), mSelected);
            mSelected = null;
         }
      };
      
      
      //group checked
      mGroupCheckedChangeListener = new android.widget.RadioGroup.OnCheckedChangeListener()
      {
         @Override
         public void onCheckedChanged(RadioGroup group, int checkedId)
         {
            switch (checkedId)
            {
              /* case R.id.layer_osm_cloudmade:
                  setOsmBaseOverlay(Constants.OSM_CLOUDMADE);
                  break;
               case R.id.layer_osm_maknik:
                  setOsmBaseOverlay(Constants.OSM_MAKNIK);
                  break;
               case R.id.layer_osm_bicycle:
                  setOsmBaseOverlay(Constants.OSM_CYCLE);
                  break;
               default:
                  mLoggerMap.onLayerCheckedChanged(checkedId, true);
                  break;*/
            }
         }
      };
      
      
/*      mCheckedChangeListener = new OnCheckedChangeListener()
      {
         @Override
         public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
         {
            int checkedId;
            checkedId = buttonView.getId();
            
            switch (checkedId)
            {
               case R.id.layer_speed:
                  setSpeedOverlay(isChecked);
                  break;
               case R.id.layer_altitude:
                  setAltitudeOverlay(isChecked);
                  break;
               case R.id.layer_distance:
                  setDistanceOverlay(isChecked);
                  break;
               case R.id.layer_compass:
                  setCompassOverlay(isChecked);
                  break;
               case R.id.layer_location:
                  setLocationOverlay(isChecked);
                  break;
               default:
                  mLoggerMap.onLayerCheckedChanged(checkedId, isChecked);
                  break;
            }
            
         }
      };  */    
      
      mNoTrackDialogListener = new DialogInterface.OnClickListener()
      
      {
         @Override
         public void onClick(DialogInterface dialog, int which)
         {
                        Log.d( TAG, "mNoTrackDialogListener" + which);
            Intent tracklistIntent = new Intent(mLoggerMap.getActivity(), IssueList.class);
            tracklistIntent.putExtra(Labels._ID, mLabelId);
            mLoggerMap.getActivity().startActivityForResult(tracklistIntent, MENU_TRACKLIST);
         }
      };      
      
      /**
       * Listeners to events outside this mapview
       */
      
      mSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener()
      
      {
         @Override
         public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
         {
            if (key.equals(Constants.TRACKCOLORING))
            {
               mAverageSpeed = 0.0;
               mAverageHeight = 0.0;
               //updateSpeedColoring();
            }
            
            else if (key.equals(Constants.DISABLEBLANKING) || key.equals(Constants.DISABLEDIMMING))
            {
               updateBlankingBehavior();
            }
            else if (key.equals(Constants.SPEED))
            {
               //updateSpeedDisplayVisibility();
            }
            else if (key.equals(Constants.ALTITUDE))
            {
               //updateAltitudeDisplayVisibility();
            }
            else if (key.equals(Constants.DISTANCE))
            {
               //updateDistanceDisplayVisibility();
            }
            else if (key.equals(Constants.COMPASS))
            {
              // updateCompassDisplayVisibility();
            }
            else if (key.equals(Constants.LOCATION))
            {
               updateLocationDisplayVisibility();
            }
            else if (key.equals(Constants.MAPPROVIDER))
            {
               //updateMapProvider();
            }
            else if (key.equals(Constants.OSMBASEOVERLAY))
            {
               mLoggerMap.updateOverlays();
            }
            else
            {
               mLoggerMap.onSharedPreferenceChanged(sharedPreferences, key);
            }
         }
      };
      
      
      mTrackMediasObserver = new ContentObserver(new Handler())
      {
         @Override
         public void onChange(boolean selfUpdate)
         {
            if (!selfUpdate)
            {
               if (mLastSegmentOverlay != null)
               {
                  mLastSegmentOverlay.calculateMedia();
               }
            }
            else
            {
               Log.w(TAG, "mTrackMediasObserver skipping change on " + mLastSegment);
            }
         }
      };      
      
      mTrackSegmentsObserver = new ContentObserver(new Handler())
      {
         @Override
         public void onChange(boolean selfUpdate)
         {
            if (!selfUpdate)
            {
               //updateDataOverlays();
            }
            else
            {
               Log.w(TAG, "mTrackSegmentsObserver skipping change on " + mLastSegment);
            }
         }
      };
      mSegmentWaypointsObserver = new ContentObserver(new Handler())
      {
         @Override
         public void onChange(boolean selfUpdate)
         {
            if (!selfUpdate)
            {
               updateTrackNumbers();
               if (mLastSegmentOverlay != null)
               {
                  //moveActiveViewWindow();
                  updateMapProviderAdministration(mLoggerMap.getDataSourceId());
               }
               else
               {
                  Log.e(TAG, "Error the last segment changed but it is not on screen! " + mLastSegment);
               }
            }
            else
            {
               Log.w(TAG, "mSegmentWaypointsObserver skipping change on " + mLastSegment);
            }
         }
      };
      mUnitsChangeListener = new UnitsI18n.UnitsChangeListener()
      {
         @Override
         public void onUnitsChange()
         {
            mAverageSpeed = 0.0;
            mAverageHeight = 0.0;
            updateTrackNumbers();
            //updateSpeedColoring();
         }
      };
   }

   public void onCreateOptionsMenu(Menu menu)
   {
      menu.add(ContextMenu.NONE, MENU_TRACKING, ContextMenu.NONE, R.string.menu_tracking).
      setIcon(R.drawable.ic_menu_movie).setAlphabeticShortcut('T');   
      
      menu.add(ContextMenu.NONE, MENU_SHARE, ContextMenu.NONE, R.string.menu_shareTrack).
      setIcon(R.drawable.ic_action_share).setAlphabeticShortcut('I');
      // More

      menu.add(ContextMenu.NONE, MENU_TRACKLIST, ContextMenu.NONE, R.string.menu_tracklist).
      setIcon(R.drawable.ic_action_share).setAlphabeticShortcut('P');
      
      menu.add(ContextMenu.NONE, MENU_SETTINGS, ContextMenu.NONE, R.string.menu_settings).
      setIcon(R.drawable.ic_action_share).setAlphabeticShortcut('C');
    
   }

   public void onPrepareOptionsMenu(Menu menu)
   {
	   //omly insert note when the media is prepared
      MenuItem noteMenu = menu.findItem(MENU_NOTE);
      noteMenu.setEnabled(mLoggerServiceManager.isMediaPrepared());

       //only allowed to share when you have one or more tracks.
      MenuItem shareMenu = menu.findItem(MENU_SHARE);
      shareMenu.setEnabled(mLabelId >= 0);
   }

   public boolean onOptionsItemSelected(MenuItem item)
   {
      boolean handled = false;

      Uri trackUri;
      Intent intent;
      
      switch (item.getItemId())
      {
         case MENU_TRACKING:
            intent = new Intent(mLoggerMap.getActivity(), ControlLogging.class);
            mLoggerMap.getActivity().startActivityForResult(intent, MENU_TRACKING);
            handled = true;
            break;
        
         case MENU_SETTINGS:
            intent = new Intent(mLoggerMap.getActivity(), ApplicationPreferenceActivity.class);
            mLoggerMap.getActivity().startActivity(intent);
            handled = true;
            break;
            
         case MENU_TRACKLIST:
            intent = new Intent(mLoggerMap.getActivity(), IssueList.class);
            intent.putExtra(Labels._ID, this.mLabelId);
            mLoggerMap.getActivity().startActivityForResult(intent, MENU_TRACKLIST);
            handled = true;
            break;

         case MENU_SHARE:
            intent = new Intent(Intent.ACTION_RUN);
            trackUri = ContentUris.withAppendedId(Labels.CONTENT_URI, mLabelId);
            intent.setDataAndType(trackUri, Labels.CONTENT_ITEM_TYPE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Bitmap bm = mLoggerMap.getDrawingCache();
            Uri screenStreamUri = ShareLabels.storeScreenBitmap(bm);
            intent.putExtra(Intent.EXTRA_STREAM, screenStreamUri);
            mLoggerMap.getActivity().startActivityForResult(Intent.createChooser(intent, mLoggerMap.getActivity().getString(R.string.share_track)), MENU_SHARE);
            handled = true;
            break;
        default:
            handled = false;
            break;
      }
      return handled;
   }

   protected Dialog onCreateDialog(int id)
   { 
	  //initialize the dialog 
      Dialog dialog = null;
      LayoutInflater factory = null;
      View view = null;
      Builder builder = null;
      switch (id)
      {
 
         case DIALOG_NOTRACK:
            builder = new AlertDialog.Builder(mLoggerMap.getActivity());
            builder.setTitle(R.string.dialog_notrack_title).setMessage(R.string.dialog_notrack_message).setIcon(android.R.drawable.ic_dialog_alert)
                  .setPositiveButton(R.string.btn_selecttrack, mNoTrackDialogListener).setNegativeButton(R.string.btn_cancel, null);
            dialog = builder.create();
            return dialog;
         case DIALOG_URIS:
            builder = new AlertDialog.Builder(mLoggerMap.getActivity());
            factory = LayoutInflater.from(mLoggerMap.getActivity());
            view = factory.inflate(R.layout.mediachooser, null);
            builder.setTitle(R.string.dialog_select_media_title).setMessage(R.string.dialog_select_media_message).setIcon(android.R.drawable.ic_dialog_alert)
                  .setNegativeButton(R.string.btn_cancel, null).setPositiveButton(R.string.btn_okay, mNoteSelectDialogListener).setView(view);
            dialog = builder.create();
            return dialog;
        default:
            return null;
      }
   }

   protected void onPrepareDialog(int id, Dialog dialog)
   {
      RadioButton satellite;
      RadioButton regular;
      RadioButton cloudmade;
      RadioButton mapnik;
      RadioButton cycle;
      switch (id)
      {
      // removed the one which creates the layers dialog
         case DIALOG_URIS:
            Gallery gallery = (Gallery) dialog.findViewById(R.id.gallery);
            gallery.setAdapter(mMediaAdapter);
            gallery.setOnItemSelectedListener(mGalerySelectListener);
         default:
            break;
      }
   }

   protected void onActivityResult(int requestCode, int resultCode, Intent intent)
   {
      Uri trackUri;
      long trackId;
      switch (requestCode)
      {
         case MENU_TRACKLIST:
            if (resultCode == Activity.RESULT_OK)
            {
               trackUri = intent.getData();
               trackId = Long.parseLong(trackUri.getLastPathSegment());
               moveToTrack(trackId, true);
            }
            break;
         case MENU_TRACKING:
            if (resultCode == Activity.RESULT_OK)
            {
               trackUri = intent.getData();
               if (trackUri != null)
               {
                  trackId = Long.parseLong(trackUri.getLastPathSegment());
                  moveToTrack(trackId, true);
               }
            }
            break;
         case MENU_SHARE:
            ShareLabels.clearScreenBitmap();
            break;
         default:
            Log.e(TAG, "Returned form unknow activity: " + requestCode);
            break;
      }
   }

   private void updateTitleBar()
   {
      ContentResolver resolver = mLoggerMap.getActivity().getContentResolver();
      Cursor trackCursor = null;
      try
      {
         trackCursor = resolver.query(ContentUris.withAppendedId(Labels.CONTENT_URI, this.mLabelId), new String[] { Labels.NAME }, null, null, null);
         if (trackCursor != null && trackCursor.moveToLast())
         {
            String trackName = trackCursor.getString(0);
            mLoggerMap.getActivity().setTitle(mLoggerMap.getActivity().getString(R.string.app_name) + ": " + trackName);
         }
      }
      finally
      {
         if (trackCursor != null)
         {
            trackCursor.close();
         }
      }
   }

   protected void updateMapProviderAdministration(String provider)
   {
      mLoggerServiceManager.storeDerivedDataSource(provider);
   }

   private void updateBlankingBehavior()
   {
      boolean disableblanking = mSharedPreferences.getBoolean(Constants.DISABLEBLANKING, false);
      boolean disabledimming = mSharedPreferences.getBoolean(Constants.DISABLEDIMMING, false);
      if (disableblanking)
      {
         if (mWakeLock == null)
         {
            PowerManager pm = (PowerManager) mLoggerMap.getActivity().getSystemService(Context.POWER_SERVICE);
            if (disabledimming)
            {
               mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, TAG);
            }
            else
            {
               mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, TAG);
            }
         }
         if (mLoggerServiceManager.getLoggingState() == Constants.LOGGING && !mWakeLock.isHeld())
         {
            mWakeLock.acquire();
            Log.w(TAG, "Acquired lock to keep screen on!");
         }
      }
   }

  
   private void updateLocationDisplayVisibility()
   {
      boolean location = mSharedPreferences.getBoolean(Constants.LOCATION, false);
      if (location)
      {
         mLoggerMap.enableMyLocation();
      }
      else
      {
         mLoggerMap.disableMyLocation();
      }
   }

   /**
    * Retrieves the numbers of the measured speed and altitude from the most
    * recent waypoint and updates UI components with this latest bit of
    * information.
    */
   private void updateTrackNumbers()
   {
      Location lastWaypoint = mLoggerServiceManager.getLastWaypoint();
      UnitsI18n units = mUnits;
      if (lastWaypoint != null && units != null)
      {
         // Speed number
         mSpeed = lastWaypoint.getSpeed();
         mAltitude = lastWaypoint.getAltitude();
         mDistance = mLoggerServiceManager.getTrackedDistance();
      }

      //Distance number
      double distance = units.conversionFromMeter(mDistance);
      String distanceText = String.format("%.2f %s", distance, units.getDistanceUnit());
      TextView mDistanceView = mLoggerMap.getDistanceTextView();
      mDistanceView.setText(distanceText);
      
      //Speed number
      double speed = units.conversionFromMetersPerSecond(mSpeed);
      String speedText = units.formatSpeed(speed, false);
      TextView lastGPSSpeedView = mLoggerMap.getSpeedTextView();
      lastGPSSpeedView.setText(speedText);
      
   }

   /**
    * For the current track identifier, the route of that track is drawn by
    * adding a OverLay for each segments in the track
    * 
    * @param trackId
    * @see SegmentRendering
    */
   
 
  
  
   /**
    * Updates the labels next to the color bar with speeds
    */
   private void drawSpeedTexts()
   {
      UnitsI18n units = mUnits;
      if (units != null)
      {
         double avgSpeed = units.conversionFromMetersPerSecond(mAverageSpeed);
         TextView[] mSpeedtexts = mLoggerMap.getSpeedTextViews();
        // SlidingIndicatorView currentScaleIndicator = mLoggerMap.getScaleIndicatorView();
         for (int i = 0; i < mSpeedtexts.length; i++)
         {
            mSpeedtexts[i].setVisibility(View.VISIBLE);
            double speed;
            if (mUnits.isUnitFlipped())
            {
               speed = ((avgSpeed * 2d) / 5d) * (mSpeedtexts.length - i - 1);
            }
            else
            {
               speed = ((avgSpeed * 2d) / 5d) * i;
            }
            if( i == 0 )
            {
              // currentScaleIndicator.setMin((float) speed);
            }
            else
            {
              // currentScaleIndicator.setMax((float) speed);
            }
            String speedText = units.formatSpeed(speed, false);
            mSpeedtexts[i].setText(speedText);
         }
      }
   }

   /**
    * Alter this to set a new track as current.
    * 
    * @param trackId
    * @param center center on the end of the track
    */
   private void moveToTrack(long trackId, boolean center)
   {
      if( trackId == mLabelId )
      {
         return;
      }
      Cursor track = null;
      try
      {
         ContentResolver resolver = mLoggerMap.getActivity().getContentResolver();
         Uri trackUri = ContentUris.withAppendedId(Labels.CONTENT_URI, trackId);
         track = resolver.query(trackUri, new String[] { Labels.NAME }, null, null, null);
         if (track != null && track.moveToFirst())
         {
            this.mLabelId = trackId;
            mLastSegment = -1;
            resolver.unregisterContentObserver(this.mTrackSegmentsObserver);
            resolver.unregisterContentObserver(this.mTrackMediasObserver);
            Uri tracksegmentsUri = Uri.withAppendedPath(Labels.CONTENT_URI, trackId + "/segments");

            resolver.registerContentObserver(tracksegmentsUri, false, this.mTrackSegmentsObserver);
            //resolver.registerContentObserver(Media.CONTENT_URI, true, this.mTrackMediasObserver);

            mLoggerMap.clearOverlays();
            //mBitmapSegmentsOverlay.clearSegments();
            mAverageSpeed = 0.0;
            mAverageHeight = 0.0;
            
            updateTitleBar();
            /*updateDataOverlays();
            updateSpeedColoring();*/
            if (center)
            {
               GeoPoint lastPoint = getLastTrackPoint();
               mLoggerMap.animateTo(lastPoint);
            }
         }
      }
      finally
      {
         if (track != null)
         {
            track.close();
         }
      }
   }

   /**
    * Get the last know position from the GPS provider and return that
    * information wrapped in a GeoPoint to which the Map can navigate.
    * 
    * @see GeoPoint
    * @return
    */
   private GeoPoint getLastKnowGeopointLocation()
   {
      int microLatitude = 0;
      int microLongitude = 0;
      LocationManager locationManager = (LocationManager) mLoggerMap.getActivity().getApplication().getSystemService(Context.LOCATION_SERVICE);
      Location locationFine = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
      if (locationFine != null)
      {
         microLatitude = (int) (locationFine.getLatitude() * 1E6d);
         microLongitude = (int) (locationFine.getLongitude() * 1E6d);
      }
      if (locationFine == null || microLatitude == 0 || microLongitude == 0)
      {
         Location locationCoarse = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
         if (locationCoarse != null)
         {
            microLatitude = (int) (locationCoarse.getLatitude() * 1E6d);
            microLongitude = (int) (locationCoarse.getLongitude() * 1E6d);
         }
         if (locationCoarse == null || microLatitude == 0 || microLongitude == 0)
         {
            microLatitude = 51985105;
            microLongitude = 5106132;
         }
      }
      GeoPoint geoPoint = new GeoPoint(microLatitude, microLongitude);
      return geoPoint;
   }

   /**
    * Retrieve the last point of the current track
    * 
    * @param context
    */
   private GeoPoint getLastTrackPoint()
   {
      Cursor waypoint = null;
      GeoPoint lastPoint = null;
      // First try the service which might have a cached version
      Location lastLoc = mLoggerServiceManager.getLastWaypoint();
      if (lastLoc != null)
      {
         int microLatitude = (int) (lastLoc.getLatitude() * 1E6d);
         int microLongitude = (int) (lastLoc.getLongitude() * 1E6d);
         lastPoint = new GeoPoint(microLatitude, microLongitude);
      }

      // If nothing yet, try the content resolver and query the track
      if (lastPoint == null || lastPoint.getLatitudeE6() == 0 || lastPoint.getLongitudeE6() == 0)
      {
         try
         {
            ContentResolver resolver = mLoggerMap.getActivity().getContentResolver();
            waypoint = resolver.query(Uri.withAppendedPath(Labels.CONTENT_URI, mLabelId + "/waypoints"), new String[] { Labels.LATITUDE,
                  Labels.LONGITUDE, "max(" + Labels.TABLE + "." + Labels._ID + ")" }, null, null, null);
            if (waypoint != null && waypoint.moveToLast())
            {
               int microLatitude = (int) (waypoint.getDouble(0) * 1E6d);
               int microLongitude = (int) (waypoint.getDouble(1) * 1E6d);
               lastPoint = new GeoPoint(microLatitude, microLongitude);
            }
         }
         finally
         {
            if (waypoint != null)
            {
               waypoint.close();
            }
         }
      }

      // If nothing yet, try the last generally known location
      if (lastPoint == null || lastPoint.getLatitudeE6() == 0 || lastPoint.getLongitudeE6() == 0)
      {
         lastPoint = getLastKnowGeopointLocation();
      }
      return lastPoint;
   }

   private void moveToLastTrack()
   {
      int trackId = -1;
      Cursor track = null;
      try
      {
         ContentResolver resolver = mLoggerMap.getActivity().getContentResolver();
         track = resolver.query(Labels.CONTENT_URI, new String[] { "max(" + Labels._ID + ")", Labels.NAME, }, null, null, null);
         if (track != null && track.moveToLast())
         {
            trackId = track.getInt(0);
            moveToTrack(trackId, false);
         }
      }
      finally
      {
         if (track != null)
         {
            track.close();
         }
      }
   }

   /**
    * Enables a SegmentOverlay to call back to the MapActivity to show a dialog
    * with choices of media
    * 
    * @param mediaAdapter
    */
   public void showMediaDialog(BaseAdapter mediaAdapter)
   {
      mMediaAdapter = mediaAdapter;
      mLoggerMap.getActivity().showDialog(DIALOG_URIS);
   }

   public SharedPreferences getPreferences()
   {
      return mSharedPreferences;
   }

   public boolean isLogging()
   {
      return mLoggerServiceManager.getLoggingState() == Constants.LOGGING;
   }

}
