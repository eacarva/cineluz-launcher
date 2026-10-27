package com.elisson.cineluz;

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
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * "Ferramentas da box" (dentro do Sobre): abre telas que o menu de algumas
 * boxes esconde, em passos guiados — ligar o ADB, desativar/reativar os
 * serviços do Google para liberar memória — e mostra o diagnóstico de energia.
 * Também existe como app separado em tools/abrir-configuracoes.
 */
public class ToolsActivity extends Activity {

    private static final String PHONE = "com.android.settings";
    private static final String TV = "com.android.tv.settings";
    private static final String GMS = "com.google.android.gms";
    private static final String PLAY_STORE = "com.android.vending";

    private static final int TEXT_DIM = 0xB3FFFFFF;

    private float unit;
    private LinearLayout list;
    private int sectionCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        unit = Math.max(getResources().getDisplayMetrics().widthPixels,
                getResources().getDisplayMetrics().heightPixels) / 1920f;

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(HomeActivity.BG_COLOR);
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
        sectionCount = 0;
        text(getString(R.string.about_tools_title), 56, Color.WHITE, 0, true);
        text(getString(R.string.tools_subtitle), 28, TEXT_DIM, 8, false);

        // ---------- 1. ADB ----------
        section(R.string.tools_adb_section);
        List<Intent> dev = developerScreens();
        if (!dev.isEmpty()) {
            action(R.string.tools_dev_title, getString(R.string.tools_dev_hint), v -> openFirst(dev));
        }
        Intent about = prefer(new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS));
        if (resolves(about)) {
            action(R.string.tools_about_title, getString(R.string.tools_about_hint), v -> open(about));
        }

        // ---------- 2. Google ----------
        if (installed(GMS) || installed(PLAY_STORE)) {
            section(R.string.tools_google_section);
            text(getString(R.string.tools_google_note), 24, TEXT_DIM, 0, false);
            if (installed(GMS)) googleServicesStep();
            if (installed(PLAY_STORE)) {
                boolean on = enabled(PLAY_STORE);
                action(on ? R.string.tools_store_off_title : R.string.tools_store_on_title,
                        getString(on ? R.string.tools_state_on_hint : R.string.tools_state_off_hint),
                        v -> open(appDetails(PLAY_STORE)));
            }
        }

        // ---------- 3. Outros ----------
        Intent full = new Intent(Settings.ACTION_SETTINGS).setPackage(PHONE);
        if (resolves(full)) {
            section(R.string.tools_other_section);
            action(R.string.tools_settings_title, getString(R.string.tools_settings_hint), v -> open(full));
        }

        // ---------- 4. Diagnóstico ----------
        section(R.string.tools_diag_section);
        text(getString(R.string.tools_diag_note), 24, TEXT_DIM, 0, false);
        String log = PowerEvents.recentLog(this);
        text(log.isEmpty() ? getString(R.string.tools_diag_empty) : log, 24, Color.WHITE, 12, false);

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
                action(R.string.tools_gms_admin_title, getString(R.string.tools_gms_admin_hint), v -> open(admins));
                return;
            }
        }
        action(on ? R.string.tools_gms_off_title : R.string.tools_gms_on_title,
                getString(on ? R.string.tools_state_on_hint : R.string.tools_state_off_hint),
                v -> open(appDetails(GMS)));
    }

    // ---------- Telas ----------

    /**
     * Caminhos conhecidos para as Opções do desenvolvedor. As de TV primeiro: numa
     * box são as do próprio aparelho e costumam ter o ADB pela rede (na UniTV S1,
     * "Internet Adb"), que as do Android de celular não têm.
     */
    private List<Intent> developerScreens() {
        Intent[] candidates = {
                new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).setPackage(TV),
                new Intent().setComponent(new ComponentName(TV, TV + ".system.development.DevelopmentActivity")),
                new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).setPackage(PHONE),
                new Intent().setComponent(new ComponentName(PHONE, PHONE + ".Settings$DevelopmentSettingsDashboardActivity")),
                new Intent().setComponent(new ComponentName(PHONE, PHONE + ".DevelopmentSettings")),
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
        Toast.makeText(this, R.string.tools_cant_open, Toast.LENGTH_LONG).show();
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

    private void section(int titleRes) {
        // Numera só as seções que aparecem neste aparelho
        TextView t = text(++sectionCount + ". " + getString(titleRes), 34, Color.WHITE, 48, true);
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

    private void action(int titleRes, String hint, View.OnClickListener onClick) {
        LinearLayout card = Ui.card(this, unit, getString(titleRes), hint);
        card.setOnClickListener(onClick);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = px(14);
        list.addView(card, lp);
    }

    private int px(float designPx) {
        return Math.round(designPx * unit);
    }
}
