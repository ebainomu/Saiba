package com.prim.db;

import java.util.Date;

import com.prim.db.Prim.Labels;
import com.prim.db.Prim.LabelsColumns;
import com.prim.db.Prim.LocationColumns;
import com.prim.db.Prim.Locations;
import com.prim.db.Prim.MetaData;
import com.prim.db.Prim.Segments;
import com.prim.db.Prim.Xyz;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.net.Uri;
import android.util.Log;

/**
 * Class to hold bare-metal database operations exposed as functionality blocks
 * To be used by database adapters, like a content provider, that implement a
 * required functionality set
 * 
 * To create and upgrade the prim database, I have created a subclass 
 * of the SQLiteOpenHelper class
 * 
 * 
 * The SQLiteOpenHelper class provides the getReadableDatabase() 
 * and getWriteableDatabase() methods to get access to an 
 * SQLiteDatabase object; either in read or write mode.

    The database tables should use the identifier _id for the 
    primary key of the table. Several Android functions rely on this standard.
 * 
 * @version $Id$
 * * @author baalmart

 */
public class DatabaseHelper extends SQLiteOpenHelper
{
   private Context mContext;
   private final static String TAG = "PRIM.DatabaseHelper";

   //a constructor for calling the super class to call the database name and the version
   public DatabaseHelper(Context context)
   {
      super(context, Prim.DATABASE_NAME, null, Prim.DATABASE_VERSION);
      this.mContext = context;
   }

   /*
    * (non-Javadoc)
    * @see
    * android.database.sqlite.SQLiteOpenHelper#onCreate(android.database.sqlite
    * .SQLiteDatabase)
    * 
    * the onCreate is called by the framework if the database but not yet created
    * 
    * SQLiteDatabase is the base class for working with a SQLite database 
    *  and provides methods to open, query, update and close the database.

       More specifically SQLiteDatabase provides the insert(), update() and 
       delete() methods.

       In addition it provides the execSQL() method, which allows to execute an
        SQL statement directly.

       The object ContentValues allows to define key/values. The key represents the table 
       column identifier and the value represents the content for the table record in 
       this column. ContentValues can be used for inserts and updates of database entries.

       Queries can be created via the rawQuery() and query() methods or via 
       the SQLiteQueryBuilder class .

       rawQuery() directly accepts an SQL select statement as input.

       query() provides a structured interface for specifying the SQL query.

       SQLiteQueryBuilder is a convenience class that helps to build SQL queries.
    * 
    * 
    * 
    */
   
   @Override
   public void onCreate(SQLiteDatabase db)
   {
      db.execSQL(Locations.CREATE_STATEMENT);
      db.execSQL(Xyz.CREATE_STATEMENT);
      db.execSQL(Labels.CREATE_STATEMENT);
   }

   /**
    * Will update version 1 through 5 to version 8
    * 
    * @see android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database.sqlite.SQLiteDatabase,
    *      int, int)
    * @see Prim.DATABASE_VERSION
    * 
    * called, if the database version is increased in your application code. 
    * This method allows you to update an existing database schema or to drop the 
    * existing database and recreate it via the onCreate() method    * 
    * 
    */
   @Override
   public void onUpgrade(SQLiteDatabase db, int current, int targetVersion)
   {
      Log.i(TAG, "Upgrading db from " + current + " to " + targetVersion);
      if (current <= 5) // From 1-5 to 6 (these before are the same before) 
      {
         current = 6;
      }
      if (current == 6) // From 6 to 7 ( no changes ) 
      {
         current = 7;
      }
      if (current == 7) // From 7 to 8 ( more locations data ) 
      {
         for (String statement : Locations.UPGRADE_STATEMENT_7_TO_8)
         {
            db.execSQL(statement);
         }
         current = 8;
      }
      
        }

