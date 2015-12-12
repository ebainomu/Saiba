package com.prim.actions.tasks;

//import com.prim.actions.ShareTrack;
import com.prim.actions.utils.ProgressListener;

import dev.baalmart.prim.R;
import android.content.Context;
import android.net.Uri;

/** 
 * @author baalmart
 */
public class GpxSharing extends GpxCreator
{


   public GpxSharing(Context context, Uri trackUri, String chosenBaseFileName, boolean attachments, ProgressListener listener)
   {
      super(context, trackUri, chosenBaseFileName, attachments, listener);
   }

   @Override
   protected void onPostExecute(Uri resultFilename)
   {
      super.onPostExecute(resultFilename);
      //ShareTrack.sendFile(mContext, resultFilename, mContext.getString(R.string.email_gpxbody), getContentType());
   }
   
}
