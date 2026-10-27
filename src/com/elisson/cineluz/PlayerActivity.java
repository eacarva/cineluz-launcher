package com.elisson.cineluz;

import android.app.Activity;
import android.graphics.Color;
import android.media.tv.TvContract;
import android.media.tv.TvView;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.WindowManager;

/**
 * Tela cheia de uma entrada HDMI, para aparelhos que não têm um app próprio de
 * HDMI. As teclas do controle que a tela não usa (setas, OK...) seguem para o
 * aparelho conectado via HDMI-CEC pela própria TvView.
 */
public class PlayerActivity extends Activity {

    static final String EXTRA_INPUT_ID = "input_id";

    private TvView tv;
    private String inputId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        inputId = getIntent().getStringExtra(EXTRA_INPUT_ID);
        tv = new TvView(this);
        tv.setBackgroundColor(Color.BLACK);
        setContentView(tv);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (inputId != null) {
            tv.tune(inputId, TvContract.buildChannelUriForPassthroughInput(inputId));
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        tv.reset();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int key = event.getKeyCode();
        if (key == KeyEvent.KEYCODE_BACK || key == KeyEvent.KEYCODE_HOME) {
            return super.dispatchKeyEvent(event);
        }
        // Repassa o resto para o aparelho no HDMI
        if (tv.dispatchKeyEvent(event)) return true;
        return super.dispatchKeyEvent(event);
    }
}