   public void vacuum()
   {
      new Thread()
      {
         @Override
         public void run()
         {
            SQLiteDatabase sqldb = getWritableDatabase();
            sqldb.execSQL("VACUUM");
         }
      }.start();

   }

   
   /*inserting locations continuously
    * The object ContentValues allows to define key/values. 
    * The key represents the table column identifier and the 
    * value represents the content for 
    * the table record in this column. ContentValues can 
    * be used for inserts and updates of database entries. 
    * 
    * */   
    
   
   int bulkInsertLocations(long labelId, long segmentId, ContentValues[] valuesArray)
   {
	   
	   //lets make some rules in our locations tables
      if (labelId < 0)
      {
         throw new IllegalArgumentException("label may not be the less than 0.");
      }
      
      int inserted = 0;
      SQLiteDatabase sqldb = getWritableDatabase();
      sqldb.beginTransaction();
      
      try
      {
         for (ContentValues args : valuesArray)
         {
            args.put(Locations.SEGMENT, segmentId);

            long id = sqldb.insert(Locations.TABLE, null, args);
            if (id >= 0)
            {
               inserted++;																																									
            }
         }
         sqldb.setTransactionSuccessful();

      }
      finally
      {
         if (sqldb.inTransaction())
         {
            sqldb.endTransaction();
         }
      }

      return inserted;
   }
   
   
   /**
    * Creates a location under the current label with the current time
    * on which the location is reached
    * 
    * @param label label
    * @param segment segment
    * @param latitude latitude
    * @param longitude longitude
    * @param time time
    * @param speed the measured speed
    * @return
    */
   long insertLocation(long labelId, long segmentId, Location location)
   {
      if (labelId < 0 || segmentId < 0)
      {
         throw new IllegalArgumentException("label and segments may not be less than 0.");
      }

      SQLiteDatabase sqldb = getWritableDatabase();

      ContentValues args = new ContentValues();
      args.put(LocationColumns.SEGMENT, segmentId);
      args.put(LocationColumns.TIME, location.getTime());
      args.put(LocationColumns.LATITUDE, location.getLatitude());
      args.put(LocationColumns.LONGITUDE, location.getLongitude());
      args.put(LocationColumns.SPEED, location.getSpeed());
      args.put(LocationColumns.ACCURACY, location.getAccuracy());
   

      long locationId = sqldb.insert(Locations.TABLE, null, args);

      ContentResolver resolver = this.mContext.getContentResolver();
      Uri notifyUri = Uri.withAppendedPath(Labels.CONTENT_URI, labelId + "/segments/" + segmentId + "/locations");
      resolver.notifyChange(notifyUri, null);

      //      Log.d( TAG, "Location stored: "+notifyUri);
      return locationId;
   }
   
   
   /**
    * Insert a key/value pair as meta-data for a label and optionally narrow the
    * scope by segment or segment/waypoint
    * 
    * @param labelId
    * @param segmentId
    * @param locationId
    * @param key
    * @param value
    * @return
    */
   long insertOrUpdateMetaData(long labelId, long segmentId, long locationId, String key, String value)
   {
      long metaDataId = -1;
      if (labelId < 0 && key != null && value != null)
      {
         throw new IllegalArgumentException("Track, key and value must be provided");
      }
      if (locationId >= 0 && segmentId < 0)
      {
         throw new IllegalArgumentException("Waypoint must have segment");
      }

      ContentValues args = new ContentValues();
      args.put(MetaData.LABEL, labelId);
      args.put(MetaData.SEGMENT, segmentId);      
      args.put(MetaData.KEY, key);
      args.put(MetaData.VALUE, value);
      String whereClause = MetaData.LABEL + " = ? AND " + MetaData.SEGMENT + " = ? AND " + MetaData.LOCATION + " = ? AND " + MetaData.KEY + " = ?";
      String[] whereArgs = new String[] { Long.toString(labelId), Long.toString(segmentId), Long.toString(locationId), key };

      SQLiteDatabase sqldb = getWritableDatabase();
      int updated = sqldb.update(MetaData.TABLE, args, whereClause, whereArgs);
      if (updated == 0)
      {
         metaDataId = sqldb.insert(MetaData.TABLE, null, args);
      }
      else
      {
         Cursor c = null;
         try
         {
            c = sqldb.query(MetaData.TABLE, new String[] { MetaData._ID }, whereClause, whereArgs, null, null, null);
            if( c.moveToFirst() )
            {
               metaDataId = c.getLong(0);
            }
         }
         finally
         {
            if (c != null)
            {
               c.close();
            }
         }
      }

      ContentResolver resolver = this.mContext.getContentResolver();
      Uri notifyUri;
      if (segmentId >= 0 && locationId >= 0)
      {
         notifyUri = Uri.withAppendedPath(Labels.CONTENT_URI, labelId + 
        		 "/segments/" + segmentId + "/waypoints/" + locationId + "/metadata");
      }
      else if (segmentId >= 0)
      {
         notifyUri = Uri.withAppendedPath(Labels.CONTENT_URI, labelId +
        		 "/segments/" + segmentId + "/metadata");
      }
      else
      {
         notifyUri = Uri.withAppendedPath(Labels.CONTENT_URI, labelId + "/metadata");
      }
      resolver.notifyChange(notifyUri, null);
      resolver.notifyChange(MetaData.CONTENT_URI, null);

      return metaDataId;
   }

  
   /**
    * Deletes a single label and all underlying locations and metadata
    * 
    * @param labelId
    * @return
    */
   int deleteLabel(long labelId)
   {
      SQLiteDatabase sqldb = getWritableDatabase();
      int affected = 0;
      Cursor cursor = null;
      long segmentId = -1;
      long metadataId = -1;

      try
      {
         sqldb.beginTransaction();
         // Iterate on each segement to delete each
         cursor = sqldb.query(Segments.TABLE, new String[] { Segments._ID },
        		 Segments.LABEL + "= ?", new String[] { String.valueOf(labelId) }, null, null,
               null, null);
         if (cursor.moveToFirst())
         {
            do
            {
               segmentId = cursor.getLong(0);
               //affected += deleteSegment(sqldb, labelId, segmentId);
            }
            while (cursor.moveToNext());
         }
         else
         {
            Log.e(TAG, "Did not find the last active segment");
         }
         // Delete the label
         affected += sqldb.delete(Labels.TABLE, Labels._ID + "= ?", new String[] { String.valueOf(labelId) });
         // Delete remaining meta-data
         affected += sqldb.delete(MetaData.TABLE, MetaData.LABEL + "= ?", new String[] { String.valueOf(labelId) });

         cursor = sqldb.query(MetaData.TABLE, new String[] { MetaData._ID }, MetaData.LABEL + "= ?", new String[] { String.valueOf(labelId) }, null, null,
               null, null);
         
         if (cursor.moveToFirst())
         {
            do
            {
               metadataId = cursor.getLong(0);
               affected += deleteMetaData(metadataId);
            }
            while (cursor.moveToNext());
         }

         sqldb.setTransactionSuccessful();
      }
      
      finally
      {
         if (cursor != null)
         {
            cursor.close();
         }
         if (sqldb.inTransaction())
         {
            sqldb.endTransaction();
         }
      }

      ContentResolver resolver = this.mContext.getContentResolver();
      resolver.notifyChange(Labels.CONTENT_URI, null);
      resolver.notifyChange(ContentUris.withAppendedId(Labels.CONTENT_URI, labelId), null);

      return affected;
   }
   
  

