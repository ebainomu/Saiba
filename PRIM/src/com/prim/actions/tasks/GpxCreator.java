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

   
   //the constructor
   public GpxCreator(Context context, Uri labelUri, String chosenBaseFileName, boolean attachments, ProgressListener listener)
   {
      super(context, labelUri, chosenBaseFileName, listener);
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
         setExportDirectoryPath(Constants.getSdCardDirectory(mContext) + 
        		 mFileName.substring(0, mFileName.length() - 4));
         xmlFilePath = getExportDirectoryPath() + "/" + mFileName;
      }
      
      else    	  
      {
         setExportDirectoryPath(Constants.getSdCardDirectory(mContext) + mFileName);
         xmlFilePath = getExportDirectoryPath() + "/" + mFileName + ".gpx";
      }
      
    //making the directory to store the contents....
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

         serializeLabelPoints(serializer, mLabelUri);
         
         /* switcged from serializeLabel to serializeLabe
         lPoints hence had to swap the arguments*/
         
         
         //close the bufferedOutputStream and then initialize it to null
         buf.close();
         buf = null;
         //close the FileOutputStream and then then initialize it to null
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

/*   private void serializeLabel(Uri labelUri, XmlSerializer serializer) throws IllegalArgumentException, IllegalStateException, IOException
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
      serializer.startTag("", "label");
      serializer.text("\n");
      serializer.startTag("", "name");
      serializer.text(mName);
      serializer.endTag("", "name");
      // The list of segments in the track
      //serializeSegments(serializer, Uri.withAppendedPath(labelUri, "segments"));
      serializer.text("\n");
      serializer.endTag("", "label");
      serializer.text("\n");
      serializer.endTag("", "gpx");
      serializer.endDocument();
   }*/

   private void serializeLabelHeader(Context context, XmlSerializer serializer, Uri labelUri) 
		   throws IOException
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
         labelCursor = resolver.query(labelUri, new String[] 
        { 
        		 Labels._ID, Labels.NAME, Labels.DETECTION_TIME }, null, null, null);
         if (labelCursor.moveToFirst())        	 
         {
            databaseName = labelCursor.getString(1);
            serializer.text("\n");
            //start the metadata tag
            serializer.startTag("", "metadata");
            serializer.text("\n");
            //time tag
            serializer.startTag("", "time");
            Date time = new Date(labelCursor.getLong(2));
            synchronized (ZULU_DATE_FORMATER)
            {
               serializer.text(ZULU_DATE_FORMATER.format(time));
            }
            serializer.endTag("", "time");
            //end time from here...
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

   private void serializeLabelPoints(XmlSerializer serializer, Uri labelUri) throws IOException
   {
      if (isCancelled())
      {
         throw new IOException("Fail to execute request due to canceling");
      }
      
      //we have to create a cursor for a start
      Cursor labelsCursor = null;
      ContentResolver resolver = mContext.getContentResolver();
      try
      {
         labelsCursor = resolver.query(mLabelUri, new String[] { 
        		 Labels.LONGITUDE,           //0
        		 Labels.LATITUDE,            //1 
        		 Labels.DETECTION_TIME,      //2
        		 Labels._ID,                 //3   
        		 Labels.SPEED,               //4
        		 Labels.ACCURACY,            //5
        		 Labels.NAME,                //6
        		 Labels.X,                   //7
        		 Labels.Y,                   //8
        		 Labels.Z                    //9
        		}, null, null, null);
         
         serializer.startDocument("UTF-8", true);
         
         if (labelsCursor.moveToFirst())
         {
            do
            {
               mProgressAdmin.addWaypointProgress(1);
               
               
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
               serializeLabelHeader(mContext, serializer, mLabelUri);

               serializer.text("\n");
               serializer.startTag("", "label");               
               serializer.attribute(null, "name", labelsCursor.getString(6));
               serializer.text("\n");
               
               //location tag
               serializer.startTag("", "location");
               serializer.attribute(null, "lat", Double.toString(labelsCursor.getDouble(1)));
               serializer.attribute(null, "lon", Double.toString(labelsCursor.getDouble(0)));
               serializer.endTag("", "location");
               serializer.text("\n");
      
               serializer.startTag("", "time");
               Date time = new Date(labelsCursor.getLong(2));
               synchronized (ZULU_DATE_FORMATER)
               {
                  serializer.text(ZULU_DATE_FORMATER.format(time));
               }
               serializer.endTag("", "time");
               
               serializer.text("\n");
               
               serializer.startTag("", "accelerometerValues");
               serializer.attribute(null, "x", Float.toString(labelsCursor.getFloat(7)));
               serializer.attribute(null, "y", Float.toString(labelsCursor.getFloat(8)));
               serializer.attribute(null, "z", Float.toString(labelsCursor.getFloat(9)));
                          
               serializer.endTag("", "acceleromterValues");
               serializer.text("\n");
               
               serializer.startTag("", "speed");               
               double speed = labelsCursor.getDouble(4);
                            
               if (speed > 0.0)
               {
                  quickTag(serializer, NS_GPX_10, "speed", Double.toString(speed));
               }                            
               serializer.endTag("", "speed");
               serializer.text("\n");
               
               serializer.startTag("", "accuracy");
               double accuracy = labelsCursor.getDouble(5);
               if (accuracy > 0.0)
               {
                  quickTag(serializer, NS_OGT_10, "accuracy", Double.toString(accuracy));
               }               
               serializer.endTag("", "accuracy");
               
               serializer.text("\n");              
               serializer.endTag("", "label");
              
            }
            //while moving to the next one.
            while (labelsCursor.moveToNext());
         }
         serializer.endDocument();
      }
      
      finally
      {
         if (labelsCursor != null)
         {
            labelsCursor.close();
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
      
      Cursor locationCursor = null;
      Cursor labelCursor = null;
      
      BufferedReader buf = null;
      ContentResolver resolver = context.getContentResolver();
      try
      {    	  
    	  //getting the media infor from the database.....
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