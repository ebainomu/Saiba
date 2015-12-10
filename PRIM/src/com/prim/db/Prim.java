package com.prim.db;

import android.content.ContentUris;
import android.net.Uri;
import android.net.Uri.Builder;
import android.provider.BaseColumns;

/**
 * 
 * @author baalmart
 * This stores all logs and GPS data.
 * this is the contract class that I am using to draw the schema of the databases and act accordingly.
 * it contains the uris, tables and columns for the database.
 * 
 * This lets you change a column name in one place 
 * and have it propagate throughout your code.

   definitions that are global to the whole database in the root level of the class. 
   inner class for each table that enumerates its columns
   Using this class to just create the look and feel of the entire database.

   @author baalmart
 */


public final class Prim
{
   /** The authority of this provider: nl.sogeti.android.gpstracker */
   public static final String AUTHORITY = "nl.sogeti.android.gpstracker";
   /** The content:// style Uri for this provider, content://nl.sogeti.android.gpstracker */
   public static final Uri CONTENT_URI = Uri.parse( "content://" + Prim.AUTHORITY );
   /** The name of the database file */
   static final String DATABASE_NAME = "PRIMLOG.db";
   /** The version of the database schema */
   static final int DATABASE_VERSION = 10;

   /**
    * This table contains routes...
    * 
    * implementing the BaseColumns interface will inherit a primary key field called _ID that some 
    * Android classes such as cursor adaptors will expect
    *  it to have. It's not required, but this can help your
    *   database work harmoniously with the Android framework.

    */
   public static final class Tracks extends TracksColumns implements android.provider.BaseColumns
   {
      /** The MIME type of a CONTENT_URI subdirectory of a single track. */
      public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.nl.sogeti.android.track";
      /** The MIME type of CONTENT_URI providing a directory of tracks. */
      public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.nl.sogeti.android.track";
      /** The content:// style URL for this provider, content://nl.sogeti.android.gpstracker/tracks */
      public static final Uri CONTENT_URI = Uri.parse( "content://" + Prim.AUTHORITY + "/" + Tracks.TABLE );

      /** The name of this table */
      public static final String TABLE = "tracks";
      static final String CREATE_STATEMENT = 
         "CREATE TABLE " + Tracks.TABLE + "(" + " " + Tracks._ID           + " " + Tracks._ID_TYPE + 
                                          "," + " " + Tracks.NAME          + " " + Tracks.NAME_TYPE + 
                                          "," + " " + Tracks.CREATION_TIME + " " + Tracks.CREATION_TIME_TYPE + 
                                          ");";
   }
   
   
   public static final class xyz extends XYZColumns implements android.provider.BaseColumns
   {
      /** The MIME type of a CONTENT_URI subdirectory of a single track. */
      public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.nl.sogeti.android.track";
      /** The MIME type of CONTENT_URI providing a directory of tracks. */
      public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.nl.sogeti.android.track";
      /** The content:// style URL for this provider, content://nl.sogeti.android.gpstracker/tracks */
      public static final Uri CONTENT_URI = Uri.parse( "content://" + Prim.AUTHORITY + "/" + Tracks.TABLE );

      /** The name of this table */
      public static final String TABLE = "xyz";
      static final String CREATE_STATEMENT = 
         "CREATE TABLE " + xyz.TABLE + "(" + " " + xyz._ID           + " " + xyz._ID_TYPE + 
                                          "," + " " + xyz.LABEL          + " " + xyz.LABEL_TYPE + 
                                          "," + " " + xyz.CREATION_TIME + " " + xyz.CREATION_TIME_TYPE + 
                                          "," + " " + xyz.TIME + " "  + xyz.TIME_TYPE + 
                                          "," + " " + xyz.SPEED + " " + xyz.SPEED_TYPE +
                                          ");";
   }
   
   /**
    * This table contains segments.

    */
   public static final class Segments extends SegmentsColumns implements android.provider.BaseColumns
   {

      /** The MIME type of a CONTENT_URI subdirectory of a single segment. */
      public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.nl.sogeti.android.segment";
      /** The MIME type of CONTENT_URI providing a directory of segments. */
      public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.nl.sogeti.android.segment";

      /** The name of this table, segments */
      public static final String TABLE = "segments";
      static final String CREATE_STATMENT = 
         "CREATE TABLE " + Segments.TABLE + "(" + " " + Segments._ID   + " " + Segments._ID_TYPE + 
                                            "," + " " + Segments.TRACK + " " + Segments.TRACK_TYPE + 
                                            ");";
   }

