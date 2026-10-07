package cn.headunit.inspector.tests;

import android.app.*;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.lang.reflect.Field;

/** Exercises the installed production APK. No fixture data is injected. */
public class SmokeTest extends Instrumentation {
    Activity activity;
    int checks;
    String failure;
    void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        checks++;
    }
    TextView find(View view, String text) {
        if (view instanceof TextView && text.equals(((TextView)view).getText().toString())) return (TextView)view;
        if (view instanceof ViewGroup) {
            ViewGroup group=(ViewGroup)view;
            for (int i=0;i<group.getChildCount();i++) { TextView v=find(group.getChildAt(i),text); if(v!=null)return v; }
        }
        return null;
    }
    void click(final String label) {
        runOnMainSync(new Runnable(){ public void run(){
            TextView view=find(activity.getWindow().getDecorView(),label);
            check(view!=null,"Missing control: "+label); view.performClick();
        }});
        waitForIdleSync();
    }
    JSONObject report() throws Exception {
        final Field f=activity.getClass().getDeclaredField("report"); f.setAccessible(true);
        final Object[] value=new Object[1];
        runOnMainSync(new Runnable(){public void run(){try{value[0]=f.get(activity);}catch(Exception e){throw new RuntimeException(e);}}});
        return (JSONObject)value[0];
    }
    void waitScan() throws Exception {
        for(int i=0;i<100;i++){if(report()!=null)return;Thread.sleep(100);}
        throw new AssertionError("Scan did not finish in 10 seconds");
    }
    void screenshot(String name) throws Exception {
        waitForIdleSync();
        getUiAutomation().waitForIdle(500,5000);
        android.graphics.Bitmap image=getUiAutomation().takeScreenshot();
        check(image!=null,"Native screenshot "+name);
        java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(getTargetContext().getExternalFilesDir(null),name));
        image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);out.close();image.recycle();
    }
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){
        Bundle result=new Bundle();
        try {
            Intent launch=getTargetContext().getPackageManager().getLaunchIntentForPackage("cn.headunit.inspector");
            check(launch!=null,"Launcher intent");
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity=startActivitySync(launch);waitScan();waitForIdleSync();
            JSONObject r=report();
            check(r.getString("appVersion").equals("1.2.0"),"Installed app version");
            check(r.getString("dataSource").equals("live_device"),"Live device source");
            check(r.getJSONObject("reportedVersion").getInt("sdkInt")==Build.VERSION.SDK_INT,"API matches Android runtime");
            check(r.getJSONObject("reportedVersion").getString("release").equals(Build.VERSION.RELEASE),"Release matches Android runtime");
            check(r.getJSONObject("device").getString("model").equals(Build.MODEL),"Model matches Android runtime");
            check(!r.getJSONObject("assessment").getBoolean("interfaceConflict"),"Stock image interfaces agree");
            check(!r.getJSONObject("recommendation").getString("source").isEmpty(),"Download recommendation");
            screenshot("native-result.png");
            click("查看适配的 DiPlay →");
            screenshot("native-recommendation.png");
            final Intent[] outbound=new Intent[1];
            ActivityMonitor monitor=new ActivityMonitor(){
                @Override public ActivityResult onStartActivity(Intent intent){outbound[0]=intent;return new ActivityResult(Activity.RESULT_CANCELED,null);}
            };
            addMonitor(monitor);
            click("前往 GitHub 下载页 ↗");
            check(outbound[0]!=null && Intent.ACTION_VIEW.equals(outbound[0].getAction()),"External browser intent");
            check(r.getJSONObject("recommendation").getString("source").equals(outbound[0].getDataString()),"Exact GitHub download URL");
            removeMonitor(monitor);
            click("复制下载页链接");
            final String[] clip=new String[1];
            runOnMainSync(new Runnable(){public void run(){ClipboardManager cm=(ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE);clip[0]=cm.getPrimaryClip().getItemAt(0).getText().toString();}});
            check(clip[0].equals(r.getJSONObject("recommendation").getString("source")),"Clipboard download URL");
            click("← 返回检测结果");
            String before=r.getString("scannedAt");Thread.sleep(1100);
            click("重新检测");
            for(int i=0;i<100 && report().getString("scannedAt").equals(before);i++)Thread.sleep(100);
            check(!report().getString("scannedAt").equals(before),"Rescan refreshes live report");
            // Invoke the same export method as the dialog; intercept only the external chooser.
            addMonitor(monitor);
            final java.lang.reflect.Method export=activity.getClass().getDeclaredMethod("export",boolean.class);export.setAccessible(true);
            runOnMainSync(new Runnable(){public void run(){try{export.invoke(activity,true);}catch(Exception e){throw new RuntimeException(e);}}});
            check(outbound[0]!=null && Intent.ACTION_CHOOSER.equals(outbound[0].getAction()),"JSON export chooser");
            Intent send=outbound[0].getParcelableExtra(Intent.EXTRA_INTENT);
            android.net.Uri uri=send.getParcelableExtra(Intent.EXTRA_STREAM);
            java.io.InputStream in=getTargetContext().getContentResolver().openInputStream(uri);
            java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] buf=new byte[1024];int n;
            while((n=in.read(buf))!=-1)bytes.write(buf,0,n);in.close();
            JSONObject exported=new JSONObject(bytes.toString("UTF-8"));
            check(exported.getString("dataSource").equals("live_device"),"Export contains real scan data");
            check((send.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION)!=0,"Report grants temporary read access");
            removeMonitor(monitor);
            result.putString("report",report().toString());
            result.putString("stream","PASS: "+checks+" native APK checks; API "+Build.VERSION.SDK_INT+" / Android "+Build.VERSION.RELEASE+"\n");
            finish(Activity.RESULT_OK,result);
        }catch(Throwable e){result.putString("stream","FAIL after "+checks+" checks: "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}
    }
}
