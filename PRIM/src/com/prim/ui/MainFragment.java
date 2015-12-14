package com.prim.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;
import com.prim.MainActivity;
import com.prim.Others;
import com.prim.IssueList;
import com.prim.custom.CustomFragment;
import com.prim.logger.GPSLoggerService;
import com.prim.logger.GPSLoggerServiceManager;
import com.prim.logger.IGPSLoggerServiceRemote;
import com.prim.model.Data;
import dev.baalmart.prim.R;
import java.io.IOException;
import java.util.ArrayList;

public class MainFragment extends CustomFragment
{
  protected static final String TAG = null;
private ArrayList<Data> iList;

private GPSLoggerServiceManager mLoggerServiceManager;
private IGPSLoggerServiceRemote mGPSLoggerRemote;
private GPSLoggerService mLoggerService;

  private void loadDummyData()
  {
    iList = new ArrayList<Data>();
    
    //potholes
    iList.add(new Data(new String[] { "potholes" }, 
    		new int[] { R.drawable.potholes, 
    		R.drawable.potholes, R.drawable.potholes }));
    
    //road humps
    iList.add(new Data(new String[] { "road humps" }, 
    		new int[] { R.drawable.road_humps, 
    		R.drawable.road_humps, 
    		R.drawable. road_humps }));
    
    //uneven roads
    iList.add(new Data(new String[] { "uneven roads" }, 
    		new int[] { R.drawable.uneven_roads, 
    		R.drawable.uneven_roads, 
    		R.drawable.uneven_roads }));
    
    //pavements
    iList.add(new Data(new String[] { "pavements" }, 
    		new int[] { R.drawable.pavements, 
    		R.drawable.pavements, 
    		R.drawable.pavements }));    
    
    //road markings
    //ArrayList localArrayList1 = iList;    
    String[] arrayOfString1 = { "road markings" };   
    int[] arrayOfInt1 = new int[3];
    arrayOfInt1[0] = R.drawable.road_markings;
    arrayOfInt1[2] = R.drawable.road_markings;
    iList.add(new Data(arrayOfString1, arrayOfInt1));    
    
    //sudden breaking
    iList.add(new Data(new String[] { "sudden breaking" }, 
    		new int[] { R.drawable.sudden_breaking, 
    		R.drawable.sudden_breaking, 
    		R.drawable.sudden_breaking }));   
    
   //gravel
   // ArrayList localArrayList2 = iList;   
    String[] arrayOfString2 = { "gravel" };
    int[] arrayOfInt2 = new int[3];
    arrayOfInt2[0] = R.drawable.gravel;
    arrayOfInt2[2] = R.drawable.gravel;
    iList.add(new Data(arrayOfString2, arrayOfInt2));
    
    //others
    iList.add(new Data(new String[] { "others" }, 
    		new int[] { R.drawable.others, 
    		R.drawable.others, 
    		R.drawable.others }));
  }

