package org.bymaicon.conferenciaexpressv4;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import androidx.core.app.ActivityCompat;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import java.io.*;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.*;

public class MainActivity extends Activity {
    static final int REQ_ANALITICO=10, REQ_OC=11;
    Uri analiticoUri, ocUri;
    String analiticoText="", ocText="", modulo="TOTAL";
    EditText dataIni, dataFim;
    TextView status, dash;
    LinearLayout lista;
    DatabaseHelper db;
    ArrayList<Item> consumo=new ArrayList<>(), oc=new ArrayList<>(), resultado=new ArrayList<>();
    int azul=Color.rgb(2,8,24), azul2=Color.rgb(0,74,173), ouro=Color.rgb(245,164,0);

    static class Item{
        String codigo="", nome="", un="", data="", entrega="", categoria="PERECÍVEL", status="", motivo="", ocNome="";
        double qtd=0, comprado=0, faltante=0, confianca=0;
        boolean selecionado=true;
        TreeSet<String> datas=new TreeSet<>();
    }

    @Override public void onCreate(Bundle b){
        super.onCreate(b); db=new DatabaseHelper(this); PDFBoxResourceLoader.init(getApplicationContext());
        if(Build.VERSION.SDK_INT>=33) ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.POST_NOTIFICATIONS},50);
        montarTela(); agendarAlertas();
    }

    void montarTela(){
        ScrollView sc=new ScrollView(this); LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(28,28,28,38); root.setBackgroundColor(azul); sc.addView(root);
        TextView t=txt("CONFERÊNCIA EXPRESS IA",30,Color.WHITE,true); t.setGravity(Gravity.CENTER); root.addView(t);
        TextView sub=txt("Inteligência operacional Analítico x O.C.",15,ouro,true); sub.setGravity(Gravity.CENTER); root.addView(sub);
        LinearLayout dates=new LinearLayout(this); dates.setOrientation(LinearLayout.HORIZONTAL); dates.setPadding(0,18,0,12); root.addView(dates);
        dataIni=campo("Data inicial"); dataFim=campo("Data final"); dates.addView(dataIni,new LinearLayout.LayoutParams(0,90,1)); dates.addView(dataFim,new LinearLayout.LayoutParams(0,90,1));
        Button a=btn("📄 Escolher PDF Analítico"); root.addView(a); a.setOnClickListener(v->abrir(REQ_ANALITICO));
        Button o=btn("🧾 Escolher PDF Ordem de Compra"); root.addView(o); o.setOnClickListener(v->abrir(REQ_OC));
        TextView mod=txt("Módulos de conferência",18,Color.WHITE,true); mod.setPadding(0,20,0,8); root.addView(mod);
        LinearLayout g=new LinearLayout(this); g.setOrientation(LinearLayout.VERTICAL); root.addView(g);
        String[] nomes={"🥩 Conferir proteínas","🥬 Conferir hortifruti","🧪 Conferir químicos","📦 Conferir perecíveis","🌐 Conferência total"};
        String[] cats={"PROTEÍNAS","HORTIFRUTI","QUÍMICOS","PERECÍVEL","TOTAL"};
        for(int i=0;i<nomes.length;i++){ final String c=cats[i]; Button bb=btn(nomes[i]); g.addView(bb); bb.setOnClickListener(v->{modulo=c; analisar();}); }
        Button revisar=btn("✅ Baixar relatório revisado"); root.addView(revisar); revisar.setOnClickListener(v->salvarSelecionados());
        Button novo=btn("🆕 Nova análise"); root.addView(novo); novo.setOnClickListener(v->novaAnalise());
        Button pend=btn("🔔 Ver/avisar resolvidos"); root.addView(pend); pend.setOnClickListener(v->mostrarPendencias());
        dash=txt("Segurança: aguardando análise",16,Color.WHITE,true); dash.setPadding(0,18,0,8); root.addView(dash);
        status=txt("Selecione o Analítico e a O.C. Rancho.",14,Color.LTGRAY,false); status.setPadding(0,8,0,8); root.addView(status);
        lista=new LinearLayout(this); lista.setOrientation(LinearLayout.VERTICAL); root.addView(lista);
        setContentView(sc);
    }

    TextView txt(String s,int sp,int cor,boolean bold){ TextView v=new TextView(this); v.setText(s); v.setTextSize(sp); v.setTextColor(cor); if(bold)v.setTypeface(Typeface.DEFAULT_BOLD); return v; }
    EditText campo(String h){ EditText e=new EditText(this); e.setHint(h); e.setTextColor(Color.WHITE); e.setHintTextColor(Color.GRAY); e.setTextSize(18); e.setSingleLine(true); return e; }
    Button btn(String s){ Button b=new Button(this); b.setText(s); b.setTextColor(Color.WHITE); b.setTextSize(16); b.setAllCaps(false); b.setBackgroundColor(azul2); b.setPadding(8,10,8,10); return b; }
    void abrir(int req){ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("application/pdf"); i.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i,req); }
    @Override protected void onActivityResult(int r,int c,Intent d){ super.onActivityResult(r,c,d); if(c!=RESULT_OK||d==null)return; try{ Uri u=d.getData(); getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION); if(r==REQ_ANALITICO){analiticoUri=u; analiticoText=lerPdf(u); log("Analítico OK: "+u);} if(r==REQ_OC){ocUri=u; ocText=lerPdf(u); log("O.C. OK: "+u);} }catch(Exception e){erro(e);} }
    String lerPdf(Uri u)throws Exception{ InputStream is=getContentResolver().openInputStream(u); PDDocument doc=PDDocument.load(is); PDFTextStripper st=new PDFTextStripper(); st.setSortByPosition(true); String s=st.getText(doc); doc.close(); if(is!=null)is.close(); return s==null?"":s; }
    void log(String s){ status.setText(status.getText()+"\n"+s); }
    void erro(Exception e){ status.setText("ERRO: "+e.getMessage()); }

    void analisar(){
        try{
            if(analiticoText.isEmpty()||ocText.isEmpty()){ toast("Selecione Analítico e O.C."); return; }
            consumo=extrairAnalitico(analiticoText); oc=extrairOC(ocText); resultado=conferir(consumo,oc);
            if(consumo.size()<10 || oc.size()<5){ salvarDebug(); throw new Exception("Extração baixa. Conferência bloqueada e debug salvo."); }
            montarRevisao(); salvarPrefs(); dash.setText("Segurança: análise pronta | Analítico: "+consumo.size()+" | O.C.: "+oc.size()+" | Revisar: "+resultado.size());
        }catch(Exception e){ erro(e); }
    }

    ArrayList<Item> extrairAnalitico(String text)throws Exception{
        ArrayList<Item> out=new ArrayList<>(); String ini=dataIni.getText().toString().trim(), fim=dataFim.getText().toString().trim();
        Date di=parseDate(ini), df=parseDate(fim); if(di==null||df==null) throw new Exception("Informe período no formato dd/mm/aaaa.");
        String dataAtual=""; Pattern dataServico=Pattern.compile("(\\d{2}/\\d{2}/\\d{4})\\s+(segunda|terça|terca|quarta|quinta|sexta|sábado|sabado|domingo)-feira?",Pattern.CASE_INSENSITIVE);
        Pattern prod=Pattern.compile("^(\\d+\\.\\d+\\.\\d+\\.\\d+\\.\\d+)\\s+(.+?)\\s+([0-9]{1,6},[0-9]{2,5})\\s+([0-9]{1,6},[0-9]{2,5})\\s+(KG|LT|UN|CX|PC)\\s*$");
        String[] linhas=text.split("\\r?\\n");
        for(String raw:linhas){ String l=raw.trim().replaceAll("\\s+"," "); if(l.length()<5)continue;
            Matcher md=dataServico.matcher(l); if(md.find()){ String d=md.group(1); Date dd=parseDate(d); if(dd!=null && !dd.before(di) && !dd.after(df)) dataAtual=d.substring(0,5); continue; }
            if(dataAtual.isEmpty()) continue; if(l.contains("Emissão")||l.contains("Limite de Entrega")||l.contains("Período de Entrega")||l.contains("Solicitação")) continue;
            Matcher m=prod.matcher(l); if(m.find()){ Item it=new Item(); it.codigo=m.group(1); it.nome=limpaNome(m.group(2)); it.qtd=num(m.group(3)); it.un=m.group(5); it.data=dataAtual; it.categoria=categoria(it.nome,it.codigo); it.datas.add(dataAtual); out.add(it); }
        }
        return agregar(out,true);
    }

    ArrayList<Item> extrairOC(String text){
        ArrayList<Item> out=new ArrayList<>(); Pattern p=Pattern.compile("^(\\d+\\.\\d+\\.\\d+\\.\\d+\\.\\d+)\\s+(.+?)\\s+(KG|LT|UN|CX|PC)\\s+([0-9]{1,7},[0-9]{1,3})\\s+(\\d{2}/\\d{2}/\\d{4})\\s+(\\d{2}/\\d{2}/\\d{4}).*$");
        for(String raw:text.split("\\r?\\n")){ String l=raw.trim().replaceAll("\\s+"," "); Matcher m=p.matcher(l); if(m.find()){ Item it=new Item(); it.codigo=m.group(1); it.nome=limpaNome(m.group(2)); it.un=m.group(3); it.qtd=converterEmbalagem(it.nome,num(m.group(4)),it.un); it.entrega=m.group(5); it.data=m.group(6).substring(0,5); it.categoria=categoria(it.nome,it.codigo); out.add(it); }}
        return agregar(out,false);
    }

    ArrayList<Item> agregar(ArrayList<Item> in, boolean datas){ LinkedHashMap<String,Item> map=new LinkedHashMap<>(); for(Item it:in){ String k=norm(it.codigo)+"|"+norm(it.nome)+"|"+it.un; Item a=map.get(k); if(a==null){a=it; map.put(k,a);} else {a.qtd+=it.qtd; a.datas.addAll(it.datas); if(a.entrega.isEmpty())a.entrega=it.entrega;} } for(Item i:map.values()) if(datas && !i.datas.isEmpty()) i.data=join(i.datas); return new ArrayList<>(map.values()); }

    ArrayList<Item> conferir(ArrayList<Item> cons,ArrayList<Item> ord){ ArrayList<Item> res=new ArrayList<>(); for(Item c:cons){ if(!modulo.equals("TOTAL") && !c.categoria.equals(modulo)) continue; Item b=melhorOC(c,ord); c.comprado=b==null?0:b.qtd; c.entrega=b==null?entregaPadrao(ord):b.entrega; c.ocNome=b==null?"":b.nome; c.faltante=Math.max(0,c.qtd-c.comprado); c.confianca=b==null?0:confianca(c,b); if(c.faltante>tol(c.un)){ c.status=b==null?"ZERADO NA O.C.":"FALTA PARCIAL"; c.motivo=b==null?"zerado na ordem de compra":"Falta parcial"; c.selecionado=true; res.add(c); } }
        Collections.sort(res,(a,b)->{ int x=a.categoria.compareTo(b.categoria); if(x!=0)return x; return a.nome.compareTo(b.nome); }); return res; }

    Item melhorOC(Item c,ArrayList<Item> ord){ String eq=db.equivalencia(c.nome); Item melhor=null; double nota=-1; for(Item b:ord){ double n=0; if(c.codigo.equals(b.codigo)) n=1; else if(!eq.isEmpty() && norm(eq).equals(norm(b.nome))) n=.98; else if(norm(c.nome).equals(norm(b.nome))) n=.96; else n=similar(norm(c.nome),norm(b.nome)); if(!c.un.equals(b.un)) n-=.25; if(n>nota){nota=n; melhor=b;} } return nota>=.82?melhor:null; }
    double confianca(Item a,Item b){ if(a.codigo.equals(b.codigo))return 1; if(norm(a.nome).equals(norm(b.nome)))return .96; return Math.max(.50,similar(norm(a.nome),norm(b.nome))); }

    void montarRevisao(){ lista.removeAllViews(); int n=1; for(Item c:resultado){ LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(12,12,12,12); card.setBackgroundColor(Color.rgb(3,22,55)); lista.addView(card); CheckBox cb=new CheckBox(this); cb.setTextColor(Color.WHITE); cb.setChecked(c.selecionado); int pos=n++; cb.setText(pos+". ["+c.categoria+"] "+c.nome+"\nNecessário: "+fmt(c.qtd)+" "+c.un+" | O.C.: "+fmt(c.comprado)+" "+c.un+" | Falta: "+fmt(c.faltante)+" "+c.un+"\nConsumo: "+c.data+" | Entrega: "+c.entrega+" | Confiança: "+Math.round(c.confianca*100)+"%\n"+c.motivo); cb.setOnCheckedChangeListener((x,on)->c.selecionado=on); card.addView(cb);
            Button vinc=btn("🔁 Vincular com item da O.C."); card.addView(vinc); vinc.setOnClickListener(v->vincular(c));
            Button cor=btn("✏️ Corrigir quantidade O.C."); card.addView(cor); cor.setOnClickListener(v->corrigirQtd(c));
            Button motivo=btn("🔎 Ver motivo"); card.addView(motivo); motivo.setOnClickListener(v->dialog("Motivo",c.motivo+"\nItem O.C.: "+c.ocNome)); }
    }
    void vincular(Item c){ final EditText e=new EditText(this); e.setHint("Digite o nome do item na O.C."); new AlertDialog.Builder(this).setTitle("Vincular equivalência").setMessage("Item analítico:\n"+c.nome).setView(e).setPositiveButton("Salvar",(d,w)->{String oc=e.getText().toString().trim(); if(!oc.isEmpty()){db.salvarEquivalencia(c.nome,oc); toast("Equivalência salva para próximas análises.");}}).setNegativeButton("Cancelar",null).show(); }
    void corrigirQtd(Item c){ final EditText e=new EditText(this); e.setHint("Quantidade entregue"); new AlertDialog.Builder(this).setTitle("Corrigir quantidade").setView(e).setPositiveButton("Aplicar",(d,w)->{try{c.comprado=num(e.getText().toString());c.faltante=Math.max(0,c.qtd-c.comprado);montarRevisao();}catch(Exception ex){erro(ex);}}).setNegativeButton("Cancelar",null).show(); }

    void salvarSelecionados(){ try{ ArrayList<Item> rows=new ArrayList<>(); for(Item i:resultado) if(i.selecionado) rows.add(i); if(rows.isEmpty()){toast("Nenhum item selecionado.");return;} gerarPdf(rows); for(Item i:rows) db.salvarPendencia(i.nome,i.categoria,i.faltante,i.un,i.data,i.entrega); toast("Relatório salvo e pendências registradas para notificações."); }catch(Exception e){erro(e);} }
    void gerarPdf(ArrayList<Item> rows)throws Exception{ PdfDocument doc=new PdfDocument(); Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); int W=1600,H=1000; PdfDocument.Page page=doc.startPage(new PdfDocument.PageInfo.Builder(W,H,1).create()); Canvas c=page.getCanvas(); c.drawColor(Color.WHITE); p.setColor(azul); c.drawRect(0,0,W,230,p); p.setColor(ouro); c.drawRect(0,230,W,238,p); p.setColor(Color.WHITE); p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(44); c.drawText("RELATÓRIO PREVISÃO DE FALTANTES",520,105,p); p.setTextSize(28); p.setColor(Color.rgb(0,74,173)); c.drawText("EXPRESS-UNIDADE COLORADO",90,105,p); p.setColor(Color.WHITE); p.setTextSize(18); c.drawText("CONFERÊNCIA REALIZADA COM BASE ANALITICO X O.C.",90,150,p); p.setColor(ouro); p.setTextSize(26); c.drawText("ENTREGA PREVISTA: "+entregaPadrao(rows),120,310,p); int y=380; header(c,p,y); y+=58; String cat=""; int n=1; for(Item r:rows){ if(!cat.equals(r.categoria)){cat=r.categoria; p.setColor(azul); c.drawRect(60,y,W-60,y+38,p); p.setColor(ouro); p.setTextSize(22); c.drawText("TIPO: "+cat,80,y+27,p); y+=45;} p.setColor(Color.BLACK); p.setTextSize(18); c.drawText(""+(n++),80,y+25,p); c.drawText(cut(r.nome,36),135,y+25,p); c.drawText(fmt(r.qtd)+" "+r.un,480,y+25,p); c.drawText(fmt(r.comprado)+" "+r.un,660,y+25,p); p.setColor(Color.RED); c.drawText(fmt(r.faltante)+" "+r.un,840,y+25,p); p.setColor(Color.BLACK); c.drawText(r.data,1040,y+25,p); c.drawText(r.entrega,1210,y+25,p); c.drawText(cut(r.motivo,25),1380,y+25,p); y+=45; if(y>900){doc.finishPage(page); page=doc.startPage(new PdfDocument.PageInfo.Builder(W,H,doc.getPages().size()+1).create()); c=page.getCanvas(); c.drawColor(Color.WHITE); y=80; header(c,p,y); y+=58;}} p.setColor(Color.DKGRAY); p.setTextSize(24); c.drawText(new SimpleDateFormat("dd/MM/yyyy",Locale.getDefault()).format(new Date())+" - By Maicon",1220,960,p); doc.finishPage(page); String nome="conferencia_express_previsao_faltantes_"+entregaPadrao(rows).replace('/','-')+".pdf"; OutputStream os=saida(nome,"application/pdf"); doc.writeTo(os); os.close(); doc.close(); }
    void header(Canvas c,Paint p,int y){ p.setColor(Color.rgb(220,235,248)); c.drawRect(60,y,1540,y+55,p); p.setColor(Color.BLACK); p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(18); c.drawText("Nº",80,y+35,p); c.drawText("ITEM",210,y+35,p); c.drawText("NECESSÁRIO",480,y+35,p); c.drawText("ENTREGA",660,y+35,p); c.drawText("FALTANTE",840,y+35,p); c.drawText("CONSUMO",1040,y+35,p); c.drawText("DATA ENTREGA",1210,y+35,p); c.drawText("OBSERVAÇÕES",1380,y+35,p); }
    OutputStream saida(String nome,String mime)throws Exception{ if(Build.VERSION.SDK_INT>=29){ android.content.ContentValues v=new android.content.ContentValues(); v.put(MediaStore.MediaColumns.DISPLAY_NAME,nome); v.put(MediaStore.MediaColumns.MIME_TYPE,mime); v.put(MediaStore.MediaColumns.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/ConferenciaExpress"); Uri u=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,v); return getContentResolver().openOutputStream(u);} File dir=new File(Environment.getExternalStorageDirectory(),"Download/ConferenciaExpress"); dir.mkdirs(); return new FileOutputStream(new File(dir,nome)); }

    void salvarDebug(){ try{ OutputStream os=saida("debug_conferencia_express.txt","text/plain"); os.write(("ANALITICO\n"+analiticoText+"\n\nOC\n"+ocText).getBytes("UTF-8")); os.close(); }catch(Exception ignored){} }
    void novaAnalise(){ analiticoUri=null; ocUri=null; analiticoText=""; ocText=""; consumo.clear(); oc.clear(); resultado.clear(); lista.removeAllViews(); status.setText("Nova conferência iniciada. Memória da IA mantida."); }
    void mostrarPendencias(){ ArrayList<String> p=db.pendenciasAbertas(); dialog("Pendências registradas",p.isEmpty()?"Sem pendências abertas.":joinList(p)); }
    void dialog(String t,String m){ new AlertDialog.Builder(this).setTitle(t).setMessage(m).setPositiveButton("OK",null).show(); }
    void toast(String s){ Toast.makeText(this,s,Toast.LENGTH_LONG).show(); }

    void agendarAlertas(){ try{ AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE); for(int h:new int[]{6,12}){ Intent i=new Intent(this,NotificationReceiver.class); PendingIntent pi=PendingIntent.getBroadcast(this,h,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE); Calendar cal=Calendar.getInstance(); cal.set(Calendar.HOUR_OF_DAY,h); cal.set(Calendar.MINUTE,0); cal.set(Calendar.SECOND,0); if(cal.getTimeInMillis()<System.currentTimeMillis()) cal.add(Calendar.DATE,1); am.setRepeating(AlarmManager.RTC_WAKEUP,cal.getTimeInMillis(),AlarmManager.INTERVAL_DAY,pi); }}catch(Exception ignored){} }
    void salvarPrefs(){ getSharedPreferences("resumo",MODE_PRIVATE).edit().putInt("faltantes",resultado.size()).apply(); }

    Date parseDate(String s){ try{return new SimpleDateFormat("dd/MM/yyyy",Locale.getDefault()).parse(s);}catch(Exception e){return null;} }
    double num(String s){ try{return Double.parseDouble(s.replace(".","").replace(",","."));}catch(Exception e){return 0;} }
    String fmt(double v){ return String.format(Locale.US,"%.3f",v).replace('.',','); }
    double tol(String u){ return u.equals("UN")?0.9:0.05; }
    String norm(String s){ String n=Normalizer.normalize(s==null?"":s,Normalizer.Form.NFD).replaceAll("\\p{M}",""); return n.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+"," ").trim(); }
    String limpaNome(String s){ return s.replaceAll("\\s+"," ").trim().toUpperCase(Locale.ROOT); }
    String cut(String s,int n){ return s.length()<=n?s:s.substring(0,n-1); }
    String join(TreeSet<String> s){ StringBuilder b=new StringBuilder(); for(String x:s){ if(b.length()>0)b.append(", "); b.append(x);} return b.toString(); }
    String joinList(ArrayList<String> a){ StringBuilder b=new StringBuilder(); int n=1; for(String x:a)b.append(n++).append(". ").append(x).append("\n"); return b.toString(); }
    String entregaPadrao(Collection<Item> xs){ for(Item i:xs) if(i.entrega!=null&&!i.entrega.isEmpty()) return i.entrega; return ""; }
    double converterEmbalagem(String nome,double q,String un){ String n=norm(nome); Matcher m=Pattern.compile("(\\d+) KG X (CX|PC)").matcher(n); if((un.equals("CX")||un.equals("PC"))&&m.find()) return q*Double.parseDouble(m.group(1)); Matcher u=Pattern.compile("(\\d+) UN X (\\d+)").matcher(n); if(un.equals("CX")&&u.find()) return q*Double.parseDouble(u.group(1))*Double.parseDouble(u.group(2)); return q; }
    String categoria(String nome,String cod){ String n=norm(nome); if(cod.startsWith("1.01")||n.matches(".*(CARNE|FRANGO|BOVINA|SUINA|LINGUICA|SALSICHA|BACON|HAMBURGUER|PEIXE|SASSAMI|PANCETA).*"))return "PROTEÍNAS"; if(cod.startsWith("1.17")||n.matches(".*(ALFACE|TOMATE|CEBOLA|BATATA|REPOLHO|LARANJA|MACA|CENOURA|MORANGA|SALSA|TEMPERO VERDE).*"))return "HORTIFRUTI"; if(cod.startsWith("3.")||n.matches(".*(DETERGENTE|SANITIZANTE|DESCARTAVEL|COPO|GUARDANAPO|GAS).*"))return "QUÍMICOS"; return "PERECÍVEL"; }
    double similar(String a,String b){ if(a.equals(b))return 1; String[] aa=a.split(" "); int hit=0; for(String x:aa) if(x.length()>2 && b.contains(x)) hit++; return aa.length==0?0:(double)hit/aa.length; }
}
