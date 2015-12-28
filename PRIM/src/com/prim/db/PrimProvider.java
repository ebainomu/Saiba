package com.prim.db;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.prim.db.Prim.Labels;
import com.prim.db.Prim.Locations;
import com.prim.db.Prim.MetaData;
import com.prim.db.Prim.Segments;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.location.Location;
import android.net.Uri;
import android.provider.LiveFolders;
import android.util.Log;

/* @author baalmart*/

/**
 * Goal of this Content Provider is to make the PRIM information uniformly 
 * available to this application and even other applications. The PRIM 
 * database can hold, routes, segments or waypoints or locations. 
 * <p>
 * A route taken from start to finish. All the GPS locations
 * collected are waypoints. Waypoints taken in sequence without loss of GPS-signal
 * are considered connected and are grouped in segments. A route is build up out of
 * 1 or more segments.
 * 
 * xyz are the accelerometer values in each scenario. Locations are the GPS locations collected
 * and taken in sequence without loss of signal are called segments
 * 
 * <p>
 * For example:<br>
 * <code>content://dev.baalmart.prim/tracks</code>
 * is the URI that returns all the stored tracks or starts a new track on insert 
 * <p>
 * <code>content://dev.baalmart.prim/tracks/2</code>
 * is the URI string that would return a single result row, the track with ID = 23. 
 * <p>
 * <code>content://dev.baalmart.prim/tracks/2/segments</code> is the URI that returns 
 * all the stored segments of a track with ID = 2 or starts a new segment on insert 
 * <p>
 * <code>content://dev.baalmart.prim/tracks/2/waypoints</code> is the URI that returns 
 * all the stored waypoints of a track with ID = 2
 * <p>
 * <code>content://dev.baalmart.prim/tracks/2/segments</code> is the URI that returns 
 * all the stored segments of a track with ID = 2 
 * <p>
 * <code>content://dev.baalmart.prim/tracks/2/segments/3</code> is
 * the URI string that would return a single result row, the segment with ID = 3 of a track with ID = 2 . 
 * <p>
 * <code>content://dev.baalmart.prim/tracks/2/segments/1/waypoints</code> is the URI that 
 * returns all the waypoints of a segment 1 of track 2.
 * <p>
 * <code>content://dev.baalmart.prim/tracks/2/segments/1/waypoints/52</code> is the URI string that 
 * would return a single result row, the waypoint with ID = 52
 * <p>
 * Media is stored under a waypoint and may be queried as:<br>
 * <code>content://dev.baalmart.prim/tracks/2/segments/3/waypoints/22/media</code>
 * <p>
 * 
 * 
 * All media for a segment can be queried with:<br>
 * <code>content://dev.baalmart.prim/tracks/2/segments/3/media</code>
 * <p>
 * All media for a track can be queried with:<br>
 * <code>content://dev.baalmart.prim/tracks/2/media</code>
 * 
 * <p>
 * The whole set of collected media may be queried as:<br>
 * <code>content://dev.baalmart.prim/media</code>
 * <p>
 * A single media is stored with an ID, for instance ID = 12:<br>
 * <code>content://dev.baalmart.prim/media/12</code>
 * <p>
 * The whole set of collected media may be queried as:<br>
 * <code>content://dev.baalmart.prim/media</code>
 * <p>
 * 
 * 
 * Meta-data regarding a single waypoint may be queried as:<br>
 * <code>content://dev.baalmart.prim/tracks/2/segments/3/waypoints/22/metadata</code>
 * <p>
 * Meta-data regarding a single segment as whole may be queried as:<br>
 * <code>content://dev.baalmart.prim/tracks/2/segments/3/metadata</code>
 * Note: This does not include meta-data of waypoints.
 * <p>
 * Meta-data regarding a single track as a whole may be queried as:<br>
 * <code>content://dev.baalmart.prim/tracks/2/metadata</code>
 * Note: This does not include meta-data of waypoints or segments.
 * 
 * @author baalmart

 */
public class PrimProvider extends ContentProvider
{

   private static final String TAG = "PRIM.PrimProvider";

