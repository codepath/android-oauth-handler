package com.codepath.oauth;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.codepath.utils.GenericsUtil;

public abstract class OAuthLoginFragment<T extends OAuthBaseClient> extends Fragment implements
		OAuthBaseClient.OAuthAccessHandler {
  
	private T client;

	@SuppressWarnings("unchecked")
	@Override
	public void onActivityCreated(Bundle saved) {
		super.onActivityCreated(saved);

		// Fetch the uri that was passed in (which exists if this is being returned from authorization flow)
		Uri uri = getActivity().getIntent().getData();
		// Fetch the client class this fragment is responsible for.
		Class<T> clientClass = getClientClass();

		try {
			client = (T) OAuthBaseClient.getInstance(clientClass, getActivity());
			client.authorize(uri, this); // fetch access token (if not stored)
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
