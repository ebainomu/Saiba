package com.prim;

import dev.baalmart.prim.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

public class Others extends Activity
{
	 String msg = "Android : ";
	
	/** Called when the activity is first created. */
	   @Override
	   public void onCreate(Bundle savedInstanceState) {
	      super.onCreate(savedInstanceState);
	      setContentView(R.layout.others);
	      Log.d(msg, "The onCreate() event");
	   }

	   /** Called when the activity is about to become visible. */
	   @Override
	   protected void onStart() {
	      super.onStart();
	      Log.d(msg, "The onStart() event");
	   }

	   /** Called when the activity has become visible. */
	   @Override
	   protected void onResume() {
	      super.onResume();
	      Log.d(msg, "The onResume() event");
	   }

	   /** Called when another activity is taking focus. */
	   @Override
	   protected void onPause() {
	      super.onPause();
	      Log.d(msg, "The onPause() event");
	   }

	   /** Called when the activity is no longer visible. */
	   @Override
	   protected void onStop() {
	      super.onStop();
	      Log.d(msg, "The onStop() event");
	   }

	   /** Called just before the activity is destroyed. */
	   @Override
	   public void onDestroy() {
	      super.onDestroy();
	      Log.d(msg, "The onDestroy() event");
	   }
	   
	   
	   //now the actual coding starts...
	   
	   
	   //sending the message to the database....
	   public void sendMessage(View view) 
	   {		   
		   EditText editText = (EditText) findViewById(R.id.EditMessage);
	/*	   Intent intent = new Intent(this, DisplayMessageActivity.class);
		   
		   String message = editText.getText().toString();
		   intent.putExtra(EXTRA_MESSAGE, message);*/
		 }
	   
	

}
