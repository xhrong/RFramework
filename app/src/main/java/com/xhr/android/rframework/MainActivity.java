package com.xhr.android.rframework;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.xhr.and.rframework.utils.LogUtils;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LogUtils.e("TEST LOG","TEST LOG");

    }
}
