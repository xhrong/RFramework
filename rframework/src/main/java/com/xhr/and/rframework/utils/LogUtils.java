package com.xhr.and.rframework.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通用日志组件，日志文件以日期记录为独立文件，存储在SDCard下
 * 1、支持设置是否输出到LogCat，即Debug模式
 * 2、支持设置Log是否保存到文件，并设置保存的Log级别
 * 3、支持设置Log文件过期时间，过期文件将会自动删除
 * <p/>
 * State：完成，测试通过。
 * <p/>
 * Created by xhrong on 2016/6/28.
 */
public class LogUtils {

    /**
     * 日志默认存储在/logs目录下
     */
    private static String LOG_PATH = Environment.getExternalStorageDirectory().getPath() + "/logs";

    /**
     * Debug模式下才会输出日志到LogCat和文件
     */
    private static boolean DEBUG = true;

    /**
     * 日志等级，大于或等于该等级的信息才会被保存到sd卡
     */
    private static int LEVEL = Log.ERROR;

    /**
     * 日志文件名前缀，如果为空，则用PACKAGE_NAME
     */
    private static String LOG_PREFIX = "log-";

    /**
     * 日志保存时长（天）
     */
    private static int KEEP_DAYS = 10;

    /**
     * 当前引用该组件的包名，需要先调用init才能获得
     */
    private static String packageName;
    private static String appVerName = "appVerName:";
    private static String appVerCode = "appVerCode:";
    private static String OsVer = "OsVer:";
    private static String vendor = "vendor:";
    private static String model = "model:";


    public static void init(Context context, int logLevel, boolean isDebug) {
        init(context, logLevel, isDebug, null, null, 10);
    }

    /**
     * 初始化，强烈建议先设置后使用
     *
     * @param context
     * @param logLevel  写文件日志的日志级别
     * @param isDebug   是否写日志，true写，false不写
     * @param logPrefix 日志文件名称前缀
     * @param logPath   日志文件保存路径
     * @param keepDays  日志保留天数，过期的日志将被删除
     */
    public static void init(Context context, int logLevel, boolean isDebug, String logPrefix, String logPath, int keepDays) {
        Context mContext = context.getApplicationContext();
        appVerName += getVerName(mContext);
        appVerCode += getVerCode(mContext);
        OsVer += Build.VERSION.RELEASE;
        vendor += Build.MANUFACTURER;
        model += Build.MODEL;
        packageName = getPackageName(mContext);
        if (TextUtils.isEmpty(logPath) && !TextUtils.isEmpty(packageName)) {
            LOG_PATH = LOG_PATH + File.separator + packageName;
        } else if (!TextUtils.isEmpty(logPath)) {
            LOG_PATH = logPath;
        }
        KEEP_DAYS = keepDays;
        if (!TextUtils.isEmpty(logPrefix)) LOG_PREFIX = logPrefix;
        DEBUG = isDebug;
        LEVEL = logLevel;

        //删除过期日志文件
        clearOutdateLogFile();
    }


    public static void v(String tag, String msg) {
        trace(Log.VERBOSE, tag, msg);
    }

    public static void d(String tag, String msg) {
        trace(Log.DEBUG, tag, msg);
    }

    public static void i(String tag, String msg) {
        trace(Log.INFO, tag, msg);
    }

    public static void w(String tag, String msg) {
        trace(Log.WARN, tag, msg);
    }

    public static void e(String tag, String msg) {
        trace(Log.ERROR, tag, msg);
    }


    /**
     * Custom Log output style
     *
     * @param type Log type
     * @param tag  TAG
     * @param msg  Log message
     */
    private static void trace(final int type, String tag, final String msg) {
        if (DEBUG) {
            switch (type) {
                case Log.VERBOSE:
                    Log.v(tag, msg);
                    break;
                case Log.DEBUG:
                    Log.d(tag, msg);
                    break;
                case Log.INFO:
                    Log.i(tag, msg);
                    break;
                case Log.WARN:
                    Log.w(tag, msg);
                    break;
                case Log.ERROR:
                    Log.e(tag, msg);
                    break;
            }

            // Write to file
            if (type >= LEVEL) {

                writeLog(type, tag, msg);
            }
        }
    }

    /**
     * Write log file to the SDCard
     *
     * @param type
     * @param msg
     */
    private static void writeLog(int type, String tag, String msg) {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return;
        }

