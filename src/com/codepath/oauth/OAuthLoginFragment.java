package com.codepath.oauth;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.codepath.utils.GenericsUtil;

public abstract class OAuthLoginFragment<T extends OAuthBaseClient> extends Fragment implements
		OAuthBaseClient.OAuthAccessHandler {
  
	private T client;

	@Override
	public void onCreate(Bundle saved) {
		super.onCreate(saved);

		Class<T> clientClass = getClientClass();
		Uri uri = getActivity().getIntent().getData();

		try {
			client = clientClass.getConstructor(Context.class).newInstance(getActivity());
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
		return (Class<T>) GenericsUtil.getTypeArguments(OAuthLoginFragment.class, this.getClass()).get(0);
	}
}
