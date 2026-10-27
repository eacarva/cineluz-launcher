package com.elisson.cineluz;

import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.service.dreams.DreamService;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextClock;

/**
 * Descanso de tela do Cineluz: relógio grande e data sobre fundo escuro, que
 * muda de lugar devagar a cada minuto. Sem anúncios, sem internet.
 */
public class ClockDream extends DreamService {

    private static final long MOVE_EVERY_MS = 60_000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private LinearLayout clock;
    private FrameLayout root;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        setScreenBright(false);

        float unit = Math.max(getResources().getDisplayMetrics().widthPixels,
                getResources().getDisplayMetrics().heightPixels) / 1920f;

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        clock = new LinearLayout(this);
        clock.setOrientation(LinearLayout.VERTICAL);
        clock.setGravity(Gravity.CENTER_HORIZONTAL);
        clock.addView(text("HH:mm", 220 * unit, 0xFFFFFFFF));
        clock.addView(text("EEEE, d 'de' MMMM", 44 * unit, 0xB3FFFFFF));
        root.addView(clock, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));

        setContentView(root);
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        root.post(this::moveClock);
    }

    @Override
    public void onDreamingStopped() {
        super.onDreamingStopped();
        handler.removeCallbacksAndMessages(null);
    }

    /** Muda o relógio de lugar devagar (evita marcar a imagem na tela). */
    private void moveClock() {
        int maxX = Math.max(0, root.getWidth() - clock.getWidth());
        int maxY = Math.max(0, root.getHeight() - clock.getHeight());
        float x = (float) (Math.random() * maxX);
        float y = (float) (Math.random() * maxY);
        if (clock.getAlpha() == 0f || clock.getTranslationX() == 0f && clock.getTranslationY() == 0f) {
            clock.setTranslationX(x);
            clock.setTranslationY(y);
            clock.setAlpha(0f);
            clock.animate().alpha(1f).setDuration(1500).start();
        } else {
            clock.animate().alpha(0f).setDuration(1500).withEndAction(() -> {
                clock.setTranslationX(x);
                clock.setTranslationY(y);
                clock.animate().alpha(1f).setDuration(1500).start();
            }).start();
        }
        handler.postDelayed(this::moveClock, MOVE_EVERY_MS);
    }

    private TextClock text(String format, float sizePx, int color) {
        TextClock tc = new TextClock(this);
        tc.setFormat12Hour(format);
        tc.setFormat24Hour(format);
        tc.setTextColor(color);
        tc.setTextSize(TypedValue.COMPLEX_UNIT_PX, sizePx);
        tc.setIncludeFontPadding(false);
        return tc;
    }
}
