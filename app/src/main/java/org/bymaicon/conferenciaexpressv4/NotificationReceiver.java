package org.bymaicon.conferenciaexpressv4;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import java.util.*;

public class NotificationReceiver extends BroadcastReceiver {
    static final String CHANNEL="conferencia_express_alertas";
    @Override public void onReceive(Context context, Intent intent){
        DatabaseHelper db=new DatabaseHelper(context);
        ArrayList<String> pend=db.pendenciasAbertas();
        String titulo="Conferência Express IA";
        String msg=pend.isEmpty()?"Nenhum faltante pendente registrado.":"Pendências abertas: "+pend.size()+". Toque para revisar no app.";
        NotificationManager nm=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26){
            NotificationChannel ch=new NotificationChannel(CHANNEL,"Alertas Conferência Express",NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(ch);
        }
        NotificationCompat.Builder b=new NotificationCompat.Builder(context,CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(titulo)
                .setContentText(msg)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(resumo(pend)))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        nm.notify(1206,b.build());
    }
    static String resumo(ArrayList<String> p){
        if(p.isEmpty()) return "Sem pendências registradas.";
        StringBuilder s=new StringBuilder("Faltantes pendentes para prevenção de falta:\n");
        int n=1; for(String x:p){ s.append(n++).append(". ").append(x).append('\n'); }
        return s.toString();
    }
}
