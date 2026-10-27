package com.elisson.cineluz;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextClock;

/**
 * Relógio grande e data sobre fundo preto, que muda de lugar devagar a cada
 * minuto (evita marcar a imagem). Usado pelo descanso de tela e pelo relógio
 * de inatividade da tela inicial.
 */
class ClockView extends FrameLayout {

    private static final long MOVE_EVERY_MS = 60_000;

    private final LinearLayout clock;
    private final Runnable mover = this::move;
    private boolean running;

    ClockView(Context ctx) {
        super(ctx);
        setBackgroundColor(Color.BLACK);
        float unit = Math.max(getResources().getDisplayMetrics().widthPixels,
                getResources().getDisplayMetrics().heightPixels) / 1920f;

        clock = new LinearLayout(ctx);
        clock.setOrientation(LinearLayout.VERTICAL);
        clock.setGravity(Gravity.CENTER_HORIZONTAL);
        clock.addView(text(ctx, "HH:mm", 220 * unit, 0xFFFFFFFF));
        clock.addView(text(ctx, "EEEE, d 'de' MMMM", 44 * unit, 0xB3FFFFFF));
        clock.setAlpha(0f);
        addView(clock, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    }

    void start() {
        if (running) return;
        running = true;
        post(mover);
    }

    void stop() {
        running = false;
        removeCallbacks(mover);
        clock.animate().cancel();
    }

    private void move() {
        if (!running) return;
        // Sorteia uma posição longe da anterior (pelo menos 1/3 da área livre),
        // para nenhum ponto da tela ficar aceso no mesmo lugar por muito tempo
        float maxX = Math.max(0, getWidth() - clock.getWidth());
        float maxY = Math.max(0, getHeight() - clock.getHeight());
        float x = 0, y = 0;
        for (int i = 0; i < 20; i++) {
            x = (float) (Math.random() * maxX);
            y = (float) (Math.random() * maxY);
            float dx = (x - clock.getTranslationX()) / Math.max(1, maxX);
            float dy = (y - clock.getTranslationY()) / Math.max(1, maxY);
            if (Math.hypot(dx, dy) > 0.33) break;
        }
        final float nx = x, ny = y;
        clock.animate().alpha(0f).setDuration(clock.getAlpha() > 0f ? 1500 : 0).withEndAction(() -> {
            clock.setTranslationX(nx);
            clock.setTranslationY(ny);
            clock.animate().alpha(1f).setDuration(1500).start();
        }).start();
        postDelayed(mover, MOVE_EVERY_MS);
    }

    private static TextClock text(Context ctx, String format, float sizePx, int color) {
        TextClock tc = new TextClock(ctx);
        tc.setFormat12Hour(format);
        tc.setFormat24Hour(format);
        tc.setTextColor(color);
        tc.setTextSize(TypedValue.COMPLEX_UNIT_PX, sizePx);
        tc.setIncludeFontPadding(false);
        return tc;
    }
}