   /* Action types as numbers for using the UriMatcher */
   private static final int LABELS            = 1;
   private static final int LABEL_ID          = 2;   
   private static final int TRACK_MEDIA       = 3;
   private static final int LABEL_LOCATIONS   = 4;
   private static final int SEGMENTS          = 5;
   private static final int SEGMENT_ID        = 6;
   private static final int SEGMENT_MEDIA     = 7;
   private static final int SEARCH_SUGGEST_ID = 8;
   private static final int LIVE_FOLDERS      = 9;
   private static final int MEDIA_ID          = 10;   
   private static final int LABEL_METADATA    = 11;
   private static final int SEGMENT_METADATA  = 12;
   private static final int WAYPOINT_METADATA = 13;
   private static final int METADATA          = 14;
   private static final int METADATA_ID       = 15;
   private static final int LOCATIONS         = 16;
   private static final int LOCATION_ID       = 17;
   
   private static final String[] SUGGEST_PROJECTION = 
      new String[] 
        { 
            Labels._ID, 
            Labels.NAME+" AS "+SearchManager.SUGGEST_COLUMN_TEXT_1,
            "datetime("+Labels.DETECTION_TIME+"/1000, 'unixepoch') as "+SearchManager.SUGGEST_COLUMN_TEXT_2,
            Labels._ID+" AS "+SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID
            
        };
   private static final String[] LIVE_PROJECTION = 
      new String[] 
        {
	   Labels._ID+" AS "+LiveFolders._ID,
	   Labels.NAME+" AS "+ LiveFolders.NAME,
            "datetime("+Labels.DETECTION_TIME+"/1000, 'unixepoch') as "+LiveFolders.DESCRIPTION
        };

   private static UriMatcher sURIMatcher = new UriMatcher( UriMatcher.NO_MATCH );

   /**
    * Although it is documented that in addURI(null, path, 0) "path" 
    * should be an absolute path this does not seem to work. 
    * A relative path gets the jobs done and matches an absolute path.
    */
   static
   {
      PrimProvider.sURIMatcher = new UriMatcher( UriMatcher.NO_MATCH );
      PrimProvider.sURIMatcher.addURI( Prim.AUTHORITY, "labels", PrimProvider.LABELS );
      PrimProvider.sURIMatcher.addURI( Prim.AUTHORITY, "labels/#", PrimProvider.LABEL_ID );
      PrimProvider.sURIMatcher.addURI( Prim.AUTHORITY, "labels/#/metadata", PrimProvider.LABEL_METADATA );
      PrimProvider.sURIMatcher.addURI( Prim.AUTHORITY, "labels/#/locations", PrimProvider.LABEL_LOCATIONS );      
      PrimProvider.sURIMatcher.addURI( Prim.AUTHORITY, "labels/#/segments/#/waypoints", PrimProvider.LOCATIONS );
      PrimProvider.sURIMatcher.addURI( Prim.AUTHORITY, "labels/#/segments/#/waypoints/#", PrimProvider.LOCATION_ID );
      PrimProvider.sURIMatcher.addURI( Prim.AUTHORITY, "metadata", PrimProvider.METADATA );
      PrimProvider.sURIMatcher.addURI( Prim.AUTHORITY, "metadata/#", PrimProvider.METADATA_ID );
      
      PrimProvider.sURIMatcher.addURI( Prim.AUTHORITY, "live_folders/labels", PrimProvider.LIVE_FOLDERS );
      PrimProvider.sURIMatcher.addURI( Prim.AUTHORITY, "search_suggest_query", PrimProvider.SEARCH_SUGGEST_ID );

   }

   private DatabaseHelper mDbHelper;

   /**
    * (non-Javadoc)
    * @see android.content.ContentProvider#onCreate()
    */
   @Override
   public boolean onCreate()
   {   
      if (this.mDbHelper == null)
      {
         this.mDbHelper = new DatabaseHelper( getContext() );
      }
      return true;
   }

