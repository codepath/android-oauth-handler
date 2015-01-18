package com.codepath.oauth;

import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;

import com.codepath.utils.GenericsUtil;


public abstract class OAuthLoginActivity<T extends OAuthBaseClient> extends FragmentActivity
		implements OAuthBaseClient.OAuthAccessHandler {

	private T client;
	
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
		Class<T> clientClass = getClientClass();
		// Extracts the authenticated url data after the user 
		// authorizes the OAuth app in the browser 
		Uri uri = getIntent().getData();

		try {
			client = (T) OAuthBaseClient.getInstance(clientClass, this);
			client.authorize(uri, this); // fetch access token (if needed)
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public T getClient() {
		return client;
	}

	@SuppressWarnings("unchecked")
	private Class<T> getClientClass() {
		return (Class<T>) GenericsUtil.getTypeArguments(OAuthLoginActivity.class, this.getClass()).get(0);
	}
}

/*
 * 1) Subclass OAuthBaseClient like TwitterClient 
 * 2) Subclass OAuthLoginActivity<TwitterClient> 
 * 3) Invoke .login 
 * 4) Optionally override 
 *   a) onLoginSuccess 
 *   b) onLoginFailure(Exception e) 
 * 5) In other activities that need the client 
 *   a) c = TwitterClient.getSharedClient() 
*    b) c.getTimeline(...)
 * 6) Modify AndroidManifest.xml to add an IntentFilter w/ the callback URL
 * defined in the OAuthBaseClient.
 */