   /**
    * This table contains waypoints.
    */
   
   public static final class Waypoints extends WaypointsColumns implements android.provider.BaseColumns
   {

      /** The MIME type of a CONTENT_URI subdirectory of a single waypoint. */
      public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.nl.sogeti.android.waypoint";
      /** The MIME type of CONTENT_URI providing a directory of waypoints. */
      public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.nl.sogeti.android.waypoint";
      
      /** The name of this table, waypoints */
      public static final String TABLE = "waypoints";
      static final String CREATE_STATEMENT = "CREATE TABLE " + Waypoints.TABLE + 
      "(" + " " + BaseColumns._ID + " " + WaypointsColumns._ID_TYPE + 
      "," + " " + WaypointsColumns.LATITUDE  + " " + WaypointsColumns.LATITUDE_TYPE + 
      "," + " " + WaypointsColumns.LONGITUDE + " " + WaypointsColumns.LONGITUDE_TYPE + 
      "," + " " + WaypointsColumns.TIME      + " " + WaypointsColumns.TIME_TYPE + 
      "," + " " + WaypointsColumns.SPEED     + " " + WaypointsColumns.SPEED + 
      "," + " " + WaypointsColumns.SEGMENT   + " " + WaypointsColumns.SEGMENT_TYPE + 
      "," + " " + WaypointsColumns.ACCURACY  + " " + WaypointsColumns.ACCURACY_TYPE + 
      "," + " " + WaypointsColumns.ALTITUDE  + " " + WaypointsColumns.ALTITUDE_TYPE + 
      "," + " " + WaypointsColumns.BEARING   + " " + WaypointsColumns.BEARING_TYPE + 
      ");";
      
      static final String[] UPGRADE_STATEMENT_7_TO_8 = 
         {
            "ALTER TABLE " + Waypoints.TABLE + " ADD COLUMN " + WaypointsColumns.ACCURACY + " " + WaypointsColumns.ACCURACY_TYPE +";",
            "ALTER TABLE " + Waypoints.TABLE + " ADD COLUMN " + WaypointsColumns.ALTITUDE + " " + WaypointsColumns.ALTITUDE_TYPE +";",
            "ALTER TABLE " + Waypoints.TABLE + " ADD COLUMN " + WaypointsColumns.BEARING  + " " + WaypointsColumns.BEARING_TYPE +";"
         };

      /**
       * Build a waypoint Uri like:
       * 
       * @param trackId
       * @param segmentId
       * @param waypointId
       * 
       * @return
       */
      public static Uri buildUri(long trackId, long segmentId, long waypointId)
      {
         Builder builder = Tracks.CONTENT_URI.buildUpon();
         ContentUris.appendId(builder, trackId);
         builder.appendPath(Segments.TABLE);
         ContentUris.appendId(builder, segmentId);
         builder.appendPath(Waypoints.TABLE);
         ContentUris.appendId(builder, waypointId);
         
         return builder.build();
      }
   }
   
   
  /* the location parameters*/
   
   public static final class Location extends LocationColumns implements android.provider.BaseColumns
   {

      /** The MIME type of a CONTENT_URI subdirectory of a single waypoint. */
      public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.nl.sogeti.android.waypoint";
      /** The MIME type of CONTENT_URI providing a directory of waypoints. */
      public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.nl.sogeti.android.waypoint";
      
      /** The name of this table, Location */
      public static final String TABLE = "location";
      static final String CREATE_STATEMENT = "CREATE TABLE " + Waypoints.TABLE + 
      "(" + " " + BaseColumns._ID + " " + Location._ID_TYPE + 
      "," + " " + Location.LATITUDE  + " " + Location.LATITUDE_TYPE + 
      "," + " " +Location.LONGITUDE + " " + Location.LONGITUDE_TYPE + 
      "," + " " + Location.TIME      + " " + Location.TIME_TYPE + 
      "," + " " + Location.SPEED     + " " + Location.SPEED + 
      "," + " " + Location.SEGMENT   + " " + Location.SEGMENT_TYPE + 
      "," + " " + Location.ACCURACY  + " " + Location.ACCURACY_TYPE +      
      ");";
      
