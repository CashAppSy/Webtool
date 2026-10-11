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
    public static void register(Context context){FirebaseAuth auth=FirebaseAuth.getInstance();if(auth.getCurrentUser()==null){auth.signInAnonymously().addOnSuccessListener(result->register(context));return;}auth.getCurrentUser().getIdToken(false).addOnSuccessListener(id->FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token->{String site=context.getSharedPreferences("webtool",MODE_PRIVATE).getString("site","");if(!site.startsWith("https://"))return;new Thread(()->{try{HttpsURLConnection c=(HttpsURLConnection)new URL(site+"/api/push/register").openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(15000);c.setReadTimeout(15000);c.setRequestMethod("POST");c.setRequestProperty("Authorization","Bearer "+id.getToken());c.setRequestProperty("Content-Type","application/json");c.setDoOutput(true);try(java.io.OutputStream out=c.getOutputStream()){out.write(new JSONObject().put("token",token).put("platform","android").toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}int status=c.getResponseCode();c.disconnect();}catch(Exception ignored){}}).start();}));}
    @Override public void onNewToken(String token){try{register(this);}catch(Exception ignored){}}
    @Override public void onMessageReceived(RemoteMessage message){channel(this);String title=message.getData().get("title"),body=message.getData().get("body");if(title==null&&message.getNotification()!=null){title=message.getNotification().getTitle();body=message.getNotification().getBody();}Intent intent=new Intent(this,MainActivity.class).putExtra("url",message.getData().getOrDefault("url","/")).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);int id=message.getMessageId()==null?1:message.getMessageId().hashCode();PendingIntent pending=PendingIntent.getActivity(this,id,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);Notification notification=new Notification.Builder(this,"webtool_updates").setSmallIcon(com.cashappsy.webtool.R.drawable.app_icon).setContentTitle(title==null?"MTN Syria":title).setContentText(body).setStyle(new Notification.BigTextStyle().bigText(body)).setContentIntent(pending).setAutoCancel(true).build();try{((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(id,notification);}catch(SecurityException ignored){}}
}
