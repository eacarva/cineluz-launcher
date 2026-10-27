package com.elisson.abrirconfig;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Abre telas de configuração que o menu de algumas TV boxes esconde, em passos
 * guiados: ligar o ADB (Opções do desenvolvedor) e desativar/reativar os
 * serviços do Google para liberar memória.
 */
public class MainActivity extends Activity {

    private static final String PHONE = "com.android.settings";
    private static final String TV = "com.android.tv.settings";
    private static final String GMS = "com.google.android.gms";
    private static final String PLAY_STORE = "com.android.vending";

    private static final int BG = Color.rgb(0x2E, 0x32, 0x36);
    private static final int CARD = Color.argb(40, 255, 255, 255);
    private static final int CARD_FOCUS = Color.argb(80, 255, 255, 255);
    private static final int TEXT_DIM = Color.argb(180, 255, 255, 255);

    private float unit;
    private LinearLayout list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        unit = Math.max(getResources().getDisplayMetrics().widthPixels,
                getResources().getDisplayMetrics().heightPixels) / 1920f;

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(px(240), px(60), px(240), px(60));
        scroll.addView(list);
        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Monta de novo ao voltar: o estado (ex.: Play Services desativado) pode ter mudado
        build();
    }

    private void build() {
        list.removeAllViews();
        text("Abrir Configurações", 56, Color.WHITE, 0, true);
        text("Telas que o menu desta box esconde. Siga os passos de cima para baixo.",
                28, TEXT_DIM, 8, false);

        // ---------- 1. ADB ----------
        section("1. Ligar o ADB (depuração)");
        List<Intent> dev = developerScreens();
        if (!dev.isEmpty()) {
            action("Abrir as Opções do desenvolvedor",
                    "Lá dentro, ligue \"Depuração USB\" e, se houver, o ADB pela rede.",
                    v -> openFirst(dev));
        }
        Intent about = prefer(new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS));
        if (resolves(about)) {
            action("Não abriu? Ative o modo desenvolvedor",
                    "Abre \"Sobre o dispositivo\": aperte OK 7 vezes em \"Número da versão\" e volte ao passo acima.",
                    v -> open(about));
        }

        // ---------- 2. Google ----------
        if (installed(GMS) || installed(PLAY_STORE)) {
            section("2. Serviços do Google (liberar memória)");
            text("Sem eles, a Play Store, o YouTube e outros apps do Google param de funcionar "
                    + "até você reativar aqui.", 24, TEXT_DIM, 0, false);
            if (installed(GMS)) googleServicesStep();
            if (installed(PLAY_STORE)) {
                boolean on = enabled(PLAY_STORE);
                action((on ? "Desativar" : "Reativar") + " a Play Store",
                        "Estado: " + (on ? "ativa" : "desativada") + ". Na tela que abrir, aperte \""
                                + (on ? "Desativar" : "Ativar") + "\".",
                        v -> open(appDetails(PLAY_STORE)));
            }
        }

        // ---------- 3. Outros ----------
        section("3. Outros");
        Intent full = new Intent(Settings.ACTION_SETTINGS).setPackage(PHONE);
        if (resolves(full)) {
            action("Configurações completas do Android",
                    "Todas as opções do Android, inclusive as que o menu da box não mostra.",
                    v -> open(full));
        }

        // Foco no primeiro botão
        for (int i = 0; i < list.getChildCount(); i++) {
            if (list.getChildAt(i).isFocusable()) {
                list.getChildAt(i).requestFocus();
                break;
            }
        }
    }

    /** O Play Services só pode ser desativado depois de deixar de ser administrador. */
    private void googleServicesStep() {
        boolean on = enabled(GMS);
        if (on && isDeviceAdmin(GMS)) {
            Intent admins = new Intent().setComponent(new ComponentName(PHONE, PHONE + ".DeviceAdminSettings"));
            if (resolves(admins)) {
                action("Desativar o Google Play Services: passo A",
                        "Estado: ativo. Abra cada item do Google (\"Localizador\", \"Screen lock service\") "
                                + "e escolha \"Desativar o administrador\". Depois volte aqui.",
                        v -> open(admins));
                return;
            }
        }
        action(on ? "Desativar o Google Play Services" : "Reativar o Google Play Services",
                "Estado: " + (on ? "ativo" : "desativado") + ". Na tela que abrir, aperte \""
                        + (on ? "Desativar" : "Ativar") + "\".",
                v -> open(appDetails(GMS)));
    }

    // ---------- Telas ----------

    /** Caminhos conhecidos para as Opções do desenvolvedor, do mais comum ao mais raro. */
    private List<Intent> developerScreens() {
        Intent[] candidates = {
                new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).setPackage(PHONE),
                new Intent().setComponent(new ComponentName(PHONE, PHONE + ".Settings$DevelopmentSettingsDashboardActivity")),
                new Intent().setComponent(new ComponentName(PHONE, PHONE + ".DevelopmentSettings")),
                new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).setPackage(TV),
                new Intent().setComponent(new ComponentName(TV, TV + ".system.development.DevelopmentActivity")),
        };
        List<Intent> found = new ArrayList<>();
        for (Intent i : candidates) if (resolves(i)) found.add(i);
        return found;
    }

    private Intent appDetails(String pkg) {
        return prefer(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", pkg, null)));
    }

    /** Boxes com dois apps de configurações: abre direto as completas, sem perguntar. */
    private Intent prefer(Intent intent) {
        for (ResolveInfo ri : getPackageManager().queryIntentActivities(intent, 0)) {
            if (PHONE.equals(ri.activityInfo.packageName)) return intent.setClassName(PHONE, ri.activityInfo.name);
        }
        return intent;
    }

    private void openFirst(List<Intent> intents) {
        for (Intent i : intents) {
            try {
                startActivity(new Intent(i).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return;
            } catch (Exception ignored) {
                // tenta o próximo caminho
            }
        }
        Toast.makeText(this, "Esta box não deixou abrir. Tente o botão de baixo.", Toast.LENGTH_LONG).show();
    }

    private void open(Intent intent) {
        List<Intent> one = new ArrayList<>();
        one.add(intent);
        openFirst(one);
    }

    private boolean resolves(Intent intent) {
        return intent.resolveActivity(getPackageManager()) != null;
    }

    private boolean installed(String pkg) {
        try {
            getPackageManager().getApplicationInfo(pkg, PackageManager.MATCH_DISABLED_COMPONENTS);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private boolean enabled(String pkg) {
        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(pkg, PackageManager.MATCH_DISABLED_COMPONENTS);
            return ai.enabled;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private boolean isDeviceAdmin(String pkg) {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        List<ComponentName> admins = dpm != null ? dpm.getActiveAdmins() : null;
        if (admins == null) return false;
        for (ComponentName cn : admins) if (pkg.equals(cn.getPackageName())) return true;
        return false;
    }

    // ---------- Visual ----------

    private void section(String title) {
        TextView t = text(title, 34, Color.WHITE, 48, true);
        t.setPadding(0, 0, 0, px(8));
    }

    private TextView text(String s, int size, int color, int topMargin, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(s);
        tv.setTextColor(color);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(size));
        if (bold) tv.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = px(topMargin);
        list.addView(tv, lp);
        return tv;
    }

    /** Botão grande: título e, embaixo, o que fazer. Borda branca quando em foco. */
    private void action(String title, String hint, View.OnClickListener onClick) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(px(36), px(22), px(36), px(22));
        card.setFocusable(true);
        card.setClickable(true);
        card.setOnClickListener(onClick);

        final GradientDrawable normal = new GradientDrawable();
        normal.setCornerRadius(px(18));
        normal.setColor(CARD);
        final GradientDrawable focused = new GradientDrawable();
        focused.setCornerRadius(px(18));
        focused.setColor(CARD_FOCUS);
        focused.setStroke(px(4), Color.WHITE);
        card.setBackground(normal);
        card.setOnFocusChangeListener((v, has) -> v.setBackground(has ? focused : normal));

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(Color.WHITE);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(32));
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        card.addView(t);

        TextView h = new TextView(this);
        h.setText(hint);
        h.setTextColor(TEXT_DIM);
        h.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(24));
        h.setPadding(0, px(6), 0, 0);
        card.addView(h);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = px(14);
        list.addView(card, lp);
    }

    private int px(float designPx) {
        return Math.round(designPx * unit);
    }
}
