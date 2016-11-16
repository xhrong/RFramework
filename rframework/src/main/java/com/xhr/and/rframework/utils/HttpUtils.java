package com.xhr.and.rframework.utils;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Http请求工具类，支持同步和异常两种请求方式。
 * 同步方式，需要在线程里调用
 * 异常方式，是基于回调的
 * <p/>
 * State：完成
 * <p/>
 * Created by xhrong on 2016/11/16.
 */
public class HttpUtils {

    private static final String TAG = "HttpUtils";


    /**
     * 所有任务都一次性开始的线程池
     */
    private static ExecutorService mCacheThreadExecutor = Executors.newCachedThreadPool();


    private static final boolean debug = true;

    private static void log(String funcName, String msg) {
        if (debug) {
            Log.e(TAG, funcName + ", " + msg);
        }
    }


    private static void handleResult(Runnable task) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(task);
    }

    /**
     * 向指定URL发送GET方法的请求，超时时间为5000
     *
     * @param url   发送请求的URL
     * @param param 请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @return URL 所代表远程资源的响应结果
     */
    public static String sendGet(String url, String param) {
        return sendGet(url, param, 5000);
    }


    /**
     * 向指定URL发送GET方法的请求
     *
     * @param url     发送请求的URL
     * @param param   请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @param timeout 超时时间
     * @return URL 所代表远程资源的响应结果
     */
    public static String sendGet(String url, String param, int timeout) {
        String result = "";
        BufferedReader in = null;
        try {
            String urlNameString = url;
            if (param != null && !param.equals("")) {
                if (urlNameString.indexOf("?") != 0) {
                    urlNameString = url + "&" + param;
                } else {
                    urlNameString = url + "?" + param;
                }
            }

            log("sendGet", "get url: " + urlNameString);

            URL realUrl = new URL(urlNameString);
            // 打开和URL之间的连接
            URLConnection connection = realUrl.openConnection();
            // 设置通用的请求属性
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            // 建立实际的连接
            connection.connect();
            // 定义 BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            System.out.println("发送GET请求出现异常！" + e);
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        log("sendGet", "get result: " + result);

        return result;
    }

    /**
     * 向指定 URL 发送POST方法的请求
     *
     * @param url   发送请求的 URL
     * @param param 请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @return 所代表远程资源的响应结果
     */
    public static String sendPost(String url, String param) {
        return sendPost(url, param, 5000);
    }

    /**
     * 向指定 URL 发送POST方法的请求
     *
     * @param url     发送请求的 URL
     * @param param   请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @param timeout 超时时间
     * @return 所代表远程资源的响应结果
     */
    public static String sendPost(String url, String param, int timeout) {
        PrintWriter out = null;
        BufferedReader in = null;
        String result = "";
        try {

            log("sendPost", "post url: " + url + ", params: " + param);

            URL realUrl = new URL(url);
            // 打开和URL之间的连接
            URLConnection conn = realUrl.openConnection();
            // 设置通用的请求属性
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            //设置连接超时
            conn.setConnectTimeout(timeout);
            int connectTime = conn.getConnectTimeout();
            // 发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setDoInput(true);
            // 获取URLConnection对象对应的输出流
            out = new PrintWriter(conn.getOutputStream());
            // 发送请求参数
            out.print(param);
            // flush输出流的缓冲
            out.flush();
            // 定义BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            log("sendPost", "发送 POST 请求出现异常！" + e.getLocalizedMessage());
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        log("sendPost", "post result: " + result);

        return result;
    }


    /**
     * 异步GET请求
     *
     * @param url
     * @param param
     * @param listener
     */
    public static void sendGetAsyn(final String url, final String param, final HttpListener listener) {
        sendGetAsyn(url, param, listener, 5000);
    }


    /**
     * 异步GET请求
     *
     * @param url
     * @param param
     * @param listener
     * @param timeout
     */
    public static void sendGetAsyn(final String url, final String param, final HttpListener listener, final int timeout) {

        mCacheThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                String result = "";
                BufferedReader in = null;
                try {
                    String urlNameString = url;
                    if (param != null && !param.equals("")) {
                        if (urlNameString.indexOf("?") != 0) {
                            urlNameString = url + "&" + param;
                        } else {
                            urlNameString = url + "?" + param;
                        }
                    }

                    log("sendGet", "get url: " + urlNameString);

                    URL realUrl = new URL(urlNameString);
                    // 打开和URL之间的连接
                    HttpURLConnection connection = (HttpURLConnection) realUrl.openConnection();
                    // 设置通用的请求属性
                    connection.setRequestProperty("accept", "*/*");
                    connection.setRequestProperty("connection", "Keep-Alive");
                    connection.setConnectTimeout(timeout);
                    connection.setReadTimeout(timeout);
                    connection.setRequestProperty("user-agent",
                            "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
                    // 建立实际的连接
                    connection.connect();

                    int responseCode = connection.getResponseCode();

                    if (responseCode == 200) {
                        // 定义 BufferedReader输入流来读取URL的响应
                        in = new BufferedReader(new InputStreamReader(
                                connection.getInputStream()));
                        String line;
                        while ((line = in.readLine()) != null) {
                            result += line;
                        }
                        final String res = result;
                        handleResult(new Runnable() {
                            @Override
                            public void run() {
                                listener.onSuccess(res);
                            }
                        });
                    } else {
                        in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                        String line;
                        while ((line = in.readLine()) != null) {
                            result += line;
                        }

                        final String res = result;
                        handleResult(new Runnable() {
                            @Override
                            public void run() {
                                listener.onSuccess(res);
                            }
                        });
                    }


                } catch (final Exception e) {
                    System.out.println("发送GET Asyn请求出现异常！" + e);
                    e.printStackTrace();
                    final String res = result;
                    handleResult(new Runnable() {
                        @Override
                        public void run() {
                            listener.onFail(e.getMessage());
                        }
                    });
                } finally {
                    try {
                        if (in != null) {
                            in.close();
                        }
                    } catch (Exception e2) {
                        e2.printStackTrace();
                    }
                }
                log("sendGet", "get result: " + result);
            }
        });
    }

    /**
     * 异步POST请求
     *
     * @param url
     * @param param
     * @param listener
     */
    public static void sendPostAsyn(final String url, final String param, final HttpListener listener) {
        sendPostAsyn(url, param, listener, 5000);
    }

    /**
     * 异步POST请求
     *
     * @param url
     * @param param
     * @param listener
     * @param timeout
     */
    public static void sendPostAsyn(final String url, final String param, final HttpListener listener, final int timeout) {
        mCacheThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                PrintWriter out = null;
                BufferedReader in = null;
                String result = "";
                try {

                    log("sendPost", "post url: " + url + ", params: " + param);

                    URL realUrl = new URL(url);
                    // 打开和URL之间的连接
                    HttpURLConnection connection = (HttpURLConnection) realUrl.openConnection();
                    // 设置通用的请求属性
                    connection.setRequestProperty("accept", "*/*");
                    connection.setRequestProperty("connection", "Keep-Alive");
                    connection.setRequestProperty("user-agent",
                            "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
                    //设置连接超时
                    connection.setConnectTimeout(timeout);
                    // 发送POST请求必须设置如下两行
                    connection.setDoOutput(true);
                    connection.setDoInput(true);
                    // 获取URLConnection对象对应的输出流
                    out = new PrintWriter(connection.getOutputStream());
                    // 发送请求参数
                    out.print(param);
                    // flush输出流的缓冲
                    out.flush();

                    int responseCode = connection.getResponseCode();
                    if (responseCode == 200) {
                        // 定义 BufferedReader输入流来读取URL的响应
                        in = new BufferedReader(new InputStreamReader(
                                connection.getInputStream()));
                        String line;
                        while ((line = in.readLine()) != null) {
                            result += line;
                        }
                        final String res = result;
                        handleResult(new Runnable() {
                            @Override
                            public void run() {
                                listener.onSuccess(res);
                            }
                        });
                    } else {
                        in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                        String line;
                        while ((line = in.readLine()) != null) {
                            result += line;
                        }

                        final String res = result;
                        handleResult(new Runnable() {
                            @Override
                            public void run() {
                                listener.onSuccess(res);
                            }
                        });
                    }
                } catch (final Exception e) {
                    log("sendPost", "发送 POST 请求出现异常！" + e.getLocalizedMessage());
                    e.printStackTrace();

                    handleResult(new Runnable() {
                        @Override
                        public void run() {
                            listener.onFail(e.getMessage());
                        }
                    });
                } finally {
                    try {
                        if (out != null) {
                            out.close();
                        }
                        if (in != null) {
                            in.close();
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
                log("sendPost", "post result: " + result);
            }
        });
    }


    //回调
    public interface HttpListener {
        void onSuccess(String result);

        void onFail(String result);
    }


    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {


    }

}
