package de.adrianbartnik.noty.application;

import android.app.Application;
import android.content.res.Configuration;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;

public class MyApplication extends Application {

    private DropboxAPI<AndroidAuthSession> mDBApi;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public void setDropboxAPI(DropboxAPI<AndroidAuthSession> api){
        mDBApi = api;
    }

    public DropboxAPI<AndroidAuthSession> getDropboxAPI(){
        return mDBApi;
    }
}
