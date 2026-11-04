package com.example.tvstudyguardian;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.WindowManager;
import android.widget.*;

public class MainActivity extends Activity {
    private EditText interval, maxMinutes, startTime, endTime, grade, password;
    private Spinner subject;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28, 20, 28, 20);
        root.setBackgroundColor(0xfff4f6fa);

        TextView title = new TextView(this);
        title.setText("电视学习管家 V1.3");
        title.setTextSize(28); title.setTextColor(0xff17233c);
        root.addView(title);

        interval = field(root, "答题间隔（分钟，建议 10 / 20 / 30）", "20");
        maxMinutes = field(root, "每日最多观看（分钟）", "120");
        startTime = field(root, "允许开始时间（HH:mm）", "17:00");
        endTime = field(root, "允许结束时间（HH:mm）", "20:30");
        grade = field(root, "年级（1～6）", "1");
        password = field(root, "家长密码（请修改默认密码）", "123456");

        TextView sl = new TextView(this); sl.setText("出题科目"); sl.setTextSize(16);
        root.addView(sl);
        subject = new Spinner(this);
        String[] subjects = {"随机", "数学", "语文", "英语"};
        subject.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, subjects));
        root.addView(subject);

        Button save = new Button(this); save.setText("保存设置");
        root.addView(save); save.setOnClickListener(v -> saveSettings());

        Button accessibility = new Button(this); accessibility.setText("开启无障碍拦截服务");
        root.addView(accessibility);
        accessibility.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        TextView tip = new TextView(this);
        tip.setText("\n首次使用：保存设置 → 开启无障碍服务。\n\n说明：答题拦截依赖电视允许无障碍服务运行。部分系统页面、主页或受保护的视频应用可能无法拦截。请先在电视上测试。");
        tip.setTextSize(16); tip.setTextColor(0xff35445e);
        root.addView(tip);

        ScrollView scroll = new ScrollView(this); scroll.addView(root); setContentView(scroll);
        loadSettings();
    }

    private EditText field(LinearLayout root, String label, String def) {
        TextView t = new TextView(this); t.setText(label); t.setTextSize(16); root.addView(t);
        EditText e = new EditText(this); e.setSingleLine(true); e.setText(def); root.addView(e); return e;
    }

    private void loadSettings() {
        android.content.SharedPreferences p = getSharedPreferences(WatchPolicy.PREF, MODE_PRIVATE);
        interval.setText(String.valueOf(p.getInt("interval", 20)));
        maxMinutes.setText(String.valueOf(p.getInt("max", 120)));
        startTime.setText(p.getString("start", "17:00"));
        endTime.setText(p.getString("end", "20:30"));
        grade.setText(String.valueOf(p.getInt("grade", 1)));
        password.setText(p.getString("password", ""));
        String s = p.getString("subject", "随机");
        String[] opts = {"随机", "数学", "语文", "英语"};
        for (int i=0;i<opts.length;i++) if (opts[i].equals(s)) subject.setSelection(i);
    }

    private void saveSettings() {
        try {
            int it = Integer.parseInt(interval.getText().toString().trim());
            int mx = Integer.parseInt(maxMinutes.getText().toString().trim());
            int gr = Integer.parseInt(grade.getText().toString().trim());
            String st = startTime.getText().toString().trim();
            String en = endTime.getText().toString().trim();
            if (it < 1 || it > 240 || mx < 1 || mx > 1440 || gr < 1 || gr > 6) throw new Exception();
            if (!st.matches("\\d{2}:\\d{2}") || !en.matches("\\d{2}:\\d{2}")) throw new Exception();
            String pw = password.getText().toString();
            if (pw.length() < 4) { Toast.makeText(this, "家长密码至少 4 位", Toast.LENGTH_LONG).show(); return; }
            getSharedPreferences(WatchPolicy.PREF, MODE_PRIVATE).edit()
                .putInt("interval", it).putInt("max", mx).putInt("grade", gr)
                .putString("start", st).putString("end", en).putString("password", pw)
                .putString("subject", String.valueOf(subject.getSelectedItem())).apply();
            Toast.makeText(this, "设置已保存", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "设置格式不正确，请检查数字和时间（HH:mm）", Toast.LENGTH_LONG).show();
        }
    }
}
