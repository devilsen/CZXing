package me.devilsen.czxing.compat;

import android.accessibilityservice.AccessibilityService;
import android.accounts.AccountManager;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AppOpsManager;
import android.app.DownloadManager;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.app.SearchManager;
import android.app.UiModeManager;
import android.app.WallpaperManager;
import android.app.admin.DevicePolicyManager;
import android.app.job.JobScheduler;
import android.app.usage.UsageStatsManager;
import android.appwidget.AppWidgetManager;
import android.bluetooth.BluetoothManager;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.RestrictionsManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherApps;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.hardware.ConsumerIrManager;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraManager;
import android.hardware.display.DisplayManager;
import android.hardware.input.InputManager;
import android.hardware.usb.UsbManager;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaRouter;
import android.media.projection.MediaProjectionManager;
import android.media.session.MediaSessionManager;
import android.media.tv.TvInputManager;
import android.net.ConnectivityManager;
import android.net.nsd.NsdManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.nfc.NfcManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.DropBoxManager;
import android.os.PowerManager;
import android.os.Process;
import android.os.UserManager;
import android.os.Vibrator;
import android.os.Build.VERSION;
import android.os.storage.StorageManager;
import android.print.PrintManager;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.view.accessibility.CaptioningManager;
import android.view.inputmethod.InputMethodManager;
import android.view.textservice.TextServicesManager;
import java.io.File;
import java.util.HashMap;

public class ContextCompat {
    private static final String TAG = "ContextCompat";
    private static final Object sLock = new Object();
    private static TypedValue sTempValue;

    protected ContextCompat() {
    }

    public static boolean startActivities( Context context,  Intent[] intents) {
        return startActivities(context, intents, (Bundle)null);
    }

    public static boolean startActivities( Context context,  Intent[] intents,  Bundle options) {
        if (VERSION.SDK_INT >= 16) {
            context.startActivities(intents, options);
        } else {
            context.startActivities(intents);
        }

        return true;
    }

    public static void startActivity( Context context,  Intent intent,  Bundle options) {
        if (VERSION.SDK_INT >= 16) {
            context.startActivity(intent, options);
        } else {
            context.startActivity(intent);
        }

    }

    
    public static File getDataDir( Context context) {
        if (VERSION.SDK_INT >= 24) {
            return context.getDataDir();
        } else {
            String dataDir = context.getApplicationInfo().dataDir;
            return dataDir != null ? new File(dataDir) : null;
        }
    }

    
    public static File[] getObbDirs( Context context) {
        return VERSION.SDK_INT >= 19 ? context.getObbDirs() : new File[]{context.getObbDir()};
    }

    
    public static File[] getExternalFilesDirs( Context context,  String type) {
        return VERSION.SDK_INT >= 19 ? context.getExternalFilesDirs(type) : new File[]{context.getExternalFilesDir(type)};
    }

    
    public static File[] getExternalCacheDirs( Context context) {
        return VERSION.SDK_INT >= 19 ? context.getExternalCacheDirs() : new File[]{context.getExternalCacheDir()};
    }

    private static File buildPath(File base, String... segments) {
        File cur = base;
        String[] var3 = segments;
        int var4 = segments.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            String segment = var3[var5];
            if (cur == null) {
                cur = new File(segment);
            } else if (segment != null) {
                cur = new File(cur, segment);
            }
        }

