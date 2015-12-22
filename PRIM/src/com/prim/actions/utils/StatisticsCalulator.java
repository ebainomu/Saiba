package com.prim.actions.utils;

import  com.prim.db.Prim.Segments;
import com.prim.db.Prim.Labels;
import  com.prim.db.Prim.Locations;
import com.prim.utils.Constants;
import com.prim.utils.UnitsI18n;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;

public class StatisticsCalulator extends AsyncTask<Uri, Void, Void>
{

   @SuppressWarnings("unused")
   private static final String TAG = "OGT.StatisticsCalulator";
   private Context mContext;   
   private String overallavgSpeedText = "Unknown";
   private String avgSpeedText = "Unknown";
   private String maxSpeedText = "Unknown";
   private String ascensionText = "Unknown";
   private String minSpeedText = "Unknown";
   private String tracknameText = "Unknown";
   private String waypointsText = "Unknown";
   private String distanceText = "Unknown";
   private long mStarttime = -1;
   private long mEndtime = -1;
   private UnitsI18n mUnits;
   private double mMaxSpeed;
   
   //the variables for altitude are below:
   private double mMaxAltitude;	
   private double mMinAltitude;
   private double mAscension;
   private double mDistanceTraveled;
   private long mDuration;
   private double mAverageActiveSpeed;
   private StatisticsDelegate mDelegate;
   
   
   public StatisticsCalulator( Context ctx, UnitsI18n units, StatisticsDelegate delegate )
   {
      mContext = ctx;
      mUnits = units;
      mDelegate = delegate;
   }

   private void updateCalculations( Uri labelUri )
   {
      mStarttime = -1;
      mEndtime = -1;
      mMaxSpeed = 0;
      mAverageActiveSpeed = 0;
      mMaxAltitude = 0;
      mMinAltitude = 0;
      mAscension = 0;
      mDistanceTraveled = 0f;
      mDuration = 0;
      long duration = 1;
      double ascension = 0;

      ContentResolver resolver = mContext.getContentResolver();

      Cursor locationsCursor = null;
      try
      {
         locationsCursor = resolver.query( 
               Uri.withAppendedPath( labelUri, "locations" ), 
               new String[] { "max  (" + Locations.TABLE + "." +Locations.SPEED + ")"
                          /*  , "max  (" + Locations.TABLE + "." + Waypoints.ALTITUDE + ")"
                            , "min  (" + Locations.TABLE + "." + Waypoints.ALTITUDE + ")"*/
                            , "count(" + Locations.TABLE + "." + Locations._ID + ")" },
               null, null, null );
         if( locationsCursor.moveToLast() )
         {
            mMaxSpeed = locationsCursor.getDouble( 0 );
         /* mMaxAltitude = waypointsCursor.getDouble( 1 );
            mMinAltitude = waypointsCursor.getDouble( 2 );*/
            long nrWaypoints = locationsCursor.getLong( 3 );
            waypointsText = nrWaypoints + "";
         }
         locationsCursor.close();
         locationsCursor = resolver.query( 
               Uri.withAppendedPath( labelUri, "locations" ), 
               new String[] { "avg  (" + Locations.TABLE + "." + Locations.SPEED + ")" },
               Locations.TABLE + "." + Locations.SPEED +"  > ?", 
               new String[] { ""+Constants.MIN_STATISTICS_SPEED }, 
               null );
         if( locationsCursor.moveToLast() )
         {
            mAverageActiveSpeed = locationsCursor.getDouble( 0 );
         }
      }
      finally
      {
         if( locationsCursor != null )
         {
            locationsCursor.close();
         }
      }
      Cursor labelCursor = null;
      try
      {
         labelCursor = resolver.query( labelUri, new String[] { Labels.NAME }, null, null, null );
         if( labelCursor.moveToLast() )
         {
            tracknameText = labelCursor.getString( 0 );
         }
      }
      
      finally
      {
         if( labelCursor != null )
         {
            labelCursor.close();
         }
      }
      Cursor segments = null;
      Location lastLocation = null;
      Location lastAltitudeLocation = null;
      Location currentLocation = null;
      try
      {
         Uri segmentsUri = Uri.withAppendedPath( labelUri, "segments" );
         segments = resolver.query( segmentsUri, new String[] { Segments._ID }, null, null, null );
         if( segments.moveToFirst() )
         {
            do
            {
               long segmentsId = segments.getLong( 0 );
               Cursor waypoints = null;
               try
               {
                  Uri waypointsUri = Uri.withAppendedPath( segmentsUri, segmentsId + "/waypoints" );
                  waypoints = resolver.query( waypointsUri, new String[] { Locations._ID, Locations.TIME, Locations.LONGITUDE, Locations.LATITUDE }, null, null, null );
                  if( waypoints.moveToFirst() )
                  {
                     do
                     {
                        if( mStarttime < 0 )
                        {
                           mStarttime = waypoints.getLong( 1 );
                        }
                        currentLocation = new Location( this.getClass().getName() );
                        currentLocation.setTime( waypoints.getLong( 1 ) );
                        currentLocation.setLongitude( waypoints.getDouble( 2 ) );
                        currentLocation.setLatitude( waypoints.getDouble( 3 ) );
                        currentLocation.setAltitude( waypoints.getDouble( 4 ) );
                        
                        // Do no include obvious wrong 0.0 lat 0.0 long, skip to next value in while-loop
                        if( currentLocation.getLatitude() == 0.0d || currentLocation.getLongitude() == 0.0d )
                        {
                           continue;
                        }
                        
                        if( lastLocation != null )
                        {
                           float travelPart = lastLocation.distanceTo( currentLocation );
                           long timePart = currentLocation.getTime() - lastLocation.getTime();
                           mDistanceTraveled += travelPart;
                           duration += timePart;
                        }
                        if( currentLocation.hasAltitude() )
                        {
                           if( lastAltitudeLocation != null  )
                           {
                              if( currentLocation.getTime() - lastAltitudeLocation.getTime() > 5*60*1000 ) // more then a 5m of climbing
                              {
                                 if( currentLocation.getAltitude() > lastAltitudeLocation.getAltitude()+1 ) // more then 1m climb
                                 {
                                    ascension += currentLocation.getAltitude() - lastAltitudeLocation.getAltitude();
                                    lastAltitudeLocation = currentLocation;
                                 }
                                 else
                                 {
                                    lastAltitudeLocation = currentLocation;
                                 }
                              }
                           }
                           else
                           {
                              lastAltitudeLocation = currentLocation;
                           }
                        }
                        lastLocation = currentLocation;
                        mEndtime = lastLocation.getTime();
                     }
                     while( waypoints.moveToNext() );
                     mDuration = mEndtime - mStarttime;
                  }
               }
               finally
               {
                  if( waypoints != null )
                  {
                     waypoints.close();
                  }
               }
               lastLocation = null;
            }
            while( segments.moveToNext() );
         }
      }
      finally
      {
         if( segments != null )
         {
            segments.close();
         }
      }
      double maxSpeed          = mUnits.conversionFromMetersPerSecond( mMaxSpeed );
      double overallavgSpeedfl = mUnits.conversionFromMeterAndMiliseconds( mDistanceTraveled, mDuration );
      double avgSpeedfl        = mUnits.conversionFromMeterAndMiliseconds( mDistanceTraveled, duration );
      double traveled          = mUnits.conversionFromMeter( mDistanceTraveled );
      avgSpeedText        = mUnits.formatSpeed( avgSpeedfl, true ); 
      overallavgSpeedText = mUnits.formatSpeed( overallavgSpeedfl, true );
      maxSpeedText        = mUnits.formatSpeed( maxSpeed, true );
      distanceText        = String.format( "%.2f %s", traveled,          mUnits.getDistanceUnit() );
      ascensionText       = String.format( "%.0f %s", ascension,         mUnits.getHeightUnit() );
   }

