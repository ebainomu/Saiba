package com.prim.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.prim.custom.CustomFragment;

import dev.baalmart.gps.R;

@SuppressLint({"InflateParams"})
public class Profile extends CustomFragment
{
  public View onCreateView(LayoutInflater paramLayoutInflater, 
		  ViewGroup paramViewGroup, Bundle paramBundle)
  {
    return paramLayoutInflater.inflate(R.layout.profile, null);
  }
}

