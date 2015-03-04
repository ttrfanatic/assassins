package com.dls.assassins;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;

public class SplashScreen extends Activity 
{
	private Thread timer;
	
	/** Called when the activity is first created. */
	@Override public void onCreate(Bundle savedInstanceState) 
	{
	    super.onCreate(savedInstanceState);
	
		setContentView(R.layout.splash);
		
		timer = new Thread()
		{
			public void run()
			{
				try
				{
					synchronized(this)
					{
						wait(5000);
					}
				}
				catch(InterruptedException e)
				{
				}
				finally
				{
					Intent splashscreenactivity = new Intent("android.intent.action.MAINACTIVITY");
					
					splashscreenactivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					
					splashscreenactivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);

					startActivity(splashscreenactivity);
				}
			}
		};
		
		timer.start();
	}

	@Override public boolean onTouchEvent(MotionEvent event) 
	{
	    if (event.getAction() == MotionEvent.ACTION_DOWN) 
	    {
            synchronized(timer)
            {
                timer.notifyAll();
            }
        }
	    
        return true;
	}

	@Override protected void onPause() 
	{
		super.onPause();
		
		finish();
	}

}
