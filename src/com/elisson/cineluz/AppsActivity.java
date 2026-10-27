package com.elisson.cineluz;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lista de todos os apps instalados. Também serve para escolher o app de um
 * bloco da tela inicial (quando aberta com {@link #EXTRA_SLOT}).
 */
public class AppsActivity extends Activity {

    static final String EXTRA_SLOT = "slot";
    static final String EXTRA_TITLE = "title";
    static final String EXTRA_PACKAGE = "package";

    private static class App {
        String label;
        Drawable icon;
        Intent intent;     // o que abrir (modo lista)
        String pkg;        // o que devolver (modo escolha); null = automático
    }

    private float unit;
    private String slotKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        unit = Math.max(getResources().getDisplayMetrics().widthPixels,
                getResources().getDisplayMetrics().heightPixels) / 1920f;
        slotKey = getIntent().getStringExtra(EXTRA_SLOT);
        boolean picking = slotKey != null;

        final List<App> apps = loadApps();
        if (picking) {
            App auto = new App();
            auto.label = getString(R.string.pick_automatic);
            auto.icon = getDrawable(TileSlot.byKey(slotKey).iconRes);
            apps.add(0, auto);
        } else {
            App about = new App();
            about.label = getString(R.string.about_title);
            about.icon = getDrawable(R.drawable.app_icon);
            about.intent = new Intent(this, AboutActivity.class);
            apps.add(about);
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(HomeActivity.BG_COLOR);

        if (picking) {
            TextView title = new TextView(this);
            title.setText(getIntent().getStringExtra(EXTRA_TITLE));
            title.setTextColor(Color.WHITE);
            title.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(44));
            title.setPadding(px(120), px(60), px(120), 0);
            root.addView(title);
        }

        GridView grid = new GridView(this);
        grid.setNumColumns(6);
        grid.setVerticalSpacing(px(24));
        grid.setHorizontalSpacing(px(24));
        grid.setPadding(px(120), px(picking ? 40 : 80), px(120), px(80));
        grid.setClipToPadding(false);
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);

        GradientDrawable selector = new GradientDrawable();
        selector.setStroke(px(5), Color.WHITE);
        selector.setColor(Color.argb(60, 255, 255, 255));
        selector.setCornerRadius(px(12));
        grid.setSelector(selector);
        grid.setDrawSelectorOnTop(false);

        grid.setAdapter(new BaseAdapter() {
            @Override public int getCount() { return apps.size(); }
            @Override public Object getItem(int i) { return apps.get(i); }
            @Override public long getItemId(int i) { return i; }

            @Override
            public View getView(int i, View convert, ViewGroup parent) {
                LinearLayout cell = (LinearLayout) convert;
                if (cell == null) {
                    cell = new LinearLayout(AppsActivity.this);
                    cell.setOrientation(LinearLayout.VERTICAL);
                    cell.setGravity(Gravity.CENTER_HORIZONTAL);
                    cell.setPadding(px(10), px(20), px(10), px(16));
                    ImageView iv = new ImageView(AppsActivity.this);
                    iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    cell.addView(iv, new LinearLayout.LayoutParams(px(150), px(110)));
                    TextView tv = new TextView(AppsActivity.this);
                    tv.setTextColor(Color.WHITE);
                    tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(26));
                    tv.setGravity(Gravity.CENTER);
                    tv.setSingleLine(true);
                    tv.setEllipsize(TextUtils.TruncateAt.END);
                    LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    tlp.topMargin = px(12);
                    cell.addView(tv, tlp);
                }
                App app = apps.get(i);
                ((ImageView) cell.getChildAt(0)).setImageDrawable(app.icon);
                ((TextView) cell.getChildAt(1)).setText(app.label);
                return cell;
            }
        });
        grid.setOnItemClickListener((parent, view, pos, id) -> {
            App app = apps.get(pos);
            if (picking) {
                setResult(RESULT_OK, new Intent()
                        .putExtra(EXTRA_SLOT, slotKey)
                        .putExtra(EXTRA_PACKAGE, app.pkg));
                finish();
            } else {
                Launcher.start(this, new Intent(app.intent));
            }
        });

        root.addView(grid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
        grid.requestFocus();
    }

    private List<App> loadApps() {
        PackageManager pm = getPackageManager();
        Map<String, App> byPackage = new LinkedHashMap<>();
        // Apps de TV primeiro (usam o banner, que fica melhor na tela)
        for (String category : new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            Intent query = new Intent(Intent.ACTION_MAIN).addCategory(category);
            for (ResolveInfo ri : pm.queryIntentActivities(query, 0)) {
                String pkg = ri.activityInfo.packageName;
                if (pkg.equals(getPackageName()) || byPackage.containsKey(pkg)) continue;
                App app = new App();
                app.label = ri.loadLabel(pm).toString();
                Drawable banner = Intent.CATEGORY_LEANBACK_LAUNCHER.equals(category)
                        ? ri.activityInfo.loadBanner(pm) : null;
                app.icon = banner != null ? banner : ri.loadIcon(pm);
                app.intent = new Intent(Intent.ACTION_MAIN).addCategory(category)
                        .setClassName(pkg, ri.activityInfo.name);
                app.pkg = pkg;
                byPackage.put(pkg, app);
            }
        }
        List<App> apps = new ArrayList<>(byPackage.values());
        final Collator collator = Collator.getInstance();
        Collections.sort(apps, (a, b) -> collator.compare(a.label, b.label));
        return apps;
    }

    private int px(float designPx) {
        return Math.round(designPx * unit);
    }
}