        try {
            msg = generateMsg(tag, msg, type, Thread.currentThread().getStackTrace()[5]);

            final String fileName = new StringBuffer().append(LOG_PREFIX)
                    .append(getDateFormat("yyyy-MM-dd"))
                    .append(".log").toString();
            recordLog(fileName, msg);
        } catch (Exception e) {

        }
    }

    private static String getDateFormat(String pattern) {
        final DateFormat format = new SimpleDateFormat(pattern);
        return format.format(new Date());
    }

    /**
     * Write log
     *
     * @param fileName
     * @param msg      Log content
     */
    private static void recordLog(String fileName, String msg) {
        try {
            final File file = new File(LOG_PATH);
            if (!file.exists()) {
                file.mkdirs();
            }
            final File saveFile = new File(LOG_PATH + File.separator + fileName);
            if (!saveFile.exists()) {
                saveFile.createNewFile();

                //如果是第一次写文件，把机器信息写上文件头部
                msg = appVerName + "\n"
                        + appVerCode + "\n"
                        + OsVer + "\n"
                        + vendor + "\n"
                        + model + "\n"
                        + msg;
            }
            write(saveFile, msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Write msg to file
     *
     * @param file
     * @param msg
     */
    private static void write(final File file, final String msg) {
        new Thread(new Runnable() {
            @Override
            public void run() {

                OutputStreamWriter write = null;
                BufferedWriter out = null;
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(file, true);
                    write = new OutputStreamWriter(fos, Charset.forName("gbk"));//一定要使用gbk格式
                    out = new BufferedWriter(write, 1024);
                    out.write(msg);
                    out.flush();
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (out != null) {
                        try {
                            out.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (write != null) {
                        try {
                            write.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }


    private static String generateMsg(String tag, String msg, int type, StackTraceElement stack) {
        String logLevel = "";
        switch (type) {
            case Log.VERBOSE:
                logLevel = "VERBOSE";
                break;
            case Log.DEBUG:
                logLevel = "DEBUG";
                break;
            case Log.INFO:
                logLevel = "INFO";
                break;
            case Log.WARN:
                logLevel = "WARN";
                break;
            case Log.ERROR:
                logLevel = "ERROR";
                break;
            default:
                break;
        }

        String newMsg = "%s: %s: %s: %s - %s(L:%d)--> %s\r\n";
        newMsg = String.format(newMsg, getDateFormat("yyyy-MM-dd HH:mm:ss"), logLevel, tag, stack.getMethodName(), stack.getClassName(), stack.getLineNumber(), msg);
        return newMsg;
    }


    private static String getPackageName(Context c) {
        try {
            return c.getApplicationInfo().packageName;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private static String getVerName(Context c) {
        PackageManager pm = c.getPackageManager();
        PackageInfo pi = null;
        try {
            pi = pm.getPackageInfo(c.getPackageName(), PackageManager.GET_ACTIVITIES);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "";
        }
        if (pi == null) {
            return "";
        }
        String versionName = pi.versionName;
        if (versionName == null) {
            return "";
        }
        return versionName;
    }

    private static String getVerCode(Context c) {
        PackageManager pm = c.getPackageManager();
        PackageInfo pi = null;
        try {
            pi = pm.getPackageInfo(c.getPackageName(), PackageManager.GET_ACTIVITIES);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "";
        }
        if (pi == null) {
            return "";
        }
        int versionCode = pi.versionCode;
        return String.valueOf(versionCode);
    }

    private static void clearOutdateLogFile() {
        //用线程也处理，以免影响应用的启动速度
        new Thread(new Runnable() {
            @Override
            public void run() {
                File file = new File(LOG_PATH);
                if (!file.exists() || !file.isDirectory()) return;
                final Date now = new Date();
                File[] logFiles = file.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File file, String s) {
                        if (s.endsWith(".log")) {
                            String fDateStr = matchDateString(s);
                            if (TextUtils.isEmpty(fDateStr))
                                return false;
                            Date fDate = stringToDate(fDateStr);
                            long m = now.getTime() - fDate.getTime();
                            if ((m / (1000 * 60 * 60 * 24)) > KEEP_DAYS)
                                return true;
                            return false;
                        }
                        return false;
                    }
                });

                //删除过期日志
                for (File f : logFiles) {
                    f.delete();
                }
            }
        }).start();

    }

    /**
     * 能匹配的年月日类型有：
     * 2014年4月19日
     * 2014年4月19号
     * 2014-4-19
     * 2014/4/19
     * 2014.4.19
     *
     * @param text
     * @return
     */
    private static String matchDateString(String text) {
        try {
            List matches = null;
            Pattern p = Pattern.compile("(\\d{1,4}[-|\\/|年|\\.]\\d{1,2}[-|\\/|月|\\.]\\d{1,2}([日|号])?)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
            Matcher matcher = p.matcher(text);
            if (matcher.find() && matcher.groupCount() >= 1) {
                matches = new ArrayList();
                for (int i = 1; i <= matcher.groupCount(); i++) {
                    String temp = matcher.group(i);
                    matches.add(temp);
                }
            } else {
                matches = Collections.EMPTY_LIST;
            }
            if (matches.size() > 0) {
                return ((String) matches.get(0)).trim();
            } else {
                return "";
            }
        } catch (Exception e) {
            return "";
        }
    }

    public static Date stringToDate(String time) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        time = time.trim();

        if (time.indexOf("/") > -1) {
            formatter = new SimpleDateFormat("yyyy/MM/dd");
        } else if (time.indexOf("\\.") > -1) {
            formatter = new SimpleDateFormat("yyy.MM.dd");
        }

        ParsePosition pos = new ParsePosition(0);
        Date ctime = formatter.parse(time, pos);

        return ctime;
    }

}