   /**
    * (non-Javadoc)
    * @see android.content.ContentProvider#getType(android.net.Uri)
    */
   @Override
   public String getType( Uri uri )
   {
      int match = PrimProvider.sURIMatcher.match( uri );
      String mime = null;
      switch (match)
      {
         case LABELS:
            mime = Labels.CONTENT_TYPE;
            break;
         case LABEL_ID:
            mime = Labels.CONTENT_ITEM_TYPE;
            break;
         case LOCATIONS:
            mime = Locations.CONTENT_TYPE;
            break;
         case LOCATION_ID:
            mime = Locations.CONTENT_ITEM_TYPE;
            break;       
         case LABEL_METADATA:      
            mime = MetaData.CONTENT_ITEM_TYPE;
            break;
         case UriMatcher.NO_MATCH:
         default:
            Log.w(TAG, "There is not MIME type defined for URI "+uri);
            break;
      }
      return mime;
   }

   /**
       * (non-Javadoc)
       * @see android.content.ContentProvider#insert(android.net.Uri, android.content.ContentValues)
       */
      @Override
      public Uri insert( Uri uri, ContentValues values )
      {
         Log.d( TAG, "insert on " +uri );
         Uri insertedUri = null;
         int match = PrimProvider.sURIMatcher.match( uri );
         List<String> pathSegments = null;
         long labelId = -1;        
         long locationId = -1;         
         switch (match)
         {
            case LOCATIONS:
               pathSegments     = uri.getPathSegments();
               labelId          = Long.parseLong( pathSegments.get( 1 ) );
               Location loc     = new Location( TAG );
               Double latitude  = values.getAsDouble( Locations.LATITUDE );
               Double longitude = values.getAsDouble( Locations.LONGITUDE );
               Long time        = values.getAsLong( Locations.TIME );
               Float speed      = values.getAsFloat( Locations.SPEED );
               if( time == null )
               {
                  time = System.currentTimeMillis();
               }
               if( speed == null )
               {
                  speed  = 0f;
               }
               loc.setLatitude( latitude );
               loc.setLongitude( longitude );
               loc.setTime( time );
               loc.setSpeed( speed );
               
               if( values.containsKey( Locations.ACCURACY ) )
               {
                  loc.setAccuracy( values.getAsFloat( Locations.ACCURACY ) );
               }
              
               locationId = this.mDbHelper.insertLocation(labelId,loc );
               
   //            Log.d( TAG, "Have inserted to segment "+segmentId+" with waypoint "+waypointId );
               insertedUri = ContentUris.withAppendedId( uri, locationId );
               break;
            case LABELS:
               String name = ( values == null ) ? "" : values.getAsString( Labels.NAME );
               labelId     = this.mDbHelper.toNextLabel( name );
               insertedUri = ContentUris.withAppendedId( uri, labelId );     
               
               break;
           
            default:
               Log.e( PrimProvider.TAG, "Unable to match the insert URI: " + uri.toString() );
               insertedUri =  null;
               break;
         }
         return insertedUri;
      }

