package com.codepath.oauth;

import com.codepath.asynchttpclient.AsyncHttpClient;
import com.facebook.stetho.okhttp3.StethoInterceptor;
import com.github.scribejava.core.model.OAuth1AccessToken;

import okhttp3.OkHttpClient;
import se.akerfeldt.okhttp.signpost.OkHttpOAuthConsumer;
import se.akerfeldt.okhttp.signpost.SigningInterceptor;

public class OAuthAsyncHttpClient extends AsyncHttpClient {

    protected OAuthAsyncHttpClient(OkHttpClient httpClient) {
        super(httpClient);
    }

    public static OAuthAsyncHttpClient create(String consumerKey, String consumerSecret, OAuth1AccessToken token) {
        OkHttpOAuthConsumer consumer = new OkHttpOAuthConsumer(consumerKey, consumerSecret);
        consumer.setTokenWithSecret(token.getToken(), token.getTokenSecret());
        OkHttpClient httpClient = new OkHttpClient.Builder().addNetworkInterceptor(new StethoInterceptor()).addInterceptor(new SigningInterceptor(consumer)).build();

        OAuthAsyncHttpClient asyncHttpClient = new OAuthAsyncHttpClient(httpClient);
        return asyncHttpClient;
    }

}