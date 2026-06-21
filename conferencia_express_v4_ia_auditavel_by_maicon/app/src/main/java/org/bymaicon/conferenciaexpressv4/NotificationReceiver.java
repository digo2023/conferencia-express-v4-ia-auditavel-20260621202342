package org.bymaicon.conferenciaexpressv4;

import android.app.*;import android.content.*;import android.os.*;import androidx.core.app.NotificationCompat;import java.text.*;import java.util.*;

public class NotificationReceiver extends BroadcastReceiver{
    static SimpleDateFormat br=new SimpleDateFormat("dd/MM/yyyy",new Locale("pt","BR"));
    public void onReceive(Context ctx,Intent intent){
        try{
            if(intent!=null && Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())){schedule(ctx,6,0,6003); schedule(ctx,12,0,1203); new DatabaseHelper(ctx).inserirEvento("SISTEMA","Conferência Express reativado","Alertas 06:00 e 12:00 restaurados após reinício do celular."); return;}
            DatabaseHelper db=new DatabaseHelper(ctx);ArrayList<DatabaseHelper.Pendente> pend=db.listarPendentes();
            int venc=0,n24=0,n48=0,n72=0;ArrayList<String> nomesVenc=new ArrayList<>(),nomes24=new ArrayList<>(),nomes48=new ArrayList<>(),nomes72=new ArrayList<>();
            for(DatabaseHelper.Pendente p:pend){int jan=janelaHoras(p.consumoDatas);if(jan<0){venc++;addNome(nomesVenc,p);}else{if(jan<=24){n24++;addNome(nomes24,p);}if(jan<=48){n48++;addNome(nomes48,p);}if(jan<=72){n72++;addNome(nomes72,p);}}}
            String titulo=pend.size()>0?"🔴 Conferência Express: faltantes pendentes":"✅ Conferência Express: sem pendências abertas";
            String msg="ALERTA PREVENTIVO EXPRESS COLORADO — conferir antes da produção.\n"+
                    "Vencidos: "+venc+" | 24h: "+n24+" | 48h: "+n48+" | 72h: "+n72+" | Total pendente: "+pend.size()+"\n"+
                    "VENCIDOS: "+join(nomesVenc)+"\n"+
                    "24H: "+join(nomes24)+"\n"+
                    "48H: "+join(nomes48)+"\n"+
                    "72H: "+join(nomes72)+"\n"+
                    "Abra o app para marcar item resolvido, manter pendente ou gerar nova conferência.";
            NotificationManager nm=(NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if(Build.VERSION.SDK_INT>=26){NotificationChannel ch=new NotificationChannel("conferencia_express_alertas","Conferência Express",NotificationManager.IMPORTANCE_DEFAULT);ch.setDescription("Alertas detalhados 06:00 e 12:00 sobre faltantes pendentes");nm.createNotificationChannel(ch);} 
            NotificationCompat.Builder b=new NotificationCompat.Builder(ctx,"conferencia_express_alertas").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(titulo).setContentText("Vencidos: "+venc+" | 24h: "+n24+" | 48h: "+n48+" | 72h: "+n72).setStyle(new NotificationCompat.BigTextStyle().bigText(msg)).setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_HIGH);
            nm.notify((int)(System.currentTimeMillis()%100000),b.build());db.inserirEvento("ALERTA",titulo,msg);
        }catch(Exception ignored){}
    }
    static void addNome(ArrayList<String> l,DatabaseHelper.Pendente p){l.add("["+(p.categoria==null?"":p.categoria)+"] "+p.nome+" ("+String.format(new Locale("pt","BR"),"%.3f",p.faltante)+" "+p.un+")");}
    static String join(ArrayList<String> l){if(l.isEmpty())return "nenhum";StringBuilder sb=new StringBuilder();for(int i=0;i<l.size();i++){if(i>0)sb.append("; ");sb.append(l.get(i));}return sb.toString();}
    static int janelaHoras(String datas){try{Calendar hoje=Calendar.getInstance();zerar(hoje);int melhor=9999;for(String d:(datas==null?"":datas).split(",")){d=d.trim();if(d.length()==5)d=d+"/"+hoje.get(Calendar.YEAR);if(d.length()<10)continue;Calendar x=Calendar.getInstance();x.setTime(br.parse(d));zerar(x);long diff=x.getTimeInMillis()-hoje.getTimeInMillis();int h=(int)Math.ceil(diff/3600000.0);if(h>=0&&h<melhor)melhor=h;}return melhor;}catch(Exception e){return 9999;}}
    static void zerar(Calendar c){c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);} 
    static void schedule(Context ctx,int hora,int minuto,int codigo){try{AlarmManager am=(AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);Intent it=new Intent(ctx,NotificationReceiver.class);it.setAction("org.bymaicon.conferenciaexpressv4.ALERTA_DIARIO");PendingIntent pi=PendingIntent.getBroadcast(ctx,codigo,it,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);Calendar cal=Calendar.getInstance();cal.set(Calendar.HOUR_OF_DAY,hora);cal.set(Calendar.MINUTE,minuto);cal.set(Calendar.SECOND,0);if(cal.getTimeInMillis()<=System.currentTimeMillis())cal.add(Calendar.DAY_OF_YEAR,1);am.setInexactRepeating(AlarmManager.RTC_WAKEUP,cal.getTimeInMillis(),AlarmManager.INTERVAL_DAY,pi);}catch(Exception ignored){}}
}
