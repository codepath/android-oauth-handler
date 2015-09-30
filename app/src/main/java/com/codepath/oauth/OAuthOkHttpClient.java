package com.codepath.oauth;

import com.codepath.utils.AsyncSimpleTask;
import com.squareup.okhttp.OkHttpClient;

import android.net.Uri;

import oauth.signpost.OAuth;
import oauth.signpost.OAuthProvider;
import oauth.signpost.exception.OAuthExpectationFailedException;
import se.akerfeldt.okhttp.signpost.OkHttpOAuthConsumer;
import se.akerfeldt.okhttp.signpost.SigningInterceptor;

/*
 * OAuthAsyncHttpClient is responsible for managing the request and access token exchanges and then
 * signing all requests with the OAuth signature after access token has been retrieved and stored.
 * The client is based on AsyncHttpClient for async http requests and uses Scribe to manage the OAuth authentication.
 */
public class OAuthOkHttpClient extends OkHttpClient {
   
    private OAuthTokenHandler handler;
    private OkHttpOAuthConsumer consumer;
    private OAuthProvider provider;
    String callbackUrl;

    public static final String CODE = "code";

    // Requires the ApiClass, consumerKey, consumerSecret and callbackUrl along with the TokenHandler
    public OAuthOkHttpClient(String consumerKey,
            String consumerSecret, String callbackUrl,
            OAuthProvider oAuthProvider,
            OAuthTokenHandler handler) {
        this.handler = handler;
        if (callbackUrl == null) {
            this.callbackUrl = OAuth.OUT_OF_BAND;
        } else {
            this.callbackUrl = callbackUrl;
        }

        consumer = new OkHttpOAuthConsumer(consumerKey, consumerSecret);
        this.interceptors().add(new SigningInterceptor(consumer));
        provider = oAuthProvider;
    }

    // Get a request token and the authorization url
    // Once fetched, fire the onReceivedRequestToken for the request token handler
    // Works for both OAuth1.0a and OAuth2
    public void fetchRequestToken() {
        new AsyncSimpleTask(new AsyncSimpleTask.AsyncSimpleTaskHandler() {
            String authorizeUrl = null;
            Exception e = null;
            Token mToken;

            public void doInBackground() {
                try {
                	if (provider.isOAuth10a()) {
                            authorizeUrl = provider.retrieveRequestToken(consumer, callbackUrl);
                            mToken = new Token(consumer.getToken(), consumer.getTokenSecret());
                        } else {
                            authorizeUrl = provider.getAuthorizationWebsiteUrl();
                        }
                } catch (Exception e) {
                    this.e = e;
                }
            }

            public void onPostExecute() {
                if (e != null) {
                    handler.onFailure(e);
                } else {
                    handler.onReceivedRequestToken(mToken, authorizeUrl);
                }
            }
        });
    }

    // Get the access token by exchanging the requestToken to the defined URL
    // Once receiving the access token, fires the onReceivedAccessToken method on the handler
    public void fetchAccessToken(final Token requestToken, final Uri uri) {

        new AsyncSimpleTask(new AsyncSimpleTask.AsyncSimpleTaskHandler() {
            Exception e = null;
  
            public void doInBackground() {
            	// Fetch the verifier code from redirect url parameters
        		Uri authorizedUri = uri;
        		String oauth_verifier = null;
        		if (authorizedUri.getQuery().contains(CODE)) {
        			oauth_verifier = authorizedUri.getQueryParameter(CODE);
        		} else if (authorizedUri.getQuery().contains(OAuth.OAUTH_VERIFIER)) {
        			oauth_verifier = authorizedUri.getQueryParameter(OAuth.OAUTH_VERIFIER);
        		}

        		// Use verifier token to fetch access token
            	try {
                    if (oauth_verifier != null) {
                        provider.retrieveAccessToken(consumer, oauth_verifier);
                    } else { // verifier was null
                	    throw new OAuthExpectationFailedException("No verifier code was returned with uri '" + uri + "' " +
                	    		"and access token cannot be retrieved");
                    }
                } catch (Exception e) {
                    this.e = e;
                }
            }

            public void onPostExecute() {
                if (e != null) {
                    handler.onFailure(e);
                } else {
                    Token accessToken = new Token(consumer.getToken(), consumer.getTokenSecret());
                    handler.onReceivedAccessToken(accessToken);
                }
            }
        });
    }
    
    public String getAccessToken() {
        return consumer.getToken();
    }

    public void clearAccessToken() {
        consumer.setTokenWithSecret("", "");
    }
    
    // Defines the interface handler for different token handlers
    public interface OAuthTokenHandler {
        public void onReceivedRequestToken(Token requestToken, String authorizeUrl);
        public void onReceivedAccessToken(Token accessToken);
        public void onFailure(Exception e);
    }
}