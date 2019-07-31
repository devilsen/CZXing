package me.devilsen.czxing.compat;

import android.app.Activity;
import android.app.SharedElementCallback;
import android.app.SharedElementCallback.OnSharedElementsReadyListener;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Build.VERSION;
import android.view.DragEvent;
import android.view.View;

public class ActivityCompat extends ContextCompat {
    private static ActivityCompat.PermissionCompatDelegate sDelegate;

    protected ActivityCompat() {
    }

    public static void setPermissionCompatDelegate( ActivityCompat.PermissionCompatDelegate delegate) {
        sDelegate = delegate;
    }

    public static ActivityCompat.PermissionCompatDelegate getPermissionCompatDelegate() {
        return sDelegate;
    }

    /** @deprecated */
    @Deprecated
    public static boolean invalidateOptionsMenu(Activity activity) {
        activity.invalidateOptionsMenu();
        return true;
    }

    public static void startActivityForResult( Activity activity,  Intent intent, int requestCode,  Bundle options) {
        if (VERSION.SDK_INT >= 16) {
            activity.startActivityForResult(intent, requestCode, options);
        } else {
            activity.startActivityForResult(intent, requestCode);
        }

    }

    public static void startIntentSenderForResult( Activity activity,  IntentSender intent, int requestCode,  Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags,  Bundle options) throws SendIntentException {
        if (VERSION.SDK_INT >= 16) {
            activity.startIntentSenderForResult(intent, requestCode, fillInIntent, flagsMask, flagsValues, extraFlags, options);
        } else {
            activity.startIntentSenderForResult(intent, requestCode, fillInIntent, flagsMask, flagsValues, extraFlags);
        }

    }

    public static void finishAffinity( Activity activity) {
        if (VERSION.SDK_INT >= 16) {
            activity.finishAffinity();
        } else {
            activity.finish();
        }

    }

    public static void finishAfterTransition( Activity activity) {
        if (VERSION.SDK_INT >= 21) {
            activity.finishAfterTransition();
        } else {
            activity.finish();
        }

    }

    
    public static Uri getReferrer( Activity activity) {
        if (VERSION.SDK_INT >= 22) {
            return activity.getReferrer();
        } else {
            Intent intent = activity.getIntent();
            Uri referrer = (Uri)intent.getParcelableExtra("android.intent.extra.REFERRER");
            if (referrer != null) {
                return referrer;
            } else {
                String referrerName = intent.getStringExtra("android.intent.extra.REFERRER_NAME");
                return referrerName != null ? Uri.parse(referrerName) : null;
            }
        }
    }

    
    public static <T extends View> T requireViewById( Activity activity,  int id) {
        if (VERSION.SDK_INT >= 28) {
            return activity.requireViewById(id);
        } else {
            T view = activity.findViewById(id);
            if (view == null) {
                throw new IllegalArgumentException("ID does not reference a View inside this Activity");
            } else {
                return view;
            }
        }
    }

    public static void postponeEnterTransition( Activity activity) {
        if (VERSION.SDK_INT >= 21) {
            activity.postponeEnterTransition();
        }

    }

    public static void startPostponedEnterTransition( Activity activity) {
        if (VERSION.SDK_INT >= 21) {
            activity.startPostponedEnterTransition();
        }

    }

    public static void requestPermissions( final Activity activity,  final String[] permissions,  final int requestCode) {
        if (sDelegate == null || !sDelegate.requestPermissions(activity, permissions, requestCode)) {
            if (VERSION.SDK_INT >= 23) {
                if (activity instanceof ActivityCompat.RequestPermissionsRequestCodeValidator) {
                    ((ActivityCompat.RequestPermissionsRequestCodeValidator)activity).validateRequestPermissionsRequestCode(requestCode);
                }

                activity.requestPermissions(permissions, requestCode);
            } else if (activity instanceof ActivityCompat.OnRequestPermissionsResultCallback) {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    public void run() {
                        int[] grantResults = new int[permissions.length];
                        PackageManager packageManager = activity.getPackageManager();
                        String packageName = activity.getPackageName();
                        int permissionCount = permissions.length;

                        for(int i = 0; i < permissionCount; ++i) {
                            grantResults[i] = packageManager.checkPermission(permissions[i], packageName);
                        }

                        ((ActivityCompat.OnRequestPermissionsResultCallback)activity).onRequestPermissionsResult(requestCode, permissions, grantResults);
                    }
                });
            }

        }
    }

    public static boolean shouldShowRequestPermissionRationale( Activity activity,  String permission) {
        return VERSION.SDK_INT >= 23 ? activity.shouldShowRequestPermissionRationale(permission) : false;
    }

    
    public static DragAndDropPermissionsCompat requestDragAndDropPermissions(Activity activity, DragEvent dragEvent) {
        return DragAndDropPermissionsCompat.request(activity, dragEvent);
    }

    public interface RequestPermissionsRequestCodeValidator {
        void validateRequestPermissionsRequestCode(int var1);
    }

    public interface PermissionCompatDelegate {
        boolean requestPermissions( Activity var1,  String[] var2,  int var3);

        boolean onActivityResult( Activity var1,  int var2, int var3,  Intent var4);
    }

    public interface OnRequestPermissionsResultCallback {
        void onRequestPermissionsResult(int var1,  String[] var2,  int[] var3);
    }
}
