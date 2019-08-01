package com.codepath.oauth;

import android.content.Intent;
import android.net.Uri;

import androidx.appcompat.app.AppCompatActivity;

public abstract class OAuthLoginActivity extends
		AppCompatActivity
		implements OAuth1Client.OAuthAccessHandler, OAuth1ClientProvider {

	// Use this to properly assign the new intent with callback code
	// for activities with a "singleTask" launch mode
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
	}
	
	// Extract the uri data and call authorize to retrieve access token
	// This is why after the browser redirects to the app, authentication is completed
	@SuppressWarnings("unchecked")
	@Override
	protected void onResume() {
		super.onResume();
		// Extracts the authenticated url data after the user
		// authorizes the OAuth app in the browser 
		Uri uri = getIntent().getData();

		try {
			OAuth1Client client = getClient(this);
			client.authorize(uri, this);  // fetch access token (if needed)
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

/*
 * 1) Instantiate an OAuth client through OAuthAsyncHttpClient
 * 2) Subclass OAuthLoginActivity and implement getClient()
 * 3) Invoke .connect()
 * 4) Optionally override 
 *   a) onLoginSuccess 
 *   b) onLoginFailure(Exception e) 
 * 5) In other activities that need the client 
 *   a) c = TwitterClient.getSharedClient() 
*    b) c.getTimeline(...)
 * 6) Modify AndroidManifest.xml to add an IntentFilter w/ the callback URL
 * defined in the OAuthBaseClient.
 */
