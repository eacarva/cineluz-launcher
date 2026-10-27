package com.elisson.cineluz;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Estilos visuais compartilhados. */
final class Ui {

    private Ui() {
    }

    /** Fundo de "pílula" translúcida, com borda branca quando em foco. */
    static void pillBackground(View view, float unit) {
        roundedBackground(view, 40 * unit, unit);
    }

    /** Fundo translúcido arredondado, com borda branca quando em foco. */
    static void roundedBackground(View view, float radius, float unit) {
        final GradientDrawable normal = new GradientDrawable();
        normal.setCornerRadius(radius);
        normal.setColor(Color.argb(40, 255, 255, 255));
        final GradientDrawable focused = new GradientDrawable();
        focused.setCornerRadius(radius);
        focused.setColor(Color.argb(70, 255, 255, 255));
        focused.setStroke(Math.round(4 * unit), Color.WHITE);
        view.setBackground(normal);
        view.setOnFocusChangeListener((v, hasFocus) -> v.setBackground(hasFocus ? focused : normal));
    }

    /** Cartão clicável com título e, embaixo, uma explicação do que ele faz. */
    static LinearLayout card(Context ctx, float unit, String title, String hint) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(Math.round(36 * unit), Math.round(22 * unit), Math.round(36 * unit), Math.round(22 * unit));
        card.setFocusable(true);
        card.setClickable(true);
        roundedBackground(card, 18 * unit, unit);

        TextView t = new TextView(ctx);
        t.setText(title);
        t.setTextColor(Color.WHITE);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, 32 * unit);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        card.addView(t);

        TextView h = new TextView(ctx);
        h.setText(hint);
        h.setTextColor(0xB3FFFFFF);
        h.setTextSize(TypedValue.COMPLEX_UNIT_PX, 24 * unit);
        h.setPadding(0, Math.round(6 * unit), 0, 0);
        card.addView(h);
        return card;
    }
}
