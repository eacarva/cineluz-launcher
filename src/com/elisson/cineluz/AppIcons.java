package com.elisson.cineluz;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Ícone dos blocos para apps escolhidos pelo usuário: no lugar do ícone do app
 * (que costuma destoar dos blocos), um ícone branco no estilo do Cineluz
 * conforme o tipo de app. Se o tipo não for reconhecido, usa o ícone do app.
 */
final class AppIcons {

    // A ordem importa: "com.android.tv.settings" é configuração, não TV
    private static final Object[][] RULES = {
            {"settings", R.drawable.ic_settings},
            {"market|store|vending|aptoide|appgallery|loja", R.drawable.ic_store},
            {"explorer|file|documentsui|xplore|arquivos", R.drawable.ic_folder},
            {"miracast|wfd|wifidisplay|screen ?share", R.drawable.ic_screen_share},
            {"airplay|airpin|airscreen|dlna|cast", R.drawable.ic_cast},
            {"iptv|live ?tv|unitv|pluto|\\btv\\b", R.drawable.ic_live_tv},
            {"youtube|netflix|prime ?video|disney|hbo|max|globoplay|kodi|plex|stremio|video|player",
                    R.drawable.ic_video},
            {"browser|chrome|firefox|navegador", R.drawable.ic_web},
    };

    private static final Pattern[] PATTERNS = new Pattern[RULES.length];

    static {
        for (int i = 0; i < RULES.length; i++) PATTERNS[i] = Pattern.compile((String) RULES[i][0]);
    }

    private AppIcons() {
    }

    static Drawable forPackage(Context ctx, String pkg) {
        String text = (pkg + " " + Launcher.appLabel(ctx, pkg)).toLowerCase(Locale.ROOT);
        for (int i = 0; i < RULES.length; i++) {
            if (PATTERNS[i].matcher(text).find()) return ctx.getDrawable((Integer) RULES[i][1]);
        }
        try {
            return ctx.getPackageManager().getApplicationIcon(pkg);
        } catch (PackageManager.NameNotFoundException e) {
            return ctx.getDrawable(R.drawable.ic_apps);
        }
    }
}
