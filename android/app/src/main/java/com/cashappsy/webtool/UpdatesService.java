package com.cashappsy.webtool;
import android.app.*;
import android.content.*;
import android.os.Build;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.*;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONObject;
public class UpdatesService extends FirebaseMessagingService {
    public static void channel(Context c){NotificationManager manager=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);manager.createNotificationChannel(new NotificationChannel("webtool_updates","تحديثات الخدمات",NotificationManager.IMPORTANCE_DEFAULT));}
    public interface RegistrationResult { void complete(boolean success,String message); }
    private static void result(RegistrationResult callback,boolean success,String message){if(callback!=null)new android.os.Handler(android.os.Looper.getMainLooper()).post(()->callback.complete(success,message));}
    public static void register(Context context){register(context,null);}
    public static void register(Context context,RegistrationResult callback){
        NotificationManager manager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        if(!manager.areNotificationsEnabled()||(Build.VERSION.SDK_INT>=33&&context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)){result(callback,false,"إشعارات التطبيق غير مسموحة. فعّلها من إعدادات الهاتف.");return;}
        NotificationChannel channel=manager.getNotificationChannel("webtool_updates");if(channel!=null&&channel.getImportance()==NotificationManager.IMPORTANCE_NONE){result(callback,false,"قناة تحديثات الخدمات معطّلة في إعدادات إشعارات التطبيق.");return;}
        FirebaseAuth auth=FirebaseAuth.getInstance();if(auth.getCurrentUser()==null){auth.signInAnonymously().addOnSuccessListener(user->register(context,callback)).addOnFailureListener(error->result(callback,false,"تعذّر تسجيل الجهاز في Firebase. تأكد من تفعيل Anonymous في Authentication واتصال الإنترنت."));return;}
        auth.getCurrentUser().getIdToken(false).addOnFailureListener(error->result(callback,false,"تعذّر التحقق من الجهاز. حاول مجدداً.")).addOnSuccessListener(id->FirebaseMessaging.getInstance().getToken().addOnFailureListener(error->result(callback,false,"تعذّر الحصول على رمز الإشعارات. تحقق من الإنترنت وخدمات Google Play وإعدادات Firebase Android.")).addOnSuccessListener(token->{
            String site=context.getSharedPreferences("webtool",MODE_PRIVATE).getString("site","");if(!site.startsWith("https://")){result(callback,false,"تعذّر تحديد موقع التطبيق.");return;}
            new Thread(()->{HttpsURLConnection c=null;try{c=(HttpsURLConnection)new URL(site+"/api/push/register").openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(15000);c.setReadTimeout(15000);c.setRequestMethod("POST");c.setRequestProperty("Authorization","Bearer "+id.getToken());c.setRequestProperty("Content-Type","application/json");c.setDoOutput(true);try(java.io.OutputStream out=c.getOutputStream()){out.write(new JSONObject().put("token",token).put("platform","android").toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}int status=c.getResponseCode();result(callback,status==200,status==200?"تم تسجيل هذا الهاتف بنجاح لتلقي إشعارات الدفع.":status==429?"تعذّر التسجيل: تم بلوغ الحد المسموح للأجهزة.":"تعذّر تسجيل الجهاز في الموقع ("+status+"). حاول مجدداً.");}catch(Exception error){result(callback,false,"تعذّر الاتصال بالموقع لتسجيل الإشعارات. تحقق من الإنترنت وحاول مجدداً.");}finally{if(c!=null)c.disconnect();}}).start();
        }));
    }
    @Override public void onNewToken(String token){try{register(this);}catch(Exception ignored){}}
    @Override public void onMessageReceived(RemoteMessage message){channel(this);String title=message.getData().get("title"),body=message.getData().get("body");if(title==null&&message.getNotification()!=null){title=message.getNotification().getTitle();body=message.getNotification().getBody();}Intent intent=new Intent(this,MainActivity.class).putExtra("url",message.getData().getOrDefault("url","/")).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);int id=message.getMessageId()==null?1:message.getMessageId().hashCode();PendingIntent pending=PendingIntent.getActivity(this,id,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);Notification notification=new Notification.Builder(this,"webtool_updates").setSmallIcon(com.cashappsy.webtool.R.drawable.notification_icon).setContentTitle(title==null?"Training Hub":title).setContentText(body).setStyle(new Notification.BigTextStyle().bigText(body)).setContentIntent(pending).setAutoCancel(true).build();try{((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(id,notification);}catch(SecurityException ignored){}}
}
