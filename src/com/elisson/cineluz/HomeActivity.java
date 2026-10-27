package com.elisson.cineluz;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tela inicial do Cineluz Launcher: HDMI em primeiro (com prévia ao vivo),
 * quatro blocos configuráveis e a lista de apps.
 */
public class HomeActivity extends Activity implements HdmiPreview.Listener {

    // Cinza chumbo
    static final int BG_COLOR = Color.rgb(0x2E, 0x32, 0x36);
    private static final int SWITCH_ON = Color.rgb(0x34, 0xC7, 0x59);
    private static final int SWITCH_OFF = Color.rgb(0xE5, 0x48, 0x4D);
    private static final int HDMI_START = 0xFF5AA9F5, HDMI_END = 0xFF3B82F6;
    private static final int APPS_START = 0xFFA6D94F, APPS_END = 0xFF3DB88B;

    private static final String PREFS = "launcher";
    private static final String PREF_AUTO_HDMI = "auto_hdmi";
    private static final String PREF_LAST_BOOT = "last_boot_count";
    private static final String PREF_HDMI_PORT = "hdmi_port";

    private static final int REQUEST_PICK_APP = 1;

    // Medidas pensadas para tela 1920x1080; o resto é escalado
    private static final int GRID_LEFT = 206;
    private static final int GRID_WIDTH = 1508;
    private static final int GRID_TOP = 205;
    private static final int ROW_H = 330;
    private static final int GAP = 13;
    private static final int RADIUS = 18;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private float unit;
    private SharedPreferences prefs;

    private FrameLayout hdmiTile;
    private TextView hdmiLabel;
    private HdmiPreview preview;
    private List<HdmiPorts.Port> ports;
    private HdmiPorts.Port currentPort;
    private Set<String> connectedBefore = new HashSet<>();

    private NetworkChip networkChip;
    private GradientDrawable switchTrack;
    private View switchKnob;
    private boolean resumed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        unit = Math.max(dm.widthPixels, dm.heightPixels) / 1920f;

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(BG_COLOR);
        root.setClipChildren(false);

        addClock(root);

