package com.xhr.android.rframework;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.xhr.and.rframework.utils.HttpUtils;
import com.xhr.and.rframework.utils.ImageUtils;
import com.xhr.and.rframework.utils.LogUtils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;


public class MainActivity extends AppCompatActivity {

    private static String TAG="TTTTT";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        HttpUtils.sendPostAsyn("http://www.baidu.coms", null, new HttpUtils.HttpListener() {
            @Override
            public void onSuccess(String result) {
                Log.e(TAG,result);
            }

            @Override
            public void onFail(String result) {
                Log.e(TAG,result);
            }
        },5000);


        ActivityManager activityManager=(ActivityManager) getSystemService(Context.ACTIVITY_SERVICE) ;
        LogUtils.e("TEST LOG",activityManager.getMemoryClass()+"");

        try {
            String imgPath = Environment.getExternalStorageDirectory()+"/ori.jpg";
            String imgOutPath = Environment.getExternalStorageDirectory()+"/ori33.jpg";


            Bitmap bitmap = BitmapFactory.decodeFile(imgPath);




            ImageUtils.compressAndGenImage(bitmap, imgOutPath, 108);
        }catch (Exception e){
            Log.e("EEE",e.getMessage());
        }
    }
}
