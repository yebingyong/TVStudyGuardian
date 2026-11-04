package com.example.tvstudyguardian;
import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Question {
    public final String subject, prompt;
    public final String[] choices;
    public final String answer, explanation, type;
    private static final Random RANDOM = new Random();
    private static List<Question> cached;
    private int grade;

    private Question(String subject, String prompt, String[] choices, String answer, String explanation, String type) {
        this.subject=subject; this.prompt=prompt; this.choices=choices;
        this.answer=answer; this.explanation=explanation; this.type=type;
    }

    public static synchronized Question random(Context context, int grade, String subjectSetting) {
        if (cached == null) cached = loadBank(context);
        List<Question> candidates = new ArrayList<>();
        for (Question q : cached) if (q.grade == grade &&
            ("随机".equals(subjectSetting) || q.subject.equals(subjectSetting))) candidates.add(q);
        if (!candidates.isEmpty()) return candidates.get(RANDOM.nextInt(candidates.size()));
        return fallback(grade, subjectSetting);
    }

    private static List<Question> loadBank(Context context) {
        List<Question> result = new ArrayList<>();
        try (InputStream in = context.getAssets().open("question_bank.json");
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192]; int n;
            while ((n=in.read(buffer)) != -1) out.write(buffer,0,n);
            JSONArray array = new JSONArray(new String(out.toByteArray(), StandardCharsets.UTF_8));
            for (int i=0;i<array.length();i++) {
                JSONObject o=array.optJSONObject(i);
                if (o==null || !"published".equalsIgnoreCase(o.optString("status","published"))) continue;
                String subject=subjectName(o.optString("subject",""));
                String prompt=o.optString("stem","").trim();
                String answer=answerString(o.opt("answer"));
                if (prompt.length()==0 || answer.length()==0) continue;
                JSONArray opts=o.optJSONArray("options"); String[] choices=null;
                if (opts!=null) { choices=new String[opts.length()]; for(int j=0;j<opts.length();j++) choices[j]=opts.optString(j,""); }
                Question q=new Question(subject,prompt,choices,answer,o.optString("explanation",""),o.optString("type","single"));
                q.grade=o.optInt("grade",1); result.add(q);
            }
        } catch (Exception ignored) {}
        return result;
    }

    private static String subjectName(String s) {
        if ("math".equals(s)) return "数学";
        if ("english".equals(s)) return "英语";
        if ("chinese".equals(s)) return "语文";
        return s;
    }
    private static String answerString(Object value) {
        if (value instanceof JSONArray) {
            JSONArray a=(JSONArray)value; StringBuilder b=new StringBuilder();
            for(int i=0;i<a.length();i++){if(i>0)b.append(",");b.append(a.optString(i,""));}
            return b.toString();
        }
        if(value==null || value==JSONObject.NULL)return "";
        return String.valueOf(value);
    }
    public String answerHint() {
        if("judge".equals(type))return "判断题：请输入“对”或“错”。";
        if("multiple".equals(type))return "多选题：输入所有正确选项字母，如 ABC。";
        if("order".equals(type))return "排序题：按正确顺序输入选项字母，如 ABCD。";
        if("match".equals(type))return "连线题：按左侧顺序输入对应选项字母，如 BCDA。";
        return "单选题：输入正确选项字母，如 A。";
    }
    public String choicesText() {
        if(choices==null || choices.length==0)return "";
        StringBuilder b=new StringBuilder();
        for(int i=0;i<choices.length;i++){if(i>0)b.append("\\n");b.append((char)('A'+i)).append(". ").append(choices[i]);}
        return b.toString();
    }
    public boolean isCorrect(String input) {
        if(input==null)return false;
        String got=input.trim().toUpperCase().replaceAll("[\\s,，、;；]+","");
        String expected=answer.trim().toUpperCase().replaceAll("[\\s,，、;；]+","");
        if("judge".equals(type)){
            boolean truth="TRUE".equalsIgnoreCase(answer)||"对".equals(answer)||"正确".equals(answer);
            if(truth)return "对".equals(got)||"正确".equals(got)||"是".equals(got)||"TRUE".equals(got)||"√".equals(got);
            return "错".equals(got)||"错误".equals(got)||"不对".equals(got)||"FALSE".equals(got)||"×".equals(got);
        }
        if("multiple".equals(type)){char[] a=got.toCharArray(),b=expected.toCharArray();java.util.Arrays.sort(a);java.util.Arrays.sort(b);return new String(a).equals(new String(b));}
        if("single".equals(type)&&choices!=null&&choices.length>0){
            if(got.equals(expected))return true;
            try{int idx=expected.charAt(0)-'A';return idx>=0&&idx<choices.length&&input.trim().equalsIgnoreCase(choices[idx]);}
            catch(Exception ignored){return false;}
        }
        return got.equals(expected);
    }
    private static Question fallback(int grade,String subjectSetting) {
        Random r=RANDOM; String subject=subjectSetting;
        if("随机".equals(subject))subject=new String[]{"数学","语文","英语"}[r.nextInt(3)];
        if("数学".equals(subject)){
            int max=grade<=2?20:grade<=4?100:500;
            int a=r.nextInt(max)+1,b=r.nextInt(max)+1;
            if(grade>=2&&r.nextBoolean()){if(b>a){int t=a;a=b;b=t;}return new Question("数学",a+" − "+b+" = ?",null,String.valueOf(a-b),"","numeric");}
            return new Question("数学",a+" + "+b+" = ?",null,String.valueOf(a+b),"","numeric");
        }
        if("英语".equals(subject)){
            String[][] bank={{"apple","苹果"},{"book","书"},{"school","学校"},{"water","水"},{"teacher","老师"}};
            String[] item=bank[r.nextInt(bank.length)];return new Question("英语","“"+item[0]+"”是什么意思？",null,item[1],"","text");
        }
        String[][] bank={{"“专心致志”通常形容什么？","做事非常专心"},{"“一望无际”的意思是？","一眼望不到边"},{"“春眠不觉晓”下一句是？","处处闻啼鸟"}};
        String[] item=bank[r.nextInt(bank.length)];return new Question("语文",item[0],null,item[1],"","text");
    }
}
