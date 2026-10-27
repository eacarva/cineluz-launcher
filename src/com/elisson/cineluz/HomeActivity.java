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
    private static final String PREF_AUTO_OPEN = "auto_hdmi"; // nome antigo, mantido para quem atualiza
    private static final String PREF_LAST_BOOT = "last_boot_count";
    private static final String PREF_HDMI_PORT = "hdmi_port";
    // App a abrir ao ligar (vazio = HDMI) e se o aparelho já mostrou entradas HDMI
    private static final String PREF_START_APP = "start_app";
    private static final String PREF_HAS_HDMI = "has_hdmi";
    private static final String START_SLOT = "start";

    private static final String PREF_ASKED_HOME = "asked_home";
    private static final String PREF_ASKED_LOCATION = "asked_location";

    private static final int REQUEST_PICK_APP = 1;
    private static final int REQUEST_HOME_ROLE = 2;
    private static final int REQUEST_LOCATION = 3;
    private static final int REQUEST_PICK_START = 4;

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

    // Bloco grande: o HDMI, ou (em aparelhos sem HDMI) o app principal
    private FrameLayout mainTile;
    private boolean hasHdmi;
    private TextView hdmiLabel;
    private HdmiPreview preview; // null sem HDMI
    private List<HdmiPorts.Port> ports;
    private HdmiPorts.Port currentPort;
    private Set<String> connectedBefore = new HashSet<>();

    private NetworkChip networkChip;
    private View startChip;
    private View switchToggle;
    private TextView switchLabel;
    private GradientDrawable switchTrack;
    private View switchKnob;
    private boolean resumed;
    private View makeDefaultButton;
    private long idleMs;
    private final Runnable showIdleClock = () ->
            startActivity(new Intent(this, IdleClockActivity.class));

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

        // Linha 1: HDMI ou app principal (largo) e dois blocos de espelhamento
        int y1 = GRID_TOP, y2 = GRID_TOP + ROW_H + GAP;
        int wide = 742, narrow = (GRID_WIDTH - wide - 2 * GAP) / 2;
        int x = GRID_LEFT;
        hasHdmi = detectHdmi();
        if (hasHdmi) {
            mainTile = addTile(root, x, y1, wide, getDrawable(R.drawable.ic_hdmi), getString(R.string.tile_hdmi),
                    HDMI_START, HDMI_END, v -> openHdmi());
            hdmiLabel = (TextView) mainTile.getTag();
            mainTile.setOnLongClickListener(v -> {
                nextPort();
                return true;
            });
            preview = new HdmiPreview(this, mainTile, mainTile.getChildAt(0), this);
        } else {
            boolean chosen = startAppIntent() != null;
            mainTile = addTile(root, x, y1, wide,
                    chosen ? appIcon(startApp()) : getDrawable(R.drawable.ic_apps),
                    chosen ? Launcher.appLabel(this, startApp()) : getString(R.string.main_app_pick),
                    HDMI_START, HDMI_END, v -> openMainApp());
            mainTile.setOnLongClickListener(v -> {
                pickStartApp();
                return true;
            });
        }
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
        addTile(root, x, y2, third, getDrawable(R.drawable.ic_apps), getString(R.string.tile_apps),
                APPS_START, APPS_END, v -> startActivity(new Intent(this, AppsActivity.class)));

        addAutoOpenSwitch(root,GRID_LEFT, y2 + ROW_H + 40);
        addSupportButton(root, GRID_LEFT + GRID_WIDTH - 72, y2 + ROW_H + 40);
        addMakeDefaultButton(root, y2 + ROW_H + 40);

        // Enquanto a tela inicial estiver aberta, a tela não apaga: depois do tempo
        // de tela do sistema sem ninguém mexer, o Cineluz mostra o relógio dele
        // (antes do descanso de tela do sistema, que em alguns aparelhos tem anúncios)
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        long screenOff = Settings.System.getLong(getContentResolver(),
                Settings.System.SCREEN_OFF_TIMEOUT, 5 * 60_000L);
        idleMs = screenOff > 0 && screenOff < 24 * 60 * 60_000L ? Math.max(60_000L, screenOff - 15_000L) : 0;

        setContentView(root);
        mainTile.requestFocus();

        // Primeira vez: oferece virar a tela inicial padrão
        if (!isDefaultHome() && !prefs.getBoolean(PREF_ASKED_HOME, false)) {
            prefs.edit().putBoolean(PREF_ASKED_HOME, true).apply();
            handler.postDelayed(this::requestDefaultHome, 800);
        } else {
            maybeAskLocation();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        networkChip.start();
        // Trava o mudo já na volta pra tela inicial: alguns apps de HDMI, ao
        // fechar, tiram o mudo por um instante e isso dava um "pico" de som
        if (preview != null) preview.resume();
        makeDefaultButton.setVisibility(isDefaultHome() ? View.GONE : View.VISIBLE);
        updateAutoOpenSwitch(false);
        resetIdleTimer();
        if (!hasHdmi && detectHdmi()) {
            // As entradas HDMI apareceram depois (ex.: logo após ligar)
            recreate();
            return;
        }
        if (hasHdmi) refreshPorts(false);
        if (!maybeOpenAfterBoot()) {
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
        if (preview != null) preview.pause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Home com o launcher já aberto: volta o foco pro HDMI e reinicia a
        // prévia (recupera a imagem se ela tiver sumido)
        mainTile.requestFocus();
        if (resumed && preview != null) preview.restart();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        resetIdleTimer();
        if (handleHoldOk(event)) return true;
        return super.dispatchKeyEvent(event);
    }

    /**
     * "Segurar OK" nos blocos. O Android detecta o toque longo por tempo, mas
     * nem todo controle segue esse tempo; aqui conta como segurar quando o
     * controle repete a tecla (é o que todos fazem com o botão pressionado).
     */
    private boolean holdHandled;

    private boolean handleHoldOk(KeyEvent event) {
        int key = event.getKeyCode();
        if (key != KeyEvent.KEYCODE_DPAD_CENTER && key != KeyEvent.KEYCODE_ENTER) return false;
        View focused = getCurrentFocus();
        if (focused == null || !focused.isLongClickable()) return false;
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() > 0) {
            if (!holdHandled) {
                holdHandled = true;
                focused.setPressed(false);
                focused.performLongClick();
            }
            return true;
        }
        if (event.getAction() == KeyEvent.ACTION_UP && holdHandled) {
            // Solta o botão depois de segurar: não vira um clique
            holdHandled = false;
            focused.setPressed(false);
            return true;
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN) holdHandled = false;
        return false;
    }

    private void resetIdleTimer() {
        handler.removeCallbacks(showIdleClock);
        if (resumed && idleMs > 0) handler.postDelayed(showIdleClock, idleMs);
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
            if (focused == mainTile && hasHdmi) {
                nextPort();
                return true;
            }
            if (focused == mainTile || focused == startChip || focused == switchToggle) {
                pickStartApp();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_HOME_ROLE) {
            makeDefaultButton.setVisibility(isDefaultHome() ? View.GONE : View.VISIBLE);
            maybeAskLocation();
            return;
        }
        if (requestCode == REQUEST_PICK_START && resultCode == RESULT_OK && data != null) {
            // Escolher o que abrir já liga a abertura automática
            String pkg = data.getStringExtra(AppsActivity.EXTRA_PACKAGE);
            SharedPreferences.Editor e = prefs.edit().putBoolean(PREF_AUTO_OPEN, true);
            if (pkg == null) e.remove(PREF_START_APP); else e.putString(PREF_START_APP, pkg);
            e.apply();
            recreate();
            return;
        }
        if (requestCode != REQUEST_PICK_APP || resultCode != RESULT_OK || data == null) return;
        TileSlot slot = TileSlot.byKey(data.getStringExtra(AppsActivity.EXTRA_SLOT));
        if (slot == null) return;
        slot.setCustomPackage(this, data.getStringExtra(AppsActivity.EXTRA_PACKAGE));
        recreate();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQUEST_LOCATION && resumed) {
            networkChip.stop();
            networkChip.start();
        }
    }

    // ---------- Tela inicial padrão e permissões ----------

    private boolean isDefaultHome() {
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        android.content.pm.ResolveInfo ri = getPackageManager()
                .resolveActivity(home, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY);
        return ri != null && ri.activityInfo != null && getPackageName().equals(ri.activityInfo.packageName);
    }

    /** Mostra a janela do Android para escolher o Cineluz como tela inicial. */
    private void requestDefaultHome() {
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            android.app.role.RoleManager rm = getSystemService(android.app.role.RoleManager.class);
            if (rm != null && rm.isRoleAvailable(android.app.role.RoleManager.ROLE_HOME)) {
                startActivityForResult(rm.createRequestRoleIntent(android.app.role.RoleManager.ROLE_HOME),
                        REQUEST_HOME_ROLE);
                return;
            }
        }
        Intent settings = new Intent(Settings.ACTION_HOME_SETTINGS);
        if (settings.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(settings, REQUEST_HOME_ROLE);
        } else {
            // Aparelhos antigos: o Android pergunta qual tela inicial usar
            startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }

    /** Uma vez só: o Android exige a permissão de localização para ler o nome do Wi-Fi. */
    private void maybeAskLocation() {
        String perm = android.Manifest.permission.ACCESS_FINE_LOCATION;
        if (checkSelfPermission(perm) == android.content.pm.PackageManager.PERMISSION_GRANTED) return;
        if (prefs.getBoolean(PREF_ASKED_LOCATION, false)) return;
        prefs.edit().putBoolean(PREF_ASKED_LOCATION, true).apply();
        requestPermissions(new String[]{perm}, REQUEST_LOCATION);
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
        if (preview == null) return;
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
        if (preview != null) preview.stop();
        HdmiPorts.open(this, currentPort);
    }

    /**
     * Na primeira vez que a tela inicial aparece depois de ligar, abre o app
     * escolhido ou o HDMI. Devolve true se vai abrir.
     */
    private boolean maybeOpenAfterBoot() {
        int bootCount = Settings.Global.getInt(getContentResolver(), "boot_count", -1);
        if (bootCount < 0) return false;
        int lastBoot = prefs.getInt(PREF_LAST_BOOT, -1);
        if (bootCount == lastBoot) return false;
        prefs.edit().putInt(PREF_LAST_BOOT, bootCount).apply();

        boolean firstRunEver = lastBoot < 0;
        boolean justBooted = SystemClock.elapsedRealtime() < 10 * 60 * 1000;
        if (firstRunEver || !justBooted || !autoOpenEnabled()) return false;
        Intent app = startAppIntent();
        if (app != null) {
            handler.postDelayed(() -> Launcher.start(this, app), 1500);
            return true;
        }
        if (!hasHdmi) return false;
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

    /** Se as entradas HDMI demorarem a aparecer, vale a última vez que foram vistas. */
    private boolean detectHdmi() {
        if (!HdmiPorts.list(this).isEmpty()) {
            if (!prefs.getBoolean(PREF_HAS_HDMI, false)) prefs.edit().putBoolean(PREF_HAS_HDMI, true).apply();
            return true;
        }
        return HdmiPorts.manager(this) != null && prefs.getBoolean(PREF_HAS_HDMI, false);
    }

    // ---------- Abrir ao ligar ----------

    private String startApp() {
        return prefs.getString(PREF_START_APP, null);
    }

    /** O app escolhido para abrir ao ligar, ou null (HDMI / nenhum escolhido). */
    private Intent startAppIntent() {
        return Launcher.launchIntent(this, startApp());
    }

    /** Ligado por padrão em aparelhos com HDMI; nos outros, só depois de escolher um app. */
    private boolean autoOpenEnabled() {
        return prefs.getBoolean(PREF_AUTO_OPEN, hasHdmi);
    }

    private void toggleAutoOpen() {
        if (!hasHdmi && startAppIntent() == null) {
            pickStartApp();
            return;
        }
        prefs.edit().putBoolean(PREF_AUTO_OPEN, !autoOpenEnabled()).apply();
        updateAutoOpenSwitch(true);
    }

    /** O bloco grande em aparelhos sem HDMI. */
    private void openMainApp() {
        Intent app = startAppIntent();
        if (app != null) Launcher.start(this, app); else pickStartApp();
    }

    /** Lista para escolher o que abrir ao ligar: HDMI (se houver) e os apps. */
    private void pickStartApp() {
        Intent intent = new Intent(this, AppsActivity.class);
        intent.putExtra(AppsActivity.EXTRA_SLOT, START_SLOT);
        intent.putExtra(AppsActivity.EXTRA_TITLE, getString(R.string.pick_start_title));
        if (hasHdmi) {
            intent.putExtra(AppsActivity.EXTRA_FIRST_LABEL, getString(R.string.tile_hdmi));
            intent.putExtra(AppsActivity.EXTRA_FIRST_ICON, R.drawable.ic_hdmi);
        }
        startActivityForResult(intent, REQUEST_PICK_START);
    }

    private String startTargetName() {
        if (startAppIntent() != null) return Launcher.appLabel(this, startApp());
        return getString(hasHdmi ? R.string.tile_hdmi : R.string.start_none);
    }

    private android.graphics.drawable.Drawable appIcon(String pkg) {
        try {
            return getPackageManager().getApplicationIcon(pkg);
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            return getDrawable(R.drawable.ic_apps);
        }
    }

    // ---------- Blocos configuráveis ----------

    private void addSlotTile(FrameLayout root, TileSlot slot, int x, int y, int w) {
        FrameLayout tile = addTile(root, x, y, w, slot.icon(this), slot.label(this),
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
        intent.putExtra(AppsActivity.EXTRA_FIRST_LABEL, getString(R.string.pick_automatic));
        intent.putExtra(AppsActivity.EXTRA_FIRST_ICON, slot.iconRes);
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
    private FrameLayout addTile(FrameLayout root, int x, int y, int w, android.graphics.drawable.Drawable iconDrawable, String label,
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
        icon.setImageDrawable(iconDrawable);
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

    /**
     * Barra "Ao ligar": um botão com o que abrir (HDMI ou um app, com setinha de
     * lista) e a chave verde/vermelha que liga e desliga a abertura automática.
     */
    private void addAutoOpenSwitch(FrameLayout root, int x, int y) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(px(30), px(6), px(6), px(6));
        GradientDrawable rowBg = new GradientDrawable();
        rowBg.setCornerRadius(px(40));
        rowBg.setColor(Color.argb(40, 255, 255, 255));
        row.setBackground(rowBg);

        TextView prefix = new TextView(this);
        prefix.setText(R.string.auto_open);
        prefix.setTextColor(0xCCFFFFFF);
        prefix.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(28));
        row.addView(prefix);

        // O que abrir: OK mostra a lista (HDMI e apps)
        LinearLayout chip = new LinearLayout(this);
        chip.setOrientation(LinearLayout.HORIZONTAL);
        chip.setGravity(Gravity.CENTER_VERTICAL);
        chip.setPadding(px(22), px(10), px(12), px(10));
        chip.setFocusable(true);
        chip.setClickable(true);
        chip.setOnClickListener(v -> pickStartApp());
        chip.setContentDescription(getString(R.string.pick_start_title));
        Ui.pillBackground(chip, unit);
        startChip = chip;

        switchLabel = new TextView(this);
        switchLabel.setTextColor(Color.WHITE);
        switchLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(28));
        switchLabel.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        switchLabel.setSingleLine(true);
        switchLabel.setEllipsize(android.text.TextUtils.TruncateAt.END);
        switchLabel.setMaxWidth(px(460));
        chip.addView(switchLabel);

        ImageView chevron = new ImageView(this);
        chevron.setImageResource(R.drawable.ic_expand);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(px(36), px(36));
        clp.leftMargin = px(6);
        chip.addView(chevron, clp);

        LinearLayout.LayoutParams chipLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        chipLp.leftMargin = px(10);
        row.addView(chip, chipLp);

        // Chave liga/desliga: trilho colorido + bolinha branca que desliza
        FrameLayout toggle = new FrameLayout(this);
        toggle.setPadding(px(14), px(10), px(14), px(10));
        toggle.setFocusable(true);
        toggle.setClickable(true);
        toggle.setOnClickListener(v -> toggleAutoOpen());
        toggle.setOnLongClickListener(v -> {
            pickStartApp();
            return true;
        });
        Ui.pillBackground(toggle, unit);
        switchToggle = toggle;

        switchTrack = new GradientDrawable();
        switchTrack.setCornerRadius(px(22));
        FrameLayout track = new FrameLayout(this);
        track.setBackground(switchTrack);
        toggle.addView(track, new FrameLayout.LayoutParams(px(84), px(44)));

        switchKnob = new View(this);
        GradientDrawable knob = new GradientDrawable();
        knob.setShape(GradientDrawable.OVAL);
        knob.setColor(Color.WHITE);
        switchKnob.setBackground(knob);
        FrameLayout.LayoutParams klp = new FrameLayout.LayoutParams(px(36), px(36),
                Gravity.CENTER_VERTICAL | Gravity.START);
        klp.leftMargin = px(4);
        track.addView(switchKnob, klp);

        LinearLayout.LayoutParams toggleLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        toggleLp.leftMargin = px(6);
        row.addView(toggle, toggleLp);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = px(x);
        lp.topMargin = px(y);
        root.addView(row, lp);
        updateAutoOpenSwitch(false);
    }

    /** Botão discreto com o símbolo do Cineluz: abre a tela "Sobre", com o QR de apoio. */
    private void addSupportButton(FrameLayout root, int x, int y) {
        ImageView button = new ImageView(this);
        button.setImageResource(R.drawable.ic_brand_mark);
        button.setAlpha(0.9f);
        button.setPadding(px(16), px(16), px(16), px(16));
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

    /** Aparece só enquanto o Cineluz não for a tela inicial padrão. */
    private void addMakeDefaultButton(FrameLayout root, int y) {
        TextView button = new TextView(this);
        button.setText(R.string.make_default);
        button.setTextColor(Color.WHITE);
        button.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(28));
        button.setGravity(Gravity.CENTER);
        button.setPadding(px(34), 0, px(34), 0);
        button.setFocusable(true);
        button.setClickable(true);
        button.setOnClickListener(v -> requestDefaultHome());
        Ui.pillBackground(button, unit);
        makeDefaultButton = button;

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, px(72), Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        lp.topMargin = px(y);
        root.addView(button, lp);
    }

    private void updateAutoOpenSwitch(boolean animate) {
        switchLabel.setText(startTargetName());
        boolean enabled = autoOpenEnabled();
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
