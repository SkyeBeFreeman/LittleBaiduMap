package com.zhtian.experimentten;

/**
 * Created by zhtian on 2016/11/30.
 */

import android.app.Application;
import android.content.Context;

import com.baidu.mapapi.SDKInitializer;

public class BaseApplication extends Application {

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        SDKInitializer.initialize(context);
    }

    public static Context getContext() {
        return context;
    }


}