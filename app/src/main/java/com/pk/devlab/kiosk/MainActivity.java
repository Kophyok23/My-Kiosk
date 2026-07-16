package com.pk.devlab.kiosk;

import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private PackageManager packageManager;
    private LinearLayout mainContainer;
    private List<AppDetail> allAppsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("KioskPrefs", MODE_PRIVATE);
        packageManager = getPackageManager();

        mainContainer = new LinearLayout(this);
        mainContainer.setOrientation(LinearLayout.VERTICAL);
        mainContainer.setPadding(32, 32, 32, 32);
        setContentView(mainContainer);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkCurrentStep();
    }

    private void checkCurrentStep() {
        mainContainer.removeAllViews(); 
        
        // Setup လုပ်နေစဉ် Wallpaper ကြီးမပေါ်နေစေရန် Window ကို အဖြူရောင် ပြန်ထားမည်
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        mainContainer.setBackgroundColor(Color.WHITE);

        if (!prefs.getBoolean("apps_selected", false)) {
            showStep1SelectApps();
        } else if (!prefs.getBoolean("pin_set", false)) {
            showStep2SetPin();
        } else if (!hasUsageStatsPermission()) {
            showStep3UsagePermission();
        } else if (!Settings.canDrawOverlays(this)) {
            showStep4OverlayPermission();
        } else {
            showFinalDashboard();
        }
    }

    // ================= STEP 1: SELECT APPS =================
    private void showStep1SelectApps() {
        TextView title = new TextView(this);
        title.setText("You decide what your child can open\n\nIn Kids Mode, only the apps you pick are available.");
        title.setTextSize(16);
        title.setTextColor(Color.BLACK);
        mainContainer.addView(title);

        ListView listView = new ListView(this);
        allAppsList = loadInstalledApps();
        
        Set<String> allowedPackages = prefs.getStringSet("allowed_apps", new HashSet<>());
        for (AppDetail app : allAppsList) {
            if (allowedPackages.contains(app.packageName)) {
                app.isSelected = true;
            }
        }
        
        AppSelectionAdapter adapter = new AppSelectionAdapter(allAppsList);
        listView.setAdapter(adapter);

        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        listView.setLayoutParams(listParams);
        mainContainer.addView(listView);

        Button btnContinue = new Button(this);
        btnContinue.setText("သိမ်းမည် (SAVE & CONTINUE)");
        btnContinue.setBackgroundColor(Color.parseColor("#1C44CE"));
        btnContinue.setTextColor(Color.WHITE);
        btnContinue.setOnClickListener(v -> {
            Set<String> selected = new HashSet<>();
            selected.add(getPackageName()); 
            for (AppDetail app : allAppsList) {
                if (app.isSelected) selected.add(app.packageName);
            }
            prefs.edit().putStringSet("allowed_apps", selected).putBoolean("apps_selected", true).apply();
            checkCurrentStep();
        });
        mainContainer.addView(btnContinue);
    }

    // ================= STEP 2: SET PIN =================
    private void showStep2SetPin() {
        TextView title = new TextView(this);
        title.setText("Create a PIN for parental controls");
        title.setTextSize(16);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);
        mainContainer.addView(title);

        EditText pinInput = new EditText(this);
        pinInput.setHint("Please enter your 4 digit PIN:");
        pinInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        mainContainer.addView(pinInput);

        Button btnConfirm = new Button(this);
        btnConfirm.setText("CONFIRM PIN");
        btnConfirm.setBackgroundColor(Color.parseColor("#1C44CE"));
        btnConfirm.setTextColor(Color.WHITE);
        btnConfirm.setOnClickListener(v -> {
            String pin = pinInput.getText().toString();
            if (pin.length() >= 4) {
                prefs.edit().putString("admin_pin", pin).putBoolean("pin_set", true).apply();
                checkCurrentStep();
            } else {
                Toast.makeText(this, "PIN အနည်းဆုံး ၄ လုံး ရိုက်ပါ", Toast.LENGTH_SHORT).show();
            }
        });
        mainContainer.addView(btnConfirm);
    }

    // ================= STEP 3: USAGE DATA ACCESS =================
    private void showStep3UsagePermission() {
        TextView title = new TextView(this);
        title.setText("Usage access request.\n\nRequired for blocking unapproved apps.");
        title.setTextSize(16);
        title.setTextColor(Color.BLACK);
        mainContainer.addView(title);

        Button btnGrant = new Button(this);
        btnGrant.setText("GRANT PERMISSION");
        btnGrant.setBackgroundColor(Color.parseColor("#1C44CE"));
        btnGrant.setTextColor(Color.WHITE);
        btnGrant.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivity(intent);
        });
        mainContainer.addView(btnGrant);
    }

    // ================= STEP 4: APPEAR ON TOP =================
    private void showStep4OverlayPermission() {
        TextView title = new TextView(this);
        title.setText("Appear on top request.\n\nRequired to block settings and notification clicks.");
        title.setTextSize(16);
        title.setTextColor(Color.BLACK);
        mainContainer.addView(title);

        Button btnGrant = new Button(this);
        btnGrant.setText("GRANT PERMISSION");
        btnGrant.setBackgroundColor(Color.parseColor("#1C44CE"));
        btnGrant.setTextColor(Color.WHITE);
        btnGrant.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            startActivity(intent);
        });
        mainContainer.addView(btnGrant);
    }

    // ================= FINAL: KIOSK DASHBOARD (HOME SCREEN) =================
    private void showFinalDashboard() {
        
        // >>> ပြင်ဆင်လိုက်သောနေရာ: Window ကိုပါ အကြည်ရောင်ပြောင်းမှ နောက်က Wallpaper အစစ်ပေါ်မည် <<<
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); 
        mainContainer.setBackgroundColor(Color.TRANSPARENT);

        Intent serviceIntent = new Intent(this, AppBlockerService.class);
        startService(serviceIntent);

        // --- အပေါ်ဘက် ခလုတ်တန်း (Manage Apps နှင့် Exit) ---
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.END); 
        
        Button btnManage = new Button(this);
        btnManage.setText("APPS ပြင်ဆင်ရန်");
        btnManage.setBackgroundColor(Color.parseColor("#2196F3")); 
        btnManage.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnParams.setMargins(0, 0, 20, 0); 
        btnManage.setLayoutParams(btnParams);
        btnManage.setOnClickListener(v -> showPinDialogForManageApps());
        topBar.addView(btnManage);

        Button btnExit = new Button(this);
        btnExit.setText("EXIT");
        btnExit.setBackgroundColor(Color.RED);
        btnExit.setTextColor(Color.WHITE);
        btnExit.setOnClickListener(v -> {
            Intent intent = new Intent(this, PinVerificationActivity.class);
            startActivity(intent);
        });
        topBar.addView(btnExit);
        
        mainContainer.addView(topBar);

        // --- Grid of Selected Apps ---
        GridView gridView = new GridView(this);
        gridView.setNumColumns(3); 
        gridView.setVerticalSpacing(50); 
        gridView.setHorizontalSpacing(30);
        gridView.setGravity(Gravity.CENTER);
        
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        gridParams.topMargin = 100; 
        gridView.setLayoutParams(gridParams);

        Set<String> allowedPackages = prefs.getStringSet("allowed_apps", new HashSet<>());
        List<AppDetail> allowedApps = new ArrayList<>();
        if (allAppsList == null) allAppsList = loadInstalledApps();
        
        for (AppDetail app : allAppsList) {
            if (allowedPackages.contains(app.packageName) && !app.packageName.equals(getPackageName())) {
                allowedApps.add(app);
            }
        }

        LauncherAdapter adapter = new LauncherAdapter(allowedApps);
        gridView.setAdapter(adapter);

        gridView.setOnItemClickListener((parent, view, position, id) -> {
            String targetPackage = allowedApps.get(position).packageName;
            Intent launchIntent = packageManager.getLaunchIntentForPackage(targetPackage);
            if (launchIntent != null) {
                startActivity(launchIntent);
            }
        });

        mainContainer.addView(gridView);
    }

    private void showPinDialogForManageApps() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        
        new AlertDialog.Builder(this)
            .setTitle("Admin Access")
            .setMessage("Apps များကို အတိုး/အလျှော့ ပြုလုပ်ရန် PIN ရိုက်ထည့်ပါ")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("ဝင်မည်", (dialog, which) -> {
                String savedPin = prefs.getString("admin_pin", "1234");
                if (input.getText().toString().equals(savedPin)) {
                    showStep1SelectAppsScreen();
                } else {
                    Toast.makeText(this, "PIN မှားနေပါသည်", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("ပယ်ဖျက်မည်", null)
            .show();
    }

    private void showStep1SelectAppsScreen() {
        mainContainer.removeAllViews();
        // Setup Screen သို့ပြန်သွားလျှင် Wallpaper ကို ဖြုတ်ပြီး အဖြူရောင်ပြန်ပြောင်းမည်
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        mainContainer.setBackgroundColor(Color.WHITE); 
        showStep1SelectApps(); 
    }

    @Override
    public void onBackPressed() {
        if (prefs.getBoolean("pin_set", false)) {
            Toast.makeText(this, "ထွက်ရန် အပေါ်ရှိ 'EXIT' ကိုနှိပ်ပါ", Toast.LENGTH_SHORT).show();
        } else {
            super.onBackPressed();
        }
    }

    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, 
                android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private List<AppDetail> loadInstalledApps() {
        List<AppDetail> list = new ArrayList<>();
        List<ApplicationInfo> allApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo appInfo : allApps) {
            if (packageManager.getLaunchIntentForPackage(appInfo.packageName) != null) {
                AppDetail app = new AppDetail();
                app.name = appInfo.loadLabel(packageManager).toString();
                app.packageName = appInfo.packageName;
                app.icon = appInfo.loadIcon(packageManager);
                list.add(app);
            }
        }
        Collections.sort(list, (a, b) -> a.name.compareToIgnoreCase(b.name));
        return list;
    }

    class AppDetail {
        String name;
        String packageName;
        Drawable icon;
        boolean isSelected = false;
    }

    class AppSelectionAdapter extends BaseAdapter {
        List<AppDetail> apps;
        AppSelectionAdapter(List<AppDetail> apps) { this.apps = apps; }
        @Override public int getCount() { return apps.size(); }
        @Override public Object getItem(int position) { return apps.get(position); }
        @Override public long getItemId(int position) { return position; }
        @Override public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout layout = new LinearLayout(MainActivity.this);
            layout.setOrientation(LinearLayout.HORIZONTAL);
            layout.setPadding(16, 24, 16, 24);
            layout.setGravity(Gravity.CENTER_VERTICAL);

            CheckBox checkBox = new CheckBox(MainActivity.this);
            checkBox.setChecked(apps.get(position).isSelected);
            checkBox.setOnCheckedChangeListener((btn, isChecked) -> apps.get(position).isSelected = isChecked);

            ImageView icon = new ImageView(MainActivity.this);
            icon.setImageDrawable(apps.get(position).icon);
            icon.setLayoutParams(new LinearLayout.LayoutParams(80, 80));
            icon.setPadding(16, 0, 0, 0);

            TextView txtName = new TextView(MainActivity.this);
            txtName.setText(apps.get(position).name);
            txtName.setTextSize(16);
            txtName.setPadding(32, 0, 0, 0);
            txtName.setTextColor(Color.BLACK);

            layout.addView(checkBox);
            layout.addView(icon);
            layout.addView(txtName);
            return layout;
        }
    }

    class LauncherAdapter extends BaseAdapter {
        List<AppDetail> apps;
        LauncherAdapter(List<AppDetail> apps) { this.apps = apps; }
        @Override public int getCount() { return apps.size(); }
        @Override public Object getItem(int position) { return apps.get(position); }
        @Override public long getItemId(int position) { return position; }
        @Override public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout layout = new LinearLayout(MainActivity.this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setGravity(Gravity.CENTER);

            int iconSize = (int) (80 * getResources().getDisplayMetrics().density);

            ImageView icon = new ImageView(MainActivity.this);
            icon.setImageDrawable(apps.get(position).icon);
            icon.setLayoutParams(new LinearLayout.LayoutParams(iconSize, iconSize));
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);

            TextView txtName = new TextView(MainActivity.this);
            txtName.setText(apps.get(position).name);
            txtName.setTextSize(15); 
            txtName.setTextColor(Color.WHITE); 
            txtName.setGravity(Gravity.CENTER);
            txtName.setPadding(0, 15, 0, 0);
            txtName.setMaxLines(1);
            txtName.setShadowLayer(4f, 2f, 2f, Color.BLACK);

            layout.addView(icon);
            layout.addView(txtName);
            return layout;
        }
    }
}
