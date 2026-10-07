package cn.headunit.inspector;

import android.app.*;
import android.os.*;
import android.content.*;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private LinearLayout body;
    private ScrollView scroll;
    private JSONObject report;
    private boolean recommendationPage;
    private volatile boolean alive = true;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private static final int INK = Color.rgb(23,38,53), MUTED = Color.rgb(83,103,119),
        BLUE = Color.rgb(20,94,167), PAPER = Color.rgb(240,245,249);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        layout();
        if (state != null && state.containsKey("report")) {
            try {
                report = new JSONObject(state.getString("report"));
                recommendationPage = state.getBoolean("recommendationPage", false);
                render();
                return;
            } catch (Exception ignored) { }
        }
        scan();
    }
    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }
    private void layout() {
        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setFitsSystemWindows(true);
        scroll.setBackgroundColor(PAPER);
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(24),dp(18),dp(24),dp(24));
        scroll.addView(body);
        setContentView(scroll);
    }
    private GradientDrawable background(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(12));
        return d;
    }
    private TextView textIn(LinearLayout parent, String s, int size, int color) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(color);
        t.setPadding(0,dp(5),0,dp(5));
        parent.addView(t);
        return t;
    }
    private TextView text(String s, int size, int color) { return textIn(body,s,size,color); }
    private void heading(String s) { text(s,27,INK).setTypeface(null,Typeface.BOLD); }
    private Button makeButton(String label, boolean primary, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(label); b.setTextSize(18); b.setMinHeight(dp(52));
        b.setPadding(dp(16),dp(8),dp(16),dp(8));
        b.setTextColor(primary ? Color.WHITE : BLUE);
        b.setBackgroundDrawable(background(primary ? BLUE : Color.WHITE));
        b.setOnClickListener(listener);
        return b;
    }
    private void button(String label, boolean primary, View.OnClickListener listener) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2);
        lp.topMargin = dp(10);
        body.addView(makeButton(label,primary,listener),lp);
    }
    private LinearLayout panel(int color) {
        LinearLayout p = new LinearLayout(this);
        p.setOrientation(LinearLayout.VERTICAL);
        p.setPadding(dp(18),dp(12),dp(18),dp(12));
        p.setBackgroundDrawable(background(color));
        return p;
    }
    private void navigation() {
        LinearLayout nav = new LinearLayout(this);
        LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(0,-2,1);
        LinearLayout.LayoutParams right = new LinearLayout.LayoutParams(0,-2,1);
        right.leftMargin = dp(10);
        nav.addView(makeButton("检测结果",!recommendationPage,new View.OnClickListener() {
            public void onClick(View v) { recommendationPage=false; render(); }
        }),left);
        nav.addView(makeButton("DiPlay 推荐",recommendationPage,new View.OnClickListener() {
            public void onClick(View v) { recommendationPage=true; render(); }
        }),right);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2);
        lp.topMargin=dp(10); lp.bottomMargin=dp(12);
        body.addView(nav,lp);
    }
    private void scan() {
        recommendationPage=false;
        body.removeAllViews(); heading("车机版本检测");
        text("正在读取系统信息与检测公开接口…",20,MUTED);
        text("离线扫描 · 无需 Root",16,MUTED);
        worker.execute(new Runnable() {
            public void run() {
                try {
                    final JSONObject r=collect();
                    if(alive) runOnUiThread(new Runnable() {
                        public void run() { if(alive) { report=r; render(); } }
                    });
                } catch(final Exception e) {
                    if(alive) runOnUiThread(new Runnable() {
                        public void run() {
                            if(!alive)return;
                            text("检测未完成："+e.getClass().getSimpleName(),20,INK);
                            button("重新检测",true,new View.OnClickListener() {
                                public void onClick(View v) { scan(); }
                            });
                        }
                    });
                }
            }
        });
    }
 private String field(Class<?> c,String name){try{return String.valueOf(c.getField(name).get(null));}catch(Exception e){return "未提供";}}
 private JSONObject collect()throws Exception{
  JSONObject r=new JSONObject();r.put("schemaVersion",1);r.put("appVersion","1.1.0");r.put("ruleVersion",Rules.RULE_VERSION);r.put("scannedAt",new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ",Locale.US).format(new Date()));
  JSONObject d=new JSONObject();d.put("model",Build.MODEL);d.put("manufacturer",Build.MANUFACTURER);d.put("board",Build.BOARD);d.put("hardware",field(Build.class,"HARDWARE"));d.put("buildId",Build.DISPLAY);d.put("fingerprint",Build.FINGERPRINT);d.put("screen",getResources().getDisplayMetrics().widthPixels+" × "+getResources().getDisplayMetrics().heightPixels);
  List<String> abis=new ArrayList<String>();try{String[] a=(String[])Build.class.getField("SUPPORTED_ABIS").get(null);abis.addAll(Arrays.asList(a));}catch(Exception e){abis.add(Build.CPU_ABI);if(Build.CPU_ABI2!=null&&!Build.CPU_ABI2.isEmpty())abis.add(Build.CPU_ABI2);}d.put("cpuAbis",new JSONArray(abis));r.put("device",d);
  int api=Build.VERSION.SDK_INT;String release=Build.VERSION.RELEASE;
  JSONObject declared=new JSONObject();declared.put("release",release);declared.put("sdkInt",api);declared.put("securityPatch",field(Build.VERSION.class,"SECURITY_PATCH"));r.put("reportedVersion",declared);
  JSONArray probes=new JSONArray();boolean conflict=false;
  String[][] specs={{"android.app.job.JobScheduler","21"},{"android.app.NotificationChannel","26"},{"android.os.SharedMemory","27"},{"android.net.MacAddress","28"},{"android.app.role.RoleManager","29"},{"android.os.ext.SdkExtensions","30"}};
  for(String[] spec:specs){int introduced=Integer.parseInt(spec[1]);JSONObject p=new JSONObject();p.put("class",spec[0]);p.put("introducedApi",introduced);long start=System.nanoTime();
   try{Class.forName(spec[0],false,getClassLoader());p.put("result","present");}
   catch(ClassNotFoundException e){p.put("result","absent");if(api>=introduced)conflict=true;}
   catch(LinkageError e){p.put("result","unavailable");p.put("errorType",e.getClass().getSimpleName());if(api>=introduced)conflict=true;}
   catch(SecurityException e){p.put("result","unavailable");p.put("errorType",e.getClass().getSimpleName());if(api>=introduced)conflict=true;}
   p.put("durationMs",(System.nanoTime()-start)/1000000);probes.put(p);
  }
  // A pure public static call: no network or system state changes.
  if(api>=28){JSONObject p=new JSONObject();p.put("class","android.net.MacAddress.fromString");p.put("introducedApi",28);try{Class<?> c=Class.forName("android.net.MacAddress");Object o=c.getMethod("fromString",String.class).invoke(null,"02:00:00:00:00:01");p.put("result",o!=null?"call_success":"unavailable");if(o==null)conflict=true;}catch(Exception e){p.put("result","call_failed");p.put("errorType",e.getClass().getSimpleName());conflict=true;}catch(LinkageError e){p.put("result","call_failed");p.put("errorType",e.getClass().getSimpleName());conflict=true;}probes.put(p);}
  r.put("probes",probes);boolean supported=false;for(String abi:abis)if(Arrays.asList("armeabi-v7a","arm64-v8a","x86","x86_64").contains(abi))supported=true;
  JSONObject a=new JSONObject();a.put("apiMappedVersion",Rules.version(api));a.put("inferredVersion",Rules.inferredVersion(api,conflict));a.put("interfaceConflict",conflict);a.put("labelMismatch",!Rules.matches(release,api));a.put("consistencyStatus",conflict?"接口证据存在冲突":(!Rules.matches(release,api)?"标称版本与 API 对应版本不一致":"未发现明显矛盾"));a.put("limitations","API 等级同样由固件报告；接口可能被回移或裁剪。本工具不能保证识破所有被修改的固件，也不能保证 CarPlay 硬件兼容。");r.put("assessment",a);
  String[] rec=Rules.recommend(api,conflict,supported);JSONObject n=new JSONObject();n.put("title",rec[0]);n.put("reason",rec[1]);n.put("apk",rec[2]);n.put("source",rec[3]);n.put("verification","满足已检查的系统门槛，不代表当前设备已实测可用");r.put("recommendation",n);return r;
 }

    private void render() {
        try {
            body.removeAllViews(); heading("车机版本检测"); navigation();
            if(recommendationPage) showRecommendation(); else showResult();
            scroll.post(new Runnable() { public void run() { scroll.scrollTo(0,0); } });
        } catch(Exception e) { text("显示报告失败："+e.getClass().getSimpleName(),18,INK); }
    }
    private void showResult() throws Exception {
        JSONObject a=report.getJSONObject("assessment"), v=report.getJSONObject("reportedVersion"),
            d=report.getJSONObject("device");
        boolean conflict=a.getBoolean("interfaceConflict");
        heading("看看车机的实际版本");
        LinearLayout versions=new LinearLayout(this);
        boolean wide=getResources().getDisplayMetrics().widthPixels /
            getResources().getDisplayMetrics().density >= 600;
        versions.setOrientation(wide ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        LinearLayout claimed=panel(Color.WHITE), inferred=panel(Color.rgb(222,236,250));
        textIn(claimed,"标称版本",17,MUTED);
        textIn(claimed,"Android "+v.getString("release"),30,INK).setTypeface(null,Typeface.BOLD);
        textIn(claimed,"车机系统显示的版本",15,MUTED);
        textIn(inferred,"真实版本（检测推定）",17,BLUE);
        textIn(inferred,a.getString("inferredVersion"),30,BLUE).setTypeface(null,Typeface.BOLD);
        textIn(inferred,"系统 API "+v.getInt("sdkInt")+" · 结合接口证据判断",15,MUTED);
        LinearLayout.LayoutParams p1=new LinearLayout.LayoutParams(wide?0:-1,-2,wide?1:0);
        LinearLayout.LayoutParams p2=new LinearLayout.LayoutParams(wide?0:-1,-2,wide?1:0);
        if(wide)p2.leftMargin=dp(12); else p2.topMargin=dp(12);
        versions.addView(claimed,p1); versions.addView(inferred,p2); body.addView(versions);
        String status=conflict?"证据冲突，暂时无法确定真实版本。":
            a.getString("inferredVersion").equals("无法确定")?"系统 API 尚未收录，暂时无法确定真实版本。":
            a.getBoolean("labelMismatch")?"两个版本不一致，请按检测推定版本选择应用。":"版本信息与已检查的接口一致。";
        text(status,18,conflict||a.getBoolean("labelMismatch")?Color.rgb(153,86,17):Color.rgb(20,110,76));
        text("推定结果不是绝对证明；部分改装固件可能修改系统信息。",14,MUTED);
        text("设备："+d.getString("model")+"   CPU："+d.getJSONArray("cpuAbis").toString(),15,MUTED);
        button("查看适配的 DiPlay →",true,new View.OnClickListener() {
            public void onClick(View v) { recommendationPage=true; render(); }
        });
        button("查看检测依据",false,new View.OnClickListener() {
            public void onClick(View v) { showDetails(); }
        });
        button("重新检测",false,new View.OnClickListener() {
            public void onClick(View v) { scan(); }
        });
        button("导出检测报告",false,new View.OnClickListener() {
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this).setTitle("选择报告格式")
                    .setItems(new String[]{"文本报告","JSON 报告"},new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface x,int index) { export(index==1); }
                    }).show();
            }
        });
    }
    private void showRecommendation() throws Exception {
        JSONObject a=report.getJSONObject("assessment"), n=report.getJSONObject("recommendation");
        heading("适合这台车机的 DiPlay");
        text("匹配依据："+a.getString("inferredVersion"),18,MUTED);
        LinearLayout card=panel(Color.WHITE);
        textIn(card,n.getString("source").isEmpty()?"暂不提供下载推荐":"推荐版本",16,MUTED);
        textIn(card,n.getString("title"),26,BLUE).setTypeface(null,Typeface.BOLD);
        textIn(card,n.getString("reason"),18,INK);
        if(!n.getString("apk").isEmpty())textIn(card,"下载文件：\n"+n.getString("apk"),17,MUTED);
        body.addView(card);
        if(!n.getString("source").isEmpty()) {
            button("前往 GitHub 下载页 ↗",true,new View.OnClickListener() {
                public void onClick(View v) { openDownload(); }
            });
            text("进入页面后，找到 Assets（文件），选择上面的 .apk 文件。GitHub 页面由项目作者维护，可能显示英文。",16,MUTED);
            button("复制下载页链接",false,new View.OnClickListener() {
                public void onClick(View v) { copyDownload(); }
            });
        } else {
            button("查看无法推荐的原因",true,new View.OnClickListener() {
                public void onClick(View v) { showDetails(); }
            });
        }
        button("← 返回检测结果",false,new View.OnClickListener() {
            public void onClick(View v) { recommendationPage=false; render(); }
        });
        text("已能正常使用的 DiPlay 建议保留。满足系统门槛，仍需验证实际连接。",15,MUTED);
        text("安装包目录：2026-10-05 · 固定版本推荐，不代表最新版本",14,MUTED);
    }
    private void openDownload() {
        try {
            String url=report.getJSONObject("recommendation").getString("source");
            if(!url.startsWith("https://github.com/"))return;
            startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));
        } catch(Exception e) {
            new AlertDialog.Builder(this).setTitle("无法打开浏览器")
                .setMessage("可复制下载页链接，在手机或电脑浏览器中打开。")
                .setPositiveButton("复制链接",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d,int which) { copyDownload(); }
                }).setNegativeButton("关闭",null).show();
        }
    }
    private void copyDownload() {
        try {
            String url=report.getJSONObject("recommendation").getString("source");
            if(url.isEmpty())return;
            ((ClipboardManager)getSystemService(CLIPBOARD_SERVICE))
                .setPrimaryClip(ClipData.newPlainText("DiPlay 下载页",url));
            Toast.makeText(this,"下载页链接已复制",Toast.LENGTH_SHORT).show();
        } catch(Exception e) { Toast.makeText(this,"复制失败，请查看检测报告中的链接",Toast.LENGTH_LONG).show(); }
    }
    @Override public void onBackPressed() {
        if(recommendationPage && report!=null) { recommendationPage=false; render(); }
        else super.onBackPressed();
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if(report!=null)state.putString("report",report.toString());
        state.putBoolean("recommendationPage",recommendationPage);
    }
 private String plain()throws Exception{JSONObject d=report.getJSONObject("device"),v=report.getJSONObject("reportedVersion"),a=report.getJSONObject("assessment"),n=report.getJSONObject("recommendation");StringBuilder s=new StringBuilder("车机版本检测 1.1.0\n检测时间："+report.getString("scannedAt")+"\n规则："+Rules.RULE_VERSION+"\n\n推荐："+n.getString("title")+"\n"+n.getString("reason")+"\n安装包："+n.getString("apk")+"\n来源："+n.getString("source")+"\n\n标称安卓："+v.getString("release")+"\n系统 API："+v.getInt("sdkInt")+"\n真实版本（检测推定）："+a.getString("inferredVersion")+"\nAPI 对应安卓："+a.getString("apiMappedVersion")+"\n结论："+a.getString("consistencyStatus")+"\n\n设备信息：\n"+d.toString(2)+"\n\n接口依据：\n");JSONArray ps=report.getJSONArray("probes");for(int i=0;i<ps.length();i++){JSONObject p=ps.getJSONObject(i);s.append("\n").append(p.getString("class")).append(" [API ").append(p.getInt("introducedApi")).append("] ").append(p.getString("result"));}s.append("\n\n").append(a.getString("limitations"));return s.toString();}
 private void showDetails(){try{TextView t=new TextView(this);t.setText(plain());t.setTextSize(17);t.setPadding(dp(16),dp(12),dp(16),dp(12));t.setTextIsSelectable(true);ScrollView sc=new ScrollView(this);sc.addView(t);new AlertDialog.Builder(this).setTitle("检测依据").setView(sc).setPositiveButton("关闭",null).show();}catch(Exception e){Toast.makeText(this,"无法显示依据",0).show();}}
 private void export(boolean json){try{String name=json?"report.json":"report.txt";File f=new File(getCacheDir(),name);FileOutputStream stream=new FileOutputStream(f);try{stream.write((json?report.toString(2):plain()).getBytes("UTF-8"));}finally{stream.close();}Uri uri=Uri.parse("content://cn.headunit.inspector.reports/"+name);Intent send=new Intent(Intent.ACTION_SEND);send.setType(json?"application/json":"text/plain");send.putExtra(Intent.EXTRA_STREAM,uri);send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);if(Build.VERSION.SDK_INT>=16)send.setClipData(ClipData.newRawUri("检测报告",uri));startActivity(Intent.createChooser(send,"导出车机检测报告"));}catch(Exception e){Toast.makeText(this,"未找到可用分享应用。可在检测依据中长按复制报告。",Toast.LENGTH_LONG).show();}}

    @Override protected void onDestroy() { alive=false; worker.shutdownNow(); super.onDestroy(); }
}
