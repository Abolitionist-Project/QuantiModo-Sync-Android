package com.quantimodo.sync;

import android.content.Context;

import com.quantimodo.android.sdk.QuantimodoApiV2;
import com.quantimodo.sync.activities.MainActivity;
import com.quantimodo.sync.activities.QuantimodoWebAuthenticatorActivity;
import com.quantimodo.sync.activities.SettingsActivity;
import com.quantimodo.sync.activities.WelcomeActivity;
import com.quantimodo.sync.sync.SyncService;
import dagger.Module;
import dagger.Provides;

@Module(
        injects = {
                //Activities
                MainActivity.class,
                QuantimodoWebAuthenticatorActivity.class,
                SettingsActivity.class,
                WelcomeActivity.class,

                //Services
                SyncService.class
        }
)
public class AppModule {

    private Context mContext;
    private QuantimodoApiV2 mClient;
    private AuthHelper mAuthHelper;

    public AppModule(Context ctx) {
        mContext = ctx.getApplicationContext();
        mAuthHelper = new AuthHelper(mContext);
        mClient = QuantimodoApiV2.getInstance(Global.QUANTIMODO_ADDRESS);
    }

//    @Provides
//    public Context getCtx(){
//        return mContext;
//    }

//    @Provides
//    @Named("authToken")
//    public String getToken(){
//        return mAuthHelper.getAuthToken();
//    }

    @Provides
    public AuthHelper getAuthHelper() {
        return mAuthHelper;
    }

    @Provides
    public QuantimodoApiV2 getClient() {
        return mClient;
    }

}