   /**
       * (non-Javadoc)
       * @see android.content.ContentProvider#query(android.net.Uri, java.lang.String[], java.lang.String, java.lang.String[], java.lang.String)
       */
      @Override
      public Cursor query( Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder )
      {
   //      Log.d( TAG, "Query on Uri:"+uri ); 
        
         int match = PrimProvider.sURIMatcher.match( uri );
   
         String tableName = null;
         String innerSelection = "1";
         String[] innerSelectionArgs = new String[]{};
         String sortorder = sortOrder;
         List<String> pathSegments = uri.getPathSegments();
         switch (match)
         {
            case LABELS:
               tableName = Labels.TABLE;
               break;
            case LABEL_ID:
               tableName = Labels.TABLE;
               innerSelection = Labels._ID + " = ? ";
               innerSelectionArgs = new String[]{ pathSegments.get( 1 ) };
               break;
            case SEGMENTS:
               tableName = Segments.TABLE;
               innerSelection = Segments.LABEL + " = ? ";
               innerSelectionArgs = new String[]{ pathSegments.get( 1 ) };
               break;
            case SEGMENT_ID:
               tableName = Segments.TABLE;
               innerSelection = Segments.LABEL + " = ?  and " + Segments._ID   + " = ? ";
               innerSelectionArgs = new String[]{ pathSegments.get( 1 ), pathSegments.get( 3 ) };
               break;
            case LOCATIONS:
               tableName = Locations.TABLE;
               innerSelection = Locations.SEGMENT + " = ? ";
               innerSelectionArgs = new String[]{ pathSegments.get( 3 ) };
               break;
            case LOCATION_ID:
               tableName = Locations.TABLE;
               innerSelection = Locations.SEGMENT + " =  ?  and " + Locations._ID     + " = ? ";
               innerSelectionArgs = new String[]{ pathSegments.get( 3 ),  pathSegments.get( 5 ) };
               break;
            case LABEL_LOCATIONS:
               tableName = Locations.TABLE + " INNER JOIN " + Segments.TABLE + " ON "+ Segments.TABLE+"."+Segments._ID +"=="+ Locations.SEGMENT;
               innerSelection = Segments.LABEL + " = ? ";
               innerSelectionArgs = new String[]{  pathSegments.get( 1 ) };
               break;
           
            case LABEL_METADATA:
               tableName = MetaData.TABLE;
               innerSelection = MetaData.LABEL + " = ? and " + MetaData.SEGMENT  + " = ? and " + MetaData.LOCATION + " = ? ";
               innerSelectionArgs = new String[]{  pathSegments.get( 1 ), "-1", "-1" };
               break;
            case SEGMENT_METADATA:
               tableName = MetaData.TABLE;
               innerSelection = MetaData.LABEL  + " = ? and " + MetaData.SEGMENT  + " = ? and " + MetaData.LOCATION + " = ? ";
               innerSelectionArgs = new String[]{pathSegments.get( 1 ), pathSegments.get( 3 ),  "-1" };
               break;
            case WAYPOINT_METADATA:
               tableName = MetaData.TABLE;
               innerSelection = MetaData.LABEL  + " = ? and " + MetaData.SEGMENT  + " = ? and " + 
               MetaData.LOCATION + " = ? ";
               innerSelectionArgs = new String[]{ pathSegments.get( 1 ), pathSegments.get( 3 ),  pathSegments.get( 5 ) };
               break;
            case PrimProvider.METADATA:
               tableName = MetaData.TABLE;
               break;
            case PrimProvider.METADATA_ID:
               tableName = MetaData.TABLE;
               innerSelection = MetaData._ID + " = ? ";
               innerSelectionArgs = new String[]{ pathSegments.get( 1 ) };
               break;
            case SEARCH_SUGGEST_ID:
               tableName = Labels.TABLE;
               if( selectionArgs[0] == null || selectionArgs[0].equals( "" ) )
               {
                  selection = null;
                  selectionArgs = null;
                  sortorder = Labels.DETECTION_TIME+" desc";
               }
               else
               {
                  selectionArgs[0] = "%" +selectionArgs[0]+ "%";
               }
               projection = SUGGEST_PROJECTION;
               break;
            case LIVE_FOLDERS:
               tableName = Labels.TABLE;
               projection = LIVE_PROJECTION;
               sortorder = Labels.DETECTION_TIME+" desc";
               break;
            default:
               Log.e( PrimProvider.TAG, "Unable to come to an action in the query uri: " + uri.toString() );
               return null;
         }
   
         // SQLiteQueryBuilder is a helper class that creates the
         // proper SQL syntax for us.
         SQLiteQueryBuilder qBuilder = new SQLiteQueryBuilder();
   
         // Set the table we're querying.
         qBuilder.setTables( tableName );
         
         if( selection == null )
         {
            selection = innerSelection;
         }
         else
         {
            selection = "( "+ innerSelection + " ) and " + selection;
         }
         LinkedList<String> allArgs = new LinkedList<String>();
         if( selectionArgs == null )
         {
            allArgs.addAll(Arrays.asList(innerSelectionArgs));
         }
         else
         {
            allArgs.addAll(Arrays.asList(innerSelectionArgs));
            allArgs.addAll(Arrays.asList(selectionArgs));
         }
         selectionArgs = allArgs.toArray(innerSelectionArgs);
         
         // Make the query.
         SQLiteDatabase mDb = this.mDbHelper.getWritableDatabase();
         Cursor c = qBuilder.query( mDb, projection, selection, selectionArgs, null, null, sortorder  );
         c.setNotificationUri( getContext().getContentResolver(), uri );
         return c;
      }

