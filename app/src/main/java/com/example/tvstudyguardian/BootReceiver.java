package com.example.tvstudyguardian;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        // Android/电视厂商可能限制开机自启；用户需重新确认无障碍服务状态。
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())
                && Settings.canDrawOverlays(context)) {
            // AccessibilityService 由系统设置管理，不在此处强行启动。
        }
    }
}
