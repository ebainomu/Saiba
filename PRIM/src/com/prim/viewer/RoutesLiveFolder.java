package com.prim.viewer;

import com.prim.db.Prim;
import dev.baalmart.prim.R;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.LiveFolders;

public class RoutesLiveFolder extends Activity
{
   @Override
   protected void onCreate( Bundle savedInstanceState )
   {
      this.setVisible( false );
      super.onCreate( savedInstanceState );

      final Intent intent = getIntent();
      final String action = intent.getAction();

      if( LiveFolders.ACTION_CREATE_LIVE_FOLDER.equals( action ) )
      {
         final Intent baseAction = new Intent( Intent.ACTION_VIEW, Prim.Tracks.CONTENT_URI );
         
         Uri liveData = Uri.withAppendedPath( Prim.CONTENT_URI, "live_folders/tracks" );
         final Intent createLiveFolder = new Intent();
         createLiveFolder.setData( liveData );
         createLiveFolder.putExtra( LiveFolders.EXTRA_LIVE_FOLDER_NAME, getString(R.string.track_list) );
         createLiveFolder.putExtra( LiveFolders.EXTRA_LIVE_FOLDER_ICON, Intent.ShortcutIconResource.fromContext( this, R.drawable.ic_action_folder ) );
         createLiveFolder.putExtra( LiveFolders.EXTRA_LIVE_FOLDER_DISPLAY_MODE, LiveFolders.DISPLAY_MODE_LIST );
         createLiveFolder.putExtra( LiveFolders.EXTRA_LIVE_FOLDER_BASE_INTENT, baseAction );
         setResult( RESULT_OK, createLiveFolder );
      }
      else
      {
         setResult( RESULT_CANCELED );
      }
      finish();
   }
}