      static final String[] UPGRADE_STATEMENT_7_TO_8 = 
         {
            "ALTER TABLE " + Waypoints.TABLE + " ADD COLUMN " + WaypointsColumns.ACCURACY + " " + WaypointsColumns.ACCURACY_TYPE +";",
            "ALTER TABLE " + Waypoints.TABLE + " ADD COLUMN " + WaypointsColumns.ALTITUDE + " " + WaypointsColumns.ALTITUDE_TYPE +";",
            "ALTER TABLE " + Waypoints.TABLE + " ADD COLUMN " + WaypointsColumns.BEARING  + " " + WaypointsColumns.BEARING_TYPE +";"
         };

      /**
       * Build a waypoint Uri like:
       * 
       * @param trackId
       * @param segmentId
       * @param waypointId
       * 
       * @return
       */
      public static Uri buildUri(long trackId, long segmentId, long waypointId)
      {
         Builder builder = Tracks.CONTENT_URI.buildUpon();
         ContentUris.appendId(builder, trackId);
         builder.appendPath(Segments.TABLE);
         ContentUris.appendId(builder, segmentId);
         builder.appendPath(Waypoints.TABLE);
         ContentUris.appendId(builder, waypointId);
         
         return builder.build();
      }
   }
   
   /**
 
    */
   
   public static final class Media extends MediaColumns implements android.provider.BaseColumns
   {

      /** The MIME type of a CONTENT_URI subdirectory of a single media entry. */
      public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.nl.sogeti.android.media";
      /** The MIME type of CONTENT_URI providing a directory of media entry. */
      public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.nl.sogeti.android.media";
      
      /** The name of this table */
      public static final String TABLE = "media";
      static final String CREATE_STATEMENT = "CREATE TABLE " + Media.TABLE + 
      "(" + " " + BaseColumns._ID       + " " + MediaColumns._ID_TYPE + 
      "," + " " + MediaColumns.TRACK    + " " + MediaColumns.TRACK_TYPE + 
      "," + " " + MediaColumns.SEGMENT  + " " + MediaColumns.SEGMENT_TYPE + 
      "," + " " + MediaColumns.WAYPOINT + " " + MediaColumns.WAYPOINT_TYPE + 
      "," + " " + MediaColumns.URI      + " " + MediaColumns.URI_TYPE + 
      ");";
      public static final Uri CONTENT_URI = Uri.parse( "content://" + Prim.AUTHORITY + "/" + Media.TABLE );
   }
   
   /**
    * This table contains media URI's.
    * 

    */
   public static final class MetaData extends MetaDataColumns implements android.provider.BaseColumns
   {

      /** The MIME type of a CONTENT_URI subdirectory of a single metadata entry. */
      public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.nl.sogeti.android.metadata";
      /** The MIME type of CONTENT_URI providing a directory of media entry. */
      public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.nl.sogeti.android.metadata";
      
      /** The name of this table */
      public static final String TABLE = "metadata";
      static final String CREATE_STATEMENT = "CREATE TABLE " + MetaData.TABLE + 
      "(" + " " + BaseColumns._ID          + " " + MetaDataColumns._ID_TYPE + 
      "," + " " + MetaDataColumns.TRACK    + " " + MetaDataColumns.TRACK_TYPE + 
      "," + " " + MetaDataColumns.SEGMENT  + " " + MetaDataColumns.SEGMENT_TYPE + 
      "," + " " + MetaDataColumns.WAYPOINT + " " + MetaDataColumns.WAYPOINT_TYPE + 
      "," + " " + MetaDataColumns.KEY      + " " + MetaDataColumns.KEY_TYPE + 
      "," + " " + MetaDataColumns.VALUE    + " " + MetaDataColumns.VALUE_TYPE + 
      ");";
     
      public static final Uri CONTENT_URI = Uri.parse( "content://" + Prim.AUTHORITY + "/" + MetaData.TABLE );
   }
   
   /**
    * Columns from the tracks table.
    * 
 
    */
   public static class TracksColumns
   {
      public static final String NAME          = "name";
      public static final String CREATION_TIME = "creationtime";
      static final String CREATION_TIME_TYPE   = "INTEGER NOT NULL";
      static final String NAME_TYPE            = "TEXT";
      static final String _ID_TYPE             = "INTEGER PRIMARY KEY AUTOINCREMENT";
   }
   
  /* columns from the XYZ table*/
   
   public static class XYZColumns
   {
	   public static final String LABEL ="label";
	   public static final String SPEED = "speed";
	   public static final String CREATION_TIME = "creationtime";
	   static final String CREATION_TIME_TYPE = "INTEGER NOT NULL";
	   static final String LABEL_TYPE = "TEXT";
	   static final String _ID_TYPE = "INTEGER PRIMARY KEY AUTOINCREMENT"; 
	   /** The recorded time */
	   public static final String TIME = "time";
	   static final String TIME_TYPE      = "INTEGER NOT NULL";
	   static final String SPEED_TYPE     = "REAL NOT NULL";	

   }   
   
