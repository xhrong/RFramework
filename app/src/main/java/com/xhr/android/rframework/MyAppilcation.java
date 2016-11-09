package com.xhr.android.rframework;

import android.app.Application;
import android.util.Log;

import com.xhr.and.rframework.utils.LogUtils;


/**
 * Created by xhrong on 2016/11/9.
 */
public class MyAppilcation extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        LogUtils.init(this, Log.INFO, true);
    }
}
