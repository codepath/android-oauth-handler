package com.codepath.oauth;

import com.codepath.asynchttpclient.AsyncHttpClient;
import com.facebook.stetho.okhttp3.StethoInterceptor;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuth2AccessToken;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import se.akerfeldt.okhttp.signpost.OkHttpOAuthConsumer;
import se.akerfeldt.okhttp.signpost.SigningInterceptor;

public class OAuthAsyncHttpClient extends AsyncHttpClient {

    protected OAuthAsyncHttpClient(OkHttpClient httpClient) {
        super(httpClient);
    }

    public static OAuthAsyncHttpClient create(String consumerKey, String consumerSecret, OAuth1AccessToken token) {
        OkHttpOAuthConsumer consumer = new OkHttpOAuthConsumer(consumerKey, consumerSecret);
        consumer.setTokenWithSecret(token.getToken(), token.getTokenSecret());
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addNetworkInterceptor(new StethoInterceptor())
                .addInterceptor(new SigningInterceptor(consumer)).build();

        OAuthAsyncHttpClient asyncHttpClient = new OAuthAsyncHttpClient(httpClient);
        return asyncHttpClient;
    }

    public static OAuthAsyncHttpClient create(final OAuth2AccessToken token) {
        final String bearer = String.format("Bearer %s", token.getAccessToken());

        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addNetworkInterceptor(new StethoInterceptor())
                .addInterceptor(new Interceptor() {
                                    @NotNull
                                    @Override
                                    public Response intercept(@NotNull Chain chain) throws IOException {
                                        Request originalRequest = chain.request();
                                        Request authedRequest = originalRequest.newBuilder().header("Authorization", bearer).build();
                                        return chain.proceed(authedRequest);
                                    }
                                }).build();

        OAuthAsyncHttpClient asyncHttpClient = new OAuthAsyncHttpClient(httpClient);
        return asyncHttpClient;
    }
}