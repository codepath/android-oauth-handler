package com.codepath.oauth;

import android.content.Context;

public abstract interface OAuth1ClientProvider {

    public OAuth1Client getClient(Context context);
}
