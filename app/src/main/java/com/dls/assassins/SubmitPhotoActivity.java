package com.dls.assassins;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Address;
import android.location.Geocoder;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Images.Media;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.SaveCallback;


public class SubmitPhotoActivity extends Activity
{
	public static final String TAG = SubmitPhotoActivity.class.getSimpleName();
	private ImageView mImageView;
	protected Uri mMediaUri;
	protected Intent returnIntent = new Intent();
	protected ParseUser mCurrentUser;
	private double dLongitude;
	private double dLatitude;
	private ParseGeoPoint point;
	TextView mAddress1;
	TextView mAddress2;

	@Override protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
	
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		setContentView(R.layout.activity_submit_photo);
		
		mImageView = (ImageView)findViewById(R.id.imagetosubmit);

		mMediaUri = getIntent().getData();
		
		checkPicOrientation();

		if (mMediaUri != null)
		{
			mImageView.setImageURI(mMediaUri);
		}
		
		dLongitude = getIntent().getDoubleExtra("longitude", 0);
		
		dLatitude = getIntent().getDoubleExtra("latitude", 0);
		
		point = new ParseGeoPoint(dLatitude, dLongitude);
		
		mAddress1 = (TextView)findViewById(R.id.tvAddress1);
		
		mAddress2 = (TextView)findViewById(R.id.tvAddress2);
	}
	
	protected void checkPicOrientation()
	{
		BitmapFactory.Options bounds = new BitmapFactory.Options();
        
		bounds.inJustDecodeBounds = true;
        
		BitmapFactory.decodeFile(mMediaUri.getPath(), bounds);
        
		ExifInterface exif;
		String orientString;
		
		try
		{
			Bitmap  bm = Media.getBitmap(this.getContentResolver(), mMediaUri);
	        
			exif = new ExifInterface(mMediaUri.getPath());

			orientString = exif.getAttribute(ExifInterface.TAG_ORIENTATION);

			int orientation = orientString != null ? Integer.parseInt(orientString) : ExifInterface.ORIENTATION_NORMAL;
	        
			int rotationAngle = 0;
	        
			if (orientation == ExifInterface.ORIENTATION_ROTATE_90)
				rotationAngle = 90;
	        
			if (orientation == ExifInterface.ORIENTATION_ROTATE_180)
				rotationAngle = 180;
	        
			if (orientation == ExifInterface.ORIENTATION_ROTATE_270)
				rotationAngle = 270;

			if (rotationAngle != 0)
			{
		        Matrix matrix = new Matrix();
		        
		        matrix.setRotate(rotationAngle, (float) bm.getWidth() / 2, (float) bm.getHeight() / 2);
		        
		        Bitmap rotatedBitmap = Bitmap.createBitmap(bm, 0, 0, bounds.outWidth, bounds.outHeight, matrix, true);
		        
	            File file = new File(mMediaUri.getPath());
	            
	            FileOutputStream fOut;
	            
	            fOut = new FileOutputStream(file);
	            
	            rotatedBitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
	            
	            fOut.flush();
	            
	            fOut.close();
	            
	            rotatedBitmap.recycle();
			}
        }
		catch (IOException e)
		{
		}
	}
	
	@Override protected void onResume() 
	{
		super.onResume();

		mCurrentUser = ParseUser.getCurrentUser();
		
		Geocoder geocoder = new Geocoder(this, Locale.getDefault());
		
		List<Address> addresses;
		
		try
		{
			addresses = geocoder.getFromLocation(dLatitude, dLongitude, 1);
			
			if (addresses != null && !addresses.isEmpty())
			{
				mAddress1.setText(addresses.get(0).getAddressLine(0));
	
				mAddress2.setText(addresses.get(0).getAddressLine(1));
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	@Override public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.submit_photo, menu);
	
		return true;
	}
	
	protected ParseObject createMessage()
	{
		ParseObject message = new ParseObject(ParseConstants.CLASS_SIGNS);
		message.put(ParseConstants.KEY_USER, ParseUser.getCurrentUser());
		message.put(ParseConstants.KEY_NOTES, "This is the notes field.");
		ParseGeoPoint pt = new ParseGeoPoint(dLatitude, dLongitude);
		message.put(ParseConstants.KEY_LOCATION, pt);
		
		byte[] fileBytes = FileHelper.getByteArrayFromFile(this, mMediaUri);

		if (fileBytes != null) 
		{
			fileBytes = FileHelper.reduceImageForUpload(fileBytes);

			String fileName = FileHelper.getFileName(this, mMediaUri, ParseConstants.TYPE_IMAGE);
			
			ParseFile file = new ParseFile(fileName, fileBytes);
			
			message.put(ParseConstants.KEY_FILE, file);
		}
		
		return message;
	}

	@Override public boolean onOptionsItemSelected(MenuItem item) 
	{
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
	
		if (id == R.id.action_send) 
		{
			setProgressBarIndeterminateVisibility(true);

			ParseObject message = createMessage();
			
			message.saveInBackground(new SaveCallback()
			{
				@Override public void done(ParseException e)
				{
					setProgressBarIndeterminateVisibility(false);
					
					if (e == null)
					{
						SubmitPhotoActivity.this.setResult(RESULT_OK, returnIntent);
					}
					else
					{
						SubmitPhotoActivity.this.setResult(RESULT_CANCELED, returnIntent);

						Log.e(TAG, e.getMessage());
						
						AlertDialog.Builder builder = new AlertDialog.Builder(SubmitPhotoActivity.this);
						
						builder.setMessage(e.getMessage()).setTitle(R.string.signup_error_title).setPositiveButton(android.R.string.ok, null);
						
						AlertDialog dialog = builder.create();
						
						dialog.show();
					}
					
					SubmitPhotoActivity.this.finish();
				}
			});
		}
		
		return super.onOptionsItemSelected(item);
	}
}
