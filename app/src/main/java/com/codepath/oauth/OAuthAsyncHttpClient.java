package com.codepath.oauth;

import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HttpContext;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.Api;
import org.scribe.exceptions.OAuthException;
import org.scribe.model.OAuthConstants;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import android.content.Context;
import android.net.Uri;

import com.codepath.utils.AsyncSimpleTask;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

/*
 * OAuthAsyncHttpClient is responsible for managing the request and access token exchanges and then
 * signing all requests with the OAuth signature after access token has been retrieved and stored.
 * The client is based on AsyncHttpClient for async http requests and uses Scribe to manage the OAuth authentication.
 */
public class OAuthAsyncHttpClient extends AsyncHttpClient {
   
	private Class<? extends Api> apiClass;
    private OAuthTokenHandler handler;
    private Token accessToken;
    private OAuthService service;

    // Requires the ApiClass, consumerKey, consumerSecret and callbackUrl along with the TokenHandler
    public OAuthAsyncHttpClient(Class<? extends Api> apiClass, String consumerKey, String consumerSecret, String callbackUrl,
                                OAuthTokenHandler handler) {
    	this.apiClass = apiClass;
        this.handler = handler;
        if (callbackUrl == null) { callbackUrl = OAuthConstants.OUT_OF_BAND; };
        this.service = new ServiceBuilder()
        	.provider(apiClass).apiKey(consumerKey)
        	.apiSecret(consumerSecret).callback(callbackUrl)
        	.build();
    }

    // Get a request token and the authorization url
    // Once fetched, fire the onReceivedRequestToken for the request token handler
    // Works for both OAuth1.0a and OAuth2
    public void fetchRequestToken() {
        new AsyncSimpleTask(new AsyncSimpleTask.AsyncSimpleTaskHandler() {
            String authorizeUrl = null;
            Exception e = null;
            Token requestToken;

            public void doInBackground() {
                try {
                	if (service.getVersion() == "1.0") {
                    	requestToken = service.getRequestToken();
                        authorizeUrl = service.getAuthorizationUrl(requestToken);
                	} else if (service.getVersion() == "2.0") {
                		authorizeUrl = service.getAuthorizationUrl(null);
                	}
                } catch (Exception e) {
                    this.e = e;
                }
            }

            public void onPostExecute() {
                if (e != null) {
                    handler.onFailure(e);
                } else {
                    handler.onReceivedRequestToken(requestToken, authorizeUrl);
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
        		if (authorizedUri.getQuery().contains(OAuthConstants.CODE)) {
        			oauth_verifier = authorizedUri.getQueryParameter(OAuthConstants.CODE);
        		} else if (authorizedUri.getQuery().contains(OAuthConstants.VERIFIER)) {
        			oauth_verifier = authorizedUri.getQueryParameter(OAuthConstants.VERIFIER);
        		}
        		
        		// Use verifier token to fetch access token
            	try {
                    if (oauth_verifier != null) {
                    	accessToken = service.getAccessToken(requestToken, new Verifier(oauth_verifier));
                    } else { // verifier was null
                	    throw new OAuthException("No verifier code was returned with uri '" + uri + "' " +
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
                    setAccessToken(accessToken);
                    handler.onReceivedAccessToken(accessToken);
                }
            }
        });
    }

    // Set the access token used for signing requests
    public void setAccessToken(Token accessToken) {
    	if (accessToken == null) {
    		this.accessToken = null;
    	} else {
    		this.accessToken = accessToken;
    	}
    }
    
    public Token getAccessToken() {
    	return this.accessToken;
    }
    
    // Send scribe signed request based on the async http client to construct a signed request
    // Accepts an HttpEntity which has the underlying entity for the request params
    protected void sendRequest(DefaultHttpClient client, HttpContext httpContext, HttpUriRequest uriRequest,
            String contentType, AsyncHttpResponseHandler responseHandler, Context context) {
    	if (this.service != null && accessToken != null) {
            try {
            	ScribeRequestAdapter adapter = new ScribeRequestAdapter(uriRequest);
                this.service.signRequest(accessToken, adapter);
            	super.sendRequest(client, httpContext, uriRequest, contentType, responseHandler, context);
            } catch (Exception e) {
            	e.printStackTrace();
            }
        } else if (accessToken == null) {
        	throw new OAuthException("Cannot send unauthenticated requests for " + apiClass.getSimpleName() + " client. Please attach an access token!");
        }
    	
    }
    
    // Defines the interface handler for different token handlers
    public interface OAuthTokenHandler {
        public void onReceivedRequestToken(Token requestToken, String authorizeUrl);
        public void onReceivedAccessToken(Token accessToken);
        public void onFailure(Exception e);
    }
}