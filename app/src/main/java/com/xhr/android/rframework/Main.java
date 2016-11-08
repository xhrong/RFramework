package com.xhr.android.rframework;

import java.util.HashMap;

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
}