   int deleteMetaData(long metadataId)
   {
      SQLiteDatabase sqldb = getWritableDatabase();

      Cursor cursor = null;
      long trackId = -1;
      long segmentId = -1;
      long waypointId = -1;
      try
      {
         cursor = sqldb.query(MetaData.TABLE, new String[] { MetaData.LABEL, MetaData.SEGMENT,
        		 MetaData.LOCATION }, MetaData._ID + "= ?",
               new String[] { String.valueOf(metadataId) }, null, null, null, null);
         if (cursor.moveToFirst())
         {
            trackId = cursor.getLong(0);
            segmentId = cursor.getLong(0);
            waypointId = cursor.getLong(0);
         }
         else
         {
            Log.e(TAG, "Did not find the media element to delete");
         }
      }
      finally
      {
         if (cursor != null)
         {
            cursor.close();
         }
      }

      int affected = sqldb.delete(MetaData.TABLE, MetaData._ID + "= ?", new String[] { String.valueOf(metadataId) });

      ContentResolver resolver = this.mContext.getContentResolver();
      Uri notifyUri;
      if (trackId >= 0 && segmentId >= 0 && waypointId >= 0)
      {
         notifyUri = Uri.withAppendedPath(Labels.CONTENT_URI, trackId + "/segments/" + segmentId + "/waypoints/" + waypointId + "/media");
         resolver.notifyChange(notifyUri, null);
      }
      if (trackId >= 0 && segmentId >= 0)
      {
         notifyUri = Uri.withAppendedPath(Labels.CONTENT_URI, trackId + "/segments/" + segmentId + "/media");
         resolver.notifyChange(notifyUri, null);
      }
      notifyUri = Uri.withAppendedPath(Labels.CONTENT_URI, trackId + "/media");
      resolver.notifyChange(notifyUri, null);
     /* resolver.notifyChange(ContentUris.withAppendedId(Media.CONTENT_URI, metadataId), null);*/

      return affected;
   }

   /**
    * Delete a segment and all member waypoints
    * 
    * @param sqldb The SQLiteDatabase in question
    * @param labelId The label id of this delete
    * @param segmentId The segment that needs deleting
    * @return
    */
   int deleteSegment(SQLiteDatabase sqldb, long labelId, long segmentId)
   {
      int affected = sqldb.delete(Segments.TABLE, Segments._ID + "= ?", new String[] { String.valueOf(segmentId) });

      // Delete all waypoints from segments
      affected += sqldb.delete(Locations.TABLE, Locations.SEGMENT + "= ?", new String[] { String.valueOf(segmentId) });
     
      /* // Delete all media from segment
      affected += sqldb.delete(Media.TABLE, Media.TRACK + "= ? AND " + Media.SEGMENT + "= ?",
            new String[] { String.valueOf(trackId), String.valueOf(segmentId) });*/
      
      
      // Delete meta-data
      affected += sqldb.delete(MetaData.TABLE, MetaData.LABEL + "= ? AND " + MetaData.SEGMENT + "= ?",
            new String[] { String.valueOf(labelId), String.valueOf(segmentId) });

      ContentResolver resolver = this.mContext.getContentResolver();
      resolver.notifyChange(Uri.withAppendedPath(Labels.CONTENT_URI, labelId + "/segments/" + segmentId), null);
      resolver.notifyChange(Uri.withAppendedPath(Labels.CONTENT_URI, labelId + "/segments"), null);

      return affected;
   }

