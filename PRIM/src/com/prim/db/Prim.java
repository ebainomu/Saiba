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
   /** The authority of this provider: dev.baalmart.prim */
   public static final String AUTHORITY = "dev.baalmart.prim";
   /** The content:// style Uri for this provider, content://dev.baalmart.prim */
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
      
   public static final class Labels extends LabelsColumns implements android.provider.BaseColumns
   
   {
      /** The MIME type of a CONTENT_URI subdirectory of a single label. */
      public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.prim.label";
      /** The MIME type of CONTENT_URI providing a directory of tracks. */
      public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.prim.label";
      /** The content:// style URL for this provider, content://dev.baalmart.prim/labels */
      public static final Uri CONTENT_URI = Uri.parse( "content://" + Prim.AUTHORITY + "/" + Labels.TABLE );

      /** The name of this table */
      public static final String TABLE = "labels";
      static final String CREATE_STATEMENT = 
         "CREATE TABLE " + Labels.TABLE + "(" + " " + Labels._ID           + " " + Labels._ID_TYPE + 
                                          "," + " " + Labels.NAME          + " " + Labels.NAME_TYPE + 
                                          "," + " " + Labels.DETECTION_TIME + " " + Labels.DETECTION_TIME_TYPE + 
                                          ");";
   }  
   
   
   public static final class Xyz extends XYZColumns implements android.provider.BaseColumns
   {
      /** The MIME type of a CONTENT_URI subdirectory of a single track. */
      public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.prim.xyz";
      /** The MIME type of CONTENT_URI providing a directory of tracks. */
      public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.prim.xyz";
      /** The content:// style URL for this provider, content://com.prim/xyz */
      public static final Uri CONTENT_URI = Uri.parse( "content://" + Prim.AUTHORITY + "/" + Xyz.TABLE );

      /** The name of this table */
      public static final String TABLE = "xyz";
      static final String CREATE_STATEMENT = 
         "CREATE TABLE " + Xyz.TABLE + "(" + " " + Xyz._ID           + " " + Xyz._ID_TYPE + 
                                          "," + " " + Xyz.LABEL          + " " + Xyz.LABEL_TYPE + 
                                          "," + " " + Xyz.CREATION_TIME + " " + Xyz.CREATION_TIME_TYPE + 
                                          "," + " " + Xyz.TIME + " "  + Xyz.TIME_TYPE + 
                                          "," + " " + Xyz.SPEED + " " + Xyz.SPEED_TYPE +
                                          "," + " " + Xyz.X          + " " + Xyz.X_TYPE +
                                          "," + " " + Xyz.Y          + " " + Xyz.Y_TYPE +
                                          "," + " " + Xyz.Z          + " " + Xyz.Z_TYPE +
                                          ");";
   }
   

  /**
   *  the location parameters*/
   
   public static final class Locations extends LocationColumns implements android.provider.BaseColumns
   {

      /** The MIME type of a CONTENT_URI subdirectory of a single waypoint. */
      public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.prim.locations";
      /** The MIME type of CONTENT_URI providing a directory of waypoints. */
      public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.prim.locations";
      
      /** The name of this table, Location */
      public static final String TABLE = "locations";
      static final String CREATE_STATEMENT = "CREATE TABLE " + Locations.TABLE + 
      "(" + " " + BaseColumns._ID + " " + Locations._ID_TYPE + 
      "," + " " + Locations.LATITUDE  + " " + Locations.LATITUDE_TYPE + 
      "," + " " +Locations.LONGITUDE + " " + Locations.LONGITUDE_TYPE + 
      "," + " " + Locations.TIME      + " " + Locations.TIME_TYPE + 
      "," + " " + Locations.SPEED     + " " + Locations.SPEED + 
      "," + " " + Locations.ACCURACY  + " " + Locations.ACCURACY_TYPE + 
      "," + " " + Locations.LABEL  + " " + Locations.LABEL_TYPE +   
      "," + " " + Locations.SEGMENT   + " " + Locations.SEGMENT_TYPE + 
      ");";
      
      //making alterations......
      static final String[] UPGRADE_STATEMENT_7_TO_8 = 
         {
            "ALTER TABLE " + Locations.TABLE + " ADD COLUMN " + LocationColumns.ACCURACY + " " + LocationColumns.ACCURACY_TYPE +";",
         };

      /**
       * Build a location Uri like:
       * 
       * @param xyzId
       * @param segmentId
       * @param locationId
       * 
       * @return
       */
      
      public static Uri buildUri(long labelId, long locationId)
      {
         Builder builder = Labels.CONTENT_URI.buildUpon();
         ContentUris.appendId(builder, labelId);
         builder.appendPath(Locations.TABLE);
         ContentUris.appendId(builder, locationId);
         
         return builder.build();
      }
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
                                            "," + " " + Segments.LABEL + " " + Segments.LABEL_TYPE + 
                                            ");";
   }
   
   /**
    * This table contains media URI's.
    * 

    */
   public static final class MetaData extends MetaDataColumns implements android.provider.BaseColumns
   {

      /** The MIME type of a CONTENT_URI subdirectory of a single metadata entry. */
      public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.prim.metadata";
      /** The MIME type of CONTENT_URI providing a directory of media entry. */
      public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.prim.metadata";
      
      /** The name of this table */
      public static final String TABLE = "metadata";
      static final String CREATE_STATEMENT = "CREATE TABLE " + MetaData.TABLE + 
      "(" + " " + BaseColumns._ID          + " " + MetaDataColumns._ID_TYPE + 
      "," + " " + MetaDataColumns.LABEL    + " " + MetaDataColumns.LABEL_TYPE + 
      "," + " " + MetaDataColumns.SEGMENT  + " " + MetaDataColumns.SEGMENT_TYPE + 
      "," + " " + MetaDataColumns.KEY      + " " + MetaDataColumns.KEY_TYPE + 
      "," + " " + MetaDataColumns.VALUE    + " " + MetaDataColumns.VALUE_TYPE +
      "," + " " + MetaDataColumns.LOCATION    + " " + MetaDataColumns.LOCATION_TYPE +
      ");";
     
      public static final Uri CONTENT_URI = Uri.parse( "content://" + Prim.AUTHORITY + "/" + MetaData.TABLE );
   }
   
   
   
   /*********************************************************
    * the section for columns is beneath here
    * *******************************************************/  
   
   
      /**
    * Columns from the labels table.
    *  
    */
   
   public static class LabelsColumns
   {
      public static final String NAME          = "name";
      public static final String DETECTION_TIME = "creationtime";
      static final String DETECTION_TIME_TYPE   = "INTEGER NOT NULL";
      static final String NAME_TYPE            = "TEXT";
      static final String _ID_TYPE             = "INTEGER PRIMARY KEY AUTOINCREMENT";
   }
   
   
  /** 
   * columns from the XYZ table as in the accelerometer values
   * */
   
   public static class XYZColumns
   {
	   public static final String LABEL ="label";
	   public static final String SPEED = "speed";
	   public static final String X ="x";
	   public static final String Y ="y";
	   public static final String Z ="z";
	   public static final String CREATION_TIME = "creationtime";
	   static final String CREATION_TIME_TYPE = "INTEGER NOT NULL";
	   static final String LABEL_TYPE = "TEXT";
	   static final String _ID_TYPE = "INTEGER PRIMARY KEY AUTOINCREMENT"; 
	   /** The recorded time */
	   public static final String TIME = "time";
	   static final String TIME_TYPE      = "INTEGER NOT NULL";
	   static final String SPEED_TYPE     = "REAL NOT NULL";
	   static final String X_TYPE = "REAL NOT NULL";
	   static final String Y_TYPE = "REAL NOT NULL";
	   static final String Z_TYPE = "REAL NOT NULL";
   }   
   
   /**
    * Columns from the Location table.
    * 
    */
      
   public static class LocationColumns 
   {
	      /** The latitude */
	      public static final String LATITUDE = "latitude";
	      /** The longitude */
	      public static final String LONGITUDE = "longitude";
	      /** The recorded time */
	      public static final String TIME = "time";	      
	      public static final String LABEL ="label";
	      /** The speed in meters per second */
	      public static final String SPEED = "speed";
	     	      /** The accuracy of the fix */
	      public static final String ACCURACY = "accuracy";
	      static final String LATITUDE_TYPE  = "REAL NOT NULL";
	      static final String LONGITUDE_TYPE = "REAL NOT NULL";
	      static final String TIME_TYPE      = "INTEGER NOT NULL";
	      static final String SPEED_TYPE     = "REAL NOT NULL";
	      static final String SEGMENT_TYPE   = "INTEGER NOT NULL";
	      /** The segment _id to which this segment belongs */
	      public static final String SEGMENT = "labelssegment";	      
	      static final String ACCURACY_TYPE  = "REAL";
	      static final String _ID_TYPE       = "INTEGER PRIMARY KEY AUTOINCREMENT";
	      static final String LABEL_TYPE = "TEXT";
   }   
   
   /**
    * Columns from the segments table.
    * 
    */
   public static class SegmentsColumns
   {
      /** The track _id to which this segment belongs */
      public static final String LABEL = "label";     
      static final String LABEL_TYPE   = "INTEGER NOT NULL";
      static final String _ID_TYPE     = "INTEGER PRIMARY KEY AUTOINCREMENT";
   }
   
   /**
    * Columns from the metadata table.
    * 
    */
   public static class MetaDataColumns
   {
      /** The track _id to which this segment belongs */
      public static final String LABEL    = "label";     
      static final String  LABEL_TYPE      = "INTEGER NOT NULL";
      public static final String SEGMENT  = "segment";     
      static final String SEGMENT_TYPE    = "INTEGER";
      public static final String KEY      = "key";          
      static final String KEY_TYPE        = "TEXT NOT NULL";
      public static final String VALUE    = "value";        
      static final String VALUE_TYPE      = "TEXT NOT NULL";
      static final String _ID_TYPE        = "INTEGER PRIMARY KEY AUTOINCREMENT";
      public static final String LOCATION = "location";
      static final String LOCATION_TYPE = "INTEGER";
   }
   
   
}
