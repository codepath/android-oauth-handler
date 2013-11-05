package com.codepath.oauth;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.codepath.utils.GenericsUtil;


public abstract class OAuthLoginActivity<T extends OAuthBaseClient> extends FragmentActivity
		implements OAuthBaseClient.OAuthAccessHandler {

	private T client;
	private Uri uri;

	@SuppressWarnings("unchecked")
	@Override
	protected void onResume() {
		super.onResume();

		Class<T> clientClass = getClientClass();
		// Extracts the authenticated url data after the user 
		// authorizes the OAuth app in the browser 

		try {
			client = (T) OAuthBaseClient.getInstance(clientClass, this);
			client.authorize(uri, this); // fetch access token (if needed)
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		uri = intent.getData();
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
