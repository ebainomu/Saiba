package com.prim.custom;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

public class CustomFragment extends Fragment
  implements View.OnClickListener
{
  public void onClick(View paramView)
  {
  }

  public View onCreateView(LayoutInflater paramLayoutInflater, ViewGroup paramViewGroup, Bundle paramBundle)
  {
    return super.onCreateView(paramLayoutInflater, paramViewGroup, paramBundle);
  }

  public View setTouchNClick(View paramView)
  {
    paramView.setOnClickListener(this);
    paramView.setOnTouchListener(CustomActivity.TOUCH);
    return paramView;
  }
}