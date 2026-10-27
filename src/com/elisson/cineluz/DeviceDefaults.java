package com.elisson.cineluz;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

/**
 * Ajustes de fábrica para aparelhos conhecidos, aplicados uma vez só na
 * primeira abertura (depois, vale o que o usuário escolher).
 */
final class DeviceDefaults {

    private static final String PREFS = "device_defaults";
    private static final String PREF_APPLIED = "applied";

    private DeviceDefaults() {
    }

    static void apply(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (prefs.getBoolean(PREF_APPLIED, false)) return;
        prefs.edit().putBoolean(PREF_APPLIED, true).apply();

        // UniTV S1 (Rockchip): sem app de AirPlay; o bloco vira a loja de apps da box
        if ("Unitv_S1".equalsIgnoreCase(Build.MODEL)) {
            setIfFree(ctx, TileSlot.AIRPLAY, "com.swl.amarket");
        }
    }

    private static void setIfFree(Context ctx, TileSlot slot, String pkg) {
        if (slot.customPackage(ctx) == null && Launcher.isInstalled(ctx, pkg)) slot.setCustomPackage(ctx, pkg);
    }
}
