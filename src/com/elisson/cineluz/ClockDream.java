package com.elisson.cineluz;

import android.service.dreams.DreamService;

/**
 * Descanso de tela do Cineluz (para aparelhos que deixam escolher o descanso
 * de tela nas configurações): relógio grande, sem anúncios, sem internet.
 */
public class ClockDream extends DreamService {

    private ClockView clock;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        setScreenBright(false);
        clock = new ClockView(this);
        setContentView(clock);
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        clock.start();
    }

    @Override
    public void onDreamingStopped() {
        super.onDreamingStopped();
        clock.stop();
    }
}
