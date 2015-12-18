package com.prim.actions.tasks;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.prim.actions.utils.ProgressListener;
import com.prim.db.Prim;
import com.prim.db.Prim.Labels;
import com.prim.db.Prim.Locations;
import com.prim.db.Prim.Segments;
import com.prim.utils.Constants;

import dev.baalmart.prim.R;

import org.xmlpull.v1.XmlSerializer;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.util.Xml;

public class GpxCreator extends XmlCreator
{
   public static final String NS_SCHEMA = "http://www.w3.org/2001/XMLSchema-instance";
   public static final String NS_GPX_11 = "http://www.topografix.com/GPX/1/1";
   public static final String NS_GPX_10 = "http://www.topografix.com/GPX/1/0";
   public static final String NS_OGT_10 = "http://com.prim/GPX/1/0";
   public static final SimpleDateFormat ZULU_DATE_FORMATER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
   static
   {
      TimeZone utc = TimeZone.getTimeZone("UTC");
      ZULU_DATE_FORMATER.setTimeZone(utc); // ZULU_DATE_FORMAT format ends with Z for UTC so make that true
   }

   private String TAG = "PRIM.GpxCreator";
   private boolean includeAttachments;
   protected String mName;

   public GpxCreator(Context context, Uri trackUri, String chosenBaseFileName, boolean attachments, ProgressListener listener)
   {
      super(context, trackUri, chosenBaseFileName, listener);
      includeAttachments = attachments;
   }

   @Override
   protected Uri doInBackground(Void... params)
   {
      determineProgressGoal();

      Uri resultFilename = exportGpx();
      return resultFilename;
   }

   protected Uri exportGpx()
   {

      String xmlFilePath;
      if (mFileName.endsWith(".gpx") || mFileName.endsWith(".xml"))
      {
         setExportDirectoryPath(Constants.getSdCardDirectory(mContext) + mFileName.substring(0, mFileName.length() - 4));

         xmlFilePath = getExportDirectoryPath() + "/" + mFileName;
      }
      else
      {
         setExportDirectoryPath(Constants.getSdCardDirectory(mContext) + mFileName);
         xmlFilePath = getExportDirectoryPath() + "/" + mFileName + ".gpx";
      }

      new File(getExportDirectoryPath()).mkdirs();

      String resultFilename = null;
      FileOutputStream fos = null;
      BufferedOutputStream buf = null;
      try
      {
         verifySdCardAvailibility();

         XmlSerializer serializer = Xml.newSerializer();
         File xmlFile = new File(xmlFilePath);
         fos = new FileOutputStream(xmlFile);
         buf = new BufferedOutputStream(fos, 8 * 8192);
         serializer.setOutput(buf, "UTF-8");

         serializeLabel(mLabelUri, serializer);
         buf.close();
         buf = null;
         fos.close();
         fos = null;

         if (needsBundling())
         {
            resultFilename = bundlingMediaAndXml(xmlFile.getParentFile().getName(), ".zip");
         }
         else
         {
            File finalFile = new File(Constants.getSdCardDirectory(mContext) + xmlFile.getName());
            xmlFile.renameTo(finalFile);
            resultFilename = finalFile.getAbsolutePath();

            XmlCreator.deleteRecursive(xmlFile.getParentFile());
         }

         mFileName = new File(resultFilename).getName();
      }
      catch (FileNotFoundException e)
      {
         String text = mContext.getString(R.string.ticker_failed) + " \"" + xmlFilePath + "\" " + mContext.getString(R.string.error_filenotfound);
         handleError(mContext.getString(R.string.taskerror_gpx_write), e, text);
      }
      catch (IllegalArgumentException e)
      {
         String text = mContext.getString(R.string.ticker_failed) + " \"" + xmlFilePath + "\" " + mContext.getString(R.string.error_filename);
         handleError(mContext.getString(R.string.taskerror_gpx_write), e, text);
      }
      catch (IllegalStateException e)
      {
         String text = mContext.getString(R.string.ticker_failed) + " \"" + xmlFilePath + "\" " + mContext.getString(R.string.error_buildxml);
         handleError(mContext.getString(R.string.taskerror_gpx_write), e, text);
      }
      catch (IOException e)
      {
         String text = mContext.getString(R.string.ticker_failed) + " \"" + xmlFilePath + "\" " + mContext.getString(R.string.error_writesdcard);
         handleError(mContext.getString(R.string.taskerror_gpx_write), e, text);
      }
      finally
      {
         if (buf != null)
         {
            try
            {
               buf.close();
            }
            catch (IOException e)
            {
               Log.e(TAG, "Failed to close buf after completion, ignoring.", e);
            }
         }
         if (fos != null)
         {
            try
            {
               fos.close();
            }
            catch (IOException e)
            {
               Log.e(TAG, "Failed to close fos after completion, ignoring.", e);
            }
         }
      }
      return Uri.fromFile(new File(resultFilename));
   }

