package com.elisson.cineluz;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Indicador de rede da tela inicial: mostra se está no cabo, no Wi-Fi (com o
 * nome da rede e o sinal) ou sem rede. OK abre a lista de redes Wi-Fi.
 */
class NetworkChip extends LinearLayout {

    private final ImageView icon;
    private final TextView label;
    private final ConnectivityManager connectivity;
    private final WifiManager wifi;
    private final Handler main = new Handler(Looper.getMainLooper());
    private ConnectivityManager.NetworkCallback callback;

    NetworkChip(Context ctx, float unit) {
        super(ctx);
        connectivity = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        wifi = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setPadding(px(unit, 18), px(unit, 10), px(unit, 30), px(unit, 10));
        setFocusable(true);
        setClickable(true);

        icon = new ImageView(ctx);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        addView(icon, new LayoutParams(px(unit, 44), px(unit, 44)));

        label = new TextView(ctx);
        label.setTextColor(Color.WHITE);
        label.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(unit, 28));
        label.setSingleLine(true);
        LayoutParams llp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        llp.leftMargin = px(unit, 12);
        addView(label, llp);

        Ui.pillBackground(this, unit);

        setOnClickListener(v -> openNetworkSettings());
        refresh();
    }

    private static int px(float unit, float designPx) {
        return Math.round(designPx * unit);
    }

    void start() {
        callback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                main.post(NetworkChip.this::refresh);
            }

            @Override
            public void onLost(Network network) {
                main.post(NetworkChip.this::refresh);
            }

            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities caps) {
                main.post(NetworkChip.this::refresh);
            }
        };
        try {
            connectivity.registerDefaultNetworkCallback(callback);
        } catch (Exception ignored) {
            callback = null;
        }
        refresh();
    }

    void stop() {
        if (callback != null) {
            try {
                connectivity.unregisterNetworkCallback(callback);
            } catch (Exception ignored) {
            }
            callback = null;
        }
    }

    private void refresh() {
        NetworkCapabilities caps = connectivity.getNetworkCapabilities(connectivity.getActiveNetwork());
        if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
            show(R.drawable.ic_lan, getContext().getString(R.string.net_ethernet));
        } else if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            WifiInfo info = wifi.getConnectionInfo();
            int level = info != null ? WifiManager.calculateSignalLevel(info.getRssi(), 4) : 3;
            int[] icons = {R.drawable.ic_wifi_1, R.drawable.ic_wifi_2, R.drawable.ic_wifi_3, R.drawable.ic_wifi_4};
            show(icons[level], "Wi-Fi  ·  " + ssid(info));
        } else {
            show(R.drawable.ic_wifi_off, getContext().getString(R.string.net_none));
        }
    }

    private String ssid(WifiInfo info) {
        String name = info != null ? info.getSSID() : null;
        if (name == null || name.isEmpty() || name.contains("unknown ssid")) {
            return getContext().getString(R.string.net_connected);
        }
        if (name.startsWith("\"") && name.endsWith("\"")) name = name.substring(1, name.length() - 1);
        return name;
    }

    private void show(int iconRes, String text) {
        icon.setImageResource(iconRes);
        label.setText(text);
    }

    private void openNetworkSettings() {
        // Direto na lista de redes Wi-Fi. Com cabo (ou adaptador USB-Ethernet)
        // o Android passa a usar a rede cabeada sozinho.
        Launcher.start(getContext(), new Intent("android.settings.WIFI_SETTINGS"));
    }
}
