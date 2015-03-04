package com.dls.assassins;

import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseUser;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class LoginActivity extends Activity 
{
	protected TextView mSignUpTextView;
	protected EditText mPassword;
	protected EditText mEmail;
	protected Button mLoginButton;
	
	@Override protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
		setContentView(R.layout.activity_login);
		
		mSignUpTextView = (TextView)findViewById(R.id.signupText);
		
		mSignUpTextView.setOnClickListener(new View.OnClickListener()
		{
			@Override public void onClick(View v) 
			{
				Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
				
				startActivity(intent);
			}
		});

		mPassword = (EditText)findViewById(R.id.editPassword);
		
		mEmail = (EditText)findViewById(R.id.editUsername);
		
		mLoginButton = (Button)findViewById(R.id.btnLogin);
		
		mLoginButton.setOnClickListener(new View.OnClickListener()
		{
			@Override public void onClick(View v) 
			{
				String strPassword = mPassword.getText().toString();
				String strEmail = mEmail.getText().toString();
				
				strPassword = strPassword.trim();
				strEmail = strEmail.trim();
				
				AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
				
				builder.setTitle(R.string.signup_error_title);
				
				builder.setPositiveButton(android.R.string.ok, null);
				
				if (strPassword.isEmpty() || strEmail.isEmpty())
				{
					builder.setMessage(R.string.signup_error_message);
					
					AlertDialog dialog = builder.create();
					
					dialog.show();
				}
				else
				{
					setProgressBarIndeterminateVisibility(true);
					
					ParseUser.logInInBackground(strEmail, strPassword, new LogInCallback()
					{
						@Override public void done(ParseUser user, ParseException e) 
						{
							setProgressBarIndeterminateVisibility(false);
							
							if (e == null)
							{
								Intent intent = new Intent(LoginActivity.this, MainActivity.class);
								
								intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								
								intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
								
								startActivity(intent);
							}
							else
							{
								AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
								
								builder.setTitle(R.string.signup_error_title);
								
								builder.setPositiveButton(android.R.string.ok, null);
								
								builder.setMessage(e.getMessage());
								
								AlertDialog dialog = builder.create();
								
								dialog.show();
							}
						}
					} );
				}
			}
		});
	}


}
