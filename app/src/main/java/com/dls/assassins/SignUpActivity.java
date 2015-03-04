package com.dls.assassins;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import com.parse.ParseUser;
import com.parse.SignUpCallback;

public class SignUpActivity extends Activity 
{
	protected EditText mPassword;
	protected EditText mConfirmPassword;
	protected EditText mEmail;
	protected Button mSignUpButton;

	@Override protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
		setContentView(R.layout.activity_sign_up);
		
		mPassword = (EditText)findViewById(R.id.editPassword);
		
		mConfirmPassword = (EditText)findViewById(R.id.editConfirmPassword);
		
		mEmail = (EditText)findViewById(R.id.editEmail);
		
		mSignUpButton = (Button)findViewById(R.id.btnSignup);
		
		mSignUpButton.setOnClickListener(new View.OnClickListener()
		{
			@Override public void onClick(View v) 
			{
				String strPassword = mPassword.getText().toString();
				String strConfirmPassword = mConfirmPassword.getText().toString();
				String strEmail = mEmail.getText().toString();
				
				strPassword = strPassword.trim();
				strConfirmPassword = strConfirmPassword.trim();
				strEmail = strEmail.trim();
				
				AlertDialog.Builder builder = new AlertDialog.Builder(SignUpActivity.this);
				
				builder.setTitle(R.string.signup_error_title);
				
				builder.setPositiveButton(android.R.string.ok, null);
				
				if (!strPassword.equals(strConfirmPassword))
				{
					builder.setMessage(R.string.mismatch_password_message);
					
					AlertDialog dialog = builder.create();
					
					dialog.show();
				}
				else if (strPassword.isEmpty() || strEmail.isEmpty())
				{
					builder.setMessage(R.string.signup_error_message);
					
					AlertDialog dialog = builder.create();
					
					dialog.show();
				}
				else if (!isValidEmail(strEmail))
				{
					builder.setMessage(R.string.invalid_email_address_message);
					
					AlertDialog dialog = builder.create();
					
					dialog.show();
				}
				else
				{
					// Create the new user.
					setProgressBarIndeterminateVisibility(true);
					
					ParseUser newUser = new ParseUser();
					
					newUser.setUsername(strEmail);
					
					newUser.setEmail(strEmail);
					
					newUser.setPassword(strPassword);
					
					newUser.signUpInBackground(new SignUpCallback()
					{
						@Override public void done(com.parse.ParseException e)
						{
							setProgressBarIndeterminateVisibility(false);
							
							if (e == null)
							{
								Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
								
								intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								
								intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
								
								startActivity(intent);
							}
							else
							{
								AlertDialog.Builder builder = new AlertDialog.Builder(SignUpActivity.this);
								
								builder.setTitle(R.string.signup_error_title);
								
								builder.setPositiveButton(android.R.string.ok, null);
								
								builder.setMessage(e.getMessage());
								
								AlertDialog dialog = builder.create();
								
								dialog.show();
							}
						}
					});
				}
			}
		});
	}
	
	public final static boolean isValidEmail(CharSequence target) 
	{
        if (TextUtils.isEmpty(target))
        {
        	return false;
        } 
        else
        {
        	return android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
        }
    }  


}
