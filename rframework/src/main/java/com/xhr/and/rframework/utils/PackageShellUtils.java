package com.xhr.and.rframework.utils;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.webkit.MimeTypeMap;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by xhrong on 2016/12/1.
 */
public class PackageShellUtils {

    /**
     * <pre>
     *     author: Blankj
     *     blog  : http://blankj.com
     *     time  : 2016/8/7
     *     desc  : Shell相关工具类
     * </pre>
     */
    public static class ShellTool {


        /**
         * 是否是在root下执行命令
         *
         * @param command 命令
         * @param isRoot  是否需要root权限执行
         * @return CommandResult
         */
        public static CommandResult execCmd(String command, boolean isRoot) {
            return execCmd(new String[]{command}, isRoot, true);
        }

        /**
         * 是否是在root下执行命令
         *
         * @param commands 多条命令链表
         * @param isRoot   是否需要root权限执行
         * @return CommandResult
         */
        public static CommandResult execCmd(List<String> commands, boolean isRoot) {
            return execCmd(commands == null ? null : commands.toArray(new String[]{}), isRoot, true);
        }

        /**
         * 是否是在root下执行命令
         *
         * @param commands 多条命令数组
         * @param isRoot   是否需要root权限执行
         * @return CommandResult
         */
        public static CommandResult execCmd(String[] commands, boolean isRoot) {
            return execCmd(commands, isRoot, true);
        }

        /**
         * 是否是在root下执行命令
         *
         * @param command         命令
         * @param isRoot          是否需要root权限执行
         * @param isNeedResultMsg 是否需要结果消息
         * @return CommandResult
         */
        public static CommandResult execCmd(String command, boolean isRoot, boolean isNeedResultMsg) {
            return execCmd(new String[]{command}, isRoot, isNeedResultMsg);
        }

        /**
         * 是否是在root下执行命令
         *
         * @param commands        命令链表
         * @param isRoot          是否需要root权限执行
         * @param isNeedResultMsg 是否需要结果消息
         * @return CommandResult
         */
        public static CommandResult execCmd(List<String> commands, boolean isRoot, boolean isNeedResultMsg) {
            return execCmd(commands == null ? null : commands.toArray(new String[]{}), isRoot, isNeedResultMsg);
        }

