package com.codepath.oauth;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.DefaultApi10a;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuth1RequestToken;
import com.github.scribejava.core.model.OAuthAsyncRequestCallback;
import com.github.scribejava.core.model.OAuthConstants;
import com.github.scribejava.core.oauth.OAuth10aService;
import com.github.scribejava.httpclient.okhttp.OkHttpHttpClientConfig;

public class OAuth1Client {
    protected String baseUrl;
    protected Context context;
    protected SharedPreferences prefs;
    protected SharedPreferences.Editor editor;
    protected OAuthAccessHandler accessHandler;
    protected String callbackUrl;
    protected int requestIntentFlags = -1;

    private static final String OAUTH1_REQUEST_TOKEN = "request_token";
    private static final String OAUTH1_REQUEST_TOKEN_SECRET = "request_token_secret";

    OAuth10aService service;

    public static String DEBUG = "debug";

    public OAuth1Client(Context c, DefaultApi10a apiInstance, String consumerUrl, String consumerKey, String consumerSecret, String callbackUrl) {
        this.baseUrl = consumerUrl;
        this.callbackUrl = callbackUrl;

        if (callbackUrl == null) { callbackUrl = OAuthConstants.OUT_OF_BAND; };
        service = new ServiceBuilder()
                .apiKey(consumerKey)
                .apiSecret(consumerSecret).callback(callbackUrl)
                .httpClientConfig(OkHttpHttpClientConfig.defaultConfig())
                .debug()
                .build(apiInstance);

        this.context = c;
        // Store preferences namespaced by the class and consumer key used
        this.prefs = this.context.getSharedPreferences("OAuth_" + apiInstance.getClass().getSimpleName() + "_" + consumerKey, 0);
        this.editor = this.prefs.edit();
    }

    // Fetches a request token and retrieve and authorization url
    // Should open a browser in onReceivedRequestToken once the url has been received
    public void connect() {
        service.getRequestTokenAsync(new OAuthAsyncRequestCallback<OAuth1RequestToken>() {
            @Override
            public void onCompleted(OAuth1RequestToken responseToken) {
                if (responseToken != null) {
                    // put in vault
                    editor.putString(OAUTH1_REQUEST_TOKEN, responseToken.getToken());
                    editor.putString(OAUTH1_REQUEST_TOKEN_SECRET, responseToken.getTokenSecret());
                    editor.commit();
                }

                String authorizeUrl = service.getAuthorizationUrl(responseToken);
                // Launch the authorization URL in the browser
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(authorizeUrl));
                if (requestIntentFlags != -1) {
                    intent.setFlags(requestIntentFlags);
                }
                OAuth1Client.this.context.startActivity(intent);
            }

            @Override
            public void onThrowable(Throwable e) {
                Log.e(DEBUG, "Error trying to connect" + e.toString());
                e.printStackTrace();

            }
        });
    }

    // Retrieves access token given authorization url
    public void authorize(Uri authorizedUri, final OAuthAccessHandler handler) {
        if (checkAccessToken() == null && authorizedUri != null) {
            // TODO: check UriServiceCallback with intent:// scheme
            if (authorizedUri.getQuery().contains(OAuthConstants.VERIFIER)) {
                String oauth_verifier = authorizedUri.getQueryParameter(OAuthConstants.VERIFIER);

                service.getAccessTokenAsync(getOAuth1RequestToken(), oauth_verifier,
                        new OAuthAsyncRequestCallback<OAuth1AccessToken>() {

                            @Override
                            public void onCompleted(OAuth1AccessToken oAuth1AccessToken) {
                                if (oAuth1AccessToken != null) {
                                    editor.putInt(OAuthConstants.VERSION, 1);
                                    editor.putString(OAuthConstants.TOKEN,
                                            oAuth1AccessToken.getToken());
                                    editor.putString(OAuthConstants.TOKEN_SECRET,
                                            oAuth1AccessToken.getTokenSecret());
                                    editor.commit();

                                    handler.onLoginSuccess();

                                }
                            }

                            @Override
                            public void onThrowable(Throwable e) {
                                Log.d(DEBUG, "Exception" + e.toString());
                                e.printStackTrace();
                                handler.onLoginFailure(new Exception());
                            }

                        });
            }
        } else {
            handler.onLoginSuccess();
        }
    }

    // Return access token if the token exists in preferences
    public OAuth1AccessToken checkAccessToken() {
        int oAuthVersion = prefs.getInt(OAuthConstants.VERSION, 0);

        if (oAuthVersion == 1 && prefs.contains(OAuthConstants.TOKEN) && prefs.contains(OAuthConstants.TOKEN_SECRET)) {
            return new OAuth1AccessToken(prefs.getString(OAuthConstants.TOKEN, ""),
                    prefs.getString(OAuthConstants.TOKEN_SECRET, ""));
        }
        return null;
    }

    // Returns the request token stored during the request token phase
    protected OAuth1RequestToken getOAuth1RequestToken() {
        return new OAuth1RequestToken(prefs.getString(OAUTH1_REQUEST_TOKEN, ""),
                prefs.getString(OAUTH1_REQUEST_TOKEN_SECRET, ""));
    }

    // Assigns the base url for the API
    protected void setBaseUrl(String url) {
        this.baseUrl = url;
    }

    // Returns the full ApiUrl
    protected String getApiUrl(String path) {
        return this.baseUrl + "/" + path;
    }

    // Removes the access tokens (for signing out)
    public void clearAccessToken() {
        //client.setAccessToken(null);
        editor.remove(OAuthConstants.TOKEN);
        editor.remove(OAuthConstants.TOKEN_SECRET);
        editor.remove(OAuthConstants.REFRESH_TOKEN);
        editor.remove(OAuthConstants.SCOPE);
        editor.commit();
    }

    // Returns true if the client is authenticated; false otherwise.
    public boolean isAuthenticated() {
        //return client.getAccessToken() != null;
        return getOAuth1RequestToken() != null;
    }

    // Sets the flags used when launching browser to authenticate through OAuth
    public void setRequestIntentFlags(int flags) {
        this.requestIntentFlags = flags;
    }

    // Defines the handler events for the OAuth flow
    public static interface OAuthAccessHandler {
        public void onLoginSuccess();

        public void onLoginFailure(Exception e);
    }

}
