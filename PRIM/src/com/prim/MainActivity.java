package com.prim;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.prim.custom.CustomActivity;
import com.prim.logger.GPSLoggerServiceManager;
import com.prim.logger.IGPSLoggerServiceRemote;
import com.prim.model.Data;
import com.prim.ui.LeftNavAdapter;
import com.prim.ui.MainFragment;
import com.prim.ui.Profile;
import com.prim.ui.Settings;

import android.location.Location;
import dev.baalmart.prim.R;

import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends CustomActivity
{
  private DrawerLayout drawerLayout;
  private ListView drawerLeft;
  @SuppressWarnings("deprecation")
  private ActionBarDrawerToggle drawerToggle;
  
  //private GPSLoggerServiceManager mLoggerServiceManager;
  private IGPSLoggerServiceRemote mGPSLoggerRemote;
  protected static final String TAG = "Main Activity";
  
  //private FixedMyLocationOverlay mMylocation;

  private void setupContainer(int pos)
  {
	    String str = getString(R.string.app_name);
	    //Object localObject = null;
	    Fragment fragment = null;
	  
	  //******************Logout********************************
    if (pos == 4)
    {
      /*startActivity(new Intent(this, Login.class));
      finish();*/
    }    
   
    if (pos == 0)
    {
    	fragment = new Profile();
      str = "Martin Bbaale";
    }
    
    while (fragment == null)
    {
      if (pos == 1)
      {
    	  fragment = new MainFragment();
      }
      //*********************favorite***********************
      else if (pos == 2)
      {
        //startActivity(new Intent(this, IssueList.class).putExtra("title", "Favorites"));
        //fragment = null;
      }      
      
      else
      {
        //**********************settings*****************
        if (pos == 3)
        {
        	fragment = new Settings();
          str = "Settings";
        }
      }
    }    
    //setting the details of each item on the nav bar....
    getActionBar().setTitle(str);
    getSupportFragmentManager().beginTransaction().
    replace(R.id.content_frame, (Fragment)fragment).commit();
  }

  private void setupDrawer()
  {
    drawerLayout = ((DrawerLayout)findViewById(R.id.drawer_layout));
    //setting the drawer shadow...
    drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START); //  //8388611
    drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, 
    		R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close)
    {
      public void onDrawerClosed(View paramAnonymousView)
      {
      }

      public void onDrawerOpened(View paramAnonymousView)
      {
      }
    };
    
    drawerLayout.setDrawerListener(drawerToggle);
    drawerLayout.closeDrawers();
    setupLeftNavDrawer();
  }

  @SuppressLint({"InflateParams"})
  private void setupLeftNavDrawer()
  {
    drawerLeft = ((ListView)findViewById(R.id.left_drawer));
    View localView = getLayoutInflater().inflate(R.layout.left_nav_header, null);
    drawerLeft.addHeaderView(localView);
    ArrayList<Data> localArrayList = new ArrayList<Data>();
    localArrayList.add(new Data(new String[] { "Find" }, new int[] { R.drawable.ic_nav1, R.drawable.ic_nav1_sel }));
    localArrayList.add(new Data(new String[] { "Favorite" }, new int[] { R.drawable.ic_nav2, R.drawable.ic_nav2_sel }));
    localArrayList.add(new Data(new String[] { "Settings" }, new int[] { R.drawable.ic_nav3, R.drawable.ic_nav3_sel }));
    localArrayList.add(new Data(new String[] { "Log Out" }, new int[] { R.drawable.ic_nav4, R.drawable.ic_nav4_sel }));
    final LeftNavAdapter localLeftNavAdapter = new LeftNavAdapter(this, localArrayList);
    drawerLeft.setAdapter(localLeftNavAdapter);    
    drawerLeft.setOnItemClickListener(new AdapterView.OnItemClickListener()
    
    {
      public void onItemClick(AdapterView<?> paramAnonymousAdapterView, View paramAnonymousView, int paramAnonymousInt, long paramAnonymousLong)
      {
    	  	 
    	try
    	   { 
    		
         if (paramAnonymousInt != 2)
         {
          localLeftNavAdapter.setSelection(paramAnonymousInt - 1);
          drawerLayout.closeDrawers();
          MainActivity.this.setupContainer(paramAnonymousInt);
         }
      	   }
    	   
    	  catch (IllegalArgumentException e)
          {
    		  //Context 
            Log.e(TAG, " IllegalArgumentException", e);
            Intent intent = new Intent();
     		Context packageContext = null;
			intent.setClass(packageContext, MainActivity.class);
          }
    	
          catch (SecurityException e)
          {
             Log.e(TAG, "SecurityException", e);
             Intent intent = new Intent();
      		Context packageContext = null;
 			intent.setClass(packageContext, MainActivity.class);
          }
          catch (IllegalStateException e)
          {
             Log.e(TAG, "IllegalStateException", e);
             Intent intent = new Intent();
      		Context packageContext = null;
 			intent.setClass(packageContext, MainActivity.class);
          }
          catch (NullPointerException e)
          {
             Log.e(TAG, "NullPointerException", e);
             Intent intent = new Intent();
      		Context packageContext = null;
 			intent.setClass(packageContext, MainActivity.class);
          }
    	 
      }
    });
  }

  @Override
  public void onConfigurationChanged(Configuration paramConfiguration)
  {
    super.onConfigurationChanged(paramConfiguration);
    drawerToggle.onConfigurationChanged(paramConfiguration);
  }

  @Override
  protected void onCreate(Bundle paramBundle)
  {
    super.onCreate(paramBundle);
    setContentView(R.layout.activity_main);
    setupDrawer();
    setupContainer(1);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem paramMenuItem)
  
  {
    if (drawerToggle.onOptionsItemSelected(paramMenuItem))
    return true;
    return super.onOptionsItemSelected(paramMenuItem);
  }

  @Override
  protected void onPostCreate(Bundle paramBundle)
  {
    super.onPostCreate(paramBundle);
    drawerToggle.syncState();
  }
  
  @Override
	protected void onPause() {
	  super.onPause();
	  setupContainer(1);
  }
  
  @Override
	protected void onResume() 
  {
	  super.onResume();
	 /* Intent  i = new Intent(this, MainActivity.class);
	  startActivity(i);*/
	  setupContainer(1);
  }
  
  @Override
	protected void onStop() {
	  super.onStop();
	  try
      {
         this.mGPSLoggerRemote.stopLogging();
      }
      catch (RemoteException e)
      {
         Log.e( TAG, "Could not stop GPSLoggerService.", e );
      }
	  finish();
	
  }
  
  
}