   /**
    * Get the overallavgSpeedText.
    *
    * @return Returns the overallavgSpeedText as a String.
    */
   public String getOverallavgSpeedText()
   {
      return overallavgSpeedText;
   }

   /**
    * Get the avgSpeedText.
    *
    * @return Returns the avgSpeedText as a String.
    */
   public String getAvgSpeedText()
   {
      return avgSpeedText;
   }

   /**
    * Get the maxSpeedText.
    *
    * @return Returns the maxSpeedText as a String.
    */
   public String getMaxSpeedText()
   {
      return maxSpeedText;
   }

   /**
    * Get the minSpeedText.
    *
    * @return Returns the minSpeedText as a String.
    */
   public String getMinSpeedText()
   {
      return minSpeedText;
   }
   
   /**
    * Get the tracknameText.
    *
    * @return Returns the tracknameText as a String.
    */
   public String getTracknameText()
   {
      return tracknameText;
   }

   /**
    * Get the waypointsText.
    *
    * @return Returns the waypointsText as a String.
    */
   public String getWaypointsText()
   {
      return waypointsText;
   }

   /**
    * Get the distanceText.
    *
    * @return Returns the distanceText as a String.
    */
   public String getDistanceText()
   {
      return distanceText;
   }

   /**
    * Get the starttime.
    *
    * @return Returns the starttime as a long.
    */
   public long getStarttime()
   {
      return mStarttime;
   }

   /**
    * Get the endtime.
    *
    * @return Returns the endtime as a long.
    */
   public long getEndtime()
   {
      return mEndtime;
   }

   /**
    * Get the maximum speed.
    *
    * @return Returns the maxSpeeddb as m/s in a double.
    */
   public double getMaxSpeed()
   {
      return mMaxSpeed;
   }
   
   /**
    * Get the min speed.
    *
    * @return Returns the average speed as m/s in a double.
    */
   public double getAverageStatisicsSpeed()
   {
      return mAverageActiveSpeed;
   }

   /**
    * Get the maxAltitude.
    *
    * @return Returns the maxAltitude as a double.
    */
   public double getMaxAltitude()
   {
      return mMaxAltitude;
   }

   /**
    * Get the minAltitude.
    *
    * @return Returns the minAltitude as a double.
    */
   public double getMinAltitude()
   {
      return mMinAltitude;
   }

   /**
    * Get the total ascension in m.
    *
    * @return Returns the ascension as a double.
    */
   public double getAscension()
   {
      return mAscension;
   }
   
   public CharSequence getAscensionText()
   {
      return ascensionText;
   }
   
   /**
    * Get the distanceTraveled.
    *
    * @return Returns the distanceTraveled as a float.
    */
   public double getDistanceTraveled()
   {
      return mDistanceTraveled;
   }

   /**
    * Get the mUnits.
    *
    * @return Returns the mUnits as a UnitsI18n.
    */
   public UnitsI18n getUnits()
   {
      return mUnits;
   }

   public String getDurationText()
   {
      long s = mDuration / 1000;
      String duration = String.format("%dh:%02dm:%02ds", s/3600, (s%3600)/60, (s%60));

      return duration;
   }

   @Override
   protected Void doInBackground(Uri... params)
   {
      this.updateCalculations(params[0]);
      return null;
   }
   
   @Override
   protected void onPostExecute(Void result)
   {
      super.onPostExecute(result);
      if( mDelegate != null )
      {
         mDelegate.finishedCalculations(this);
      }
      
   }
}
