package com.cashappsy.webtool;

import android.Manifest;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.webkit.*;
import android.widget.*;
import org.json.JSONObject;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

public class MainActivity extends Activity {
    private WebView web;
    private ProgressBar progress;
    private String site;
    private LinearLayout layout;
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        UpdatesService.channel(this);
        site=getSharedPreferences("webtool",MODE_PRIVATE).getString("site",BuildConfig.DEFAULT_SITE_URL);
        layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);layout.setBackgroundColor(Color.rgb(245,246,248));layout.setOnApplyWindowInsetsListener((v,insets)->{if(Build.VERSION.SDK_INT>=30){android.graphics.Insets system=insets.getInsets(android.view.WindowInsets.Type.systemBars()|android.view.WindowInsets.Type.displayCutout());v.setPadding(system.left,system.top,system.right,system.bottom);}else v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});
        LinearLayout bar=new LinearLayout(this);bar.setPadding(dp(8),dp(4),dp(8),dp(4));bar.setBackgroundColor(Color.rgb(24,27,34));
        TextView title=new TextView(this);title.setText("MTN · دليل الخدمات");title.setTextColor(Color.WHITE);title.setTextSize(15);title.setGravity(android.view.Gravity.CENTER_VERTICAL);bar.addView(title,new LinearLayout.LayoutParams(0,dp(48),1));
        for(String label:new String[]{"↻","إشعارات","الرابط"}){Button button=new Button(this);button.setText(label);button.setTextSize(11);button.setMinimumWidth(0);button.setContentDescription(label);button.setOnClickListener(v->{if(label.equals("↻"))web.reload();else if(label.equals("الرابط"))configure();else enableNotifications();});bar.addView(button,new LinearLayout.LayoutParams(dp(label.equals("↻")?44:label.equals("إشعارات")?80:65),dp(48)));}
        layout.addView(bar);progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);layout.addView(progress,new LinearLayout.LayoutParams(-1,dp(3)));
        web=new WebView(this);layout.addView(web,new LinearLayout.LayoutParams(-1,0,1));setContentView(layout);
        WebSettings settings=web.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);settings.setMediaPlaybackRequiresUserGesture(true);settings.setUserAgentString(settings.getUserAgentString()+" WebtoolAndroid/1.0");
        web.setWebChromeClient(new WebChromeClient(){@Override public void onProgressChanged(WebView view,int value){progress.setProgress(value);progress.setVisibility(value==100?android.view.View.GONE:android.view.View.VISIBLE);}});
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){Uri u=request.getUrl();if(!request.isForMainFrame())return false;if(isSameSite(u))return false;if("https".equals(u.getScheme())||"tel".equals(u.getScheme())||"mailto".equals(u.getScheme())){try{startActivity(new Intent(Intent.ACTION_VIEW,u));}catch(Exception e){message("لا يوجد تطبيق لفتح الرابط");}}return true;}
            @Override public void onReceivedError(WebView view,WebResourceRequest request,WebResourceError error){if(request.isForMainFrame())new AlertDialog.Builder(MainActivity.this).setTitle("تعذّر الاتصال").setMessage("تحقق من الإنترنت ورابط الموقع ثم أعد المحاولة.").setPositiveButton("إعادة المحاولة",(d,w)->web.reload()).setNegativeButton("تغيير الرابط",(d,w)->configure()).show();}
        });
        if(saved!=null&&validSite(site)){web.restoreState(saved);initializeFirebase();}else if(validSite(site)){web.loadUrl(site);initializeFirebase();}else configure();
        openNotification(getIntent());
    }
    private boolean validSite(String value){try{Uri u=Uri.parse(value);return "https".equals(u.getScheme())&&u.getHost()!=null&&u.getUserInfo()==null&&u.getQuery()==null&&u.getFragment()==null&&(u.getPath()==null||u.getPath().equals("")||u.getPath().equals("/"));}catch(Exception e){return false;}}
    private boolean isSameSite(Uri value){try{Uri s=Uri.parse(site);return "https".equals(value.getScheme())&&s.getHost().equalsIgnoreCase(value.getHost())&&s.getPort()==value.getPort();}catch(Exception e){return false;}}
    private void configure(){EditText input=new EditText(this);input.setSingleLine(true);input.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);input.setHint("https://your-site.workers.dev");input.setText(site);AlertDialog d=new AlertDialog.Builder(this).setTitle("رابط الموقع المنشور").setMessage("أدخل رابط موقعك على Cloudflare. المحتوى المنشور يتحدث تلقائياً داخل التطبيق.").setView(input).setPositiveButton("اتصال",null).setNegativeButton("إلغاء",null).create();d.setOnShowListener(x->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String value=input.getText().toString().trim();if(!validSite(value)){input.setError("أدخل رابط HTTPS رئيسياً صالحاً");return;}String old=site;site=value.endsWith("/")?value.substring(0,value.length()-1):value;getSharedPreferences("webtool",MODE_PRIVATE).edit().putString("site",site).apply();if(!old.equals(site)){for(FirebaseApp app:FirebaseApp.getApps(this))app.delete();CookieManager.getInstance().removeAllCookies(null);web.clearCache(true);WebStorage.getInstance().deleteAllData();}web.loadUrl(site);initializeFirebase();d.dismiss();}));d.show();}
    private void initializeFirebase(){if(!validSite(site))return;final String origin=site;new Thread(()->{try{HttpsURLConnection c=(HttpsURLConnection)new URL(origin+"/api/config").openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(15000);c.setReadTimeout(15000);if(c.getResponseCode()!=200)throw new Exception();JSONObject json;try(java.io.InputStream in=c.getInputStream()){java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] chunk=new byte[4096];int read,total=0;while((read=in.read(chunk))!=-1){total+=read;if(total>65536)throw new Exception("Configuration too large");bytes.write(chunk,0,read);}json=new JSONObject(bytes.toString("UTF-8"));}finally{c.disconnect();}JSONObject f=json.getJSONObject("firebase");String appId=json.optString("androidAppId");if(appId.isEmpty())return;FirebaseOptions options=new FirebaseOptions.Builder().setApplicationId(appId).setApiKey(f.getString("apiKey")).setProjectId(f.getString("projectId")).setGcmSenderId(f.getString("messagingSenderId")).build();runOnUiThread(()->{if(!site.equals(origin))return;try{if(FirebaseApp.getApps(this).isEmpty())FirebaseApp.initializeApp(this,options);UpdatesService.register(this);}catch(Exception e){message("تعذّر إعداد إشعارات Firebase");}});}catch(Exception e){runOnUiThread(()->message("الموقع متاح؛ إعدادات دفع Android لم تُفعّل بعد"));}}).start();}
    private void enableNotifications(){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},10);return;}if(FirebaseApp.getApps(this).isEmpty()){message("أضف إعدادات Firebase Android في الموقع أولاً");initializeFirebase();return;}UpdatesService.register(this);message("جارٍ تسجيل الجهاز للإشعارات");}
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] results){super.onRequestPermissionsResult(code,permissions,results);if(code==10&&results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)enableNotifications();}
    @Override public void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);openNotification(intent);}
    private void openNotification(Intent intent){String path=intent.getStringExtra("url");if(path!=null&&validSite(site)){try{Uri target=Uri.parse(new URL(new URL(site),path).toString());if(isSameSite(target))web.loadUrl(target.toString());}catch(Exception ignored){}}}
    @Override public void onSaveInstanceState(Bundle state){web.saveState(state);super.onSaveInstanceState(state);}
    @Override public void onBackPressed(){if(web.canGoBack())web.goBack();else super.onBackPressed();}
    @Override public void onDestroy(){web.destroy();super.onDestroy();}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private void message(String text){Toast.makeText(this,text,Toast.LENGTH_LONG).show();}
}
