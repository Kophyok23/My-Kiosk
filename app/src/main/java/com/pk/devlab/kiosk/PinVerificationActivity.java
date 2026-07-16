package com.pk.devlab.kiosk;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.EditText;
import android.widget.Toast;

public class PinVerificationActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EditText input = new EditText(this);
        new android.app.AlertDialog.Builder(this)
            .setTitle("Admin Access (Exit)")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("Unlock", (d, w) -> {
                String savedPin = getSharedPreferences("KioskPrefs", MODE_PRIVATE).getString("admin_pin", "1234");
                if (input.getText().toString().equals(savedPin)) {
                    exitKioskMode();
                } else {
                    Toast.makeText(this, "PIN မှားနေပါသည်", Toast.LENGTH_SHORT).show();
                    finish();
                }
            })
            .setNegativeButton("Cancel", (d, w) -> finish())
            .show();
    }

    private void exitKioskMode() {
        // ၁။ App Monitor ပိတ်ရန် Service ကို အရင်ရပ်ပါမည်
        Intent serviceIntent = new Intent(this, AppBlockerService.class);
        stopService(serviceIntent);

        PackageManager pm = getPackageManager();
        ComponentName cn = new ComponentName(this, MainActivity.class);

        // ၂။ Launcher အဖြစ် သတ်မှတ်ထားသည်ကို ခေတ္တ ပိတ်ပါမည်
        pm.setComponentEnabledSetting(cn, 
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 
            PackageManager.DONT_KILL_APP);
        
        // ၃။ ဖုန်းရဲ့ မူလ Home Screen ဆီသို့ ပြန်သွားစေပါမည်
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(homeIntent);
        
        Toast.makeText(this, "Kiosk Mode မှ ထွက်လိုက်ပါပြီ", Toast.LENGTH_SHORT).show();

        // ၄။ နောက်တစ်ကြိမ် ပြန်ဝင်ရင် သုံးနိုင်အောင် Launcher Mode ကို ၁ စက္ကန့်အကြာတွင် ပြန်ဖွင့်ပေးထားပါမည်
        new Handler().postDelayed(() -> {
            pm.setComponentEnabledSetting(cn, 
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, 
                PackageManager.DONT_KILL_APP);
            finish();
        }, 1000);
    }
}