  private void setupView(View paramView)
  {
    setTouchNClick(paramView.findViewById(R.id.nearby));
    loadDummyData();
    GridView localGridView = (GridView)paramView.findViewById(R.id.grid);
    localGridView.setAdapter(new GridAdapter());    
    
    ///handling item click events
    localGridView.setOnItemClickListener(new AdapterView.OnItemClickListener()
    {
      private Location location;

	public void onItemClick(AdapterView<?> paramAnonymousAdapterView, View paramAnonymousView, 
    		  int paramAnonymousInt, long paramAnonymousLong)
      {
		  //mLoggerService.storeLocation(location);
		try
		{
			/* in this section
	     	  * the logging has to momentarily stop
	     	  * then the x,y,z,longitude and latitude values are all stored with respect to the time
	     	  * that data is changed to XML using the XML creator class
	     	  * And then stored in a file on the SD card     	
	     	  * 
	     	  * */ 			
			mLoggerService.stopLogging();
			mLoggerService.StoreLatLongTimeSpeed(location);
			mLoggerService.startLogging();			
			
		 /* Intent intent = new Intent();
		  intent.setClass(getActivity(), IssueList.class);
		  intent.putExtra("title", ((Data)iList.get(paramAnonymousInt)).getTexts()[0]).
 		  putExtra("icon", ((Data)iList.get(paramAnonymousInt)).getResources()[1]).
 		  putExtra("icon1", ((Data)iList.get(paramAnonymousInt)).getResources()[2]);*/		  
		     	  
	    
	   /*     startActivity(new Intent(getActivity(), IssueList.class).putExtra
	        		("title", ((Data)iList.get(paramAnonymousInt)).getTexts()[0]).
	        		putExtra("icon", ((Data)iList.get(paramAnonymousInt)).getResources()[1]).
	        		putExtra("icon1", ((Data)iList.get(paramAnonymousInt)).getResources()[2]));} 
	   */	  
		  
		}
		
    	catch (IllegalArgumentException e)
        {
           Log.e(TAG, "Could not start GPSLoggerService.", e);
       	Intent intent = new Intent();
		intent.setClass(getActivity(), MainActivity.class);
        }
        catch (SecurityException e)
        {
           Log.e(TAG, "Could not start GPSLoggerService.", e);
       	Intent intent = new Intent();
		intent.setClass(getActivity(), MainActivity.class);
        }
        catch (IllegalStateException e)
        {
           Log.e(TAG, "Could not start GPSLoggerService.", e);
       	Intent intent = new Intent();
		intent.setClass(getActivity(), MainActivity.class);
        }
    	
    	catch (NullPointerException e)
        {
           Log.e(TAG, "Could not start GPSLoggerService.", e);
       	Intent intent = new Intent();
		intent.setClass(getActivity(), MainActivity.class);
        }
	
   
      }
    });    
    
    //from online sources:    
/*    public void onItemClick(AdapterView<?> parent, View v, int position, long id)*/ 
      
  }

  
  //the click listener for the upper section of the main fragment....the sign for location image....
  @Override
  public void onClick(View paramView)
  {
    super.onClick(paramView);
    if (paramView.getId() == R.id.nearby) 
    {
    	//planning to use this particular one to start and stop the logging
      	
         Message msg = null;
	
    	try 
    	{
    	 //mLoggerService._handleMessage(msg);
    		
    	mLoggerService. soundGpsSignalAlarm();	
    	mLoggerService.startLogging();	
    	
		} 
    	catch (IllegalArgumentException e)
        {
           Log.e(TAG, "Could not start GPSLoggerService.", e);
       	Intent intent = new Intent();
		intent.setClass(getActivity(), MainActivity.class);
        }
        catch (SecurityException e)
        {
           Log.e(TAG, "Could not start GPSLoggerService.", e);
       	Intent intent = new Intent();
		intent.setClass(getActivity(), MainActivity.class);
        }
        catch (IllegalStateException e)
        {
           Log.e(TAG, "Could not start GPSLoggerService.", e);
       	Intent intent = new Intent();
		intent.setClass(getActivity(), MainActivity.class);
        }
    	
    	catch (NullPointerException e)
        {
           Log.e(TAG, "Could not start GPSLoggerService.", e);
       	Intent intent = new Intent();
		intent.setClass(getActivity(), MainActivity.class);
        }
    
    }
      //startActivity(new Intent(getActivity(), IssueList.class).putExtra("title", getString(R.string.nearby)));
  }

  @SuppressLint({"InflateParams"})
  public View onCreateView(LayoutInflater paramLayoutInflater, ViewGroup paramViewGroup, Bundle paramBundle)
  {
    View localView = paramLayoutInflater.inflate(R.layout.main_container, null);
    setupView(localView);
    return localView;
  }

  private class GridAdapter extends BaseAdapter
  {
    private GridAdapter()
    {
    }

    public int getCount()
    {
      return iList.size();
    }

    public Data getItem(int paramInt)
    {
      return (Data)iList.get(paramInt);
    }

    public long getItemId(int paramInt)
    {
      return paramInt;
    }

    @SuppressLint({"InflateParams"})
    public View getView(int paramInt, View paramView, ViewGroup paramViewGroup)
    {
      if (paramView == null)
        paramView = getActivity().getLayoutInflater().inflate(R.layout.grid_item, null);
      Data localData = getItem(paramInt);
      TextView localTextView = (TextView)paramView;
      localTextView.setText(localData.getTexts()[0]);
      localTextView.setCompoundDrawablesWithIntrinsicBounds(0, localData.getResources()[0], 0, 0);
      return paramView;
    }
  }
}
