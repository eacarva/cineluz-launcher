package com.elisson.cineluz;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Descobre as portas HDMI do aparelho pela lista de entradas de TV do Android
 * (funciona em qualquer projetor/TV com entradas HDMI) e sabe abri-las em tela cheia.
 */
final class HdmiPorts {

    /** Uma porta HDMI física. */
    static final class Port {
        final String inputId;
        final int number;          // 1, 2, 3... (0 se não deu para descobrir)
        final String label;        // "HDMI 2"
        final boolean connected;   // tem cabo/sinal
        final String deviceName;   // nome do aparelho via HDMI-CEC, se houver

        Port(String inputId, int number, String label, boolean connected, String deviceName) {
            this.inputId = inputId;
            this.number = number;
            this.label = label;
            this.connected = connected;
            this.deviceName = deviceName;
        }
    }

    // Projetores Allwinner (como o HY320) têm um app próprio de HDMI, que cuida
    // do som e do controle remoto melhor que uma tela cheia genérica
    private static final String ALLWINNER_HDMI_APP = "com.softwinner.awlivetv";
    private static final String ALLWINNER_INPUT_SERVICE = "com.softwinner.vis";

    private static final Pattern TRAILING_NUMBER = Pattern.compile("(\\d+)\\D*$");

    // Nomes genéricos que o HDMI-CEC usa enquanto o aparelho ainda não se
    // identificou (ex.: ao ligar ou acordar): "Playback_1", "Recorder_2"...
    private static final Pattern GENERIC_CEC_NAME = Pattern.compile(
            "(?i)^(playback|recorder|tuner|audio[ _]?system|tv|reserved|free[ _]?use|unregistered)[ _]?\\d*$");
    private static final String PREFS = "hdmi_names";

    private HdmiPorts() {
    }

    static TvInputManager manager(Context ctx) {
        if (!ctx.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LIVE_TV)) return null;
        return (TvInputManager) ctx.getSystemService(Context.TV_INPUT_SERVICE);
    }

    /** Portas HDMI físicas, na ordem do número da porta. */
    static List<Port> list(Context ctx) {
        List<Port> ports = new ArrayList<>();
        TvInputManager tim = manager(ctx);
        if (tim == null) return ports;

        List<TvInputInfo> inputs = tim.getTvInputList();
        Set<String> hidden = hiddenInputs(ctx);

        // Aparelhos ligados por HDMI-CEC aparecem como entradas "filhas" da porta
        Map<String, String> deviceNames = new HashMap<>();
        android.content.SharedPreferences names = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        for (TvInputInfo info : inputs) {
            if (info.getParentId() != null && info.getType() == TvInputInfo.TYPE_HDMI) {
                CharSequence name = info.loadLabel(ctx);
                if (TextUtils.isEmpty(name)) continue;
                String n = name.toString().trim();
                if (GENERIC_CEC_NAME.matcher(n).matches()) {
                    // Nome provisório: usa o último nome de verdade desta porta, se houver
                    String last = names.getString(info.getParentId(), null);
                    if (last != null) deviceNames.put(info.getParentId(), last);
                } else {
                    deviceNames.put(info.getParentId(), n);
                    names.edit().putString(info.getParentId(), n).apply();
                }
            }
        }

        for (TvInputInfo info : inputs) {
            if (info.getType() != TvInputInfo.TYPE_HDMI || info.getParentId() != null) continue;
            String id = info.getId();
            if (hidden.contains(id)) continue; // portas que o fabricante escondeu (sem conector)
            int number = portNumber(id, info.loadLabel(ctx));
            String label = number > 0 ? "HDMI " + number : String.valueOf(info.loadLabel(ctx));
            CharSequence custom = info.loadCustomLabel(ctx);
            if (!TextUtils.isEmpty(custom)) label = custom.toString();
            boolean connected = tim.getInputState(id) == TvInputManager.INPUT_STATE_CONNECTED;
            ports.add(new Port(id, number, label, connected, deviceNames.get(id)));
        }
        Collections.sort(ports, (a, b) -> Integer.compare(a.number, b.number));
        return ports;
    }

    static Port find(List<Port> ports, String inputId) {
        if (inputId == null) return null;
        for (Port p : ports) if (p.inputId.equals(inputId)) return p;
        return null;
    }

    /**
     * Porta a mostrar: a última escolhida se ainda tiver sinal; senão a primeira
     * com sinal; senão a última escolhida; senão a primeira que existir.
     */
    static Port choose(List<Port> ports, String preferredId) {
        Port preferred = find(ports, preferredId);
        if (preferred != null && preferred.connected) return preferred;
        for (Port p : ports) if (p.connected) return p;
        if (preferred != null) return preferred;
        return ports.isEmpty() ? null : ports.get(0);
    }

    /** Abre a porta em tela cheia. */
    static void open(Context ctx, Port port) {
        Intent intent;
        if (isAllwinner(ctx) && port.number > 0) {
            intent = new Intent();
            intent.setComponent(new ComponentName(ALLWINNER_HDMI_APP, ALLWINNER_HDMI_APP + ".MainActivity"));
            intent.putExtra("input_source", "HDMI" + port.number);
            // NEW_TASK + CLEAR_TASK: a entrada sempre reabre limpa (sem travar na troca)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        } else {
            intent = new Intent(ctx, PlayerActivity.class);
            intent.putExtra(PlayerActivity.EXTRA_INPUT_ID, port.inputId);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        Launcher.start(ctx, intent);
    }

    static boolean isAllwinner(Context ctx) {
        return installed(ctx, ALLWINNER_HDMI_APP) && installed(ctx, ALLWINNER_INPUT_SERVICE);
    }

    private static boolean installed(Context ctx, String pkg) {
        try {
            ctx.getPackageManager().getPackageInfo(pkg, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /** Entradas escondidas nas configurações de TV do Android. */
    private static Set<String> hiddenInputs(Context ctx) {
        Set<String> hidden = new HashSet<>();
        String value = Settings.Secure.getString(ctx.getContentResolver(), "tv_input_hidden_inputs");
        if (TextUtils.isEmpty(value)) return hidden;
        for (String part : value.split(":")) {
            if (!part.isEmpty()) hidden.add(Uri.decode(part));
        }
        return hidden;
    }

    /** "…/HdmiInputService/HW2" -> 2, "HDMI 3" -> 3. */
    private static int portNumber(String inputId, CharSequence label) {
        for (CharSequence s : new CharSequence[]{inputId, label}) {
            if (s == null) continue;
            Matcher m = TRAILING_NUMBER.matcher(s);
            if (m.find()) {
                try {
                    return Integer.parseInt(m.group(1));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return 0;
    }
}
