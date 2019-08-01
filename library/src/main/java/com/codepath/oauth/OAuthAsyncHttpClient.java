package com.codepath.oauth;

import com.codepath.asynchttpclient.AsyncHttpClient;

import okhttp3.OkHttpClient;
import se.akerfeldt.okhttp.signpost.OkHttpOAuthConsumer;
import se.akerfeldt.okhttp.signpost.SigningInterceptor;

public class OAuthAsyncHttpClient extends AsyncHttpClient {

    protected OAuthAsyncHttpClient(OkHttpClient httpClient) {
        super(httpClient);
    }

    public static OAuthAsyncHttpClient create(String consumerKey, String consumerSecret, String tokenKey,
                                String tokenSecret) {
        OkHttpOAuthConsumer consumer = new OkHttpOAuthConsumer(consumerKey, consumerSecret);
        consumer.setTokenWithSecret(tokenKey, tokenSecret);
        OkHttpClient httpClient = new OkHttpClient.Builder().addInterceptor(new SigningInterceptor(consumer)).build();

        OAuthAsyncHttpClient asyncHttpClient = new OAuthAsyncHttpClient(httpClient);
        return asyncHttpClient;
    }

}
