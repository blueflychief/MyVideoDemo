package com.example.administrator.myvideodemo;

import android.app.Application;

import com.example.administrator.myvideodemo.util.KLog;

/**
 * Created by LuoShuiquan on 9/5/2016.
 */
public class MyApp extends Application {
    public static MyApp sMyApp;

    @Override
    public void onCreate() {
        super.onCreate();
        sMyApp = this;
        KLog.init(true, "myvideodemo");
    }

    public static MyApp getMyApp() {
        return sMyApp;
    }
}
