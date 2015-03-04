package com.dls.assassins;

import android.app.Application;

import com.parse.Parse;
import com.parse.ParseACL;
import com.parse.ParseUser;

public class DLSApplication extends Application
{

	@Override public void onCreate() 
	{
		super.onCreate();

		Parse.initialize(this, "LwwErPYeHLiVxAT8CM64gGYZwmz51gXYXaELd73o", "ueg2GxIFJW3kYNR7VflVveVlo9PaUW7KoITZraKX");

		//ParseUser.enableAutomaticUser();
		ParseACL defaultACL = new ParseACL();
	    
		// If you would like all objects to be private by default, remove this line.
		defaultACL.setPublicReadAccess(true);
		
		ParseACL.setDefaultACL(defaultACL, true);
	}

}
