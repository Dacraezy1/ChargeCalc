package com.dacraezy.chargecalc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // UI Components
    private TextView tvBatteryPercent;
    private TextView tvChargingStatus;
    private TextView tvChargingSpeed;
    private TextView tvSpeedLabel;
    private TextView tvVoltage;
    private TextView tvTemperature;
    private TextView tvPlugType;
    private TextView tvTimeToFull;
    private TextView tvCurrentNow;
    private TextView tvPowerWatts;
    private TextView tvSessionTime;
    private TextView tvEnergyAdded;
    private TextView tvAvgSpeed;
    private TextView tvHealthStatus;
    private TextView tvCycleInfo;
    private ProgressBar batteryProgressBar;
    private View chargingIndicator;
    private LinearLayout statsContainer;
    private TextView tvLastCharged;
    private TextView tvDeviceTemp;

    // Data
    private BroadcastReceiver batteryReceiver;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateRunnable;
    private SharedPreferences prefs;

    private long sessionStartTime = 0;
    private int sessionStartPercent = 0;
    private boolean isCharging = false;
    private List<Float> speedHistory = new ArrayList<>();
    private int previousPercent = -1;
    private long percentChangeTime = 0;

    // Constants for charging speed classification (mA)
    private static final int SPEED_SLOW = 500;
    private static final int SPEED_NORMAL = 1000;
    private static final int SPEED_FAST = 2000;
    private static final int SPEED_SUPERFAST = 4000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("chargecalc_prefs", MODE_PRIVATE);

        // Apply saved theme
        boolean darkMode = prefs.getBoolean("dark_mode", true);
        AppCompatDelegate.setDefaultNightMode(
            darkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        setContentView(R.layout.activity_main);
        initViews();
        setupBatteryReceiver();
        startPeriodicUpdates();
    }

    private void initViews() {
        tvBatteryPercent  = findViewById(R.id.tvBatteryPercent);
        tvChargingStatus  = findViewById(R.id.tvChargingStatus);
        tvChargingSpeed   = findViewById(R.id.tvChargingSpeed);
        tvSpeedLabel      = findViewById(R.id.tvSpeedLabel);
        tvVoltage         = findViewById(R.id.tvVoltage);
        tvTemperature     = findViewById(R.id.tvTemperature);
        tvPlugType        = findViewById(R.id.tvPlugType);
        tvTimeToFull      = findViewById(R.id.tvTimeToFull);
        tvCurrentNow      = findViewById(R.id.tvCurrentNow);
        tvPowerWatts      = findViewById(R.id.tvPowerWatts);
        tvSessionTime     = findViewById(R.id.tvSessionTime);
        tvEnergyAdded     = findViewById(R.id.tvEnergyAdded);
        tvAvgSpeed        = findViewById(R.id.tvAvgSpeed);
        tvHealthStatus    = findViewById(R.id.tvHealthStatus);
        tvCycleInfo       = findViewById(R.id.tvCycleInfo);
        batteryProgressBar = findViewById(R.id.batteryProgressBar);
        chargingIndicator = findViewById(R.id.chargingIndicator);
        statsContainer    = findViewById(R.id.statsContainer);
        tvLastCharged     = findViewById(R.id.tvLastCharged);
        tvDeviceTemp      = findViewById(R.id.tvDeviceTemp);

        // Theme toggle button
        findViewById(R.id.btnTheme).setOnClickListener(v -> toggleTheme());
    }

    private void setupBatteryReceiver() {
        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateBatteryInfo(intent);
            }
        };
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryReceiver, filter);

        // Get initial state
        Intent batteryStatus = registerReceiver(null, filter);
        if (batteryStatus != null) {
            updateBatteryInfo(batteryStatus);
        }
    }

    private void updateBatteryInfo(Intent intent) {
        // ----- Core battery data -----
        int level   = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale   = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int percent = (scale > 0) ? (int) ((level / (float) scale) * 100) : 0;

        int status  = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        int health  = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
        int voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1); // mV
        int tempRaw = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1); // tenths of Celsius
        int technology = 0; // not used directly

        boolean charging = (status == BatteryManager.BATTERY_STATUS_CHARGING
                         || status == BatteryManager.BATTERY_STATUS_FULL);

        // ----- Session tracking -----
        if (charging && !isCharging) {
            sessionStartTime    = System.currentTimeMillis();
            sessionStartPercent = percent;
            speedHistory.clear();
            saveLastChargeTime();
        }
        isCharging = charging;

        // ----- Current (mA) from BatteryManager API -----
        long currentMicroAmps = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
            if (bm != null) {
                currentMicroAmps = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
            }
        }
        long currentMa = Math.abs(currentMicroAmps / 1000); // convert µA → mA

        // Fallback: estimate from voltage if current reading is 0
        if (currentMa == 0 && charging && voltage > 0) {
            // Rough estimate: assume 5W charger at reported voltage
            currentMa = (long) ((5000.0 / voltage) * 1000);
        }

        // ----- Speed history -----
        if (charging && currentMa > 0) {
            speedHistory.add((float) currentMa);
            if (speedHistory.size() > 60) speedHistory.remove(0);
        }

        // ----- Power (W) -----
        float voltageV = voltage / 1000.0f;
        float powerW   = (voltageV * currentMa) / 1000.0f;

        // ----- Temperature -----
        float tempC = tempRaw / 10.0f;
        float tempF = (tempC * 9 / 5) + 32;

        // ----- Plug type -----
        String plugType;
        switch (plugged) {
            case BatteryManager.BATTERY_PLUGGED_AC:  plugType = "AC Adapter";   break;
            case BatteryManager.BATTERY_PLUGGED_USB: plugType = "USB";           break;
            case BatteryManager.BATTERY_PLUGGED_WIRELESS: plugType = "Wireless"; break;
            default: plugType = "Unplugged"; break;
        }

        // ----- Health -----
        String healthStr;
        int healthColor;
        switch (health) {
            case BatteryManager.BATTERY_HEALTH_GOOD:
                healthStr = "Good"; healthColor = 0xFF4CAF50; break;
            case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                healthStr = "Overheating!"; healthColor = 0xFFFF5252; break;
            case BatteryManager.BATTERY_HEALTH_DEAD:
                healthStr = "Dead"; healthColor = 0xFF9E9E9E; break;
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                healthStr = "Over Voltage!"; healthColor = 0xFFFF9800; break;
            case BatteryManager.BATTERY_HEALTH_COLD:
                healthStr = "Too Cold"; healthColor = 0xFF64B5F6; break;
            default:
                healthStr = "Unknown"; healthColor = 0xFF9E9E9E; break;
        }

        // ----- Speed classification -----
        String speedLabel;
        int speedColor;
        int speedBg;
        if (!charging) {
            speedLabel = "Not Charging";
            speedColor = 0xFF9E9E9E;
            speedBg    = 0xFF2A2A2A;
        } else if (currentMa < SPEED_SLOW) {
            speedLabel = "Very Slow";
            speedColor = 0xFFFF5252;
            speedBg    = 0xFF3D1515;
        } else if (currentMa < SPEED_NORMAL) {
            speedLabel = "Slow";
            speedColor = 0xFFFF9800;
            speedBg    = 0xFF3D2A00;
        } else if (currentMa < SPEED_FAST) {
            speedLabel = "Normal";
            speedColor = 0xFFFFEB3B;
            speedBg    = 0xFF3D3800;
        } else if (currentMa < SPEED_SUPERFAST) {
            speedLabel = "Fast";
            speedColor = 0xFF69F0AE;
            speedBg    = 0xFF003D1A;
        } else {
            speedLabel = "Super Fast";
            speedColor = 0xFF40C4FF;
            speedBg    = 0xFF00253D;
        }

        // ----- Time to full -----
        String timeToFull = "N/A";
        if (charging && percent < 100 && currentMa > 0) {
            int remaining = 100 - percent;
            // Very rough estimate: minutes per percent based on current
            // Typical phone ~4000mAh battery
            long estimatedBatteryMah = 4000;
            float hoursToFull = (estimatedBatteryMah * remaining / 100.0f) / currentMa;
            int totalMinutes = (int) (hoursToFull * 60);
            int hours = totalMinutes / 60;
            int minutes = totalMinutes % 60;
            if (hours > 0) {
                timeToFull = hours + "h " + minutes + "m";
            } else {
                timeToFull = minutes + " min";
            }
        } else if (percent == 100) {
            timeToFull = "Fully Charged!";
        }

        // ----- Session stats -----
        String sessionTime = "—";
        String energyAdded = "—";
        if (charging && sessionStartTime > 0) {
            long elapsed = System.currentTimeMillis() - sessionStartTime;
            long secs = elapsed / 1000;
            long mins = secs / 60;
            long hrs  = mins / 60;
            if (hrs > 0) {
                sessionTime = hrs + "h " + (mins % 60) + "m";
            } else {
                sessionTime = mins + "m " + (secs % 60) + "s";
            }
            int gained = percent - sessionStartPercent;
            energyAdded = (gained >= 0 ? "+" : "") + gained + "%";
        }

        // ----- Avg speed -----
        String avgSpeed = "—";
        if (!speedHistory.isEmpty()) {
            float sum = 0;
            for (float s : speedHistory) sum += s;
            float avg = sum / speedHistory.size();
            avgSpeed = String.format(Locale.US, "%.0f mA", avg);
        }

        // ----- Last charge time -----
        String lastCharged = prefs.getString("last_charge_time", "Never");

        // ----- Update UI (must be on main thread — receiver runs on main thread already) -----
        final int fPercent    = percent;
        final long fCurrentMa = currentMa;
        final float fVoltageV = voltageV;
        final float fTempC    = tempC;
        final float fTempF    = tempF;
        final float fPowerW   = powerW;
        final String fPlugType     = plugType;
        final String fSpeedLabel   = speedLabel;
        final String fTimeToFull   = timeToFull;
        final String fSessionTime  = sessionTime;
        final String fEnergyAdded  = energyAdded;
        final String fAvgSpeed     = avgSpeed;
        final String fHealthStr    = healthStr;
        final int fHealthColor     = healthColor;
        final int fSpeedColor      = speedColor;
        final int fSpeedBg         = speedBg;
        final boolean fCharging    = charging;
        final String fLastCharged  = lastCharged;

        runOnUiThread(() -> {
            tvBatteryPercent.setText(fPercent + "%");
            tvChargingStatus.setText(fCharging ? "⚡ Charging" : "🔋 On Battery");
            tvChargingStatus.setTextColor(fCharging ? 0xFF69F0AE : 0xFFBBBBBB);

            tvChargingSpeed.setText(fCurrentMa > 0 ? fCurrentMa + " mA" : "—");
            tvSpeedLabel.setText(fSpeedLabel);
            tvSpeedLabel.setTextColor(fSpeedColor);
            tvSpeedLabel.setBackgroundColor(fSpeedBg);

            tvVoltage.setText(String.format(Locale.US, "%.2f V", fVoltageV));
            tvTemperature.setText(String.format(Locale.US, "%.1f°C / %.1f°F", fTempC, fTempF));
            tvPlugType.setText(fPlugType);
            tvTimeToFull.setText(fTimeToFull);
            tvCurrentNow.setText(fCurrentMa + " mA");
            tvPowerWatts.setText(String.format(Locale.US, "%.2f W", fPowerW));
            tvSessionTime.setText(fSessionTime);
            tvEnergyAdded.setText(fEnergyAdded);
            tvAvgSpeed.setText(fAvgSpeed);
            tvHealthStatus.setText(fHealthStr);
            tvHealthStatus.setTextColor(fHealthColor);
            tvLastCharged.setText(fLastCharged);

            batteryProgressBar.setProgress(fPercent);

            // Progress bar color
            if (fPercent <= 20) {
                batteryProgressBar.getProgressDrawable().setColorFilter(
                    0xFFFF5252, android.graphics.PorterDuff.Mode.SRC_IN);
            } else if (fPercent <= 50) {
                batteryProgressBar.getProgressDrawable().setColorFilter(
                    0xFFFF9800, android.graphics.PorterDuff.Mode.SRC_IN);
            } else {
                batteryProgressBar.getProgressDrawable().setColorFilter(
                    0xFF69F0AE, android.graphics.PorterDuff.Mode.SRC_IN);
            }

            chargingIndicator.setVisibility(fCharging ? View.VISIBLE : View.INVISIBLE);

            // Temperature warning
            if (fTempC > 40) {
                tvTemperature.setTextColor(0xFFFF5252);
                tvDeviceTemp.setText("⚠ High Temp!");
                tvDeviceTemp.setTextColor(0xFFFF5252);
            } else if (fTempC > 35) {
                tvTemperature.setTextColor(0xFFFF9800);
                tvDeviceTemp.setText("Warm");
                tvDeviceTemp.setTextColor(0xFFFF9800);
            } else {
                tvTemperature.setTextColor(0xFF69F0AE);
                tvDeviceTemp.setText("Normal");
                tvDeviceTemp.setTextColor(0xFF69F0AE);
            }

            tvCycleInfo.setText("Realme C25Y · Unisoc T610");
        });
    }

    private void saveLastChargeTime() {
        String timestamp = new SimpleDateFormat("MMM d, h:mm a", Locale.US).format(new Date());
        prefs.edit().putString("last_charge_time", timestamp).apply();
    }

    private void startPeriodicUpdates() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                // Re-query battery to update session timer
                IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                Intent batteryStatus = registerReceiver(null, filter);
                if (batteryStatus != null) {
                    updateBatteryInfo(batteryStatus);
                }
                handler.postDelayed(this, 2000); // update every 2 seconds
            }
        };
        handler.post(updateRunnable);
    }

    private void toggleTheme() {
        boolean currentDark = prefs.getBoolean("dark_mode", true);
        boolean newDark = !currentDark;
        prefs.edit().putBoolean("dark_mode", newDark).apply();
        AppCompatDelegate.setDefaultNightMode(
            newDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
        recreate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (batteryReceiver != null) {
            unregisterReceiver(batteryReceiver);
        }
        if (handler != null && updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
        }
    }
}