   private void serializeLabel(Uri labelUri, XmlSerializer serializer) throws IllegalArgumentException, IllegalStateException, IOException
   {
      if (isCancelled())
      {
         throw new IOException("Fail to execute request due to canceling");
      }
      serializer.startDocument("UTF-8", true);
      serializer.setPrefix("xsi", NS_SCHEMA);
      serializer.setPrefix("gpx10", NS_GPX_10);
      serializer.setPrefix("ogt10", NS_OGT_10);
      serializer.text("\n");
      
      serializer.startTag("", "gpx");
      serializer.attribute(null, "version", "1.1");
      serializer.attribute(null, "creator", "com.prim");
      serializer.attribute(NS_SCHEMA, "schemaLocation", NS_GPX_11 + " http://www.topografix.com/gpx/1/1/gpx.xsd");
      serializer.attribute(null, "xmlns", NS_GPX_11);

      // <metadata/> Big header of the track
      serializeLabelHeader(mContext, serializer, labelUri);

      // <wpt/> [0...] Waypoints 
      if (includeAttachments)
      {
         //serializeWaypoints(mContext, serializer, Uri.withAppendedPath(trackUri, "/media"));
      }

      // <trk/> [0...] Track 
      serializer.text("\n");
      serializer.startTag("", "trk");
      serializer.text("\n");
      serializer.startTag("", "name");
      serializer.text(mName);
      serializer.endTag("", "name");
      // The list of segments in the track
      serializeSegments(serializer, Uri.withAppendedPath(labelUri, "segments"));
      serializer.text("\n");
      serializer.endTag("", "trk");

      serializer.text("\n");
      serializer.endTag("", "gpx");
      serializer.endDocument();
   }

   private void serializeLabelHeader(Context context, XmlSerializer serializer, Uri trackUri) throws IOException
   {
      if (isCancelled())
      {
         throw new IOException("Fail to execute request due to canceling");
      }
      ContentResolver resolver = context.getContentResolver();
      Cursor labelCursor = null;

      String databaseName = null;
      try
      {
         labelCursor = resolver.query(trackUri, new String[] { Labels._ID, Labels.NAME, Labels.DETECTION_TIME }, null, null, null);
         if (labelCursor.moveToFirst())
         {
            databaseName = labelCursor.getString(1);
            serializer.text("\n");
            serializer.startTag("", "metadata");
            serializer.text("\n");
            serializer.startTag("", "time");
            Date time = new Date(labelCursor.getLong(2));
            synchronized (ZULU_DATE_FORMATER)
            {
               serializer.text(ZULU_DATE_FORMATER.format(time));
            }
            serializer.endTag("", "time");
            serializer.text("\n");
            serializer.endTag("", "metadata");
         }
      }
      finally
      {
         if (labelCursor != null)
         {
            labelCursor.close();
         }
      }
      if (mName == null)
      {
         mName = "Untitled";
      }
      if (databaseName != null && !databaseName.equals(""))
      {
         mName = databaseName;
      }
      if (mChosenName != null && !mChosenName.equals(""))
      {
         mName = mChosenName;
      }
   }

