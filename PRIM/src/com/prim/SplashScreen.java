package com.prim;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import dev.baalmart.prim.R;

public class SplashScreen extends Activity
{
  private boolean isRunning;

  private void doFinish()
  {
    try
    {
      if (isRunning)
      {
        isRunning = false;
        Intent localIntent = new Intent(this, Login.class);
        localIntent.addFlags(67108864);
        startActivity(localIntent);
        finish();
      }
      return;
    }
    finally
    {
      /*localObject = finally;
      throw localObject;*/
    }
  }

  private void startSplash()
  {
    new Thread(new Runnable()
    {
      public void run()
      {
        try
        {
          Thread.sleep(3000L);
          return;
        }
        catch (Exception localException)
        {
          localException.printStackTrace();
          return;
        }
        finally
        {
          runOnUiThread(new Runnable()
          {
            public void run()
            {
              SplashScreen.this.doFinish();
            }
          });
        }
      }
    }).start();
  }

  public void onCreate(Bundle paramBundle)
  {
    super.onCreate(paramBundle);
    setContentView(R.layout.splash);
    isRunning = true;
    startSplash();
  }

  public boolean onKeyDown(int paramInt, KeyEvent paramKeyEvent)
  {
    if (paramInt == 4)
    {
      isRunning = false;
      finish();
      return true;
    }
    return super.onKeyDown(paramInt, paramKeyEvent);
  }
}

