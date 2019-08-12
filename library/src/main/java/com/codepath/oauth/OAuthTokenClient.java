package com.codepath.oauth;

import android.net.Uri;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.BaseApi;
import com.github.scribejava.core.exceptions.OAuthException;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuth1RequestToken;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthAsyncRequestCallback;
import com.github.scribejava.core.model.OAuthConstants;
import com.github.scribejava.core.model.Token;
import com.github.scribejava.core.oauth.OAuth10aService;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.github.scribejava.core.oauth.OAuthService;
import com.github.scribejava.httpclient.okhttp.OkHttpHttpClientConfig;

/*
 * OAuthTokenClient is responsible for managing the request and access token exchanges and then
 * signing all requests with the OAuth signature after access token has been retrieved and stored.
 * The client is based on AsyncHttpClient for async http requests and uses Scribe to manage the OAuth authentication.
 */
public class OAuthTokenClient {

    private BaseApi apiInstance;
    private OAuthTokenHandler handler;
    private Token accessToken;
    private OAuthService service;

    // Requires the apiClass, consumerKey, consumerSecret and callbackUrl along with the TokenHandler
    public OAuthTokenClient(BaseApi apiInstance, String consumerKey, String consumerSecret, String callbackUrl,
                            OAuthTokenHandler handler) {
        this.apiInstance = apiInstance;
        this.handler = handler;
        if (callbackUrl == null) { callbackUrl = OAuthConstants.OUT_OF_BAND; };
        this.service = new ServiceBuilder()
                .apiKey(consumerKey)
                .apiSecret(consumerSecret).callback(callbackUrl)
                .httpClientConfig(OkHttpHttpClientConfig.defaultConfig())
                .build(apiInstance);
    }

    // Get a request token and the authorization url
    // Once fetched, fire the onReceivedRequestToken for the request token handler
    // Works for both OAuth1.0a and OAuth2
    public void fetchRequestToken() {
        if (service.getVersion() == "1.0") {
            final OAuth10aService oAuth10aService = (OAuth10aService) service;
            oAuth10aService.getRequestTokenAsync(new OAuthAsyncRequestCallback<OAuth1RequestToken>() {
                @Override
                public void onCompleted(OAuth1RequestToken requestToken) {
                    String authorizeUrl = oAuth10aService.getAuthorizationUrl((OAuth1RequestToken) requestToken);
                    handler.onReceivedRequestToken(requestToken, authorizeUrl, service.getVersion());

                }

                @Override
                public void onThrowable(Throwable t) {
                    handler.onFailure(new Exception(t.getMessage()));
                }
            });
        }
        if (service.getVersion() == "2.0") {
            OAuth20Service oAuth20Service = (OAuth20Service) service;
            String authorizeUrl = oAuth20Service.getAuthorizationUrl(null);
            handler.onReceivedRequestToken(null, authorizeUrl, service.getVersion());
        }
    }

    // Get the access token by exchanging the requestToken to the defined URL
    // Once receiving the access token, fires the onReceivedAccessToken method on the handler
    public void fetchAccessToken(final Token requestToken, final Uri uri) {

        Uri authorizedUri = uri;

        if (service.getVersion() == "1.0") {
            // Use verifier token to fetch access token

            if (authorizedUri.getQuery().contains(OAuthConstants.VERIFIER)) {
                String oauth_verifier = authorizedUri.getQueryParameter(OAuthConstants.VERIFIER);
                OAuth1RequestToken oAuth1RequestToken = (OAuth1RequestToken) requestToken;
                OAuth10aService oAuth10aService = (OAuth10aService) service;

                oAuth10aService.getAccessTokenAsync(oAuth1RequestToken, oauth_verifier,
                        new OAuthAsyncRequestCallback<OAuth1AccessToken>() {

                            @Override
                            public void onCompleted(OAuth1AccessToken oAuth1AccessToken) {
                                setAccessToken(oAuth1AccessToken);
                                handler.onReceivedAccessToken(oAuth1AccessToken, service.getVersion());
                            }

                            @Override
                            public void onThrowable(Throwable e) {
                                handler.onFailure(new OAuthException(e.getMessage()));
                            }
                        });

            }
            else { // verifier was null
                throw new OAuthException("No verifier code was returned with uri '" + uri + "' " +
                        "and access token cannot be retrieved");
            }
        } else if (service.getVersion() == "2.0") {
            if (authorizedUri.getQuery().contains(OAuthConstants.CODE)) {
                String code = authorizedUri.getQueryParameter(OAuthConstants.CODE);
                OAuth20Service oAuth20Service = (OAuth20Service) service;
                oAuth20Service.getAccessToken(code, new OAuthAsyncRequestCallback<OAuth2AccessToken>() {
                    @Override
                    public void onCompleted(OAuth2AccessToken accessToken) {
                        setAccessToken(accessToken);
                        handler.onReceivedAccessToken(accessToken, service.getVersion());

                    }

                    @Override
                    public void onThrowable(Throwable t) {

                    }
                });
            }
            else { // verifier was null
                handler.onFailure(new OAuthException("No code was returned with uri '" + uri + "' " +
                        "and access token cannot be retrieved"));
            }
        }
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

    // Defines the interface handler for different token handlers
    public interface OAuthTokenHandler {
        public void onReceivedRequestToken(Token requestToken, String authorizeUrl, String oAuthVersion);
        public void onReceivedAccessToken(Token accessToken, String oAuthVersion);
        public void onFailure(Exception e);
    }
}