   private void serializeSegments(XmlSerializer serializer, Uri segments) throws IOException
   {
      if (isCancelled())
      {
         throw new IOException("Fail to execute request due to canceling");
      }
      Cursor segmentCursor = null;
      ContentResolver resolver = mContext.getContentResolver();
      try
      {
         segmentCursor = resolver.query(segments, new String[] { Segments._ID }, null, null, null);
         if (segmentCursor.moveToFirst())
         {
            do
            {
               Uri waypoints = Uri.withAppendedPath(segments, segmentCursor.getLong(0) + "/waypoints");
               serializer.text("\n");
               serializer.startTag("", "trkseg");
               serializeLabelPoints(serializer, waypoints);
               serializer.text("\n");
               serializer.endTag("", "trkseg");
            }
            while (segmentCursor.moveToNext());
         }
      }
      finally
      {
         if (segmentCursor != null)
         {
            segmentCursor.close();
         }
      }
   }

   private void serializeLabelPoints(XmlSerializer serializer, Uri locations) throws IOException
   {
      if (isCancelled())
      {
         throw new IOException("Fail to execute request due to canceling");
      }
      Cursor locationsCursor = null;
      ContentResolver resolver = mContext.getContentResolver();
      try
      {
         locationsCursor = resolver.query(locations, new String[] { Locations.LONGITUDE, Locations.LATITUDE,Locations.TIME, Locations._ID, Locations.SPEED, Locations.ACCURACY,
        		}, null, null, null);
         if (locationsCursor.moveToFirst())
         {
            do
            {
               mProgressAdmin.addWaypointProgress(1);

               serializer.text("\n");
               serializer.startTag("", "trkpt");
               serializer.attribute(null, "lat", Double.toString(locationsCursor.getDouble(1)));
               serializer.attribute(null, "lon", Double.toString(locationsCursor.getDouble(0)));
               serializer.text("\n");
               serializer.startTag("", "ele");
               serializer.text(Double.toString(locationsCursor.getDouble(3)));
               serializer.endTag("", "ele");
               serializer.text("\n");
               serializer.startTag("", "time");
               Date time = new Date(locationsCursor.getLong(2));
               synchronized (ZULU_DATE_FORMATER)
               {
                  serializer.text(ZULU_DATE_FORMATER.format(time));
               }
               serializer.endTag("", "time");
               serializer.text("\n");
               serializer.startTag("", "extensions");

               double speed = locationsCursor.getDouble(5);
               double accuracy = locationsCursor.getDouble(6);
               double bearing = locationsCursor.getDouble(7);
               if (speed > 0.0)
               {
                  quickTag(serializer, NS_GPX_10, "speed", Double.toString(speed));
               }
               if (accuracy > 0.0)
               {
                  quickTag(serializer, NS_OGT_10, "accuracy", Double.toString(accuracy));
               }
               if (bearing != 0.0)
               {
                  quickTag(serializer, NS_GPX_10, "course", Double.toString(bearing));
               }
               serializer.endTag("", "extensions");
               serializer.text("\n");
               serializer.endTag("", "trkpt");
            }
            while (locationsCursor.moveToNext());
         }
      }
      finally
      {
         if (locationsCursor != null)
         {
            locationsCursor.close();
         }
      }

   }

