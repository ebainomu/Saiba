package com.prim;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.prim.custom.CustomActivity;
import com.prim.model.Data;
import com.prim.ui.LeftNavAdapter;
import com.prim.ui.MainFragment;
import com.prim.ui.Profile;
import com.prim.ui.Settings;
import android.location.Location;
import dev.baalmart.prim.R;
import java.util.ArrayList;

public class MainActivity extends CustomActivity
{
  private DrawerLayout drawerLayout;
  private ListView drawerLeft;
  @SuppressWarnings("deprecation")
  private ActionBarDrawerToggle drawerToggle;
  
  //private FixedMyLocationOverlay mMylocation;

  private void setupContainer(int paramInt)
  {
	  //Logout
    if (paramInt == 4)
    {
      startActivity(new Intent(this, Login.class));
      finish();
    }
    
    String str = getString(R.string.app_name);
    Object localObject = null;
    if (paramInt == 0)
    {
      localObject = new Profile();
      str = "Martin Bbaale";
    }
    
    while (localObject == null)
    {
      //return;
    	
    	//find
      if (paramInt == 1)
      {
        localObject = new MainFragment();
      }
      //favorite
      else if (paramInt == 2)
      {
        startActivity(new Intent(this, IssueList.class).putExtra("title", "Favorites"));
        localObject = null;
      }      
      
      else
      {
        localObject = null;
        
        //settings
        if (paramInt == 3)
        {
          localObject = new Settings();
          str = "Settings";
        }
      }
    }
    
    //setting the details of each item on the nav bar....
    getActionBar().setTitle(str);
    getSupportFragmentManager().beginTransaction().
    replace(R.id.content_frame, (Fragment)localObject).commit();
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
    ArrayList localArrayList = new ArrayList();
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
        /*if (paramAnonymousInt != 2)
          localLeftNavAdapter.setSelection(paramAnonymousInt - 1);
        drawerLayout.closeDrawers();
        MainActivity.this.setupContainer(paramAnonymousInt);*/
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
	 /* Intent  i = new Intent(this, MainActivity.class);
	  startActivity(i);*/
	  /*Intent  i = new Intent(this, MainActivity.class);
	  startActivity(i);*/
  }
  
  @Override
	protected void onResume() 
  {
	  super.onResume();
	 /* Intent  i = new Intent(this, MainActivity.class);
	  startActivity(i);*/
  }
  
  @Override
	protected void onStop() {
	  super.onStop();
	/*  Intent  i = new Intent(this, MainActivity.class);
	  startActivity(i);*/
  }
  
  
}
