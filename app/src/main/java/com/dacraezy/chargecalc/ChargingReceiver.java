package com.dacraezy.chargecalc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;

/**
 * Listens for power connect/disconnect events.
 * Could be extended to show notifications when charging starts/stops.
 */
public class ChargingReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        switch (action) {
            case Intent.ACTION_POWER_CONNECTED:
                // Charger plugged in — could trigger notification here
                break;
            case Intent.ACTION_POWER_DISCONNECTED:
                // Charger unplugged
                break;
            case Intent.ACTION_BATTERY_CHANGED:
                // Battery state changed
                break;
        }
    }
}
