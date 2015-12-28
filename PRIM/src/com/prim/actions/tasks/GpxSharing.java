package com.prim.actions.tasks;

//import com.prim.actions.ShareTrack;
import com.prim.actions.ShareLabels;
import com.prim.actions.utils.ProgressListener;

import dev.baalmart.prim.R;
import android.content.Context;
import android.net.Uri;

/** 
 * @author baalmart
 */
public class GpxSharing extends GpxCreator
{

   public GpxSharing(Context context, Uri labelUri, String chosenBaseFileName, boolean attachments, ProgressListener listener)
   {
      super(context, labelUri, chosenBaseFileName, listener); //the GPX creator constructor
   }

   @Override
   protected void onPostExecute(Uri resultFilename)
   {
      super.onPostExecute(resultFilename);
      ShareLabels.sendFile(mContext, resultFilename, mContext.getString(R.string.email_gpxbody), getContentType());
   }
   
}
