package com.dls.assassins;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.ParseAnalytics;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;


public class MainActivity extends Activity implements GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener, LocationListener
{
	public static final String TAG = MainActivity.class.getSimpleName();
	static final LatLng HAMBURG = new LatLng(53.558, 9.927);
	static final LatLng KIEL = new LatLng(53.551, 9.993);
	private GoogleMap map;
    private LocationClient locationClient = null;
    private LocationRequest locationRequest;
    private LatLng mLocation = null;
    private final long UPDATE_INTERVAL = 15000;
    private final long FASTEST_INTERVAL = 5000;
    private boolean locationEnabled = false;
    private Marker me = null;
	public static final int TAKE_PHOTO_REQUEST = 0;
	public static final int TAKE_VIDEO_REQUEST = 1;
	public static final int PICK_PHOTO_REQUEST = 2;
	public static final int PICK_VIDEO_REQUEST = 3;
	
	public static final int MEDIA_TYPE_IMAGE = 4;
	public static final int MEDIA_TYPE_VIDEO = 5;
	
	public static final int SUBMIT_PHOTO_REQUEST = 6;
	
	public static final int FILE_SIZE_LIMIT = 1024*1024*10; // 10 MB
	
	protected Uri mMediaUri;
	private final Map<String, Marker> mapMarkers = new HashMap<String, Marker>();
	
	private Bitmap bMarker;
	
	private boolean bCameraMoved = false;

	@Override protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
	
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		setContentView(R.layout.activity_main);

		locationClient = new LocationClient(this, this, this);

		locationRequest = LocationRequest.create();
		
		// Use high accuracy
		locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		
		// Set the update interval to 5 seconds
		locationRequest.setInterval(UPDATE_INTERVAL);
		
		// Set the fastest update interval to 1 second
		locationRequest.setFastestInterval(FASTEST_INTERVAL);
		
		checkForLocationServices();
		
		map = ((MapFragment)getFragmentManager().findFragmentById(R.id.map)).getMap();
		
		map.setMyLocationEnabled(true);
		
		Resources res = this.getResources();
		
		bMarker = scaleImage(res, R.drawable.ic_launcher, 48);
	}
	
	@Override protected void onActivityResult(int requestCode, int resultCode, Intent data) 
	{
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == RESULT_OK)
		{
			if (requestCode == TAKE_PHOTO_REQUEST)
			{
				Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
			
				mediaScanIntent.setData(mMediaUri);
				
				sendBroadcast(mediaScanIntent);
				
				Intent submitintent = new Intent(this, SubmitPhotoActivity.class);
				
				submitintent.setData(mMediaUri);
				
				submitintent.putExtra("longitude", mLocation.longitude);
				
				submitintent.putExtra("latitude", mLocation.latitude);
				
				startActivityForResult(submitintent, SUBMIT_PHOTO_REQUEST);
			}
			else if (requestCode == SUBMIT_PHOTO_REQUEST)
			{
				if (locationClient != null && locationClient.isConnected())
				{
					doMapQuery();
				}
			}
		}
		else if (resultCode != RESULT_CANCELED)
		{
			Toast.makeText(this, R.string.general_error, Toast.LENGTH_LONG).show();
		}
	}
	
	private void checkForLocationServices()
	{
	    LocationManager manager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
	    
	    if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
	    {
	    	locationEnabled = false;
	    
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			
			builder.setTitle(R.string.signup_error_title);
			
			builder.setPositiveButton(android.R.string.ok, null);
			
			builder.setMessage(R.string.location_services_message);
			
			AlertDialog dialog = builder.create();

			dialog.show();
	    }
	    else
	    {
	    	locationEnabled = true;
	    }
	}
	
	@Override protected void onResume() 
	{
		super.onResume();

		boolean bFound = checkForGooglePlayServices();

		if (bFound)
		{
			ParseAnalytics.trackAppOpened(getIntent());
			
			ParseUser currentUser = ParseUser.getCurrentUser();
			
			if (currentUser == null)
			{
				navigateToLogin();
			}
			else
			{
				Log.i(TAG, currentUser.getUsername());
			}
			
			doMapQuery();
		}
	}
	
	private Bitmap scaleImage(Resources res, int id, int lessSideSize)
	{
        Bitmap b = null;
    
        BitmapFactory.Options o = new BitmapFactory.Options();
        
        o.inJustDecodeBounds = true;

        BitmapFactory.decodeResource(res, id, o);

        float sc = 0.0f;
        
        int scale = 1;
        
        // if image height is greater than width
        if (o.outHeight > o.outWidth) 
        {
            sc = o.outHeight / lessSideSize;
        
            scale = Math.round(sc);
        }
        // if image width is greater than height
        else
        {
            sc = o.outWidth / lessSideSize;
        
            scale = Math.round(sc);
        }

        // Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        
        o2.inSampleSize = scale;
        
        b = BitmapFactory.decodeResource(res, id, o2);
        
        return b;
    }
	
