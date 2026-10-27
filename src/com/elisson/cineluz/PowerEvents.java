package com.elisson.cineluz;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;

/**
 * Muitas TV boxes não desligam de verdade: o botão do controle só apaga a tela
 * (modo de espera). Aqui a tela acender depois de um tempo apagada conta como
 * "ligar o aparelho", para a abertura automática funcionar nelas também.
 */
final class PowerEvents {

    // Tela apagada por menos que isso (ex.: um toque sem querer no botão) não conta
    private static final long MIN_OFF_MS = 60_000;
    // A tela inicial precisa aparecer logo depois de acordar
    private static final long WAKE_WINDOW_MS = 60_000;

    private static boolean registered;
    private static long screenOffAt = -1;
    private static long wokeAt = -1;

    private PowerEvents() {
    }

    static synchronized void register(Context appContext) {
        if (registered) return;
        registered = true;
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        appContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long now = SystemClock.elapsedRealtime();
                if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                    screenOffAt = now;
                } else if (screenOffAt >= 0 && now - screenOffAt >= MIN_OFF_MS) {
                    wokeAt = now;
                }
            }
        }, filter);
    }

    /** True uma vez só, se o aparelho acabou de acordar do modo de espera. */
    static synchronized boolean consumeWake() {
        boolean recent = wokeAt >= 0 && SystemClock.elapsedRealtime() - wokeAt < WAKE_WINDOW_MS;
        wokeAt = -1;
        return recent;
    }
}
