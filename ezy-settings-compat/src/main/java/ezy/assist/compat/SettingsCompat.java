/*
 * Copyright 2016 czy1121
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ezy.assist.compat;

import android.Manifest;
import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import java.lang.reflect.Method;

public class SettingsCompat {
    private static final String TAG = "ezy-settings-compat";

    private static final int OP_WRITE_SETTINGS = 23;
    private static final int OP_SYSTEM_ALERT_WINDOW = 24;

    public static boolean canDrawOverlays(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return checkOp(context, OP_SYSTEM_ALERT_WINDOW);
        } else {
            return true;
        }
    }

    public static boolean canWriteSettings(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.System.canWrite(context);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return checkOp(context, OP_WRITE_SETTINGS);
        } else {
            return true;
        }
    }

    public static boolean setDrawOverlays(Context context, boolean allowed) {
        return setMode(context, OP_SYSTEM_ALERT_WINDOW, allowed);
    }

    public static boolean setWriteSettings(Context context, boolean allowed) {
        return setMode(context, OP_WRITE_SETTINGS, allowed);
    }

    public static void manageDrawOverlays(Context context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (RomUtil.isVivo()) {
                gotoSoftPermissionDetailActivity(context);
            } else {
                //启动Activity让用户授权
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                context.startActivity(intent);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if (manageDrawOverlaysForRom(context)) {
                return;
            }
        }

    }

    public static void gotoSoftPermissionDetailActivity(Context context) {
        Intent intent = new Intent();
        intent.putExtra("packagename", context.getPackageName());
        intent.putExtra("title", "test");
        //i管家包名 6.0有不相同的两款手机
        intent.setComponent(ComponentName.unflattenFromString("com.iqoo.secure/.safeguard.SoftPermissionDetailActivity"));
        if (startSafely(context, intent)) {
            return;
        }
        intent.setComponent(ComponentName.unflattenFromString("com.vivo.permissionmanager/.activity.SoftPermissionDetailActivity"));
        if (startSafely(context, intent)) {
            return;
        }
    }

    /**
     * 获取悬浮窗权限状态
     *
     * @return 1或其他是没有打开，0是打开，该状态的定义和{@link android.app.AppOpsManager#MODE_ALLOWED}，MODE_IGNORED
     * 等值差不多，自行查阅源码
     */
    public static int getFloatPermissionStatus(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context is null");
        }
        String packageName = context.getPackageName();
        Uri uri = Uri.parse(
                "content://com.iqoo.secure.provider.secureprovider/allowfloatwindowapp");
        String selection = "pkgname = ?";
        String[] selectionArgs = new String[]{packageName};
        Cursor cursor = context
                .getContentResolver()
                .query(uri, null, selection, selectionArgs, null);
        if (cursor != null) {
            cursor.getColumnNames();
            if (cursor.moveToFirst()) {
                int currentmode = cursor.getInt(cursor.getColumnIndex("currentlmode"));
                cursor.close();
                return currentmode;
            } else {
                cursor.close();
                return getFloatPermissionStatus2(context);
            }

        } else {
            return getFloatPermissionStatus2(context);
        }
    }


    /**
     * vivo比较新的系统获取方法
     */
    private static int getFloatPermissionStatus2(Context context) {
        String packageName = context.getPackageName();
        Uri uri2 = Uri.parse(
                "content://com.vivo.permissionmanager.provider.permission/float_window_apps");
        String selection = "pkgname = ?";
        String[] selectionArgs = new String[]{packageName};
        Cursor cursor = context
                .getContentResolver()
                .query(uri2, null, selection, selectionArgs, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int currentmode = cursor.getInt(cursor.getColumnIndex("currentmode"));
                cursor.close();
                return currentmode;
            } else {
                cursor.close();
                return 1;
            }
        }
        return 1;
    }

    public static void manageWriteSettings(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            context.startActivity(intent);
        }
    }

    private static boolean manageDrawOverlaysForRom(Context context) {
        if (RomUtil.isMiui()) {
            return manageDrawOverlaysForMiui(context);
        }
        if (RomUtil.isEmui()) {
            return manageDrawOverlaysForEmui(context);
        }
        if (RomUtil.isFlyme()) {
            return manageDrawOverlaysForFlyme(context);
        }
        if (RomUtil.isOppo()) {
            return manageDrawOverlaysForOppo(context);
        }
        if (RomUtil.isVivo()) {
            return manageDrawOverlaysForVivo(context);
        }
        if (RomUtil.isQiku()) {
            return manageDrawOverlaysForQihu(context);
        }
        if (RomUtil.isSmartisan()) {
            return manageDrawOverlaysForSmartisan(context);
        }
        return false;
    }


    private static boolean checkOp(Context context, int op) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            AppOpsManager manager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            try {
                Method method = AppOpsManager.class.getDeclaredMethod("checkOp", int.class, int.class,
                        String.class);
                return AppOpsManager.MODE_ALLOWED == (int) method.invoke(manager, op,
                        Binder.getCallingUid(), context.getPackageName());
            } catch (Exception e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }
        return false;
    }

    // 可设置Android 4.3/4.4的授权状态
    private static boolean setMode(Context context, int op, boolean allowed) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2
                || Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return false;
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            AppOpsManager manager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            try {
                Method method = AppOpsManager.class.getDeclaredMethod("setMode", int.class, int.class,
                        String.class, int.class);
                method.invoke(manager, op, Binder.getCallingUid(), context.getPackageName(),
                        allowed ? AppOpsManager.MODE_ALLOWED : AppOpsManager
                                .MODE_IGNORED);
                return true;
            } catch (Exception e) {
                Log.e(TAG, Log.getStackTraceString(e));

            }
        }
        return false;
    }

    private static boolean startSafely(Context context, Intent intent) {
        if (context.getPackageManager().queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY).size() > 0) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } else {
            Log.e(TAG, "Intent is not available! " + intent);
            return false;
        }
    }


    // 小米
    private static boolean manageDrawOverlaysForMiui(Context context) {
        Intent intent = new Intent("miui.intent.action.APP_PERM_EDITOR");
        intent.putExtra("extra_pkgname", context.getPackageName());
        intent.setClassName("com.miui.securitycenter",
                "com.miui.permcenter.permissions.AppPermissionsEditorActivity");
        if (startSafely(context, intent)) {
            return true;
        }
        intent.setClassName("com.miui.securitycenter",
                "com.miui.permcenter.permissions.PermissionsEditorActivity");
        if (startSafely(context, intent)) {
            return true;
        }
        // miui v5 的支持的android版本最高 4.x
        // http://www.romzj.com/list/search?keyword=MIUI%20V5#search_result
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Intent intent1 = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent1.setData(Uri.fromParts("package", context.getPackageName(), null));
            return startSafely(context, intent1);
        }
        return false;
    }

    private final static String HUAWEI_PACKAGE = "com.huawei.systemmanager";

    // 华为
    private static boolean manageDrawOverlaysForEmui(Context context) {
        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            intent.setClassName(HUAWEI_PACKAGE,
                    "com.huawei.systemmanager.addviewmonitor.AddViewMonitorActivity");
            if (startSafely(context, intent)) {
                return true;
            }
        }
        // Huawei Honor P6|4.4.4|3.0
        intent.setClassName(HUAWEI_PACKAGE,
                "com.huawei.notificationmanager.ui.NotificationManagmentActivity");
        intent.putExtra("showTabsNumber", 1);
        if (startSafely(context, intent)) {
            return true;
        }
        intent.setClassName(HUAWEI_PACKAGE, "com.huawei.permissionmanager.ui.MainActivity");
        if (startSafely(context, intent)) {
            return true;
        }
        return false;
    }

    // VIVO
    private static boolean manageDrawOverlaysForVivo(Context context) {
        // 不支持直接到达悬浮窗设置页，只能到 i管家 首页
        Intent intent = new Intent("com.iqoo.secure");
        intent.setClassName("com.iqoo.secure", "com.iqoo.secure.MainActivity");
        // com.iqoo.secure.ui.phoneoptimize.SoftwareManagerActivity
        // com.iqoo.secure.ui.phoneoptimize.FloatWindowManager
        return startSafely(context, intent);
    }

    // OPPO
    private static boolean manageDrawOverlaysForOppo(Context context) {
        Intent intent = new Intent();
        intent.putExtra("packageName", context.getPackageName());
        // OPPO A53|5.1.1|2.1
        intent.setAction("com.oppo.safe");
        intent.setClassName("com.oppo.safe",
                "com.oppo.safe.permission.floatwindow.FloatWindowListActivity");
        if (startSafely(context, intent)) {
            return true;
        }
        // OPPO R7s|4.4.4|2.1
        intent.setAction("com.color.safecenter");
        intent.setClassName("com.color.safecenter",
                "com.color.safecenter.permission.floatwindow.FloatWindowListActivity");
        if (startSafely(context, intent)) {
            return true;
        }
        intent.setAction("com.coloros.safecenter");
        intent.setClassName("com.coloros.safecenter",
                "com.coloros.safecenter.sysfloatwindow.FloatWindowListActivity");
        return startSafely(context, intent);
    }

    // 魅族
    private static boolean manageDrawOverlaysForFlyme(Context context) {
        Intent intent = new Intent("com.meizu.safe.security.SHOW_APPSEC");
        intent.setClassName("com.meizu.safe", "com.meizu.safe.security.AppSecActivity");
        intent.putExtra("packageName", context.getPackageName());
        return startSafely(context, intent);
    }

    // 360
    private static boolean manageDrawOverlaysForQihu(Context context) {
        Intent intent = new Intent();
        intent.setClassName("com.android.settings",
                "com.android.settings.Settings$OverlaySettingsActivity");
        if (startSafely(context, intent)) {
            return true;
        }
        intent.setClassName("com.qihoo360.mobilesafe",
                "com.qihoo360.mobilesafe.ui.index.AppEnterActivity");
        return startSafely(context, intent);
    }

    // 锤子
    private static boolean manageDrawOverlaysForSmartisan(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // 锤子 坚果|5.1.1|2.5.3
            Intent intent = new Intent("com.smartisanos.security.action.SWITCHED_PERMISSIONS_NEW");
            intent.setClassName("com.smartisanos.security",
                    "com.smartisanos.security.SwitchedPermissions");
            intent.putExtra("index", 17); // 不同版本会不一样
            return startSafely(context, intent);
        } else {
            // 锤子 坚果|4.4.4|2.1.2
            Intent intent = new Intent("com.smartisanos.security.action.SWITCHED_PERMISSIONS");
            intent.setClassName("com.smartisanos.security",
                    "com.smartisanos.security.SwitchedPermissions");
            intent.putExtra("permission", new String[]{Manifest.permission.SYSTEM_ALERT_WINDOW});

            //        Intent intent = new Intent("com.smartisanos.security.action.MAIN");
            //        intent.setClassName("com.smartisanos.security", "com.smartisanos.security
            //        .MainActivity");
            return startSafely(context, intent);
        }
    }
}
