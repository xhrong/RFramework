package com.xhr.android.rframework;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ImageWriter;

import com.xhr.and.rframework.utils.ImageUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by xhrong on 2016/11/8.
 */
public class Main {
    public static void main(String args[]) {





        //这两个字符串的HashCode是一样的
        String a="FB";
        String b="Ea";
        if (a.hashCode()==b.hashCode()) {
            System.out.print("true");
        } else {
            System.out.print("false");
        }

        HashMap<String,String> map=new HashMap<>();
        map.put(a,"aaaa");
        map.put(b,"bbbb");

        System.out.println(map.get(b));
    }

    public static byte[] encrypt(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        int len = bytes.length;
        int key = 0x12;
        for (int i = 0; i < len; i++) {
            bytes[i] ^= key;
        }
        return bytes;
    }
}