   int updateLabel(long labelId, String name)
   {
      int updates;
      String whereclause = Labels._ID + " = " + labelId;
      ContentValues args = new ContentValues();
      args.put(Labels.NAME, name);

      // Execute the query.
      SQLiteDatabase mDb = getWritableDatabase();
      updates = mDb.update(Labels.TABLE, args, whereclause, null);

      ContentResolver resolver = this.mContext.getContentResolver();
      Uri notifyUri = ContentUris.withAppendedId(Labels.CONTENT_URI, labelId);
      resolver.notifyChange(notifyUri, null);

      return updates;
   }

   /**
    * Insert a key/value pair as meta-data for a label and optionally narrow the
    * scope by segment or segment/waypoint
    * 
    * @param trackId
    * @param segmentId
    * @param locationId
    * @param key
    * @param value
    * @return
    */
   int updateMetaData(long labelId, long segmentId, long locationId, long metadataId, String selection, String[] selectionArgs, String value)
   {
      {
         if ((metadataId < 0 && labelId < 0))
         {
            throw new IllegalArgumentException("Track or meta-data id be provided");
         }
         if (labelId >= 0 && (selection == null || !selection.contains("?") || selectionArgs.length != 1))
         {
            throw new IllegalArgumentException("A where clause selection must be provided to select the correct KEY");
         }
         if (labelId >= 0 && locationId >= 0 && segmentId < 0)
         {
            throw new IllegalArgumentException("Waypoint must have segment");
         }

         SQLiteDatabase sqldb = getWritableDatabase();

         String[] whereParams;
         String whereclause;
         if (metadataId >= 0)
         {
            whereclause = MetaData._ID + " = ? ";
            whereParams = new String[] { Long.toString(metadataId) };
         }
         else
         {
            whereclause = MetaData.LABEL + " = ? AND " + MetaData.SEGMENT + " = ? AND " + MetaData.LOCATION + " = ? AND " + MetaData.KEY + " = ? ";
            whereParams = new String[] { Long.toString(labelId), Long.toString(segmentId), Long.toString(locationId), selectionArgs[0] };
         }
         ContentValues args = new ContentValues();
         args.put(MetaData.VALUE, value);

         int updates = sqldb.update(MetaData.TABLE, args, whereclause, whereParams);

         ContentResolver resolver = this.mContext.getContentResolver();
         Uri notifyUri;
         if (labelId >= 0 && segmentId >= 0 && locationId >= 0)
         {
            notifyUri = Uri.withAppendedPath(Labels.CONTENT_URI, labelId + "/segments/" + segmentId + "/waypoints/" + locationId + "/metadata");
         }
         else if (labelId >= 0 && segmentId >= 0)
         {
            notifyUri = Uri.withAppendedPath(Labels.CONTENT_URI, labelId  + "/segments/" + segmentId + "/metadata");
         }
         else if (labelId  >= 0)
         {
            notifyUri = Uri.withAppendedPath(Labels.CONTENT_URI, labelId  + "/metadata");
         }
         else
         {
            notifyUri = Uri.withAppendedPath(MetaData.CONTENT_URI, "" + metadataId);
         }

         resolver.notifyChange(notifyUri, null);
         resolver.notifyChange(MetaData.CONTENT_URI, null);

         return updates;
      }
   }

   /**
    * Move to a fresh track with a new first segment for this track
    * 
    * @return
    */
   long toNextLabel(String name)
   {
      long currentTime = new Date().getTime();
      ContentValues args = new ContentValues();
      args.put(LabelsColumns.NAME, name);
      args.put(LabelsColumns.DETECTION_TIME, currentTime);

      SQLiteDatabase sqldb = getWritableDatabase();
      long trackId = sqldb.insert(Labels.TABLE, null, args);

      ContentResolver resolver = this.mContext.getContentResolver();
      resolver.notifyChange(Labels.CONTENT_URI, null);

      return trackId;
   }

   /**
    * Moves to a fresh segment to which waypoints can be connected
    * 
    * @return
    */
   long toNextSegment(long trackId)
   {
      SQLiteDatabase sqldb = getWritableDatabase();

      ContentValues args = new ContentValues();
      args.put(Segments.LABEL, trackId);
      long segmentId = sqldb.insert(Segments.TABLE, null, args);

      ContentResolver resolver = this.mContext.getContentResolver();
      resolver.notifyChange(Uri.withAppendedPath(Labels.CONTENT_URI, trackId + "/segments"), null);

      return segmentId;
   }
}
