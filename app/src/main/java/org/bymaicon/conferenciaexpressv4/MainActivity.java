package org.bymaicon.conferenciaexpressv4;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import androidx.core.app.ActivityCompat;
import androidx.documentfile.provider.DocumentFile;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import java.io.*;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.*;

public class MainActivity extends Activity {
    static final int REQ_ANALITICO=10, REQ_OC=11, REQ_PASTA=12;
    Uri analiticoUri, ocUri, pastaUri;
    String analiticoText="", ocText="", modulo="TOTAL", pastaNome="Downloads/ConferenciaRancho";
    EditText dataIni, dataFim;
    TextView status, dash, pastaStatus;
    LinearLayout lista;
    DatabaseHelper db;
    ArrayList<Item> consumo=new ArrayList<>(), oc=new ArrayList<>(), resultado=new ArrayList<>();
    final int AZUL=Color.rgb(2,8,24), AZUL2=Color.rgb(0,74,173), OURO=Color.rgb(245,164,0), VERDE=Color.rgb(0,150,95);

    static class Item{
        String codigo="", nome="", un="", data="", entrega="", categoria="PERECÍVEL", status="", motivo="", ocNome="";
        double qtd=0, comprado=0, faltante=0, confianca=0;
        boolean selecionado=true;
        TreeSet<String> datas=new TreeSet<>();
    }
    static class Convertido{ double qtd; String un; Convertido(double q,String u){qtd=q;un=u;} }

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        db=new DatabaseHelper(this);
        PDFBoxResourceLoader.init(getApplicationContext());
        if(Build.VERSION.SDK_INT>=33) ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.POST_NOTIFICATIONS},50);
        montarTela(); agendarAlertas();
    }

    void montarTela(){
        ScrollView sc=new ScrollView(this);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(28,28,28,38); root.setBackgroundColor(AZUL); sc.addView(root);
        TextView t=txt("CONFERÊNCIA RANCHO",31,Color.WHITE,true); t.setGravity(Gravity.CENTER); root.addView(t);
        TextView sub=txt("EXPRESS • UNIDADE COLORADO",15,OURO,true); sub.setGravity(Gravity.CENTER); root.addView(sub);
        TextView sub2=txt("Conferência inteligente Analítico x O.C. Rancho",14,Color.LTGRAY,false); sub2.setGravity(Gravity.CENTER); root.addView(sub2);

        LinearLayout dates=new LinearLayout(this); dates.setOrientation(LinearLayout.HORIZONTAL); dates.setPadding(0,18,0,12); root.addView(dates);
        dataIni=campo("Data inicial"); dataFim=campo("Data final"); dates.addView(dataIni,new LinearLayout.LayoutParams(0,92,1)); dates.addView(dataFim,new LinearLayout.LayoutParams(0,92,1));

        Button a=btn("📄 Escolher PDF Analítico"); root.addView(a); a.setOnClickListener(v->abrir(REQ_ANALITICO));
        Button o=btn("🧾 Escolher PDF Ordem de Compra / Rancho"); root.addView(o); o.setOnClickListener(v->abrir(REQ_OC));
        Button p=btn("📁 Selecionar pasta para salvar relatórios"); root.addView(p); p.setOnClickListener(v->abrirPasta());
        pastaStatus=txt("Pasta: "+pastaNome,13,Color.LTGRAY,false); pastaStatus.setPadding(0,2,0,8); root.addView(pastaStatus);

        TextView mod=txt("Módulos de conferência",18,Color.WHITE,true); mod.setPadding(0,16,0,8); root.addView(mod);
        String[] nomes={"🥩 Conferir proteínas","🥬 Conferir hortifruti","🧪 Conferir químicos","📦 Conferir não perecíveis","🥫 Conferir perecíveis","🌐 Conferência total"};
        String[] cats={"PROTEÍNAS","HORTIFRUTI","QUÍMICOS","NÃO PERECÍVEIS","PERECÍVEL","TOTAL"};
        for(int i=0;i<nomes.length;i++){ final String c=cats[i]; Button bb=btn(nomes[i]); root.addView(bb); bb.setOnClickListener(v->{modulo=c; analisar();}); }

        Button revisar=btn("✅ Baixar relatório revisado"); root.addView(revisar); revisar.setOnClickListener(v->salvarSelecionados());
        Button novo=btn("🆕 Nova análise"); root.addView(novo); novo.setOnClickListener(v->novaAnalise());
        Button pend=btn("🔔 Ver pendências / marcar resolvido"); root.addView(pend); pend.setOnClickListener(v->mostrarPendencias());

        dash=txt("Segurança: aguardando análise",16,Color.WHITE,true); dash.setPadding(0,18,0,8); root.addView(dash);
        status=txt("Selecione o Analítico e a O.C. Rancho.",14,Color.LTGRAY,false); status.setPadding(0,8,0,8); root.addView(status);
        lista=new LinearLayout(this); lista.setOrientation(LinearLayout.VERTICAL); root.addView(lista);
        setContentView(sc);
    }

    TextView txt(String s,int sp,int cor,boolean bold){ TextView v=new TextView(this); v.setText(s); v.setTextSize(sp); v.setTextColor(cor); if(bold)v.setTypeface(Typeface.DEFAULT_BOLD); return v; }
    EditText campo(String h){ EditText e=new EditText(this); e.setHint(h); e.setTextColor(Color.WHITE); e.setHintTextColor(Color.GRAY); e.setTextSize(18); e.setSingleLine(true); return e; }
    Button btn(String s){ Button b=new Button(this); b.setText(s); b.setTextColor(Color.WHITE); b.setTextSize(16); b.setAllCaps(false); GradientDrawable gd=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{Color.rgb(0,55,130),Color.rgb(0,122,230)}); gd.setCornerRadius(34); gd.setStroke(2,Color.rgb(12,92,190)); b.setBackground(gd); b.setPadding(8,14,8,14); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,92); lp.setMargins(0,8,0,8); b.setLayoutParams(lp); return b; }

    void abrir(int req){ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("application/pdf"); i.addCategory(Intent.CATEGORY_OPENABLE); i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION); startActivityForResult(i,req); }
    void abrirPasta(){ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE); i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION|Intent.FLAG_GRANT_PREFIX_URI_PERMISSION); startActivityForResult(i,REQ_PASTA); }

    @Override protected void onActivityResult(int r,int c,Intent d){
        super.onActivityResult(r,c,d); if(c!=RESULT_OK||d==null)return;
        try{
            Uri u=d.getData(); if(u==null)return;
            if(r==REQ_PASTA){ int flags=d.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION); getContentResolver().takePersistableUriPermission(u,flags); pastaUri=u; pastaNome="Pasta escolhida OK"; pastaStatus.setText("Pasta: escolhida para salvar relatórios"); log("Pasta de saída OK."); return; }
            getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if(r==REQ_ANALITICO){analiticoUri=u; analiticoText=lerPdf(u); log("Analítico OK.");}
            if(r==REQ_OC){ocUri=u; ocText=lerPdf(u); log("O.C. Rancho OK.");}
        }catch(Exception e){erro(e);}
    }
    String lerPdf(Uri u)throws Exception{ InputStream is=getContentResolver().openInputStream(u); PDDocument doc=PDDocument.load(is); PDFTextStripper st=new PDFTextStripper(); st.setSortByPosition(true); String s=st.getText(doc); doc.close(); if(is!=null)is.close(); return s==null?"":s; }
    void log(String s){ status.setText(status.getText()+"\n"+s); }
    void erro(Exception e){ status.setText("ERRO: "+e.getMessage()); }

    void analisar(){
        try{
            if(analiticoText.isEmpty()||ocText.isEmpty()){ toast("Selecione Analítico e O.C. Rancho."); return; }
            consumo=extrairAnalitico(analiticoText); oc=extrairOC(ocText); resultado=conferir(consumo,oc);
            if(consumo.size()<10 || oc.size()<5){ salvarDebug(); throw new Exception("Extração baixa. Conferência bloqueada e debug salvo."); }
            montarRevisao(); salvarPrefs();
            dash.setText("Segurança: revisão obrigatória | Analítico: "+consumo.size()+" | O.C.: "+oc.size()+" | Faltantes: "+resultado.size());
        }catch(Exception e){erro(e);}
    }

    ArrayList<Item> extrairAnalitico(String text)throws Exception{
        ArrayList<Item> out=new ArrayList<>(); String ini=dataIni.getText().toString().trim(), fim=dataFim.getText().toString().trim();
        Date di=parseDate(ini), df=parseDate(fim); if(di==null||df==null) throw new Exception("Informe período no formato dd/mm/aaaa.");
        String dataAtual="";
        Pattern dataServico=Pattern.compile("(\\d{2}/\\d{2}/\\d{4})\\s+(segunda|terça|terca|quarta|quinta|sexta|sábado|sabado|domingo)-feira?",Pattern.CASE_INSENSITIVE);
        Pattern prod1=Pattern.compile("^(\\d+\\.\\d+\\.\\d+\\.\\d+\\.\\d+)\\s+(.+?)\\s+([0-9]{1,7},[0-9]{2,5})\\s+([0-9]{1,7},[0-9]{2,5})\\s+(KG|LT|UN|CX|PC)\\s*$");
        Pattern prod2=Pattern.compile("^(\\d+\\.\\d+\\.\\d+\\.\\d+\\.\\d+)\\s+(.+?)\\s+([0-9]{1,7},[0-9]{2,5})\\s+(KG|LT|UN|CX|PC)\\s*$");
        for(String raw:text.split("\\r?\\n")){
            String l=raw.trim().replaceAll("\\s+"," "); if(l.length()<5)continue;
            if(l.contains("Emissão")||l.contains("Limite de Entrega")||l.contains("Período de Entrega")||l.contains("Solicitação")||l.contains("Utilização")){
                Matcher md=dataServico.matcher(l); if(!md.find()) continue;
            }
            Matcher md=dataServico.matcher(l); if(md.find()){ String d=md.group(1); Date dd=parseDate(d); if(dd!=null && !dd.before(di) && !dd.after(df)) dataAtual=d.substring(0,5); continue; }
            if(dataAtual.isEmpty()) continue;
            Matcher m=prod1.matcher(l); boolean ok=m.find();
            if(ok){ Item it=new Item(); it.codigo=m.group(1); it.nome=limpaNome(m.group(2)); it.qtd=num(m.group(3)); it.un=m.group(5); it.data=dataAtual; it.categoria=categoria(it.nome,it.codigo); it.datas.add(dataAtual); out.add(it); continue; }
            Matcher m2=prod2.matcher(l); if(m2.find()){ Item it=new Item(); it.codigo=m2.group(1); it.nome=limpaNome(m2.group(2)); it.qtd=num(m2.group(3)); it.un=m2.group(4); it.data=dataAtual; it.categoria=categoria(it.nome,it.codigo); it.datas.add(dataAtual); out.add(it); }
        }
        return agregar(out,true);
    }

    ArrayList<Item> extrairOC(String text){
        ArrayList<Item> out=new ArrayList<>();
        Pattern p=Pattern.compile("^(\\d+\\.\\d+\\.\\d+\\.\\d+\\.\\d+)\\s+(.+?)\\s+(KG|LT|UN|CX|PC)\\s+([0-9]{1,7},[0-9]{1,3})\\s+(\\d{2}/\\d{2}/\\d{4})\\s+(\\d{2}/\\d{2}/\\d{4}).*$");
        for(String raw:text.split("\\r?\\n")){
            String l=raw.trim().replaceAll("\\s+"," "); Matcher m=p.matcher(l);
            if(m.find()){ Item it=new Item(); it.codigo=m.group(1); it.nome=limpaNome(m.group(2)); Convertido cv=converterEmbalagem(it.nome,num(m.group(4)),m.group(3)); it.qtd=cv.qtd; it.un=cv.un; it.entrega=m.group(5); it.data=m.group(6).substring(0,5); it.categoria=categoria(it.nome,it.codigo); out.add(it); }
        }
        return agregar(out,false);
    }

    ArrayList<Item> agregar(ArrayList<Item> in, boolean datas){
        LinkedHashMap<String,Item> map=new LinkedHashMap<>();
        for(Item it:in){ String k=norm(it.codigo)+"|"+norm(it.nome)+"|"+it.un; Item a=map.get(k); if(a==null){a=it; map.put(k,a);} else {a.qtd+=it.qtd; a.datas.addAll(it.datas); if(a.entrega.isEmpty())a.entrega=it.entrega;} }
        for(Item i:map.values()) if(datas && !i.datas.isEmpty()) i.data=join(i.datas);
        return new ArrayList<>(map.values());
    }

    ArrayList<Item> conferir(ArrayList<Item> cons,ArrayList<Item> ord){
        ArrayList<Item> res=new ArrayList<>();
        for(Item c:cons){
            if(!modulo.equals("TOTAL") && !c.categoria.equals(modulo)) continue;
            Item b=melhorOC(c,ord); c.comprado=b==null?0:b.qtd; c.entrega=b==null?entregaPadrao(ord):b.entrega; c.ocNome=b==null?"":b.nome; c.faltante=Math.max(0,c.qtd-c.comprado); c.confianca=b==null?0:confianca(c,b);
            if(c.faltante>tol(c.un)){ c.status=b==null?"INSUMO NÃO LOCALIZADO NA O.C.":"FALTA PARCIAL"; c.motivo=b==null?"insumo não localizado na oc":"Falta parcial"; c.selecionado=true; res.add(c); }
        }
        Collections.sort(res,(a,b)->{ int x=a.categoria.compareTo(b.categoria); if(x!=0)return x; return a.nome.compareTo(b.nome); });
        return res;
    }

    Item melhorOC(Item c,ArrayList<Item> ord){
        String eq=db.equivalencia(c.nome); Item melhor=null; double nota=-1;
        for(Item b:ord){ double n=0;
            if(!c.un.equals(b.un)) continue;
            if(c.codigo.equals(b.codigo)) n=1.0;
            else if(!eq.isEmpty() && norm(eq).equals(norm(b.nome))) n=.99;
            else if(norm(c.nome).equals(norm(b.nome))) n=.98;
            else n=scoreSeguro(norm(c.nome),norm(b.nome));
            if(n>nota){ nota=n; melhor=b; }
        }
        return nota>=.88?melhor:null;
    }
    double confianca(Item a,Item b){ if(a.codigo.equals(b.codigo))return 1; if(norm(a.nome).equals(norm(b.nome)))return .98; return Math.max(.50,scoreSeguro(norm(a.nome),norm(b.nome))); }

    double scoreSeguro(String a,String b){
        if(a.equals(b)) return 1;
        String[] aa=a.split(" "), bb=b.split(" "); LinkedHashSet<String> A=new LinkedHashSet<>(), B=new LinkedHashSet<>();
        for(String x:aa) if(x.length()>2) A.add(x); for(String x:bb) if(x.length()>2) B.add(x);
        if(A.isEmpty()||B.isEmpty())return 0; int hit=0; for(String x:A) if(B.contains(x)) hit++;
        double recall=(double)hit/A.size(); double precision=(double)hit/B.size(); double dice=(2*precision*recall)/(precision+recall+0.0001);
        if(A.size()<=2 && !B.containsAll(A)) return Math.min(.50,dice);
        return dice;
    }

    void montarRevisao(){
        lista.removeAllViews(); int n=1; for(Item c:resultado){ LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(14,14,14,14); card.setBackgroundColor(Color.rgb(3,22,55)); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,10,0,10); lista.addView(card,lp);
            CheckBox cb=new CheckBox(this); cb.setTextColor(Color.WHITE); cb.setChecked(c.selecionado); int pos=n++; cb.setText(pos+". ["+c.categoria+"] "+c.nome+"\nNecessário: "+fmt(c.qtd)+" "+c.un+" | O.C.: "+fmt(c.comprado)+" "+c.un+" | Falta: "+fmt(c.faltante)+" "+c.un+"\nConsumo: "+c.data+" | Entrega: "+c.entrega+" | Confiança: "+Math.round(c.confianca*100)+"%\n"+c.motivo); cb.setOnCheckedChangeListener((x,on)->c.selecionado=on); card.addView(cb);
            Button vinc=btn("🔁 Tornar igual item encontrado na O.C."); card.addView(vinc); vinc.setOnClickListener(v->vincular(c));
            Button cor=btn("✏️ Corrigir quantidade O.C."); card.addView(cor); cor.setOnClickListener(v->corrigirQtd(c));
            Button motivo=btn("🔎 Ver motivo da classificação"); card.addView(motivo); motivo.setOnClickListener(v->dialog("Motivo",c.motivo+"\nItem O.C.: "+c.ocNome));
        }
    }
    void vincular(Item c){ final EditText e=new EditText(this); e.setHint("Digite exatamente o nome do item encontrado na O.C."); new AlertDialog.Builder(this).setTitle("Tornar igual item da O.C.").setMessage("Item do analítico:\n"+c.nome+"\n\nA IA salvará esta equivalência para as próximas análises.").setView(e).setPositiveButton("Salvar",(d,w)->{String oc=e.getText().toString().trim(); if(!oc.isEmpty()){db.salvarEquivalencia(c.nome,oc.toUpperCase(Locale.ROOT)); toast("Equivalência salva. Rode a conferência novamente para recalcular.");}}).setNegativeButton("Cancelar",null).show(); }
    void corrigirQtd(Item c){ final EditText e=new EditText(this); e.setHint("Quantidade entregue"); new AlertDialog.Builder(this).setTitle("Corrigir quantidade").setView(e).setPositiveButton("Aplicar",(d,w)->{try{c.comprado=num(e.getText().toString());c.faltante=Math.max(0,c.qtd-c.comprado);c.motivo=c.faltante>tol(c.un)?"Falta parcial":"OK após correção";montarRevisao();}catch(Exception ex){erro(ex);}}).setNegativeButton("Cancelar",null).show(); }

    void salvarSelecionados(){ try{ ArrayList<Item> rows=new ArrayList<>(); for(Item i:resultado) if(i.selecionado) rows.add(i); if(rows.isEmpty()){toast("Nenhum item selecionado.");return;} gerarPdf(rows); for(Item i:rows) db.salvarPendencia(i.nome,i.categoria,i.faltante,i.un,i.data,i.entrega); toast("Relatório salvo e pendências registradas sem duplicar itens repetidos."); }catch(Exception e){erro(e);} }

    void gerarPdf(ArrayList<Item> rows)throws Exception{
        PdfDocument doc=new PdfDocument(); Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); int W=1684,H=1190; int pageNo=1;
        PdfDocument.Page page=doc.startPage(new PdfDocument.PageInfo.Builder(W,H,pageNo).create()); Canvas c=page.getCanvas(); int y=desenharTopo(c,p,W);
        y=desenharCabecalho(c,p,y,W); String cat=""; int n=1;
        for(Item r:rows){
            if(!cat.equals(r.categoria)){ int hcat=40; if(y+hcat+80>1040){doc.finishPage(page); pageNo++; page=doc.startPage(new PdfDocument.PageInfo.Builder(W,H,pageNo).create()); c=page.getCanvas(); y=desenharTopo(c,p,W); y=desenharCabecalho(c,p,y,W);} cat=r.categoria; p.setColor(AZUL); c.drawRect(50,y,W-50,y+hcat,p); p.setColor(OURO); p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(22); c.drawText("TIPO: "+cat,70,y+27,p); y+=hcat; }
            int rowH=alturaLinha(r); if(y+rowH>1060){doc.finishPage(page); pageNo++; page=doc.startPage(new PdfDocument.PageInfo.Builder(W,H,pageNo).create()); c=page.getCanvas(); y=desenharTopo(c,p,W); y=desenharCabecalho(c,p,y,W); p.setColor(AZUL); c.drawRect(50,y,W-50,y+40,p); p.setColor(OURO); p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(22); c.drawText("TIPO: "+cat,70,y+27,p); y+=40; }
            desenharLinha(c,p,n++,r,y,rowH); y+=rowH;
        }
        p.setColor(Color.DKGRAY); p.setTextSize(24); p.setTypeface(Typeface.DEFAULT_BOLD); c.drawText(new SimpleDateFormat("dd/MM/yyyy",Locale.getDefault()).format(new Date())+" - By Maicon",1220,1140,p); doc.finishPage(page);
        String nome="conferencia_rancho_previsao_faltantes_"+entregaPadrao(rows).replace('/','-')+".pdf"; OutputStream os=saida(nome,"application/pdf"); doc.writeTo(os); os.close(); doc.close();
    }
    int desenharTopo(Canvas c,Paint p,int W){ c.drawColor(Color.WHITE); p.setColor(AZUL); c.drawRect(0,0,W,220,p); p.setColor(OURO); c.drawRect(0,220,W,228,p); p.setColor(Color.rgb(0,94,210)); p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(30); c.drawText("EXPRESS-UNIDADE COLORADO",80,95,p); p.setColor(Color.WHITE); p.setTextSize(44); c.drawText("RELATÓRIO PREVISÃO DE FALTANTES",520,95,p); p.setTextSize(18); c.drawText("CONFERÊNCIA REALIZADA COM BASE ANALITICO X O.C.",80,145,p); p.setColor(OURO); p.setTextSize(28); c.drawText("ENTREGA PREVISTA: "+entregaPadrao(resultado),90,305,p); return 365; }
    int desenharCabecalho(Canvas c,Paint p,int y,int W){ p.setColor(Color.rgb(220,235,248)); c.drawRect(50,y,W-50,y+55,p); p.setColor(Color.BLACK); p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(18); c.drawText("Nº",70,y+34,p); c.drawText("ITEM",165,y+34,p); c.drawText("NECESSÁRIO",500,y+34,p); c.drawText("ENTREGA",675,y+34,p); c.drawText("FALTANTE",850,y+34,p); c.drawText("CONSUMO",1025,y+34,p); c.drawText("DATA ENTREGA",1165,y+34,p); c.drawText("OBSERVAÇÕES",1350,y+34,p); return y+55; }
    int alturaLinha(Item r){ int li=Math.max(wrap(r.nome,30).size(),wrap(r.motivo,24).size()); return Math.max(54,li*22+22); }
    void desenharLinha(Canvas c,Paint p,int n,Item r,int y,int h){ p.setStyle(Paint.Style.FILL); p.setColor(Color.rgb(250,250,250)); c.drawRect(50,y,1634,y+h,p); p.setColor(Color.BLACK); p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(18); c.drawText(String.valueOf(n),75,y+30,p); drawWrapped(c,p,r.nome,135,y+26,330,30,22,Color.BLACK); c.drawText(fmt(r.qtd)+" "+r.un,500,y+30,p); c.drawText(fmt(r.comprado)+" "+r.un,675,y+30,p); p.setColor(Color.RED); c.drawText(fmt(r.faltante)+" "+r.un,850,y+30,p); p.setColor(Color.BLACK); c.drawText(r.data,1025,y+30,p); c.drawText(r.entrega,1165,y+30,p); drawWrapped(c,p,r.motivo,1350,y+26,255,24,22,Color.BLACK); }
    void drawWrapped(Canvas c,Paint p,String text,int x,int y,int w,int chars,int lh,int color){ p.setColor(color); p.setTextSize(18); int yy=y; for(String s:wrap(text,chars)){ c.drawText(s,x,yy,p); yy+=lh; } }
    ArrayList<String> wrap(String s,int max){ ArrayList<String> out=new ArrayList<>(); String[] parts=(s==null?"":s).split(" "); String line=""; for(String part:parts){ if((line+" "+part).trim().length()>max){ if(!line.isEmpty())out.add(line); line=part; } else line=(line+" "+part).trim(); } if(!line.isEmpty())out.add(line); if(out.isEmpty())out.add(""); return out; }

    OutputStream saida(String nome,String mime)throws Exception{
        if(pastaUri!=null){ try{ DocumentFile dir=DocumentFile.fromTreeUri(this,pastaUri); if(dir!=null && dir.canWrite()){ DocumentFile old=dir.findFile(nome); if(old!=null)old.delete(); DocumentFile f=dir.createFile(mime,nome); if(f!=null){ OutputStream os=getContentResolver().openOutputStream(f.getUri()); if(os!=null)return os; } } }catch(Exception ignored){} }
        if(Build.VERSION.SDK_INT>=29){ android.content.ContentValues v=new android.content.ContentValues(); v.put(MediaStore.MediaColumns.DISPLAY_NAME,nome); v.put(MediaStore.MediaColumns.MIME_TYPE,mime); v.put(MediaStore.MediaColumns.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/ConferenciaRancho"); Uri u=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,v); return getContentResolver().openOutputStream(u); }
        File dir=new File(Environment.getExternalStorageDirectory(),"Download/ConferenciaRancho"); dir.mkdirs(); return new FileOutputStream(new File(dir,nome));
    }

    void salvarDebug(){ try{ OutputStream os=saida("debug_conferencia_rancho.txt","text/plain"); os.write(("ANALITICO\n"+analiticoText+"\n\nOC\n"+ocText).getBytes("UTF-8")); os.close(); }catch(Exception ignored){} }
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
    String join(TreeSet<String> s){ StringBuilder b=new StringBuilder(); for(String x:s){ if(b.length()>0)b.append(", "); b.append(x);} return b.toString(); }
    String joinList(ArrayList<String> a){ StringBuilder b=new StringBuilder(); int n=1; for(String x:a)b.append(n++).append(". ").append(x).append("\n"); return b.toString(); }
    String entregaPadrao(Collection<Item> xs){ for(Item i:xs) if(i.entrega!=null&&!i.entrega.isEmpty()) return i.entrega; return ""; }
    Convertido converterEmbalagem(String nome,double q,String un){ String n=norm(nome); Matcher m=Pattern.compile("(\\d+) KG X (CX|PC)").matcher(n); if((un.equals("CX")||un.equals("PC"))&&m.find()) return new Convertido(q*Double.parseDouble(m.group(1)),"KG"); Matcher u=Pattern.compile("(\\d+) UN X (\\d+)").matcher(n); if(un.equals("CX")&&u.find()) return new Convertido(q*Double.parseDouble(u.group(1))*Double.parseDouble(u.group(2)),"UN"); return new Convertido(q,un); }
    String categoria(String nome,String cod){ String n=norm(nome); if(cod.startsWith("1.01")||n.matches(".*(CARNE|FRANGO|BOVINA|SUINA|LINGUICA|SALSICHA|BACON|HAMBURGUER|PEIXE|SASSAMI|PANCETA).*"))return "PROTEÍNAS"; if(cod.startsWith("1.17")||n.matches(".*(ALFACE|TOMATE|CEBOLA|BATATA|REPOLHO|LARANJA|MACA|CENOURA|MORANGA|SALSA|TEMPERO VERDE|HORTIFRUTI).*"))return "HORTIFRUTI"; if(n.matches(".*(DETERGENTE|SANITIZANTE|DESINFETANTE|HIPOCLORITO|ALVEJANTE|SABAO|ESPONJA|LIMPEZA|QUIMICO).*"))return "QUÍMICOS"; if(cod.startsWith("3.")||cod.startsWith("1.04")||cod.startsWith("1.05")||cod.startsWith("1.08")||cod.startsWith("1.09")||cod.startsWith("1.12")||n.matches(".*(ARROZ|FEIJAO|FARINHA|OLEO|SAL |ACUCAR|SUCO|GELATINA|VINAGRE|MOLHO|COPO|GUARDANAPO|EMB|MARMITEX|DESCARTAVEL|GAS|SACHET).*"))return "NÃO PERECÍVEIS"; return "PERECÍVEL"; }
}
