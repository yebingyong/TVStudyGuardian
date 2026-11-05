package com.example.tvstudyguardian;
import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class StudyAccessibilityService extends AccessibilityService {
    private static final String TAG = "TVStudyGuardian";
    private final Handler handler=new Handler(Looper.getMainLooper());
    private WindowManager windowManager;
    private String foregroundPackage="";
    private long lastTick, intervalAccumulated, parentOverrideUntil;
    private boolean overlayShowing, hardLock;
    private View overlay;
    private int wrongCount;
    private Question currentQuestion;

    private final Runnable ticker=new Runnable(){@Override public void run(){
        long now=System.currentTimeMillis();
        long delta=lastTick==0?0:Math.max(0,Math.min(now-lastTick,5000)); lastTick=now;
        boolean overrideActive=now<parentOverrideUntil;
        boolean screenOn=isScreenInteractive();
        boolean watchingApp=isWatchedForegroundApp(foregroundPackage);
        if(screenOn&&watchingApp&&!overlayShowing&&!overrideActive&&WatchPolicy.withinAllowedTime(StudyAccessibilityService.this)&&!WatchPolicy.dailyLimitReached(StudyAccessibilityService.this)){
            intervalAccumulated+=delta; WatchPolicy.addWatched(StudyAccessibilityService.this,delta);
            getSharedPreferences(WatchPolicy.PREF,MODE_PRIVATE).edit().putLong("interval_accumulated",intervalAccumulated).apply();
        }
        if(screenOn && watchingApp && !overlayShowing && !overrideActive){
            if(!WatchPolicy.withinAllowedTime(StudyAccessibilityService.this)){hardLock=true;showQuestion("现在不是允许观看时间。需要家长验证才能临时解锁。");}
            else if(WatchPolicy.dailyLimitReached(StudyAccessibilityService.this)){hardLock=true;showQuestion("今天的观看时间已经用完。需要家长验证才能临时解锁。");}
            else if(intervalAccumulated>=WatchPolicy.intervalMinutes(StudyAccessibilityService.this)*60000L){hardLock=false;showQuestion("休息学习一下，答对题目即可继续观看！");}
        }
        handler.postDelayed(this,1000);
    }};
    @Override public void onServiceConnected(){
        super.onServiceConnected();
        windowManager=(WindowManager)getSystemService(WINDOW_SERVICE);
        intervalAccumulated=getSharedPreferences(WatchPolicy.PREF,MODE_PRIVATE).getLong("interval_accumulated",0L);
        lastTick=System.currentTimeMillis(); handler.removeCallbacks(ticker); handler.post(ticker);
        Log.i(TAG, "Accessibility service connected");
        Toast.makeText(this, "电视学习管家拦截服务已启动", Toast.LENGTH_LONG).show();
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event){
        if(event==null)return; int type=event.getEventType();
        if(type==AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED||type==AccessibilityEvent.TYPE_WINDOWS_CHANGED){
            CharSequence pkg=event.getPackageName();if(pkg!=null&&pkg.length()>0)foregroundPackage=pkg.toString();
        }
    }
    private boolean isScreenInteractive(){PowerManager pm=(PowerManager)getSystemService(Context.POWER_SERVICE);return pm!=null&&pm.isInteractive();}
    private boolean isWatchedForegroundApp(String pkg){
        if(pkg==null||pkg.length()==0||pkg.equals(getPackageName()))return false;
        String p=pkg.toLowerCase();String[] excluded={"systemui","launcher","home","settings","inputmethod","keyboard","packageinstaller","permissioncontroller","resolver"};
        for(String item:excluded)if(p.contains(item))return false;return true;
    }
    private void showQuestion(String message){
        if(overlayShowing)return;
        overlayShowing=true;wrongCount=0;currentQuestion=makeQuestion();
        try { buildQuestionOverlay(message); }
        catch(Exception e){ overlayShowing=false;overlay=null;Log.e(TAG,"Unable to build question overlay",e);Toast.makeText(this,"题目窗口启动失败，请检查电视无障碍权限",Toast.LENGTH_LONG).show(); }
    }
    private Question makeQuestion(){
        int grade=getSharedPreferences(WatchPolicy.PREF,MODE_PRIVATE).getInt("grade",1);
        String subject=getSharedPreferences(WatchPolicy.PREF,MODE_PRIVATE).getString("subject","随机");
        return Question.random(this,grade,subject);
    }
    private void buildQuestionOverlay(String message){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER);
        root.setFocusable(true);root.setFocusableInTouchMode(true);root.setPadding(55,30,55,30);root.setBackgroundColor(0xff10213d);
        TextView title=new TextView(this);title.setText("电视学习管家");title.setTextColor(Color.WHITE);title.setTextSize(30);title.setTypeface(null,Typeface.BOLD);title.setGravity(Gravity.CENTER);root.addView(title);
        TextView notice=new TextView(this);notice.setText(message);notice.setTextColor(0xffffdf70);notice.setTextSize(20);notice.setGravity(Gravity.CENTER);notice.setPadding(0,15,0,15);root.addView(notice);
        TextView questionView=new TextView(this);questionView.setText(currentQuestion.subject+" · "+currentQuestion.prompt);questionView.setTextColor(Color.WHITE);questionView.setTextSize(30);questionView.setGravity(Gravity.CENTER);questionView.setPadding(0,20,0,20);root.addView(questionView);
        TextView optionsView=new TextView(this);optionsView.setTextColor(0xffe6edf7);optionsView.setTextSize(22);optionsView.setGravity(Gravity.CENTER_VERTICAL);optionsView.setPadding(20,8,20,8);root.addView(optionsView);
        TextView answerHint=new TextView(this);answerHint.setTextColor(0xff9fc5ff);answerHint.setTextSize(18);answerHint.setGravity(Gravity.CENTER);answerHint.setPadding(0,8,0,8);root.addView(answerHint);
        renderQuestion(questionView,optionsView,answerHint);
        EditText answer=new EditText(this);answer.setSingleLine(true);answer.setTextSize(25);answer.setTextColor(Color.WHITE);answer.setHintTextColor(0xffaab8cc);answer.setHint("输入答案");answer.setSelectAllOnFocus(true);
        answer.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);root.addView(answer);
        TextView feedback=new TextView(this);feedback.setTextColor(0xffffdf70);feedback.setTextSize(19);feedback.setGravity(Gravity.CENTER);root.addView(feedback);
        Button submit=new Button(this);submit.setText("确认答案");root.addView(submit);
        submit.setOnClickListener(v->{
            String got=answer.getText().toString().trim();
            if(currentQuestion.isCorrect(got)){
                if(hardLock)feedback.setText("当前是家长限制状态，请家长输入密码。");
                else{intervalAccumulated=0;getSharedPreferences(WatchPolicy.PREF,MODE_PRIVATE).edit().putLong("interval_accumulated",0L).apply();removeOverlay();}
            }else{wrongCount++;feedback.setText("答案不正确，再试一次。已答错 "+wrongCount+" 次。");answer.setText("");currentQuestion=makeQuestion();renderQuestion(questionView,optionsView,answerHint);answer.setHint("输入答案");answer.requestFocus();}
        });
        TextView parent=new TextView(this);parent.setText("家长模式：输入密码临时解锁 30 分钟");parent.setTextColor(0xffb9c8e2);parent.setTextSize(18);parent.setGravity(Gravity.CENTER);parent.setPadding(0,24,0,0);root.addView(parent);
        parent.setOnClickListener(v->showParentUnlock());overlay=root;if(addAccessibilityOverlay())answer.requestFocus();else{overlay=null;overlayShowing=false;}
    }
    private void renderQuestion(TextView q,TextView o,TextView h){q.setText(currentQuestion.subject+" · "+currentQuestion.prompt);o.setText(currentQuestion.choicesText());h.setText(currentQuestion.answerHint());}
    private void showParentUnlock(){
        if(overlay!=null){try{windowManager.removeView(overlay);}catch(Exception ignored){}overlay=null;}
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER);root.setPadding(60,40,60,40);root.setBackgroundColor(0xff10213d);
        TextView title=new TextView(this);title.setText("家长验证");title.setTextColor(Color.WHITE);title.setTextSize(30);root.addView(title);
        EditText pwd=new EditText(this);pwd.setSingleLine(true);pwd.setTextColor(Color.WHITE);pwd.setHintTextColor(0xffaab8cc);pwd.setHint("请输入家长密码");pwd.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);root.addView(pwd);
        TextView feedback=new TextView(this);feedback.setTextColor(0xffffdf70);root.addView(feedback);
        Button unlock=new Button(this);unlock.setText("验证并临时解锁 30 分钟");root.addView(unlock);
        unlock.setOnClickListener(v->{
            String expected=getSharedPreferences(WatchPolicy.PREF,MODE_PRIVATE).getString("password","123456");
            if(expected.equals(pwd.getText().toString())){
                parentOverrideUntil=System.currentTimeMillis()+30*60000L;hardLock=false;intervalAccumulated=0;
                getSharedPreferences(WatchPolicy.PREF,MODE_PRIVATE).edit().putLong("interval_accumulated",0L).apply();removeOverlay();
            }else{feedback.setText("密码错误，请重试。");pwd.setText("");pwd.requestFocus();}
        });
        Button cancel=new Button(this);cancel.setText("返回答题");root.addView(cancel);
        cancel.setOnClickListener(v->{try{windowManager.removeView(root);}catch(Exception ignored){}overlay=null;currentQuestion=makeQuestion();buildQuestionOverlay("请答对题目，或由家长验证。");});
        overlay=root;if(addAccessibilityOverlay())pwd.requestFocus();else{overlay=null;overlayShowing=false;}
    }
    private boolean addAccessibilityOverlay(){
        if(overlay==null||windowManager==null){Log.e(TAG,"Overlay unavailable: view or WindowManager is null");return false;}
        WindowManager.LayoutParams lp=new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN|WindowManager.LayoutParams.FLAG_FULLSCREEN|WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,PixelFormat.TRANSLUCENT);
        lp.gravity=Gravity.CENTER;
        try{windowManager.addView(overlay,lp);overlay.requestFocus();Log.i(TAG,"Question overlay displayed");return true;}
        catch(Exception e){Log.e(TAG,"Unable to display accessibility overlay",e);Toast.makeText(this,"电视系统拒绝显示答题窗口，请确认无障碍服务已开启",Toast.LENGTH_LONG).show();return false;}
    }
    private void removeOverlay(){
        if(overlay!=null&&windowManager!=null){try{windowManager.removeView(overlay);}catch(Exception ignored){}}
        overlay=null;overlayShowing=false;wrongCount=0;
    }
    @Override public void onInterrupt(){handler.removeCallbacks(ticker);}
    @Override public void onDestroy(){handler.removeCallbacks(ticker);removeOverlay();super.onDestroy();}
}