//	private void setCurrentLocationMarker()
//	{
//		if (me != null)
//		{
//			me.remove();
//		}
//		
//		if (mLocation != null)
//		{
//			Resources res = this.getResources();
//			
//			Bitmap bMarker = scaleImage(res, R.drawable.ic_launcher, 48);
//			
//			me = map.addMarker(new MarkerOptions().position(mLocation).title("Me").snippet("You are here.").icon(BitmapDescriptorFactory.fromBitmap(bMarker)));
//
//		    map.moveCamera(CameraUpdateFactory.newLatLngZoom(mLocation, 18));
//
//			// Zoom in, animating the camera.
//			map.animateCamera(CameraUpdateFactory.zoomTo(18), 2000, null);
//		}
//	}

	private void navigateToLogin() 
	{
		Intent intent = new Intent(this, LoginActivity.class);
		
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
		
		startActivity(intent);
	}
	
	@Override protected void onStart() 
	{
		super.onStart();
		
		if (locationClient != null)
		{
			locationClient.connect();
		}
	}
	
	@Override protected void onStop() 
	{
		super.onStop();
		
		if (locationClient != null)
		{
			if (locationClient.isConnected())
			{
				locationClient.removeLocationUpdates(this);
			}
			
			locationClient.disconnect();
		}
	}

	@Override public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
	
		return true;
	}

	@Override public boolean onOptionsItemSelected(MenuItem item) 
	{
		int itemId = item.getItemId();
		
		switch(itemId) 
		{
			case R.id.action_logout:
			{
				ParseUser.logOut();
			
				navigateToLogin();
			
				break;
			}
			
			case R.id.action_camera:
			{
				Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				
				mMediaUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
				
				if (mMediaUri == null)
				{
					// display an error
					Toast.makeText(MainActivity.this, R.string.error_external_storage, Toast.LENGTH_LONG).show();
				}
				else 
				{
					takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, mMediaUri);
				
					startActivityForResult(takePhotoIntent, TAKE_PHOTO_REQUEST);
				}
				
				break;
			}
		}
		
		return super.onOptionsItemSelected(item);
	}

    private boolean checkForGooglePlayServices() 
    {
        int testResult = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        boolean bFound = false;
 
        if (testResult == ConnectionResult.SUCCESS)
        {
            Log.d(TAG, "Google Play Services confirmed.");
            
            bFound = true;
        }
        else
        {
            Log.d(TAG, getString(R.string.google_play_services_are_missing));
 
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			
			builder.setTitle(R.string.signup_error_title);
			
			builder.setPositiveButton(android.R.string.ok, null);
			
			builder.setMessage(getString(R.string.google_play_services_are_missing));
			
			AlertDialog dialog = builder.create();

			dialog.show();
        }
        
        return bFound;
    }

	@Override public void onConnectionFailed(ConnectionResult result) 
	{
	}

	@Override public void onConnected(Bundle connectionHint) 
	{
		Location location = locationClient.getLastLocation();
		
		if (location != null)
		{
        	double lat = location.getLatitude();
       	 
        	double lon = location.getLongitude();
        
        	mLocation = new LatLng(lat, lon);
        	
        	if (!bCameraMoved)
        	{
        		bCameraMoved = true;
        		
	    	    map.moveCamera(CameraUpdateFactory.newLatLngZoom(mLocation, 18));
	
	    		// Zoom in, animating the camera.
	    		map.animateCamera(CameraUpdateFactory.zoomTo(18), 2000, null);
        	}
		}

		if (locationEnabled)
		{
			locationClient.requestLocationUpdates(locationRequest, this);
		}
	}
	
	@Override public void onDisconnected() 
	{
	}

	@Override public void onLocationChanged(Location location) 
	{
		double lat = location.getLatitude();
    	 
    	double lon = location.getLongitude();

    	mLocation = new LatLng(lat, lon);
    	
    	if (!bCameraMoved)
    	{
    		bCameraMoved = true;
    		
    	    map.moveCamera(CameraUpdateFactory.newLatLngZoom(mLocation, 18));

    		// Zoom in, animating the camera.
    		map.animateCamera(CameraUpdateFactory.zoomTo(18), 2000, null);
    	}

    	doMapQuery();
	}
	
	private Uri getOutputMediaFileUri(int mediaType) 
	{
		// To be safe, you should check that the SDCard is mounted
	    // using Environment.getExternalStorageState() before doing this.
		if (isExternalStorageAvailable()) 
		{
			// get the URI
			
			// 1. Get the external storage directory
			String appName = MainActivity.this.getString(R.string.folder_name);

			File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), appName);
			
			// 2. Create our subdirectory
			if (! mediaStorageDir.exists()) 
			{
				if (!mediaStorageDir.mkdirs()) 
				{
					Log.e(TAG, "Failed to create directory.");
					
					return null;
				}
			}
			
			// 3. Create a file name
			// 4. Create the file
			File mediaFile;
			
			Date now = new Date();
			
			String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(now);
			
			String path = mediaStorageDir.getPath() + File.separator;
			
			if (mediaType == MEDIA_TYPE_IMAGE) 
			{
				mediaFile = new File(path + "IMG_" + timestamp + ".jpg");
			}
			else if (mediaType == MEDIA_TYPE_VIDEO)
			{
				mediaFile = new File(path + "VID_" + timestamp + ".mp4");
			}
			else
			{
				return null;
			}
			
			Log.d(TAG, "File: " + Uri.fromFile(mediaFile));
			
			// 5. Return the file's URI				
			return Uri.fromFile(mediaFile);
		}
		else
		{
			return null;
		}
	}
	
	private boolean isExternalStorageAvailable() 
	{
		String state = Environment.getExternalStorageState();
		
		if (state.equals(Environment.MEDIA_MOUNTED)) 
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	private ParseGeoPoint geoPointFromLocation(Location loc) 
	{
		return new ParseGeoPoint(loc.getLatitude(), loc.getLongitude());
	}
	
	private void doMapQuery() 
	{
		if (mLocation != null)
		{
			Location myLoc = new Location("New");
			
			myLoc.setLatitude(mLocation.latitude);
			
			myLoc.setLongitude(mLocation.longitude);
			
			final ParseGeoPoint myPoint = geoPointFromLocation(myLoc);
			
			ParseQuery<ParseObject> mapQuery = ParseQuery.getQuery(ParseConstants.CLASS_SIGNS);
			
			mapQuery.whereWithinMiles("location", myPoint, 20);
	
			mapQuery.include("user");
	
			mapQuery.orderByDescending("createdAt");
			
			mapQuery.setLimit(100);
	
			mapQuery.findInBackground(new FindCallback<ParseObject>() 
			{
				@Override public void done(List<ParseObject> objects, ParseException e) 
				{
					if (e != null)
					{
						Log.e(TAG, e.getMessage());
						
						AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
						
						builder.setMessage(e.getMessage()).setTitle(R.string.signup_error_title).setPositiveButton(android.R.string.ok, null);
						
						AlertDialog dialog = builder.create();
						
						dialog.show();
					}
					else
					{
						// No errors, process query results
						Set<String> toKeep = new HashSet<String>();

						for (ParseObject post : objects) 
						{
							toKeep.add(post.getObjectId());

							Marker oldMarker = mapMarkers.get(post.getObjectId());
							
							ParseGeoPoint pgp = post.getParseGeoPoint("location");
							
							LatLng ll = new LatLng(pgp.getLatitude(), pgp.getLongitude());

							MarkerOptions markerOpts = new MarkerOptions().position(ll);

							if (pgp.distanceInMilesTo(myPoint) > 20)
							{
								// Out of range.
								if (oldMarker != null)
								{
									oldMarker.remove();
								}
						    }
						    else
						    {
						    	if (oldMarker != null)
						    	{
						    		continue;
						    	}
						    	else
						    	{
									markerOpts.icon(BitmapDescriptorFactory.fromBitmap(bMarker));
						    	}
						    }

							Marker marker = map.addMarker(markerOpts);
						    
						    mapMarkers.put(post.getObjectId(), marker);
						}

						cleanUpMarkers(toKeep);
					}
				}
			});
		}
	}
	
	private void cleanUpMarkers(Set<String> markersToKeep) 
	{
		for (String objId : new HashSet<String>(mapMarkers.keySet())) 
		{
			if (!markersToKeep.contains(objId)) 
			{
				Marker marker = mapMarkers.get(objId);
		      
				marker.remove();
		      
				mapMarkers.get(objId).remove();
		      
				mapMarkers.remove(objId);
			}
		}
	}
}
