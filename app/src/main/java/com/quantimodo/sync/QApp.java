package com.quantimodo.sync;

import android.app.Application;
import com.uservoice.uservoicesdk.Config;
import com.uservoice.uservoicesdk.UserVoice;
import dagger.ObjectGraph;
import de.greenrobot.event.EventBus;

public class QApp extends Application{

    ObjectGraph objectGraph;
    private static QApp instance;

    @Override
    public void onCreate() {
        super.onCreate();

        //Init uservoice
        Config config = new Config("quantimodo.uservoice.com");
        config.setForumId(211661);
        UserVoice.init(config, this);

        instance = this;
        AppModule requestModule = new AppModule(getApplicationContext());
        objectGraph = ObjectGraph.create(requestModule);
    }

    public static void inject(Object object){
        instance.objectGraph.inject(object);
    }

    public static void register(Object object){
        EventBus.getDefault().register(object);
    }

    public static void registerSticky(Object object){
        EventBus.getDefault().registerSticky(object);
    }

    public static void postStickyEvent(Object object){
        EventBus.getDefault().postSticky(object);
    }

    public static void postEvent(Object object){
        EventBus.getDefault().post(object);
    }

    public static void unregister(Object object){
        EventBus.getDefault().unregister(object);
    }
}
