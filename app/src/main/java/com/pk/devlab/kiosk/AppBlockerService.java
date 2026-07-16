package com.pk.devlab.kiosk;

import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.widget.Toast;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Set;
import java.util.HashSet;

public class AppBlockerService extends Service {
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable checkRunnable;
    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("KioskPrefs", MODE_PRIVATE);
        startMonitoring();
    }

    private void startMonitoring() {
        checkRunnable = new Runnable() {
            @Override
            public void run() {
                checkForegroundApp();
                handler.postDelayed(this, 1000); // ၁ စက္ကန့်လျှင် တစ်ကြိမ် စောင့်ကြည့်မည်
            }
        };
        handler.post(checkRunnable);
    }

    private void checkForegroundApp() {
        String foregroundApp = getForegroundPackageName();
        if (foregroundApp == null) return;

        Set<String> allowedApps = prefs.getStringSet("allowed_apps", new HashSet<>());
        
        // ကလေးက ခွင့်မပြုထားတဲ့ App ကို ဖွင့်လိုက်ရင်
        if (!allowedApps.contains(foregroundApp) && !foregroundApp.equals(getPackageName())) {
            
            // ၁။ စာတန်းအနီရောင် Toast ပြသရန်
            handler.post(() -> {
                String message = "တစ်ခြားအရာများကို အသုံးပြုခွင့်မပြုပါ";
                SpannableString redToastText = new SpannableString(message);
                redToastText.setSpan(new ForegroundColorSpan(Color.RED), 0, message.length(), 0);
                Toast.makeText(getApplicationContext(), redToastText, Toast.LENGTH_SHORT).show();
            });

            // ၂။ ဖုန်း Home သို့ သွားမည့်အစား ကိုယ့် App (MainActivity) ကို တိုက်ရိုက် ပြန်ဆွဲခေါ်မည်
            Intent lockIntent = new Intent(this, MainActivity.class);
            lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
                                 Intent.FLAG_ACTIVITY_CLEAR_TOP | 
                                 Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(lockIntent);
        }
    }

    // လက်ရှိ ပွင့်နေသော App (Foreground Package) ကို ရှာဖွေခြင်း
    private String getForegroundPackageName() {
        UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        long time = System.currentTimeMillis();
        List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 10, time);
        if (appList != null && !appList.isEmpty()) {
            SortedMap<Long, UsageStats> mySortedMap = new TreeMap<>();
            for (UsageStats usageStats : appList) {
                mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
            }
            if (!mySortedMap.isEmpty()) {
                return mySortedMap.get(mySortedMap.lastKey()).getPackageName();
            }
        }
        return null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(checkRunnable);
        super.onDestroy();
    }
}
