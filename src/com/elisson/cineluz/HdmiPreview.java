package com.elisson.cineluz;

import android.content.Context;
import android.media.AudioManager;
import android.media.tv.TvContract;
import android.media.tv.TvInputManager;
import android.media.tv.TvView;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.FrameLayout;

/**
 * Prévia ao vivo de uma porta HDMI dentro do bloco, sempre sem som.
 *
 * Por cima do vídeo fica uma "capa" com o degradê do bloco, que some quando a
 * imagem chega e volta se o sinal cair.
 */
final class HdmiPreview {

    interface Listener {
        /** A porta mostrada mudou (ou o estado de alguma porta mudou). */
        void onPortsChanged();
    }

    private final Context ctx;
    private final TvView tv;
    private final View cover;
    private final View icon;
    private final Listener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final AudioQuirks audio;
    private final TvInputManager tim;

    private String tunedInputId;
    private boolean active;

    private final TvInputManager.TvInputCallback inputsCallback = new TvInputManager.TvInputCallback() {
        @Override
        public void onInputStateChanged(String inputId, int state) {
            listener.onPortsChanged();
        }

        @Override
        public void onInputAdded(String inputId) {
            listener.onPortsChanged();
        }

        @Override
        public void onInputRemoved(String inputId) {
            listener.onPortsChanged();
        }

        @Override
        public void onTvInputInfoUpdated(android.media.tv.TvInputInfo info) {
            // Ex.: o aparelho no HDMI terminou de se identificar via CEC
            listener.onPortsChanged();
        }
    };

    HdmiPreview(Context ctx, FrameLayout tile, View icon, Listener listener) {
        this.ctx = ctx;
        this.icon = icon;
        this.listener = listener;
        this.audio = new AudioQuirks(ctx);
        this.tim = HdmiPorts.manager(ctx);

        tv = new TvView(ctx);
        tile.addView(tv, 0, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        cover = new View(ctx);
        cover.setBackground(tile.getBackground().getConstantState().newDrawable().mutate());
        tile.addView(cover, 1, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        tv.setCallback(new TvView.TvInputCallback() {
            @Override
            public void onVideoAvailable(String inputId) {
                showVideo(true);
                // O serviço de HDMI de alguns aparelhos religa o som quando o vídeo chega
                handler.postDelayed(audio::reinforceMute, 500);
                handler.postDelayed(audio::reinforceMute, 1500);
            }

            @Override
            public void onVideoUnavailable(String inputId, int reason) {
                showVideo(false);
            }

            @Override
            public void onDisconnected(String inputId) {
                // O serviço de HDMI às vezes reinicia quando o aparelho liga;
                // a sessão cai e é preciso conectar de novo
                showVideo(false);
                String id = tunedInputId;
                tunedInputId = null;
                if (active && id != null) handler.postDelayed(() -> tune(id), 1500);
            }
        });
    }

    /** Começa a acompanhar as entradas (chamar no onResume). */
    void resume() {
        active = true;
        audio.lockMute();
        if (tim != null) tim.registerCallback(inputsCallback, handler);
    }

    /** Solta a entrada e para de acompanhar (chamar no onPause). */
    void pause() {
        active = false;
        if (tim != null) tim.unregisterCallback(inputsCallback);
        stop();
    }

    /** Mostra a porta indicada (null = nenhuma). */
    void tune(String inputId) {
        if (!active) return;
        if (inputId == null) {
            stop();
            return;
        }
        if (inputId.equals(tunedInputId)) return;
        if (tunedInputId != null) stop();
        try {
            // Liga a entrada já em mudo travado: a prévia nunca toca som
            audio.beforeTune();
            tv.tune(inputId, TvContract.buildChannelUriForPassthroughInput(inputId));
            tv.setStreamVolume(0f);
            tunedInputId = inputId;
        } catch (Exception e) {
            audio.restore();
        }
    }

    /** Reinicia a prévia (recupera a imagem se ela tiver sumido). */
    void restart() {
        String id = tunedInputId;
        if (id == null) return;
        stop();
        handler.postDelayed(() -> tune(id), 300);
    }

    /**
     * Encerra a prévia sem "pico" de som: desliga a entrada ainda em mudo,
     * solta a sessão e só depois tira o mudo.
     */
    void stop() {
        handler.removeCallbacksAndMessages(null);
        if (tunedInputId != null) {
            audio.beforeRelease();
            tv.reset();
            tunedInputId = null;
            cover.setAlpha(1f);
            icon.setAlpha(1f);
        }
        audio.restore();
        if (active) audio.lockMute();
    }

    private void showVideo(boolean video) {
        float coverAlpha = video ? 0f : 1f;
        cover.animate().alpha(coverAlpha).setDuration(250).start();
        icon.animate().alpha(coverAlpha).setDuration(250).start();
    }

    /**
     * Controle de som. Em aparelhos Allwinner (como o HY320) o serviço de HDMI
     * liga o áudio direto no sistema de som e ignora o volume da prévia; lá usamos
     * os parâmetros do próprio sistema de som. Nos outros, o volume zero da prévia basta.
     */
    static final class AudioQuirks {
        private final AudioManager am;
        private final boolean allwinner;
        private boolean muted;

        AudioQuirks(Context ctx) {
            am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
            allwinner = HdmiPorts.isAllwinner(ctx);
        }

        /** "lock_mute" impede que o serviço de HDMI tire o mudo. */
        void lockMute() {
            if (!allwinner) return;
            am.setParameters("lock_mute=true");
            am.setParameters("audio_mute=true");
            muted = true;
        }

        void beforeTune() {
            if (!allwinner) return;
            lockMute();
            am.setParameters("hdmi_input=on");
        }

        void reinforceMute() {
            if (allwinner && muted) am.setParameters("audio_mute=true");
        }

        void beforeRelease() {
            if (allwinner) am.setParameters("hdmi_input=off");
        }

        void restore() {
            if (!allwinner || !muted) return;
            am.setParameters("lock_mute=false");
            am.setParameters("audio_mute=false");
            muted = false;
        }
    }
}
