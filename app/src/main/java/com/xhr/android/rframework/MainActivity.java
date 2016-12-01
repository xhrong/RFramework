package com.xhr.android.rframework;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.xhr.and.rframework.utils.HttpUtils;
import com.xhr.and.rframework.utils.ImageUtils;
import com.xhr.and.rframework.utils.LogUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.GPUImage3x3ConvolutionFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageCGAColorspaceFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageColorBurnBlendFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageSharpenFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageSobelEdgeDetection;
import jp.co.cyberagent.android.gpuimage.GPUImageTwoPassFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageView;


public class MainActivity extends AppCompatActivity {

    private static String TAG = "TTTTT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        try {
            String imgPath = Environment.getExternalStorageDirectory() + "/ori.jpg";
            String imgOutPath = Environment.getExternalStorageDirectory() + "/ori33.jpg";



            BitmapFactory.Options options = new BitmapFactory.Options();

            options.inScaled = false;
            Bitmap bitmap = BitmapFactory.decodeFile(imgPath,options);

            Log.e("TTTTT",bitmap.getByteCount()+"");
          Bitmap    bitmapa = ImageUtils.rotateImage(90, bitmap);
            Log.e("TTTTT",bitmap.getByteCount()+"");
            ImageUtils.saveImage(bitmapa, imgOutPath, 100);
        } catch (Exception e) {
            Log.e("EEE", e.getMessage());
        }
    }
}
