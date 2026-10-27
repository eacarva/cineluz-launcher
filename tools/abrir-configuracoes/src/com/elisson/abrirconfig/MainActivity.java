package com.elisson.abrirconfig;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Abre telas de configuração que o menu da box esconde: as Configurações
 * completas do Android, "Sobre o dispositivo" (tocar 7x no número da versão)
 * e as Opções do desenvolvedor (Depuração USB / ADB).
 */
public class MainActivity extends Activity {

    private static final String PHONE = "com.android.settings";
    private static final String TV = "com.android.tv.settings";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(80, 40, 80, 40);

        TextView title = new TextView(this);
        title.setText("Abrir Configurações escondidas");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView hint = new TextView(this);
        hint.setText("1) \"Sobre o dispositivo\": toque 7 vezes em \"Número da versão\".\n"
                + "2) \"Opções do desenvolvedor\": ligue \"Depuração USB\" / \"Depuração ADB\" "
                + "(e \"Depuração por rede\", se existir).");
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, 16, 0, 24);
        root.addView(hint);

        addButton(root, "Sobre o dispositivo (Configurações completas)",
                Settings.ACTION_DEVICE_INFO_SETTINGS, PHONE);
        addButton(root, "Opções do desenvolvedor (Configurações completas)",
                Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS, PHONE);
        addButton(root, "Configurações completas do Android", Settings.ACTION_SETTINGS, PHONE);
        addButton(root, "Sobre o dispositivo (Configurações de TV)",
                Settings.ACTION_DEVICE_INFO_SETTINGS, TV);
        addButton(root, "Opções do desenvolvedor (Configurações de TV)",
                Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS, TV);
        addComponentButton(root, "Opções do desenvolvedor (tela direta)",
                new ComponentName(PHONE, PHONE + ".Settings$DevelopmentSettingsDashboardActivity"));
        addComponentButton(root, "Opções do desenvolvedor (tela direta, Android antigo)",
                new ComponentName(PHONE, PHONE + ".DevelopmentSettings"));
        addComponentButton(root, "Opções do desenvolvedor (tela direta de TV)",
                new ComponentName(TV, TV + ".system.development.DevelopmentActivity"));

        if (root.getChildCount() == 2) {
            TextView none = new TextView(this);
            none.setText("Nenhuma dessas telas existe neste aparelho.");
            root.addView(none);
        }

        setContentView(root);
    }

    private void addButton(LinearLayout root, String label, String action, String pkg) {
        addButton(root, label, new Intent(action).setPackage(pkg));
    }

    private void addComponentButton(LinearLayout root, String label, ComponentName cn) {
        addButton(root, label, new Intent().setComponent(cn));
    }

    private void addButton(LinearLayout root, String label, Intent intent) {
        // Só mostra o que existe neste aparelho
        if (intent.resolveActivity(getPackageManager()) == null) return;
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setOnClickListener(v -> {
            try {
                startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (Exception e) {
                Toast.makeText(this, "Esta tela não existe neste aparelho", Toast.LENGTH_SHORT).show();
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 8;
        root.addView(b, lp);
    }
}
