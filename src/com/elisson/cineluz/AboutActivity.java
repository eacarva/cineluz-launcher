package com.elisson.cineluz;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * "Sobre o Cineluz", em duas colunas: à esquerda a marca (versão, projeto e
 * versículo); à direita as Ferramentas da box e, se configurado, o QR code
 * discreto para quem quiser apoiar o projeto.
 */
public class AboutActivity extends Activity {

    // Âmbar da identidade "Feixe"
    private static final int ACCENT = 0xFFFFB547;
    private static final int TEXT_DIM = 0xB3FFFFFF;

    private float unit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        unit = Math.max(getResources().getDisplayMetrics().widthPixels,
                getResources().getDisplayMetrics().heightPixels) / 1920f;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setGravity(Gravity.CENTER_VERTICAL);
        root.setBackgroundColor(HomeActivity.BG_COLOR);
        root.setPadding(px(200), px(80), px(200), px(80));

        root.addView(brandColumn(), new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(px(640),
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rlp.leftMargin = px(100);
        root.addView(sideColumn(), rlp);

        setContentView(root);
    }

    /** Marca: ícone, nome, versão, descrição, endereço do projeto e versículo. */
    private LinearLayout brandColumn() {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.app_icon);
        col.addView(icon, new LinearLayout.LayoutParams(px(160), px(160)));

        TextView name = text(getString(R.string.app_name), 76, Color.WHITE, 36);
        name.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        col.addView(name);
        col.addView(text(getString(R.string.about_version, versionName()), 30, ACCENT, 4));
        col.addView(text(getString(R.string.about_tagline), 32, TEXT_DIM, 24));
        col.addView(text(getString(R.string.about_project_url), 30, Color.WHITE, 16));

        View line = new View(this);
        line.setBackgroundColor(ACCENT);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(px(80), px(4));
        llp.topMargin = px(48);
        col.addView(line, llp);

        TextView verse = text(getString(R.string.about_verse).replace("\n", " "), 30, 0xDDFFFFFF, 28);
        verse.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
        col.addView(verse);
        col.addView(text(getString(R.string.about_verse_ref), 26, TEXT_DIM, 10));
        return col;
    }

    /** Ferramentas da box e, se o projeto tiver link de doação, o cartão de apoio. */
    private LinearLayout sideColumn() {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);

        LinearLayout tools = Ui.card(this, unit, getString(R.string.about_tools_title),
                getString(R.string.about_tools_hint));
        tools.setOnClickListener(v -> startActivity(new Intent(this, ToolsActivity.class)));
        col.addView(tools, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        tools.requestFocus();

        String donateUrl = getString(R.string.donate_url);
        int qr = getResources().getIdentifier("donate_qr", "drawable", getPackageName());
        if (!TextUtils.isEmpty(donateUrl) && qr != 0) {
            LinearLayout donate = new LinearLayout(this);
            donate.setOrientation(LinearLayout.VERTICAL);
            donate.setGravity(Gravity.CENTER_HORIZONTAL);
            donate.setPadding(px(36), px(28), px(36), px(32));
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(px(18));
            bg.setColor(Color.argb(25, 255, 255, 255));
            donate.setBackground(bg);

            TextView title = text(getString(R.string.about_donate_title), 32, Color.WHITE, 0);
            title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            donate.addView(title);
            TextView hint = text(getString(R.string.donate_text), 24, TEXT_DIM, 6);
            hint.setGravity(Gravity.CENTER);
            donate.addView(hint);

            ImageView code = new ImageView(this);
            code.setImageResource(qr);
            GradientDrawable white = new GradientDrawable();
            white.setCornerRadius(px(12));
            white.setColor(Color.WHITE);
            code.setBackground(white);
            code.setPadding(px(14), px(14), px(14), px(14));
            LinearLayout.LayoutParams qlp = new LinearLayout.LayoutParams(px(240), px(240));
            qlp.topMargin = px(20);
            donate.addView(code, qlp);
            donate.addView(text(donateUrl, 22, TEXT_DIM, 14));

            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            dlp.topMargin = px(24);
            col.addView(donate, dlp);
        }
        return col;
    }

    private TextView text(String s, int sizePx, int color, int topMargin) {
        TextView tv = new TextView(this);
        tv.setText(s);
        tv.setTextColor(color);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(sizePx));
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
