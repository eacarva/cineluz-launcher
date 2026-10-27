package com.elisson.cineluz;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * Os blocos configuráveis da tela inicial. Cada um tem um papel (espelhar
 * tela, arquivos...), uma lista de apps conhecidos para esse papel em vários
 * aparelhos, e pode apontar para qualquer app que o usuário escolher.
 */
enum TileSlot {

    AIRPLAY("airplay", R.string.tile_airplay, R.drawable.ic_cast,
            0xFFFF5F7E, 0xFFFF4560,
            new String[]{
                    "com.waxrain.airplaydmr2",   // AirPin (Pro)
                    "com.waxrain.airplaydmr",    // AirPin
                    "com.ionitech.airscreen",    // AirScreen
            },
            null),

    MIRACAST("miracast", R.string.tile_miracast, R.drawable.ic_screen_share,
            0xFFC0413F, 0xFF9E2B2B,
            new String[]{
                    "com.softwinner.miracastReceiver",
                    "com.droidlogic.miracast",
                    "com.rockchip.wfd",
                    "com.amlogic.miracast",
            },
            null),

    FILES("files", R.string.tile_files, R.drawable.ic_folder,
            0xFFFDB750, 0xFFF08A3E,
            new String[]{
                    "com.softwinner.TvdFileManager",
                    "com.droidlogic.FileBrower",
                    "com.lonelycatgames.Xplore",     // X-plore
                    "com.alphainventor.filemanager", // File Manager+
            },
            null),

    SETTINGS("settings", R.string.tile_settings, R.drawable.ic_settings,
            0xFFB43BB8, 0xFF8524D0,
            new String[]{
                    "com.htc.htcsettings",           // configurações dos projetores HY
            },
            "android.settings.SETTINGS");

    private static final String PREFS = "tiles";

    final String key;
    final int labelRes;
    final int iconRes;
    final int colorStart;
    final int colorEnd;
    private final String[] candidates;
    private final String fallbackAction;

    TileSlot(String key, int labelRes, int iconRes, int colorStart, int colorEnd,
             String[] candidates, String fallbackAction) {
        this.key = key;
        this.labelRes = labelRes;
        this.iconRes = iconRes;
        this.colorStart = colorStart;
        this.colorEnd = colorEnd;
        this.candidates = candidates;
        this.fallbackAction = fallbackAction;
    }

    /** App escolhido pelo usuário para este bloco (null = automático). */
    String customPackage(Context ctx) {
        return prefs(ctx).getString(key, null);
    }

    void setCustomPackage(Context ctx, String pkg) {
        SharedPreferences.Editor e = prefs(ctx).edit();
        if (pkg == null) e.remove(key); else e.putString(key, pkg);
        e.apply();
    }

    /** Nome a mostrar no bloco: o do app escolhido, ou o nome do papel. */
    String label(Context ctx) {
        String custom = customPackage(ctx);
        if (custom != null && Launcher.isInstalled(ctx, custom)) return Launcher.appLabel(ctx, custom);
        return ctx.getString(labelRes);
    }

    /** O que abrir ao apertar o bloco, ou null se não houver app para ele. */
    Intent intent(Context ctx) {
        Intent custom = Launcher.launchIntent(ctx, customPackage(ctx));
        if (custom != null) return custom;
        for (String pkg : candidates) {
            Intent i = Launcher.launchIntent(ctx, pkg);
            if (i != null) return i;
        }
        if (fallbackAction != null) {
            Intent i = new Intent(fallbackAction);
            if (i.resolveActivity(ctx.getPackageManager()) != null) return i;
        }
        return null;
    }

    static TileSlot byKey(String key) {
        for (TileSlot s : values()) if (s.key.equals(key)) return s;
        return null;
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
