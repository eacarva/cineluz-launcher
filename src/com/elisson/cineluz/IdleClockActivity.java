package com.elisson.cineluz;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.WindowManager;

/**
 * Relógio que a tela inicial abre quando ninguém mexe no controle por um
 * tempo. Aparece antes do descanso de tela do sistema — que em alguns
 * projetores é do fabricante e mostra anúncios. Qualquer tecla volta.
 *
 * Fica ligado pelo tempo de "suspender" do aparelho (padrão de 4 horas) e
 * depois deixa o sistema seguir seu curso normal.
 */
public class IdleClockActivity extends Activity {

    private static final long DEFAULT_SLEEP_MS = 4 * 60 * 60 * 1000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private ClockView clock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        clock = new ClockView(this);
        setContentView(clock);

        long sleepMs = Settings.Secure.getLong(getContentResolver(), "sleep_timeout", DEFAULT_SLEEP_MS);
        if (sleepMs <= 0) sleepMs = DEFAULT_SLEEP_MS;
        handler.postDelayed(() ->
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON), sleepMs);
    }

    @Override
    protected void onStart() {
        super.onStart();
        clock.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        clock.stop();
        handler.removeCallbacksAndMessages(null);
        finish();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Qualquer tecla (inclusive Home) só fecha o relógio
        if (event.getAction() == KeyEvent.ACTION_UP) finish();
        return true;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        finish();
        return true;
    }
}
