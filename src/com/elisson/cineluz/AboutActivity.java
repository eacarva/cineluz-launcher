package com.elisson.cineluz;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * "Sobre o Cineluz": versão, endereço do projeto e, se configurado, um QR code
 * discreto para quem quiser apoiar o projeto (aponta o celular e pronto).
 */
public class AboutActivity extends Activity {

    private float unit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        unit = Math.max(getResources().getDisplayMetrics().widthPixels,
                getResources().getDisplayMetrics().heightPixels) / 1920f;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(HomeActivity.BG_COLOR);
        root.setFocusable(true);

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.app_icon);
        root.addView(icon, new LinearLayout.LayoutParams(px(120), px(120)));

        root.addView(text(getString(R.string.app_name), 60, Color.WHITE, 24));
        root.addView(text(getString(R.string.about_version, versionName()), 30, 0xB3FFFFFF, 8));
        root.addView(text(getString(R.string.about_tagline), 30, 0xB3FFFFFF, 8));
        root.addView(text(getString(R.string.about_project_url), 30, Color.WHITE, 24));

        TextView verse = text(getString(R.string.about_verse), 28, 0xCCFFFFFF, 36);
        verse.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
        verse.setMaxWidth(px(1100));
        root.addView(verse);
        root.addView(text(getString(R.string.about_verse_ref), 26, 0x99FFFFFF, 6));

        // Apoio: só aparece se o projeto tiver um link de doação configurado
        String donateUrl = getString(R.string.donate_url);
        int qr = getResources().getIdentifier("donate_qr", "drawable", getPackageName());
        if (!TextUtils.isEmpty(donateUrl) && qr != 0) {
            root.addView(text(getString(R.string.donate_text), 30, Color.WHITE, 40));
            ImageView code = new ImageView(this);
            code.setImageResource(qr);
            code.setBackgroundColor(Color.WHITE);
            code.setPadding(px(14), px(14), px(14), px(14));
            LinearLayout.LayoutParams qlp = new LinearLayout.LayoutParams(px(200), px(200));
            qlp.topMargin = px(16);
            root.addView(code, qlp);
            root.addView(text(donateUrl, 26, 0xB3FFFFFF, 14));
        }

        // Diagnóstico: o que o aparelho avisou ao desligar/ligar (ajuda em boxes
        // onde a abertura automática não funciona)
        String power = PowerEvents.recentLog(this);
        if (!power.isEmpty()) {
            TextView diag = text(getString(R.string.about_power_log) + "\n" + power, 20, 0x80FFFFFF, 36);
            root.addView(diag);
        }

        setContentView(root);
    }

    private TextView text(String s, int sizePx, int color, int topMargin) {
        TextView tv = new TextView(this);
        tv.setText(s);
        tv.setTextColor(color);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(sizePx));
        tv.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = px(topMargin);
        tv.setLayoutParams(lp);
        return tv;
    }

    private String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "?";
        }
    }

    private int px(float designPx) {
        return Math.round(designPx * unit);
    }
}
