package com.xhr.android.rframework;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.xhr.and.rframework.utils.DeviceUtils;
import com.xhr.and.rframework.utils.NetworkUtils;

public class MainActivity extends AppCompatActivity {

    private static String TAG = "TTTTT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NetworkUtils.enableWifi(this,true);

        Log.e("getIPAddress", NetworkUtils.isInternetAvailable()+"");

//        Log.e("getDomainAddress", NetworkUtils.getIPAddress(true));
//
//        Log.e("getNetworkType", NetworkUtils.getNetworkType(this).toString());
//
//        Log.e("getMacAddress", DeviceUtils.getMacAddress(this));
//
//        Log.e("getPhoneStatus",DeviceUtils.getPhoneStatus(this));
//
//
//        try {
//            String imgPath = Environment.getExternalStorageDirectory() + "/ori.jpg";
//            String imgOutPath = Environment.getExternalStorageDirectory() + "/ori33.jpg";
//
//
//
//            BitmapFactory.Options options = new BitmapFactory.Options();
//
//            options.inScaled = false;
//            Bitmap bitmap = BitmapFactory.decodeFile(imgPath,options);
//
//            Log.e("TTTTT",bitmap.getByteCount()+"");
//          Bitmap    bitmapa = ImageUtils.rotateImage(90, bitmap);
//            Log.e("TTTTT",bitmap.getByteCount()+"");
//            ImageUtils.saveImage(bitmapa, imgOutPath, 100);
//        } catch (Exception e) {
//            Log.e("EEE", e.getMessage());
//        }
    }
}