   public static class LocationColumns 
   {
	      /** The latitude */
	      public static final String LATITUDE = "latitude";
	      /** The longitude */
	      public static final String LONGITUDE = "longitude";
	      /** The recorded time */
	      public static final String TIME = "time";
	      /** The speed in meters per second */
	      public static final String SPEED = "speed";
	      	      /** The segment _id to which this segment belongs */
	      public static final String SEGMENT = "tracksegment";
	      /** The accuracy of the fix */
	      public static final String ACCURACY = "accuracy";
	      static final String LATITUDE_TYPE  = "REAL NOT NULL";
	      static final String LONGITUDE_TYPE = "REAL NOT NULL";
	      static final String TIME_TYPE      = "INTEGER NOT NULL";
	      static final String SPEED_TYPE     = "REAL NOT NULL";
	      static final String SEGMENT_TYPE   = "INTEGER NOT NULL";
	      static final String ACCURACY_TYPE  = "REAL";
	      static final String _ID_TYPE       = "INTEGER PRIMARY KEY AUTOINCREMENT";
   }
   
   /**
    * Columns from the segments table.
    * 
    */
   public static class SegmentsColumns
   {
      /** The track _id to which this segment belongs */
      public static final String TRACK = "track";     
      static final String TRACK_TYPE   = "INTEGER NOT NULL";
      static final String _ID_TYPE     = "INTEGER PRIMARY KEY AUTOINCREMENT";
   }

   /**
    * Columns from the waypoints table.

    */
   public static class WaypointsColumns
   {

      /** The latitude */
      public static final String LATITUDE = "latitude";
      /** The longitude */
      public static final String LONGITUDE = "longitude";
      /** The recorded time */
      public static final String TIME = "time";
      /** The speed in meters per second */
      public static final String SPEED = "speed";
      /** The segment _id to which this segment belongs */
      public static final String SEGMENT = "tracksegment";
      /** The accuracy of the fix */
      public static final String ACCURACY = "accuracy";
      /** The altitude */
      public static final String ALTITUDE = "altitude";
      /** the bearing of the fix */
      public static final String BEARING = "bearing";

      static final String LATITUDE_TYPE  = "REAL NOT NULL";
      static final String LONGITUDE_TYPE = "REAL NOT NULL";
      static final String TIME_TYPE      = "INTEGER NOT NULL";
      static final String SPEED_TYPE     = "REAL NOT NULL";
      static final String SEGMENT_TYPE   = "INTEGER NOT NULL";
      static final String ACCURACY_TYPE  = "REAL";
      static final String ALTITUDE_TYPE  = "REAL";
      static final String BEARING_TYPE   = "REAL";
      static final String _ID_TYPE       = "INTEGER PRIMARY KEY AUTOINCREMENT";
   }
   
   /**
    * Columns from the media table.

    */
   public static class MediaColumns
   {
      /** The track _id to which this segment belongs */
      public static final String TRACK    = "track";     
      static final String TRACK_TYPE      = "INTEGER NOT NULL";
      public static final String SEGMENT  = "segment";     
      static final String SEGMENT_TYPE    = "INTEGER NOT NULL";
      public static final String WAYPOINT = "waypoint";     
      static final String WAYPOINT_TYPE   = "INTEGER NOT NULL";
      public static final String URI      = "uri";     
      static final String URI_TYPE        = "TEXT";
      static final String _ID_TYPE        = "INTEGER PRIMARY KEY AUTOINCREMENT";
   }
   
   /**
    * Columns from the media table.
    * 

    */
   public static class MetaDataColumns
   {
      /** The track _id to which this segment belongs */
      public static final String TRACK    = "track";     
      static final String TRACK_TYPE      = "INTEGER NOT NULL";
      public static final String SEGMENT  = "segment";     
      static final String SEGMENT_TYPE    = "INTEGER";
      public static final String WAYPOINT = "waypoint";     
      static final String WAYPOINT_TYPE   = "INTEGER";
      public static final String KEY      = "key";          
      static final String KEY_TYPE        = "TEXT NOT NULL";
      public static final String VALUE    = "value";        
      static final String VALUE_TYPE      = "TEXT NOT NULL";
      static final String _ID_TYPE        = "INTEGER PRIMARY KEY AUTOINCREMENT";
   }
}