  /* private void serializeWaypoints(Context context, XmlSerializer serializer, Uri media) throws IOException
   {
      if (isCancelled())
      {
         throw new IOException("Fail to execute request due to canceling");
      }
      Cursor mediaCursor = null;
      Cursor waypointCursor = null;
      BufferedReader buf = null;
      ContentResolver resolver = context.getContentResolver();
      try
      {
         mediaCursor = resolver.query(media, new String[] { Media.URI, Media.TRACK, Media.SEGMENT, Media.WAYPOINT }, null, null, null);
         if (mediaCursor.moveToFirst())
         {
            do
            {
               Uri waypointUri = Locations.buildUri(mediaCursor.getLong(1), mediaCursor.getLong(2), mediaCursor.getLong(3));
               waypointCursor = resolver.query(waypointUri, new String[] { Locations.LATITUDE, Locations.LONGITUDE, Waypoints.ALTITUDE, Waypoints.TIME }, null, null, null);
               serializer.text("\n");
               serializer.startTag("", "wpt");
               if (waypointCursor != null && waypointCursor.moveToFirst())
               {
                  serializer.attribute(null, "lat", Double.toString(waypointCursor.getDouble(0)));
                  serializer.attribute(null, "lon", Double.toString(waypointCursor.getDouble(1)));
                  serializer.text("\n");
                  serializer.startTag("", "ele");
                  serializer.text(Double.toString(waypointCursor.getDouble(2)));
                  serializer.endTag("", "ele");
                  serializer.text("\n");
                  serializer.startTag("", "time");
                  Date time = new Date(waypointCursor.getLong(3));
                  synchronized (ZULU_DATE_FORMATER)
                  {
                     serializer.text(ZULU_DATE_FORMATER.format(time));
                  }
                  serializer.endTag("", "time");
               }
               if (waypointCursor != null)
               {
                  waypointCursor.close();
                  waypointCursor = null;
               }

               Uri mediaUri = Uri.parse(mediaCursor.getString(0));
               if (mediaUri.getScheme().equals("file"))
               {
                  if (mediaUri.getLastPathSegment().endsWith("3gp"))
                  {
                     String fileName = includeMediaFile(mediaUri.getLastPathSegment());
                     quickTag(serializer, "", "name", fileName);
                     serializer.startTag("", "link");
                     serializer.attribute(null, "href", fileName);
                     quickTag(serializer, "", "text", fileName);
                     serializer.endTag("", "link");
                  }
                  else if (mediaUri.getLastPathSegment().endsWith("jpg"))
                  {
                     String mediaPathPrefix = Constants.getSdCardDirectory(mContext);
                     String fileName = includeMediaFile(mediaPathPrefix + mediaUri.getLastPathSegment());
                     quickTag(serializer, "", "name", fileName);
                     serializer.startTag("", "link");
                     serializer.attribute(null, "href", fileName);
                     quickTag(serializer, "", "text", fileName);
                     serializer.endTag("", "link");
                  }
                  else if (mediaUri.getLastPathSegment().endsWith("txt"))
                  {
                     quickTag(serializer, "", "name", mediaUri.getLastPathSegment());
                     serializer.startTag("", "desc");
                     if (buf != null)
                     {
                        buf.close();
                     }
                     buf = new BufferedReader(new FileReader(mediaUri.getEncodedPath()));
                     String line;
                     while ((line = buf.readLine()) != null)
                     {
                        serializer.text(line);
                        serializer.text("\n");
                     }
                     serializer.endTag("", "desc");
                  }
               }
               else if (mediaUri.getScheme().equals("content"))
               {
                  if ((Prim.AUTHORITY + ".string").equals(mediaUri.getAuthority()))
                  {
                     quickTag(serializer, "", "name", mediaUri.getLastPathSegment());
                  }
                  else if (mediaUri.getAuthority().equals("media"))
                  {

                     Cursor mediaItemCursor = null;
                     try
                     {
                        mediaItemCursor = resolver.query(mediaUri, new String[] { MediaColumns.DATA, MediaColumns.DISPLAY_NAME }, null, null, null);
                        if (mediaItemCursor.moveToFirst())
                        {
                           String fileName = includeMediaFile(mediaItemCursor.getString(0));
                           quickTag(serializer, "", "name", fileName);
                           serializer.startTag("", "link");
                           serializer.attribute(null, "href", fileName);
                           quickTag(serializer, "", "text", mediaItemCursor.getString(1));
                           serializer.endTag("", "link");
                        }
                     }
                     finally
                     {
                        if (mediaItemCursor != null)
                        {
                           mediaItemCursor.close();
                        }
                     }
                  }
               }
               serializer.text("\n");
               serializer.endTag("", "wpt");
            }
            while (mediaCursor.moveToNext());
         }
      }
      finally
      {
         if (mediaCursor != null)
         {
            mediaCursor.close();
         }
         if (waypointCursor != null)
         {
            waypointCursor.close();
         }
         if (buf != null)
            buf.close();
      }
   }*/

   @Override
   protected String getContentType()
   {
      return needsBundling() ? "application/zip" : "text/xml";
   }
}