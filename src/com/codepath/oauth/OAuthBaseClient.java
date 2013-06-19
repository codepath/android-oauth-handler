package com.codepath.oauth;

import org.scribe.builder.api.Api;
import org.scribe.model.OAuthConstants;
import org.scribe.model.Token;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

public abstract class OAuthBaseClient {
    protected String baseUrl;
    protected Context context;
    protected OAuthAsyncHttpClient client;
    protected SharedPreferences prefs;
    protected SharedPreferences.Editor editor;
    protected OAuthAccessHandler accessHandler;
    
    public static OAuthBaseClient instance;
    
    public static OAuthBaseClient getInstance(Class<? extends OAuthBaseClient> klass, Context context) {
    	if (instance == null) {
    		try {
				instance = (OAuthBaseClient) klass.getConstructor(Context.class).newInstance(context);
			} catch (Exception e) {
				e.printStackTrace();
			}
    	}
    	return instance;
    }
    
    public OAuthBaseClient(Context c, Class<? extends Api> apiClass, String consumerUrl, String consumerKey, String consumerSecret, String callbackUrl) {
        this.baseUrl = consumerUrl;
        client = new OAuthAsyncHttpClient(apiClass, consumerKey,
                consumerSecret, callbackUrl, new OAuthAsyncHttpClient.OAuthTokenHandler() {
            @Override
            public void onReceivedRequestToken(Token requestToken, String authorizeUrl) {
            	editor.putString("request_token", requestToken.getToken());
                editor.putString("request_token_secret", requestToken.getSecret());
                editor.commit();
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(authorizeUrl + "&perms=delete"));
                OAuthBaseClient.this.context.startActivity(intent);
            }

            @Override
            public void onReceivedAccessToken(Token accessToken) {
                client.setAccessToken(accessToken);
                editor.putString(OAuthConstants.TOKEN, accessToken.getToken());
                editor.putString(OAuthConstants.TOKEN_SECRET, accessToken.getSecret());
                editor.commit();
                accessHandler.onLoginSuccess();
            }

            @Override
            public void onFailure(Exception e) {
                accessHandler.onLoginFailure(e);
            }
        });

        this.context = c;
        this.prefs = this.context.getSharedPreferences("OAuth", 0);
        this.editor = this.prefs.edit();
        // Set access token if already stored
        if (this.checkAccessToken() != null) {
            client.setAccessToken(this.checkAccessToken());
        }
    }

    // Fetches a request token and retrieve and authorization url
    // Should open a browser in onReceivedRequestToken once the url has been received
    public void connect() {
        client.fetchRequestToken();
    }

    // Retrieves access token given authorization url
    public void authorize(Uri uri, OAuthAccessHandler handler) {
        this.accessHandler = handler;
        if (checkAccessToken() == null && uri != null) {
            client.fetchAccessToken(getRequestToken(), uri);
        } else if (checkAccessToken() != null) { // already have access token
            this.accessHandler.onLoginSuccess();
        }
    }

    // Return access token if the token exists in preferences
    public Token checkAccessToken() {
        if (prefs.contains(OAuthConstants.TOKEN) && prefs.contains(OAuthConstants.TOKEN_SECRET)) {
            return new Token(prefs.getString(OAuthConstants.TOKEN, ""),
                    prefs.getString(OAuthConstants.TOKEN_SECRET, ""));
        } else {
            return null;
        }
    }
    
    protected OAuthAsyncHttpClient getClient() {
    	return client;
    }
    
    // Returns the request token stored during the request token phase
    protected Token getRequestToken() {
    	return new Token(prefs.getString("request_token", ""),
                prefs.getString("request_token_secret", ""));
    }

    // Assigns the base url for the API
    protected void setBaseUrl(String url) {
        this.baseUrl = url;
    }

    // Returns the full ApiUrl
    protected String getApiUrl(String path) {
       return this.baseUrl + "/" + path;
    }
    
    // Removes the access tokens (for signout)
    public void clearAccessToken() {
    	client.setAccessToken(null);
    	editor.remove(OAuthConstants.TOKEN);
    	editor.remove(OAuthConstants.TOKEN_SECRET);
        editor.commit();
    }

    // Defines the handler events for the oauth flow
    public static interface OAuthAccessHandler {
        public void onLoginSuccess();
        public void onLoginFailure(Exception e);
    }

}
