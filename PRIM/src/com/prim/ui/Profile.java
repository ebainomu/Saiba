package com.prim.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import dev.baalmart.prim.R;

import com.prim.custom.CustomFragment;

@SuppressLint({"InflateParams"})
public class Profile extends CustomFragment
{
  public View onCreateView(LayoutInflater paramLayoutInflater, 
		  ViewGroup paramViewGroup, Bundle paramBundle)
  {
    return paramLayoutInflater.inflate(R.layout.profile, null);
  }
}