        // Indicador de rede no canto superior direito, alinhado aos blocos
        networkChip = new NetworkChip(this, unit);
        FrameLayout.LayoutParams nlp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.END);
        nlp.rightMargin = px(1920 - (GRID_LEFT + GRID_WIDTH));
        nlp.topMargin = px(48);
        root.addView(networkChip, nlp);

        // Linha 1: HDMI (largo) e dois blocos de espelhamento
        int y1 = GRID_TOP, y2 = GRID_TOP + ROW_H + GAP;
        int wide = 742, narrow = (GRID_WIDTH - wide - 2 * GAP) / 2;
        int x = GRID_LEFT;
        hdmiTile = addTile(root, x, y1, wide, R.drawable.ic_hdmi, getString(R.string.tile_hdmi),
                HDMI_START, HDMI_END, v -> openHdmi());
        hdmiLabel = (TextView) hdmiTile.getTag();
        hdmiTile.setOnLongClickListener(v -> {
            nextPort();
            return true;
        });
        preview = new HdmiPreview(this, hdmiTile, hdmiTile.getChildAt(0), this);
        x += wide + GAP;
        addSlotTile(root, TileSlot.AIRPLAY, x, y1, narrow);
        x += narrow + GAP;
        addSlotTile(root, TileSlot.MIRACAST, x, y1, narrow);

        // Linha 2: Arquivos, Configurações, Apps
        int third = (GRID_WIDTH - 2 * GAP) / 3;
        x = GRID_LEFT;
        addSlotTile(root, TileSlot.FILES, x, y2, third);
        x += third + GAP;
        addSlotTile(root, TileSlot.SETTINGS, x, y2, third);
        x += third + GAP;
        addTile(root, x, y2, third, R.drawable.ic_apps, getString(R.string.tile_apps),
                APPS_START, APPS_END, v -> startActivity(new Intent(this, AppsActivity.class)));

        addAutoHdmiSwitch(root, GRID_LEFT, y2 + ROW_H + 40);
        addSupportButton(root, GRID_LEFT + GRID_WIDTH - 72, y2 + ROW_H + 40);

        setContentView(root);
        hdmiTile.requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        networkChip.start();
        // Trava o mudo já na volta pra tela inicial: alguns apps de HDMI, ao
        // fechar, tiram o mudo por um instante e isso dava um "pico" de som
        preview.resume();
        refreshPorts(false);
        if (!maybeOpenHdmiAfterBoot()) {
            handler.postDelayed(this::tuneCurrentPort, 400);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        resumed = false;
        handler.removeCallbacksAndMessages(null);
        networkChip.stop();
        // Solta a entrada HDMI antes de outro app usar
        preview.pause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Home com o launcher já aberto: volta o foco pro HDMI e reinicia a
        // prévia (recupera a imagem se ela tiver sumido)
        hdmiTile.requestFocus();
        if (resumed) preview.restart();
    }

    @Override
    public void onBackPressed() {
        // Tela inicial: o "voltar" não leva a lugar nenhum
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Menu num bloco configurável: escolher o app dele
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            View focused = getCurrentFocus();
            if (focused != null && focused.getTag(R.id.tile_slot) instanceof TileSlot) {
                pickApp((TileSlot) focused.getTag(R.id.tile_slot));
                return true;
            }
            if (focused == hdmiTile) {
                nextPort();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_PICK_APP || resultCode != RESULT_OK || data == null) return;
        TileSlot slot = TileSlot.byKey(data.getStringExtra(AppsActivity.EXTRA_SLOT));
        if (slot == null) return;
        slot.setCustomPackage(this, data.getStringExtra(AppsActivity.EXTRA_PACKAGE));
        recreate();
    }

    // ---------- HDMI ----------

    @Override
    public void onPortsChanged() {
        if (resumed) refreshPorts(true);
    }

    /**
     * Relê as portas HDMI. Se um aparelho acabou de ser ligado numa porta,
     * a prévia passa para ela.
     */
    private void refreshPorts(boolean followNewConnection) {
        ports = HdmiPorts.list(this);
        Set<String> connectedNow = new HashSet<>();
        HdmiPorts.Port newlyConnected = null;
        for (HdmiPorts.Port p : ports) {
            if (!p.connected) continue;
            connectedNow.add(p.inputId);
            if (!connectedBefore.contains(p.inputId)) newlyConnected = p;
        }
        connectedBefore = connectedNow;

        if (followNewConnection && newlyConnected != null) {
            prefs.edit().putString(PREF_HDMI_PORT, newlyConnected.inputId).apply();
        }
        currentPort = HdmiPorts.choose(ports, prefs.getString(PREF_HDMI_PORT, null));
        updateHdmiLabel();
        if (followNewConnection) tuneCurrentPort();
    }

    private void tuneCurrentPort() {
        preview.tune(currentPort != null ? currentPort.inputId : null);
    }

    /** Passa para a próxima porta HDMI (segurar OK no bloco, ou Menu). */
    private void nextPort() {
        if (ports == null || ports.size() < 2) {
            toast(getString(ports == null || ports.isEmpty() ? R.string.hdmi_none : R.string.hdmi_single));
            return;
        }
        int i = ports.indexOf(currentPort);
        currentPort = ports.get((i + 1) % ports.size());
        prefs.edit().putString(PREF_HDMI_PORT, currentPort.inputId).apply();
        updateHdmiLabel();
        tuneCurrentPort();
        toast(currentPort.label + (currentPort.connected ? "" : " · " + getString(R.string.hdmi_no_signal)));
    }

    private void updateHdmiLabel() {
        if (currentPort == null) {
            hdmiLabel.setText(R.string.tile_hdmi);
            return;
        }
        String text = currentPort.label;
        if (currentPort.deviceName != null && currentPort.connected) text += "  ·  " + currentPort.deviceName;
        hdmiLabel.setText(text);
    }

    private void openHdmi() {
        if (currentPort == null) {
            toast(getString(R.string.hdmi_none));
            return;
        }
        preview.stop();
        HdmiPorts.open(this, currentPort);
    }

    /**
     * Na primeira vez que a tela inicial aparece depois de ligar, abre o HDMI.
     * Devolve true se vai abrir.
     */
    private boolean maybeOpenHdmiAfterBoot() {
        int bootCount = Settings.Global.getInt(getContentResolver(), "boot_count", -1);
        if (bootCount < 0) return false;
        int lastBoot = prefs.getInt(PREF_LAST_BOOT, -1);
        if (bootCount == lastBoot) return false;
        prefs.edit().putInt(PREF_LAST_BOOT, bootCount).apply();

        boolean firstRunEver = lastBoot < 0;
        boolean justBooted = SystemClock.elapsedRealtime() < 10 * 60 * 1000;
        if (firstRunEver || !justBooted || !prefs.getBoolean(PREF_AUTO_HDMI, true)) return false;
        openHdmiWhenReady(0);
        return true;
    }

    /** Logo depois de ligar, as entradas podem demorar a aparecer: tenta por alguns segundos. */
    private void openHdmiWhenReady(int attempt) {
        handler.postDelayed(() -> {
            refreshPorts(false);
            boolean anyConnected = currentPort != null && currentPort.connected;
            if (anyConnected || attempt >= 6) {
                if (currentPort != null) openHdmi(); else tuneCurrentPort();
            } else {
                openHdmiWhenReady(attempt + 1);
            }
        }, attempt == 0 ? 1500 : 1000);
    }

    private void toggleAutoHdmi() {
        boolean enabled = !prefs.getBoolean(PREF_AUTO_HDMI, true);
        prefs.edit().putBoolean(PREF_AUTO_HDMI, enabled).apply();
        updateAutoHdmiSwitch(true);
    }

    // ---------- Blocos configuráveis ----------

    private void addSlotTile(FrameLayout root, TileSlot slot, int x, int y, int w) {
        FrameLayout tile = addTile(root, x, y, w, slot.iconRes, slot.label(this),
                slot.colorStart, slot.colorEnd, v -> {
                    Intent intent = slot.intent(this);
                    if (intent != null) Launcher.start(this, intent); else pickApp(slot);
                });
        tile.setTag(R.id.tile_slot, slot);
        tile.setOnLongClickListener(v -> {
            pickApp(slot);
            return true;
        });
    }

    private void pickApp(TileSlot slot) {
        Intent intent = new Intent(this, AppsActivity.class);
        intent.putExtra(AppsActivity.EXTRA_SLOT, slot.key);
        intent.putExtra(AppsActivity.EXTRA_TITLE, getString(R.string.pick_title, getString(slot.labelRes)));
        startActivityForResult(intent, REQUEST_PICK_APP);
    }

    // ---------- Montagem da tela ----------

    private int px(float designPx) {
        return Math.round(designPx * unit);
    }

    private void addClock(FrameLayout root) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout date = new LinearLayout(this);
        date.setOrientation(LinearLayout.VERTICAL);
        date.addView(clock("EEE", 34));
        date.addView(clock("dd-MM-yyyy", 34));
        row.addView(date);

        View divider = new View(this);
        divider.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(px(4), px(76));
        dlp.leftMargin = px(24);
        dlp.rightMargin = px(24);
        row.addView(divider, dlp);

        row.addView(clock("HH:mm", 84));

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = px(GRID_LEFT + 16);
        lp.topMargin = px(36);
        root.addView(row, lp);
    }

    private TextClock clock(String format, int sizePx) {
        TextClock tc = new TextClock(this);
        tc.setFormat12Hour(format);
        tc.setFormat24Hour(format);
        tc.setTextColor(Color.WHITE);
        tc.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(sizePx));
        tc.setIncludeFontPadding(false);
        return tc;
    }

    /** Bloco com degradê, ícone em cima e nome embaixo. O TextView do nome fica no tag. */
    private FrameLayout addTile(FrameLayout root, int x, int y, int w, int iconRes, String label,
                                int colorStart, int colorEnd, View.OnClickListener onClick) {
        final FrameLayout tile = new FrameLayout(this);
        tile.setFocusable(true);
        tile.setFocusableInTouchMode(true);
        tile.setClickable(true);
        tile.setOnClickListener(onClick);

        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{colorStart, colorEnd});
        bg.setCornerRadius(px(RADIUS));
        tile.setBackground(bg);
        tile.setClipToOutline(true);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setScaleType(ImageView.ScaleType.FIT_START);
        FrameLayout.LayoutParams ilp = new FrameLayout.LayoutParams(px(96), px(96),
                Gravity.TOP | Gravity.START);
        ilp.leftMargin = px(34);
        ilp.topMargin = px(30);
        tile.addView(icon, ilp);

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(38));
        tv.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        tv.setSingleLine(true);
        tv.setShadowLayer(px(6), 0, px(2), Color.argb(90, 0, 0, 0));
        FrameLayout.LayoutParams tlp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.START);
        tlp.leftMargin = px(38);
        tlp.rightMargin = px(38);
        tlp.bottomMargin = px(30);
        tile.addView(tv, tlp);
        tile.setTag(tv);

        final GradientDrawable border = new GradientDrawable();
        border.setStroke(px(5), Color.WHITE);
        border.setCornerRadius(px(RADIUS));
        border.setColor(Color.TRANSPARENT);

        tile.setOnFocusChangeListener((v, hasFocus) -> {
            tile.setForeground(hasFocus ? border : null);
            tile.setTranslationZ(hasFocus ? px(16) : 0);
            float s = hasFocus ? 1.05f : 1f;
            tile.animate().scaleX(s).scaleY(s).setDuration(120).start();
        });

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(px(w), px(ROW_H));
        lp.leftMargin = px(x);
        lp.topMargin = px(y);
        root.addView(tile, lp);
        return tile;
    }

    /** Chave na tela inicial para ligar/desligar a abertura do HDMI ao ligar. */
    private void addAutoHdmiSwitch(FrameLayout root, int x, int y) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(px(30), px(14), px(18), px(14));
        row.setFocusable(true);
        row.setClickable(true);
        row.setOnClickListener(v -> toggleAutoHdmi());

        TextView label = new TextView(this);
        label.setText(R.string.auto_hdmi);
        label.setTextColor(Color.WHITE);
        label.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(28));
        row.addView(label);

        // Trilho colorido + bolinha branca que desliza
        switchTrack = new GradientDrawable();
        switchTrack.setCornerRadius(px(22));
        FrameLayout track = new FrameLayout(this);
        track.setBackground(switchTrack);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(px(84), px(44));
        tlp.leftMargin = px(22);
        row.addView(track, tlp);

        switchKnob = new View(this);
        GradientDrawable knob = new GradientDrawable();
        knob.setShape(GradientDrawable.OVAL);
        knob.setColor(Color.WHITE);
        switchKnob.setBackground(knob);
        FrameLayout.LayoutParams klp = new FrameLayout.LayoutParams(px(36), px(36),
                Gravity.CENTER_VERTICAL | Gravity.START);
        klp.leftMargin = px(4);
        track.addView(switchKnob, klp);

        Ui.pillBackground(row, unit);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = px(x);
        lp.topMargin = px(y);
        root.addView(row, lp);
        updateAutoHdmiSwitch(false);
    }

    /** Botão discreto (xícara de café) que abre a tela "Sobre", com o QR de apoio. */
    private void addSupportButton(FrameLayout root, int x, int y) {
        ImageView button = new ImageView(this);
        button.setImageResource(R.drawable.ic_coffee);
        button.setAlpha(0.85f);
        button.setPadding(px(18), px(18), px(18), px(18));
        button.setContentDescription(getString(R.string.support_button));
        button.setFocusable(true);
        button.setClickable(true);
        button.setOnClickListener(v -> startActivity(new Intent(this, AboutActivity.class)));
        Ui.pillBackground(button, unit);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(px(72), px(72));
        lp.leftMargin = px(x);
        lp.topMargin = px(y);
        root.addView(button, lp);
    }

    private void updateAutoHdmiSwitch(boolean animate) {
        boolean enabled = prefs.getBoolean(PREF_AUTO_HDMI, true);
        switchTrack.setColor(enabled ? SWITCH_ON : SWITCH_OFF);
        float knobX = enabled ? px(40) : 0;
        if (animate) {
            switchKnob.animate().translationX(knobX).setDuration(150).start();
        } else {
            switchKnob.setTranslationX(knobX);
        }
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}
