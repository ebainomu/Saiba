package com.prim;

import android.app.ActionBar;
import android.os.Bundle;

import com.prim.custom.CustomActivity;

import dev.baalmart.prim.R;

public class PlaceDetail extends CustomActivity
{

  @Override
  protected void onCreate(Bundle paramBundle)
  {
    super.onCreate(paramBundle);
    setContentView(R.layout.place_details);
    getActionBar().setTitle("Superba Food");
  }
}