        /**
         * 是否是在root下执行命令
         *
         * @param commands        命令数组
         * @param isRoot          是否需要root权限执行
         * @param isNeedResultMsg 是否需要结果消息
         * @return CommandResult
         */
        public static CommandResult execCmd(String[] commands, boolean isRoot, boolean isNeedResultMsg) {
            int result = -1;
            if (commands == null || commands.length == 0) {
                return new CommandResult(result, null, null);
            }
            Process process = null;
            BufferedReader successResult = null;
            BufferedReader errorResult = null;
            StringBuilder successMsg = null;
            StringBuilder errorMsg = null;
            DataOutputStream os = null;
            try {
                process = Runtime.getRuntime().exec(isRoot ? "su" : "sh");
                os = new DataOutputStream(process.getOutputStream());
                for (String command : commands) {
                    if (command == null) continue;
                    os.write(command.getBytes());
                    os.writeBytes("\n");
                    os.flush();
                }
                os.writeBytes("exit\n");
                os.flush();
                result = process.waitFor();
                if (isNeedResultMsg) {
                    successMsg = new StringBuilder();
                    errorMsg = new StringBuilder();
                    successResult = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
                    errorResult = new BufferedReader(new InputStreamReader(process.getErrorStream(), "UTF-8"));
                    String s;
                    while ((s = successResult.readLine()) != null) {
                        successMsg.append(s);
                    }
                    while ((s = errorResult.readLine()) != null) {
                        errorMsg.append(s);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (os != null)
                        os.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (process != null) {
                    process.destroy();
                }
            }
            return new CommandResult(
                    result,
                    successMsg == null ? null : successMsg.toString(),
                    errorMsg == null ? null : errorMsg.toString()
            );
        }

        /**
         * 返回的命令结果
         */
        public static class CommandResult {
            /**
             * 结果码
             **/
            public int result;
            /**
             * 成功信息
             **/
            public String successMsg;
            /**
             * 错误信息
             **/
            public String errorMsg;

            public CommandResult(int result, String successMsg, String errorMsg) {
                this.result = result;
                this.successMsg = successMsg;
                this.errorMsg = errorMsg;
            }
        }
    }

    public static class PackageTool {
        /**
         * 判断App是否安装
         *
         * @param context     上下文
         * @param packageName 包名
         * @return {@code true}: 已安装<br>{@code false}: 未安装
         */
        public static boolean isAppInstalled(Context context, String packageName) {
            return !InnerUtils.isSpace(packageName) && InnerUtils.getLaunchAppIntent(context, packageName) != null;
        }

        /**
         * 安装App(支持6.0)
         *
         * @param context  上下文
         * @param filePath 文件路径
         */
        public static void installApp(Context context, String filePath) {
            installApp(context, InnerUtils.getFileByPath(filePath));
        }

        /**
         * 安装App（支持6.0）
         *
         * @param context 上下文
         * @param file    文件
         */
        public static void installApp(Context context, File file) {
            if (!InnerUtils.isFileExists(file)) return;
            context.startActivity(InnerUtils.getInstallAppIntent(file));
        }


        /**
         * 静默安装App
         * <p>非root需添加权限 {@code <uses-permission android:name="android.permission.INSTALL_PACKAGES" />}</p>
         *
         * @param context  上下文
         * @param filePath 文件路径
         * @return {@code true}: 安装成功<br>{@code false}: 安装失败
         */
        public static boolean installAppSilent(Context context, String filePath) {
            File file = InnerUtils.getFileByPath(filePath);
            if (!InnerUtils.isFileExists(file)) return false;
            String command = "LD_LIBRARY_PATH=/vendor/lib:/system/lib pm install " + filePath;
            ShellTool.CommandResult commandResult = ShellTool.execCmd(command, !isSystemApp(context), true);
            return commandResult.successMsg != null && commandResult.successMsg.toLowerCase().contains("success");
        }

        /**
         * 卸载App
         *
         * @param context     上下文
         * @param packageName 包名
         */
        public static void uninstallApp(Context context, String packageName) {
            if (InnerUtils.isSpace(packageName)) return;
            context.startActivity(InnerUtils.getUninstallAppIntent(packageName));
        }


        /**
         * 静默卸载App
         * <p>非root需添加权限 {@code <uses-permission android:name="android.permission.DELETE_PACKAGES" />}</p>
         *
         * @param context     上下文
         * @param packageName 包名
         * @param isKeepData  是否保留数据
         * @return {@code true}: 卸载成功<br>{@code false}: 卸载成功
         */
        public static boolean uninstallAppSilent(Context context, String packageName, boolean isKeepData) {
            if (InnerUtils.isSpace(packageName)) return false;
            String command = "LD_LIBRARY_PATH=/vendor/lib:/system/lib pm uninstall " + (isKeepData ? "-k " : "") + packageName;
            ShellTool.CommandResult commandResult = ShellTool.execCmd(command, !isSystemApp(context), true);
            return commandResult.successMsg != null && commandResult.successMsg.toLowerCase().contains("success");
        }


        /**
         * 判断App是否有root权限
         *
         * @return {@code true}: 是<br>{@code false}: 否
         */
        public static boolean isAppRoot() {
            ShellTool.CommandResult result = ShellTool.execCmd("echo root", true);
            if (result.result == 0) {
                return true;
            }
            if (result.errorMsg != null) {
                LogUtils.d("isAppRoot", result.errorMsg);
            }
            return false;
        }

        /**
         * 打开App
         *
         * @param context     上下文
         * @param packageName 包名
         */
        public static void launchApp(Context context, String packageName) {
            if (InnerUtils.isSpace(packageName)) return;
            context.startActivity(InnerUtils.getLaunchAppIntent(context, packageName));
        }


        /**
         * 获取App包名
         *
         * @param context 上下文
         * @return App包名
         */
        public static String getAppPackageName(Context context) {
            return context.getPackageName();
        }

        /**
         * 获取App名称
         *
         * @param context 上下文
         * @return App名称
         */
        public static String getAppName(Context context) {
            return getAppName(context, context.getPackageName());
        }

        /**
         * 获取App名称
         *
         * @param context     上下文
         * @param packageName 包名
         * @return App名称
         */
        public static String getAppName(Context context, String packageName) {
            if (InnerUtils.isSpace(packageName)) return null;
            try {
                PackageManager pm = context.getPackageManager();
                PackageInfo pi = pm.getPackageInfo(packageName, 0);
                return pi == null ? null : pi.applicationInfo.loadLabel(pm).toString();
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                return null;
            }
        }

        /**
         * 获取App图标
         *
         * @param context 上下文
         * @return App图标
         */
        public static Drawable getAppIcon(Context context) {
            return getAppIcon(context, context.getPackageName());
        }

        /**
         * 获取App图标
         *
         * @param context     上下文
         * @param packageName 包名
         * @return App图标
         */
        public static Drawable getAppIcon(Context context, String packageName) {
            if (InnerUtils.isSpace(packageName)) return null;
            try {
                PackageManager pm = context.getPackageManager();
                PackageInfo pi = pm.getPackageInfo(packageName, 0);
                return pi == null ? null : pi.applicationInfo.loadIcon(pm);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                return null;
            }
        }

        /**
         * 获取App路径
         *
         * @param context 上下文
         * @return App路径
         */
        public static String getAppPath(Context context) {
            return getAppPath(context, context.getPackageName());
        }

        /**
         * 获取App路径
         *
         * @param context     上下文
         * @param packageName 包名
         * @return App路径
         */
        public static String getAppPath(Context context, String packageName) {
            if (InnerUtils.isSpace(packageName)) return null;
            try {
                PackageManager pm = context.getPackageManager();
                PackageInfo pi = pm.getPackageInfo(packageName, 0);
                return pi == null ? null : pi.applicationInfo.sourceDir;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                return null;
            }
        }

        /**
         * 获取App版本号
         *
         * @param context 上下文
         * @return App版本号
         */
        public static String getAppVersionName(Context context) {
            return getAppVersionName(context, context.getPackageName());
        }

        /**
         * 获取App版本号
         *
         * @param context     上下文
         * @param packageName 包名
         * @return App版本号
         */
        public static String getAppVersionName(Context context, String packageName) {
            if (InnerUtils.isSpace(packageName)) return null;
            try {
                PackageManager pm = context.getPackageManager();
                PackageInfo pi = pm.getPackageInfo(packageName, 0);
                return pi == null ? null : pi.versionName;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                return null;
            }
        }

        /**
         * 获取App版本码
         *
         * @param context 上下文
         * @return App版本码
         */
        public static int getAppVersionCode(Context context) {
            return getAppVersionCode(context, context.getPackageName());
        }

        /**
         * 获取App版本码
         *
         * @param context     上下文
         * @param packageName 包名
         * @return App版本码
         */
        public static int getAppVersionCode(Context context, String packageName) {
            if (InnerUtils.isSpace(packageName)) return -1;
            try {
                PackageManager pm = context.getPackageManager();
                PackageInfo pi = pm.getPackageInfo(packageName, 0);
                return pi == null ? -1 : pi.versionCode;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                return -1;
            }
        }

        /**
         * 判断App是否是系统应用
         *
         * @param context 上下文
         * @return {@code true}: 是<br>{@code false}: 否
         */
        public static boolean isSystemApp(Context context) {
            return isSystemApp(context, context.getPackageName());
        }

        /**
         * 判断App是否是系统应用
         *
         * @param context     上下文
         * @param packageName 包名
         * @return {@code true}: 是<br>{@code false}: 否
         */
        public static boolean isSystemApp(Context context, String packageName) {
            if (InnerUtils.isSpace(packageName)) return false;
            try {
                PackageManager pm = context.getPackageManager();
                ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
                return ai != null && (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                return false;
            }
        }


        /**
         * 获取App签名
         *
         * @param context 上下文
         * @return App签名
         */
        public static Signature[] getAppSignature(Context context) {
            return getAppSignature(context, context.getPackageName());
        }

        /**
         * 获取App签名
         *
         * @param context     上下文
         * @param packageName 包名
         * @return App签名
         */
        @SuppressLint("PackageManagerGetSignatures")
        public static Signature[] getAppSignature(Context context, String packageName) {
            if (InnerUtils.isSpace(packageName)) return null;
            try {
                PackageManager pm = context.getPackageManager();
                PackageInfo pi = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
                return pi == null ? null : pi.signatures;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                return null;
            }
        }


        /**
         * 判断App是否处于前台
         *
         * @param context 上下文
         * @return {@code true}: 是<br>{@code false}: 否
         */
        public static boolean isAppForeground(Context context) {
            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> infos = manager.getRunningAppProcesses();
            if (infos == null || infos.size() == 0) return false;
            for (ActivityManager.RunningAppProcessInfo info : infos) {
                if (info.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    return info.processName.equals(context.getPackageName());
                }
            }
            return false;
        }

        /**
         * 判断App是否处于前台
         * <p>当不是查看当前App，且SDK大于21时，
         * 需添加权限 {@code <uses-permission android:name="android.permission.PACKAGE_USAGE_STATS"/>}</p>
         *
         * @param context     上下文
         * @param packageName 包名
         * @return {@code true}: 是<br>{@code false}: 否
         */
        public static boolean isAppForeground(Context context, String packageName) {
            return !InnerUtils.isSpace(packageName) && packageName.equals(InnerUtils.getForegroundProcessName(context));
        }

        /**
         * 封装App信息的Bean类
         */
        public static class AppInfo {

            private String name;
            private Drawable icon;
            private String packageName;
            private String packagePath;
            private String versionName;
            private int versionCode;
            private boolean isSystem;

            public Drawable getIcon() {
                return icon;
            }

            public void setIcon(Drawable icon) {
                this.icon = icon;
            }

            public boolean isSystem() {
                return isSystem;
            }

            public void setSystem(boolean isSystem) {
                this.isSystem = isSystem;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getPackageName() {
                return packageName;
            }

            public void setPackageName(String packagName) {
                this.packageName = packagName;
            }

            public String getPackagePath() {
                return packagePath;
            }

            public void setPackagePath(String packagePath) {
                this.packagePath = packagePath;
            }

            public int getVersionCode() {
                return versionCode;
            }

            public void setVersionCode(int versionCode) {
                this.versionCode = versionCode;
            }

            public String getVersionName() {
                return versionName;
            }

            public void setVersionName(String versionName) {
                this.versionName = versionName;
            }

            /**
             * @param name        名称
             * @param icon        图标
             * @param packageName 包名
             * @param packagePath 包路径
             * @param versionName 版本号
             * @param versionCode 版本码
             * @param isSystem    是否系统应用
             */
            public AppInfo(String packageName, String name, Drawable icon, String packagePath,
                           String versionName, int versionCode, boolean isSystem) {
                this.setName(name);
                this.setIcon(icon);
                this.setPackageName(packageName);
                this.setPackagePath(packagePath);
                this.setVersionName(versionName);
                this.setVersionCode(versionCode);
                this.setSystem(isSystem);
            }

            @Override
            public String toString() {
                return "App包名：" + getPackageName() +
                        "\nApp名称：" + getName() +
                        "\nApp图标：" + getIcon() +
                        "\nApp路径：" + getPackagePath() +
                        "\nApp版本号：" + getVersionName() +
                        "\nApp版本码：" + getVersionCode() +
                        "\n是否系统App：" + isSystem();
            }
        }

        /**
         * 获取App信息
         * <p>AppInfo（名称，图标，包名，版本号，版本Code，是否系统应用）</p>
         *
         * @param context 上下文
         * @return 当前应用的AppInfo
         */
        public static AppInfo getAppInfo(Context context) {
            return getAppInfo(context, context.getPackageName());
        }

        /**
         * 获取App信息
         * <p>AppInfo（名称，图标，包名，版本号，版本Code，是否系统应用）</p>
         *
         * @param context     上下文
         * @param packageName 包名
         * @return 当前应用的AppInfo
         */
        public static AppInfo getAppInfo(Context context, String packageName) {
            try {
                PackageManager pm = context.getPackageManager();
                PackageInfo pi = pm.getPackageInfo(packageName, 0);
                return getBean(pm, pi);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                return null;
            }
        }

        /**
         * 得到AppInfo的Bean
         *
         * @param pm 包的管理
         * @param pi 包的信息
         * @return AppInfo类
         */
        private static AppInfo getBean(PackageManager pm, PackageInfo pi) {
            if (pm == null || pi == null) return null;
            ApplicationInfo ai = pi.applicationInfo;
            String packageName = pi.packageName;
            String name = ai.loadLabel(pm).toString();
            Drawable icon = ai.loadIcon(pm);
            String packagePath = ai.sourceDir;
            String versionName = pi.versionName;
            int versionCode = pi.versionCode;
            boolean isSystem = (ApplicationInfo.FLAG_SYSTEM & ai.flags) != 0;
            return new AppInfo(packageName, name, icon, packagePath, versionName, versionCode, isSystem);
        }

        /**
         * 获取所有已安装App信息
         * <p>{@link #getBean(PackageManager, PackageInfo)}（名称，图标，包名，包路径，版本号，版本Code，是否系统应用）</p>
         * <p>依赖上面的getBean方法</p>
         *
         * @param context 上下文
         * @return 所有已安装的AppInfo列表
         */
        public static List<AppInfo> getAppsInfo(Context context) {
            List<AppInfo> list = new ArrayList<>();
            PackageManager pm = context.getPackageManager();
            // 获取系统中安装的所有软件信息
            List<PackageInfo> installedPackages = pm.getInstalledPackages(0);
            for (PackageInfo pi : installedPackages) {
                AppInfo ai = getBean(pm, pi);
                if (ai == null) continue;
                list.add(ai);
            }
            return list;
        }
    }

    private static class InnerUtils {
        public static boolean isSpace(String str) {
            return str == null || str.equals("");
        }

        /**
         * 获取前台线程包名
         * <p>当不是查看当前App，且SDK大于21时，
         * 需添加权限 {@code <uses-permission android:name="android.permission.PACKAGE_USAGE_STATS"/>}</p>
         *
         * @param context 上下文
         * @return 前台应用包名
         */
        public static String getForegroundProcessName(Context context) {
            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> infos = manager.getRunningAppProcesses();
            if (infos != null && infos.size() != 0) {
                for (ActivityManager.RunningAppProcessInfo info : infos) {
                    if (info.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                        return info.processName;
                    }
                }
            }
            if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.LOLLIPOP) {
                PackageManager packageManager = context.getPackageManager();
                Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                System.out.println(list);
                if (list.size() > 0) {// 有"有权查看使用权限的应用"选项
                    try {
                        ApplicationInfo info = packageManager.getApplicationInfo(context.getPackageName(), 0);
                        AppOpsManager aom = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
                        if (aom.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, info.uid, info.packageName) != AppOpsManager.MODE_ALLOWED) {
                            context.startActivity(intent);
                        }
                        if (aom.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, info.uid, info.packageName) != AppOpsManager.MODE_ALLOWED) {
                            LogUtils.d("getForegroundApp", "没有打开\"有权查看使用权限的应用\"选项");
                            return null;
                        }
                        UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
                        long endTime = System.currentTimeMillis();
                        long beginTime = endTime - 86400000 * 7;
                        List<UsageStats> usageStatses = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, beginTime, endTime);
                        if (usageStatses == null || usageStatses.isEmpty()) return null;
                        UsageStats recentStats = null;
                        for (UsageStats usageStats : usageStatses) {
                            if (recentStats == null || usageStats.getLastTimeUsed() > recentStats.getLastTimeUsed()) {
                                recentStats = usageStats;
                            }
                        }
                        return recentStats == null ? null : recentStats.getPackageName();
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }
                } else {
                    LogUtils.d("getForegroundApp", "无\"有权查看使用权限的应用\"选项");
                }
            }
            return null;
        }