   /**
    * (non-Javadoc)
    * @see android.content.ContentProvider#update(android.net.Uri, android.content.ContentValues, java.lang.String, java.lang.String[])
    */
   @Override
   public int update( Uri uri, ContentValues givenValues, String selection, String[] selectionArgs )
   {
      int updates = -1 ;
      long labelId;
      long segmentId;
      long waypointId;
      long metaDataId;
      List<String> pathSegments;

      int match = PrimProvider.sURIMatcher.match( uri );
      String value;
      switch (match)
      {
         case LABEL_ID:
            labelId = new Long( uri.getLastPathSegment() ).longValue();
            String name = givenValues.getAsString( Labels.NAME );
            updates = mDbHelper.updateLabel(labelId, name);   
            break;
         case LABEL_METADATA:
            pathSegments = uri.getPathSegments();
            labelId      = Long.parseLong( pathSegments.get( 1 ) );
            value = givenValues.getAsString( MetaData.VALUE );
            updates = mDbHelper.updateMetaData( labelId, -1L, -1L, -1L, selection, selectionArgs, value);
            break;
         case SEGMENT_METADATA:
            pathSegments = uri.getPathSegments();
            labelId      = Long.parseLong( pathSegments.get( 1 ) );
            segmentId    = Long.parseLong( pathSegments.get( 3 ) );
            value = givenValues.getAsString( MetaData.VALUE );
            updates = mDbHelper.updateMetaData( labelId, segmentId, -1L, -1L, selection, selectionArgs, value);
            break;
         case WAYPOINT_METADATA:
            pathSegments = uri.getPathSegments();
            labelId      = Long.parseLong( pathSegments.get( 1 ) );
            segmentId    = Long.parseLong( pathSegments.get( 3 ) );
            waypointId   = Long.parseLong( pathSegments.get( 5 ) );
            value = givenValues.getAsString( MetaData.VALUE );
            updates = mDbHelper.updateMetaData( labelId, segmentId, waypointId, -1L, selection, selectionArgs, value);
            break;
         case METADATA_ID:
            pathSegments = uri.getPathSegments();
            metaDataId   = Long.parseLong( pathSegments.get( 1 ) );
            value = givenValues.getAsString( MetaData.VALUE );
            updates = mDbHelper.updateMetaData( -1L, -1L, -1L, metaDataId, selection, selectionArgs, value);
            break;
         default:
            Log.e( PrimProvider.TAG, "Unable to come to an action in the query uri" + uri.toString() );
            return -1;
      }
      
      return updates;
   }

   /**
    * (non-Javadoc)
    * @see android.content.ContentProvider#delete(android.net.Uri, java.lang.String, java.lang.String[])
    */
   @Override
   public int delete( Uri uri, String selection, String[] selectionArgs )
   {
      int match = PrimProvider.sURIMatcher.match( uri );
      int affected = 0; 
      switch( match )
      {
         case PrimProvider.LABEL_ID:
            affected = this.mDbHelper.deleteLabel( new Long( uri.getLastPathSegment() ).longValue() );
            break;
       /*  case PrimProvider.MEDIA_ID:
            affected = this.mDbHelper.deleteMedia( new Long( uri.getLastPathSegment() ).longValue() );
            break;*/
         case PrimProvider.METADATA_ID:
            affected = this.mDbHelper.deleteMetaData( new Long( uri.getLastPathSegment() ).longValue() );
            break;
         default:
            affected = 0;
            break;   
      }
      return affected;
   }

   @Override
   public int bulkInsert( Uri uri, ContentValues[] valuesArray )
   {
      int inserted = 0;
      int match = PrimProvider.sURIMatcher.match( uri );
      switch (match)
      {
         case LOCATIONS:
            List<String> pathSegments = uri.getPathSegments();
            int labelId = Integer.parseInt( pathSegments.get( 1 ) );
            int segmentId = Integer.parseInt( pathSegments.get( 3 ) );
            inserted = this.mDbHelper.bulkInsertLocations( labelId, segmentId, valuesArray );
            break;
         default:
            inserted = super.bulkInsert( uri, valuesArray );
            break;
      }
      return inserted;
   }

}