        return cur;
    }

    
    public static Drawable getDrawable( Context context,  int id) {
        if (VERSION.SDK_INT >= 21) {
            return context.getDrawable(id);
        } else if (VERSION.SDK_INT >= 16) {
            return context.getResources().getDrawable(id);
        } else {
            int resolvedId;
            synchronized(sLock) {
                if (sTempValue == null) {
                    sTempValue = new TypedValue();
                }

                context.getResources().getValue(id, sTempValue, true);
                resolvedId = sTempValue.resourceId;
            }

            return context.getResources().getDrawable(resolvedId);
        }
    }

    
    public static ColorStateList getColorStateList( Context context,  int id) {
        return VERSION.SDK_INT >= 23 ? context.getColorStateList(id) : context.getResources().getColorStateList(id);
    }

    
    public static int getColor( Context context,  int id) {
        return VERSION.SDK_INT >= 23 ? context.getColor(id) : context.getResources().getColor(id);
    }

    public static int checkSelfPermission( Context context,  String permission) {
        if (permission == null) {
            throw new IllegalArgumentException("permission is null");
        } else {
            return context.checkPermission(permission, Process.myPid(), Process.myUid());
        }
    }

    
    public static File getNoBackupFilesDir( Context context) {
        if (VERSION.SDK_INT >= 21) {
            return context.getNoBackupFilesDir();
        } else {
            ApplicationInfo appInfo = context.getApplicationInfo();
            return createFilesDir(new File(appInfo.dataDir, "no_backup"));
        }
    }

    public static File getCodeCacheDir( Context context) {
        if (VERSION.SDK_INT >= 21) {
            return context.getCodeCacheDir();
        } else {
            ApplicationInfo appInfo = context.getApplicationInfo();
            return createFilesDir(new File(appInfo.dataDir, "code_cache"));
        }
    }

    private static synchronized File createFilesDir(File file) {
        if (!file.exists() && !file.mkdirs()) {
            if (file.exists()) {
                return file;
            } else {
                Log.w("ContextCompat", "Unable to create files subdir " + file.getPath());
                return null;
            }
        } else {
            return file;
        }
    }

    
    public static Context createDeviceProtectedStorageContext( Context context) {
        return VERSION.SDK_INT >= 24 ? context.createDeviceProtectedStorageContext() : null;
    }

    public static boolean isDeviceProtectedStorage( Context context) {
        return VERSION.SDK_INT >= 24 ? context.isDeviceProtectedStorage() : false;
    }

    public static void startForegroundService( Context context,  Intent intent) {
        if (VERSION.SDK_INT >= 26) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }

    }

    public static String getSystemServiceName( Context context,  Class<?> serviceClass) {
        return VERSION.SDK_INT >= 23 ? context.getSystemServiceName(serviceClass) : (String)ContextCompat.LegacyServiceMapHolder.SERVICES.get(serviceClass);
    }

    private static final class LegacyServiceMapHolder {
        static final HashMap<Class<?>, String> SERVICES = new HashMap();

        private LegacyServiceMapHolder() {
        }

        static {
            if (VERSION.SDK_INT > 22) {
                SERVICES.put(SubscriptionManager.class, "telephony_subscription_service");
                SERVICES.put(UsageStatsManager.class, "usagestats");
            }

            if (VERSION.SDK_INT > 21) {
                SERVICES.put(AppWidgetManager.class, "appwidget");
                SERVICES.put(BatteryManager.class, "batterymanager");
                SERVICES.put(CameraManager.class, "camera");
                SERVICES.put(JobScheduler.class, "jobscheduler");
                SERVICES.put(LauncherApps.class, "launcherapps");
                SERVICES.put(MediaProjectionManager.class, "media_projection");
                SERVICES.put(MediaSessionManager.class, "media_session");
                SERVICES.put(RestrictionsManager.class, "restrictions");
                SERVICES.put(TelecomManager.class, "telecom");
                SERVICES.put(TvInputManager.class, "tv_input");
            }

            if (VERSION.SDK_INT > 19) {
                SERVICES.put(AppOpsManager.class, "appops");
                SERVICES.put(CaptioningManager.class, "captioning");
                SERVICES.put(ConsumerIrManager.class, "consumer_ir");
                SERVICES.put(PrintManager.class, "print");
            }

            if (VERSION.SDK_INT > 18) {
                SERVICES.put(BluetoothManager.class, "bluetooth");
            }

            if (VERSION.SDK_INT > 17) {
                SERVICES.put(DisplayManager.class, "display");
                SERVICES.put(UserManager.class, "user");
            }

            if (VERSION.SDK_INT > 16) {
                SERVICES.put(InputManager.class, "input");
                SERVICES.put(MediaRouter.class, "media_router");
                SERVICES.put(NsdManager.class, "servicediscovery");
            }

            SERVICES.put(AccessibilityService.class, "accessibility");
            SERVICES.put(AccountManager.class, "account");
            SERVICES.put(ActivityManager.class, "activity");
            SERVICES.put(AlarmManager.class, "alarm");
            SERVICES.put(AudioManager.class, "audio");
            SERVICES.put(ClipboardManager.class, "clipboard");
            SERVICES.put(ConnectivityManager.class, "connectivity");
            SERVICES.put(DevicePolicyManager.class, "device_policy");
            SERVICES.put(DownloadManager.class, "download");
            SERVICES.put(DropBoxManager.class, "dropbox");
            SERVICES.put(InputMethodManager.class, "input_method");
            SERVICES.put(KeyguardManager.class, "keyguard");
            SERVICES.put(LayoutInflater.class, "layout_inflater");
            SERVICES.put(LocationManager.class, "location");
            SERVICES.put(NfcManager.class, "nfc");
            SERVICES.put(NotificationManager.class, "notification");
            SERVICES.put(PowerManager.class, "power");
            SERVICES.put(SearchManager.class, "search");
            SERVICES.put(SensorManager.class, "sensor");
            SERVICES.put(StorageManager.class, "storage");
            SERVICES.put(TelephonyManager.class, "phone");
            SERVICES.put(TextServicesManager.class, "textservices");
            SERVICES.put(UiModeManager.class, "uimode");
            SERVICES.put(UsbManager.class, "usb");
            SERVICES.put(Vibrator.class, "vibrator");
            SERVICES.put(WallpaperManager.class, "wallpaper");
            SERVICES.put(WifiP2pManager.class, "wifip2p");
            SERVICES.put(WifiManager.class, "wifi");
            SERVICES.put(WindowManager.class, "src/window");
        }
    }
}
