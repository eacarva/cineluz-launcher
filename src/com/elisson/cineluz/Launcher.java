package com.elisson.cineluz;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.widget.Toast;

/** Abrir apps e telas com segurança (sem derrubar o launcher se algo faltar). */
final class Launcher {

    private Launcher() {
    }

    static void start(Context ctx, Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            ctx.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(ctx, R.string.cant_open, Toast.LENGTH_SHORT).show();
        }
    }

    /** Intent que abre um app (preferindo a versão para TV), ou null se não estiver instalado. */
    static Intent launchIntent(Context ctx, String pkg) {
        if (pkg == null) return null;
        PackageManager pm = ctx.getPackageManager();
        Intent intent = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (intent == null) intent = pm.getLaunchIntentForPackage(pkg);
        return intent;
    }

    static boolean isInstalled(Context ctx, String pkg) {
        return launchIntent(ctx, pkg) != null;
    }

    static String appLabel(Context ctx, String pkg) {
        try {
            PackageManager pm = ctx.getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }
}
