package com.elisson.cineluz;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;

/** Estilos visuais compartilhados. */
final class Ui {

    private Ui() {
    }

    /** Fundo de "pílula" translúcida, com borda branca quando em foco. */
    static void pillBackground(View view, float unit) {
        final GradientDrawable normal = new GradientDrawable();
        normal.setCornerRadius(40 * unit);
        normal.setColor(Color.argb(40, 255, 255, 255));
        final GradientDrawable focused = new GradientDrawable();
        focused.setCornerRadius(40 * unit);
        focused.setColor(Color.argb(70, 255, 255, 255));
        focused.setStroke(Math.round(4 * unit), Color.WHITE);
        view.setBackground(normal);
        view.setOnFocusChangeListener((v, hasFocus) -> v.setBackground(hasFocus ? focused : normal));
    }
}
