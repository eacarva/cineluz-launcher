package com.elisson.cineluz;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Display;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Muitas TV boxes não desligam de verdade: o botão do controle só apaga a tela
 * ou desliga a saída HDMI (modo de espera), às vezes sem o Android nem pausar
 * a tela inicial. Aqui qualquer um desses sinais, depois de um tempo apagado,
 * conta como "ligar o aparelho", para a abertura automática funcionar nelas.
 *
 * Os últimos eventos ficam guardados e aparecem na tela "Sobre", para ajudar
 * a descobrir o que cada aparelho avisa.
 */
final class PowerEvents {

    interface Listener {
        /** O aparelho acabou de acordar do modo de espera. */
        void onWake();
    }

    // Qualquer apagar/acender conta: algumas boxes (ex.: Rockchip) param o relógio
    // durante o modo de espera, e minutos desligada parecem só alguns segundos
    private static final long MIN_OFF_MS = 0;
    // A tela inicial precisa aparecer logo depois de acordar (dá tempo de
    // fechar o relógio de descanso, se ele estiver por cima)
    private static final long WAKE_WINDOW_MS = 3 * 60_000;

    private static final String PREFS = "power";
    private static final String PREF_OFF_AT = "off_at";   // elapsedRealtime de quando apagou
    private static final String PREF_LOG = "log";
    private static final int LOG_LINES = 5;

    private static boolean registered;
    private static SharedPreferences prefs;
    private static long wokeAt = -1;
    private static Listener listener;

    private PowerEvents() {
    }

    static synchronized void register(Context appContext) {
        if (registered) return;
        registered = true;
        prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Handler main = new Handler(Looper.getMainLooper());

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_DREAMING_STARTED);
        filter.addAction(Intent.ACTION_DREAMING_STOPPED);
        filter.addAction("android.media.action.HDMI_AUDIO_PLUG");
        appContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action == null) return;
                switch (action) {
                    case Intent.ACTION_SCREEN_OFF:
                        off("tela apagou");
                        break;
                    case Intent.ACTION_SCREEN_ON:
                        on("tela acendeu");
                        break;
                    case Intent.ACTION_DREAMING_STARTED:
                        log("descanso de tela começou");
                        break;
                    case Intent.ACTION_DREAMING_STOPPED:
                        log("descanso de tela terminou");
                        break;
                    default:
                        // Estado atual entregue ao registrar: não é um evento novo
                        if (isInitialStickyBroadcast()) return;
                        int state = intent.getIntExtra("android.media.extra.AUDIO_PLUG_STATE", -1);
                        if (state == 0) off("HDMI desligado");
                        else if (state == 1) on("HDMI ligado");
                }
            }
        }, filter);

        // Algumas boxes desligam só o monitor, sem avisar "tela apagou"
        DisplayManager dm = (DisplayManager) appContext.getSystemService(Context.DISPLAY_SERVICE);
        if (dm != null) {
            Display display = dm.getDisplay(Display.DEFAULT_DISPLAY);
            final int[] lastState = {display != null ? display.getState() : Display.STATE_UNKNOWN};
            dm.registerDisplayListener(new DisplayManager.DisplayListener() {
                @Override
                public void onDisplayChanged(int displayId) {
                    if (displayId != Display.DEFAULT_DISPLAY) return;
                    Display d = dm.getDisplay(displayId);
                    if (d == null) return;
                    int state = d.getState();
                    if (state == lastState[0]) return;
                    lastState[0] = state;
                    if (state == Display.STATE_OFF) off("monitor desligou");
                    else if (state == Display.STATE_ON) on("monitor ligou");
                }

                @Override
                public void onDisplayAdded(int displayId) {
                }

                @Override
                public void onDisplayRemoved(int displayId) {
                }
            }, main);
        }
    }

    static synchronized void setListener(Listener l) {
        listener = l;
    }

    private static synchronized void off(String what) {
        log(what);
        // Guarda no armazenamento: a box pode fechar o Cineluz enquanto dorme
        if (prefs.getLong(PREF_OFF_AT, -1) < 0) {
            prefs.edit().putLong(PREF_OFF_AT, SystemClock.elapsedRealtime()).apply();
        }
    }

    private static synchronized void on(String what) {
        log(what);
        if (checkWoke()) {
            wokeAt = SystemClock.elapsedRealtime();
            if (listener != null) listener.onWake();
        }
    }

    /** Estava apagado há tempo suficiente? Limpa a marca de "apagado". */
    private static boolean checkWoke() {
        long offAt = prefs.getLong(PREF_OFF_AT, -1);
        if (offAt < 0) return false;
        prefs.edit().remove(PREF_OFF_AT).apply();
        long now = SystemClock.elapsedRealtime();
        // offAt maior que agora: o aparelho reiniciou (isso é tratado à parte)
        return offAt <= now && now - offAt >= MIN_OFF_MS;
    }

    /**
     * True uma vez só, se o aparelho acabou de acordar do modo de espera.
     * Também cobre o caso em que o Cineluz foi fechado enquanto o aparelho
     * dormia e não viu a tela acender.
     */
    static synchronized boolean consumeWake() {
        if (prefs == null) return false;
        long now = SystemClock.elapsedRealtime();
        boolean recent = wokeAt >= 0 && now - wokeAt < WAKE_WINDOW_MS;
        wokeAt = -1;
        if (recent) return true;
        if (checkWoke()) {
            log("acordou (visto ao abrir)");
            return true;
        }
        return false;
    }

    private static void log(String what) {
        String time = new SimpleDateFormat("dd/MM HH:mm:ss", Locale.ROOT).format(new Date());
        String old = prefs.getString(PREF_LOG, "");
        StringBuilder sb = new StringBuilder(time).append("  ").append(what);
        String[] lines = old.isEmpty() ? new String[0] : old.split("\n");
        for (int i = 0; i < lines.length && i < LOG_LINES - 1; i++) sb.append('\n').append(lines[i]);
        prefs.edit().putString(PREF_LOG, sb.toString()).apply();
    }

    /** Últimos eventos de energia, do mais novo para o mais antigo. */
    static String recentLog(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(PREF_LOG, "");
    }
}
