package com.quantimodo.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;
import com.crashlytics.android.Crashlytics;
import com.google.gson.JsonObject;
import com.koushikdutta.ion.Ion;
import com.quantimodo.android.sdk.SdkDefs;
import com.quantimodo.sync.events.NoAuthEvent;

import java.util.concurrent.ExecutionException;

public class AuthHelper {

    public static final String KEY_SCOPES = "scopes";
    public static final String AUTH_PREF = "authPref";
    public static final String PREF_KEY = "key";
    public static final String PREF_EXPIRE = "expire_in";
    public static final String PREF_REFRESH = "refresh";
    // An account type, in the form of a domain name
    public static final String ACCOUNT_TYPE = "com.quantimodo.sync";
    // The account name
    public static final String ACCOUNT = "Sync Account";

    private Context mCtx;
    private String mClientId;
    private String mClientSecret;
    private SharedPreferences mPrefs;
    private String mToken;

    public AuthHelper(Context context) {
        mCtx = context.getApplicationContext();
        mPrefs = context.getSharedPreferences(AUTH_PREF, Context.MODE_PRIVATE);
        mToken = mPrefs.getString(PREF_KEY, null);
        mClientId = Global.QM_ID;
        mClientSecret = Global.QM_SECRET;
    }

    public AuthToken refreshToken(AuthToken authToken){
        try {
            JsonObject tokenResult = Ion.with(mCtx, Global.QUANTIMODO_ADDRESS + "api/oauth2/token")
                    .setBodyParameter("client_id", mClientId)
                    .setBodyParameter("client_secret", mClientSecret)
                    .setBodyParameter("grant_type", "refresh_token")
                    .setBodyParameter("refresh_token", authToken.refreshToken)
                    .asJsonObject()
                    .get();

            String accessToken = null;
            String refreshToken = authToken.refreshToken;
            int expiresIn = 0;
            if (tokenResult.has("error")){
                Toast.makeText(mCtx, R.string.oauth_refresh_failed, Toast.LENGTH_LONG).show();
                QApp.postEvent(new NoAuthEvent());
                return null;
            }
            if (tokenResult.has("access_token")) {
                accessToken = tokenResult.get("access_token").getAsString();
                expiresIn = tokenResult.get("expires_in").getAsInt();
            }
            if (tokenResult.has("refresh_token")){
                refreshToken = tokenResult.get("refresh_token").getAsString();
            }

            AuthToken at = new AuthToken(accessToken,refreshToken, System.currentTimeMillis()/1000 + expiresIn);
            saveAuthToken(at);
            return authToken;
        } catch (InterruptedException | ExecutionException e) {
            Log.i("Couldn't get new auth token");
            Crashlytics.getInstance().core.logException(e);
        }

        return null;
    }

    public void setAuthToken(AuthToken token){
        saveAuthToken(token);
        mToken = token.accessToken;
    }

    private void saveAuthToken(AuthToken token){
        mPrefs.edit()
                .putString(PREF_KEY,token.accessToken)
                .putString(PREF_REFRESH,token.refreshToken)
                .putLong(PREF_EXPIRE,token.expireDate).commit();
    }

    private AuthToken readAuthToken(){
        return new AuthToken(
                mPrefs.getString(PREF_KEY, null),
                mPrefs.getString(PREF_REFRESH,null),
                mPrefs.getLong(PREF_EXPIRE,0)
        );
    }


    public void onEvent(NoAuthEvent event){
        mPrefs.edit().clear().apply();
        mToken = null;
    }

    public boolean isLoggedIn(){
        return readAuthToken().refreshToken != null;
    }

    public String getAuthTokenWithRefresh(){
        AuthToken token = readAuthToken();
        if (!token.isExpired()){
            return token.accessToken;
        }

        if (token.refreshToken != null){
            token = refreshToken(token);
            if (token != null){
                return token.accessToken;
            }
        }

        return null;
    }

    public String getAuthToken(){
        return mToken;
    }

    public String getClientId() {
        return mClientId;
    }

    public String getClientSecret() {
        return mClientSecret;
    }

    public void logOut() {
        mPrefs.edit().clear().apply();
    }

    public static class AuthToken {
        public final String accessToken;
        public final String refreshToken;
        public final long expireDate;

        public AuthToken(String accessToken, String refreshToken, long expireTimestamp ) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.expireDate = expireTimestamp;
        }

        public AuthToken(String refreshToken) {
            this.accessToken = null;
            this.expireDate = 0;
            this.refreshToken = refreshToken;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() / 1000 > expireDate;
        }
    }
}