        /**
         * 获取打开App的意图
         *
         * @param context     上下文
         * @param packageName 包名
         * @return intent
         */
        public static Intent getLaunchAppIntent(Context context, String packageName) {
            return context.getPackageManager().getLaunchIntentForPackage(packageName);
        }


        /**
         * 获取安装App(支持6.0)的意图
         *
         * @param file 文件
         * @return intent
         */
        public static Intent getInstallAppIntent(File file) {
            if (file == null) return null;
            Intent intent = new Intent(Intent.ACTION_VIEW);
            String type;
            if (Build.VERSION.SDK_INT < 23) {
                type = "application/vnd.android.package-archive";
            } else {
                type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(getFileExtension(file));
            }
            intent.setDataAndType(Uri.fromFile(file), type);
            return intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        /**
         * 获取卸载App的意图
         *
         * @param packageName 包名
         * @return intent
         */
        public static Intent getUninstallAppIntent(String packageName) {
            Intent intent = new Intent(Intent.ACTION_DELETE);
            intent.setData(Uri.parse("package:" + packageName));
            return intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        /**
         * 根据文件路径获取文件
         *
         * @param filePath 文件路径
         * @return 文件
         */
        public static File getFileByPath(String filePath) {
            return isSpace(filePath) ? null : new File(filePath);
        }


        /**
         * 判断文件是否存在
         *
         * @param file 文件
         * @return {@code true}: 存在<br>{@code false}: 不存在
         */
        public static boolean isFileExists(File file) {
            return file != null && file.exists();
        }


        /**
         * 获取全路径中的文件拓展名
         *
         * @param file 文件
         * @return 文件拓展名
         */
        public static String getFileExtension(File file) {
            if (file == null) return null;
            return getFileExtension(file.getPath());
        }

        /**
         * 获取全路径中的文件拓展名
         *
         * @param filePath 文件路径
         * @return 文件拓展名
         */
        public static String getFileExtension(String filePath) {
            if (isSpace(filePath)) return filePath;
            int lastPoi = filePath.lastIndexOf('.');
            int lastSep = filePath.lastIndexOf(File.separator);
            if (lastPoi == -1 || lastSep >= lastPoi) return "";
            return filePath.substring(lastPoi + 1);
        }
    }
}
