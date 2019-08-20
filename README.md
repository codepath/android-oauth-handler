# CodePath OAuth Handler [![Build Status](https://travis-ci.org/codepath/android-oauth-handler.svg?branch=master)](https://travis-ci.org/codepath/android-oauth-handler)

This library is an Android library for managing OAuth requests with an extremely easy
approach that keeps the details of the OAuth process abstracted from the end-user developer.

This library leverages a few key libraries underneath to power the functionality:

 * [scribe-java](https://github.com/scribejava/scribejava) - Simple OAuth library for handling the authentication flow.
 * [Android Async HTTP](https://github.com/codepath/asynchttpclient) - Simple asynchronous HTTP requests with JSON parsing.

## Installation

Inside your project's root `build.gradle`, make sure the jCenter repository is added:

```gradle
allprojects {
    repositories {
        jcenter()
    }
}
```

Next, add this line to your `app/build.gradle` file:

```gradle
dependencies {
    compile 'com.codepath.libraries:android-oauth-handler:2.1.3'
}
```

If you want an easier way to get setup with this library, try downloading the
[android-rest-client-template](https://github.com/thecodepath/android-rest-client-template/archive/master.zip)
instead and using that as the template for your project.

## Getting Started

This library is very simple to use and simply requires you to create an Activity that is used for authenticating with OAuth and ultimately give your application access to an authenticated API.

### Creating a REST Client

The first step is to create a REST Client that will be used to access the authenticated APIs
within your application. A REST Client is defined in the structure below:

```java
public class TwitterClient extends OAuthBaseClient {
    public static final BaseApi REST_API_INSTANCE = TwitterApi.instance();
    public static final String REST_URL = "https://api.twitter.com/1.1";
    public static final String REST_CONSUMER_KEY = "SOME_KEY_HERE";
    public static final String REST_CONSUMER_SECRET = "SOME_SECRET_HERE";
    public static final String REST_CALLBACK_URL = "oauth://arbitraryname.com";

    public TwitterClient(Context context) {
        super(context, REST_API_INSTANCE, REST_URL,
          REST_CONSUMER_KEY, REST_CONSUMER_SECRET, null, REST_CALLBACK_URL);
    }

    // ENDPOINTS BELOW

    public void getHomeTimeline(int page, JsonHttpResponseHandler handler) {
      String apiUrl = getApiUrl("statuses/home_timeline.json");
      RequestParams params = new RequestParams();
      params.put("page", String.valueOf(page));
      client.get(apiUrl, params, handler);
    }
}
```

Configure the `REST_API_INSTANCE`, `REST_URL`, `REST_CONSUMER_KEY`, `REST_CONSUMER_SECRET` based on the values needed to connect to your particular API. The `REST_URL` should be the base URL used for connecting to the API (i.e `https://api.twitter.com`). The `REST_API_INSTANCE` should be the instance defining the service you wish to connect to. Check out the [full list of services](https://github.com/scribejava/scribejava/tree/master/scribejava-apis/src/main/java/com/github/scribejava/apis) you can select (i.e `FlickrApi.instance()`).

Make sure that the project's `AndroidManifest.xml` has the appropriate `intent-filter` tags that correspond
with the `REST_CALLBACK_URL` defined in the client:

```xml
<activity ...>
  <intent-filter>
      <action android:name="android.intent.action.VIEW" />

      <category android:name="android.intent.category.DEFAULT" />
      <category android:name="android.intent.category.BROWSABLE" />

      <data
          android:scheme="oauth"
          android:host="arbitraryname.com"
      />
  </intent-filter>
</activity>
```

If the manifest does not have a matching `intent-filter` then the OAuth flow will not work.

#### Customizations for Twitter

The above instructions cover most OAuth integrations. When using this template to build a Twitter integration, you will need to make a few changes.

In `AndroidManifest.xml`, use the OAuth scheme name `x-oauthflow-twitter`:

```xml
<activity ...>
  <intent-filter ...>
      <data
          android:scheme="x-oauthflow-twitter"
          android:host="arbitraryname.com"
      />
  </intent-filter>
</activity>
```

In `TwitterClient.java`, the value for `REST_CALLBACK_URL` must also use the `x-oauthflow-twitter` protocol.

```java
  public static final String REST_CALLBACK_URL = "x-oauthflow-twitter://arbitraryname.com";
```

Note that the `arbitraryname.com` value can be any string. If you leave the setting unchecked for callback locking in your Twitter developer settings, then you can use any placeholder value. The callback host value in `AndroidManifest.xml` must correspond to the part after the `://` in `REST_CALLBACK_URL`, but it need not match the setting in your Twitter developer settings page.

### Creating a LoginActivity

The next step to add support for authenticating with a service is to create a `LoginActivity` which is responsible for the task:

```java
public class LoginActivity extends OAuthLoginActivity<FlickrClient> {
  // This fires once the user is authenticated, or fires immediately
  // if the user is already authenticated.

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_login);
  }

  @Override
  public void onLoginSuccess() {
    Intent i = new Intent(this, PhotosActivity.class);
    startActivity(i);
  }

  // Fires if the authentication process fails for any reason.
  @Override
  public void onLoginFailure(Exception e) {
      e.printStackTrace();
  }

  // Method to be called to begin the authentication process
  // assuming user is not authenticated.
  // Typically used as an event listener for a button for the user to press.
  public void loginToRest(View view) {
      getClient().connect();
  }
}
```

A few notes for your `LoginActivity`:

 * Your activity must extend from `OAuthLoginActivity<SomeRestClient>`
 * Your activity must implement `onLoginSuccess` and `onLoginFailure`
 * The `onLoginSuccess` should launch an "authenticated" activity.
 * The activity should have a button or other view a user can press to trigger authentication
   * Authentication is initiated by invoking `getClient().connect()` within the LoginActivity.

In more advanced cases where you want to authenticate **multiple services from a single activity**, check out the related
[guide for using OAuthLoginFragment](https://github.com/thecodepath/android-oauth-handler/wiki/Advanced-Use-with-OAuthLoginFragment).

### Using the REST Client

These endpoint methods will automatically execute asynchronous requests signed with the authenticated access token anywhere your application. To use JSON endpoints, simply invoke the method
with a `JsonHttpResponseHandler` handler:

```java
// SomeActivity.java
RestClient client = RestClientApp.getRestClient();
client.getHomeTimeline(1, new JsonHttpResponseHandler() {
  public void onSuccess(int statusCode, Headers headers, JSON json) {
    // Response is automatically parsed into a JSONArray
    // json.jsonArray.getJSONObject(0).getLong("id");
  }
});
```

Based on the JSON response (array or object), you need to declare the expected type inside the `onSuccess` signature i.e `public void onSuccess(int statusCode, Header[] headers, JSONObject json)`. If the endpoint does not return JSON, then you can use the `AsyncHttpResponseHandler`:

```java
RestClient client = RestClientApp.getRestClient();
client.get("http://www.google.com", new JsonHttpResponseHandler() {
    @Override
    public void onSuccess(int statusCode, Headers headers, String response) {
        System.out.println(response);
    }
});
```

Check out [Android Async HTTP Docs](https://github.com/codepath/asynchttpclient) for more request creation details.

## Extra Functionality


### Access Authorization

Once the request token has been received, an access token is granted by redirecting to the device's browser to allow the user to grant permission on the API provider's web address. The browser is opened using an implicit intent with no intent flags specified:

```java
Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(authorizeUrl));
```

However, specifying [intent flags](http://guides.codepath.com/android/Navigation-and-Task-Stacks#configuring-intent-flags) to alter that behavior can be added to the message using the following:

```java
RestClient client = RestApplication.getRestClient();
// Specify the intent flags as desired
client.setRequestIntentFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
// Trigger authorization
client.connect();
```

This can be helpful in cases where you must add a flag such as when encountering the `android.util.AndroidRuntimeException: Calling startActivity() from outside of an Activity  context requires the FLAG_ACTIVITY_NEW_TASK flag. Is this really what you want?` error.

### Logging Out

You can log out by clearing the access token at any time through the client object:

```java
RestClient client = RestApplication.getRestClient();
client.clearAccessToken();
```

### Debugging

In order to [troubleshoot API calls](http://guides.codepath.com/android/Troubleshooting-API-calls), you can take advantage of the Stetho library:

Next, initialize Stetho inside your Application object:
```java
public class MyApplication extends Application {
  public void onCreate() {
    super.onCreate();
    Stetho.initializeWithDefaults(this);
  }
}
```

Edit the manifest.xml file in your project. To let the Android operating system know that you have a custom Application class, add an attribute called  `android:name`  to the manifest’s application tag and set the value to the name of your custom Application class.
```xml
 <application
    ...
    android:name=".MyApplication"
    ...
    >
```

You can then use `chrome://inspect`, pick the app currently running, and click on the Network tab to view.  See [this guide](https://github.com/codepath/android_guides/wiki/Debugging-with-Stetho) for more context.
