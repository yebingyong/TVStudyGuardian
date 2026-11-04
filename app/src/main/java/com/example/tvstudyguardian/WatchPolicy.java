package com.example.tvstudyguardian;

import android.content.Context;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WatchPolicy {
    public static final String PREF = "settings";

    public static String today() {
        return new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
    }

    public static long watchedToday(Context c) {
        return c.getSharedPreferences(PREF, 0).getLong("watched_" + today(), 0L);
    }

    public static void addWatched(Context c, long ms) {
        if (ms <= 0) return;
        c.getSharedPreferences(PREF, 0).edit()
            .putLong("watched_" + today(), watchedToday(c) + ms).apply();
    }

    public static boolean withinAllowedTime(Context c) {
        android.content.SharedPreferences p = c.getSharedPreferences(PREF, 0);
        String start = p.getString("start", "17:00");
        String end = p.getString("end", "20:30");
        try {
            int now = minuteNow();
            int s = minuteOfDay(start);
            int e = minuteOfDay(end);
            if (s == e) return true;
            if (s < e) return now >= s && now < e;
            return now >= s || now < e;
        } catch (Exception ex) { return false; }
    }

    public static boolean dailyLimitReached(Context c) {
        int max = c.getSharedPreferences(PREF, 0).getInt("max", 120);
        return watchedToday(c) >= max * 60000L;
    }

    public static int intervalMinutes(Context c) {
        return c.getSharedPreferences(PREF, 0).getInt("interval", 20);
    }

    private static int minuteNow() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        return cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE);
    }
    private static int minuteOfDay(String s) {
        String[] a = s.split(":");
        int h = Integer.parseInt(a[0]), m = Integer.parseInt(a[1]);
        if (h < 0 || h > 23 || m < 0 || m > 59) throw new IllegalArgumentException();
        return h * 60 + m;
    }
}
