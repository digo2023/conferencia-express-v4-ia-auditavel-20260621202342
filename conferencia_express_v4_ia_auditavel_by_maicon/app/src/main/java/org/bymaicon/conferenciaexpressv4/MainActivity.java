package org.bymaicon.conferenciaexpressv4;

import android.Manifest;
import android.app.*;import android.os.*;import android.content.*;import android.content.ContentValues;import android.content.pm.PackageManager;import android.net.*;import android.provider.DocumentsContract;import android.provider.MediaStore;import android.graphics.*;import android.graphics.pdf.PdfDocument;import android.view.*;import android.widget.*;import android.text.method.ScrollingMovementMethod;import android.graphics.drawable.*;import android.content.res.ColorStateList;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;import com.tom_roush.pdfbox.pdmodel.PDDocument;import com.tom_roush.pdfbox.text.PDFTextStripper;
import java.io.*;import java.text.*;import java.util.*;import java.util.regex.*;import java.util.zip.*;

public class MainActivity extends Activity{
    static final int REQ_ANALITICO=10, REQ_OC=11, REQ_FOLDER=12;
    Uri analiticoUri, ocUri, folderUri;
    String analiticoText="", ocText="", saveInfo="";
    EditText dataIni,dataFim; TextView status,log,dashboard; LinearLayout reviewBox;
    DatabaseHelper db;
    ArrayList<Item> itensConsumo=new ArrayList<>(), itensOC=new ArrayList<>();
    ArrayList<CandidatoIA> candidatos=new ArrayList<>();
    int ultimoAnalisados=0, ultimoCandidatos=0, ultimoSelecionados=0, ultimoOK=0; String ultimaEntrega="", ultimoRisco="BAIXO";
    String modoAnaliseAtual="CONFERÊNCIA TOTAL", filtroCategoriaAtual="";
    String nivelIA="SEGURA"; // RÁPIDA, SEGURA ou AUDITORIA
    int azul=Color.rgb(0,58,140), azul2=Color.rgb(0,170,220), dourado=Color.rgb(255,183,36), fundo=Color.rgb(1,7,20), card=Color.rgb(3,24,58);
    SimpleDateFormat br=new SimpleDateFormat("dd/MM/yyyy",new Locale("pt","BR"));
    SimpleDateFormat arquivoDt=new SimpleDateFormat("dd-MM-yyyy",new Locale("pt","BR"));

    public void onCreate(Bundle b){super.onCreate(b);PDFBoxResourceLoader.init(getApplicationContext());db=new DatabaseHelper(this);criarCanalNotificacao();pedirPermissaoNotificacao();montarTela();carregarPrefs();programarNotificacoesDiarias();}

    TextView tv(String s,int sp,int color,int style){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(color);v.setTypeface(Typeface.DEFAULT,style);v.setPadding(14,7,14,7);return v;}
    Button btn(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(15);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setAllCaps(false);GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{Color.rgb(0,55,145),Color.rgb(0,155,230),Color.rgb(0,215,190)});g.setStroke(2,Color.rgb(255,183,36));g.setCornerRadius(44);b.setBackground(g);b.setPadding(22,18,22,18);b.setMinHeight(70);if(Build.VERSION.SDK_INT>=21)b.setElevation(10);return b;}
    GradientDrawable bgCard(){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{Color.rgb(2,18,50),Color.rgb(5,46,92),Color.rgb(2,24,62)});g.setStroke(2,Color.rgb(255,183,36));g.setCornerRadius(28);return g;}

    public void montarTela(){
        ScrollView sv=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(18,18,18,18);root.setBackgroundColor(fundo);sv.addView(root);
        ImageView logo=new ImageView(this);logo.setImageResource(getResources().getIdentifier("icon","drawable",getPackageName()));logo.setAdjustViewBounds(true);root.addView(logo,new LinearLayout.LayoutParams(-1,220));
        TextView title=tv("CONFERÊNCIA EXPRESS IA",34,Color.WHITE,Typeface.BOLD);title.setGravity(Gravity.CENTER);root.addView(title);
        TextView sub=tv("IA Auditável • Equivalências aprendidas • Revisão inteligente",14,dourado,Typeface.BOLD);sub.setGravity(Gravity.CENTER);root.addView(sub);
        TextView aviso=tv("Central de controle para a Express-Unidade Colorado: cruza Analítico x O.C., aprende equivalências, converte embalagens, alerta pendências e exige revisão manual antes do relatório final.",12,Color.LTGRAY,Typeface.NORMAL);aviso.setGravity(Gravity.CENTER);root.addView(aviso);

        LinearLayout datas=new LinearLayout(this);datas.setOrientation(LinearLayout.HORIZONTAL);datas.setPadding(0,8,0,8);root.addView(datas);
        dataIni=new EditText(this);dataFim=new EditText(this);
        for(EditText e:new EditText[]{dataIni,dataFim}){e.setTextColor(Color.WHITE);e.setHintTextColor(Color.LTGRAY);e.setSingleLine(true);e.setTextSize(15);e.setBackgroundTintList(ColorStateList.valueOf(dourado));}
        dataIni.setHint("Início dd/mm/aaaa");dataFim.setHint("Fim dd/mm/aaaa");datas.addView(dataIni,new LinearLayout.LayoutParams(0,-2,1));datas.addView(dataFim,new LinearLayout.LayoutParams(0,-2,1));

        Button b1=btn("📄 Escolher PDF Analítico / Requisição");root.addView(b1);b1.setOnClickListener(v->abrirArquivo(REQ_ANALITICO));
        Button b2=btn("🧾 Escolher PDF Ordem de Compra / O.C.");root.addView(b2);b2.setOnClickListener(v->abrirArquivo(REQ_OC));
        Button b3=btn("📁 Escolher pasta segura para salvar relatórios");root.addView(b3);b3.setOnClickListener(v->abrirPasta());
        Button bm=btn("🧠 Modo IA: SEGURA");root.addView(bm);bm.setOnClickListener(v->{cicloModoIA();bm.setText("🧠 Modo IA: "+nivelIA);});
        Button b4p=btn("🥩 Conferir proteínas");root.addView(b4p);b4p.setOnClickListener(v->analisarIA("CONFERÊNCIA DE PROTEÍNAS","PROTEÍNAS"));
        Button b4pe=btn("📦 Conferir perecíveis");root.addView(b4pe);b4pe.setOnClickListener(v->analisarIA("CONFERÊNCIA DE PERECÍVEIS","PERECÍVEL"));
        Button b4h=btn("🥬 Conferir hortifruti");root.addView(b4h);b4h.setOnClickListener(v->analisarIA("CONFERÊNCIA DE HORTIFRUTI","HORTIFRUTI"));
        Button b4q=btn("🧪 Conferir químicos");root.addView(b4q);b4q.setOnClickListener(v->analisarIA("CONFERÊNCIA DE QUÍMICOS","QUÍMICOS"));
        Button b4t=btn("🌐 Conferência total");root.addView(b4t);b4t.setOnClickListener(v->analisarIA("CONFERÊNCIA TOTAL",""));
        Button b5=btn("⬇️ Baixar relatório conferido e revisado");root.addView(b5);b5.setOnClickListener(v->baixarRelatorioRevisado());
        Button b6=btn("📋 Resumo executivo / WhatsApp");root.addView(b6);b6.setOnClickListener(v->mostrarResumoIA());
        Button b7=btn("📁 Histórico e auditoria");root.addView(b7);b7.setOnClickListener(v->mostrarHistorico());
        Button b7e=btn("🧩 Equivalências aprendidas");root.addView(b7e);b7e.setOnClickListener(v->mostrarEquivalencias());
        Button b8=btn("🔔 Ativar alertas preventivos 06:00 e 12:00");root.addView(b8);b8.setOnClickListener(v->{programarNotificacoesDiarias();Toast.makeText(this,"Alertas Conferência Express ativados",Toast.LENGTH_LONG).show();append("Alertas diários 06:00 e 12:00 ativados.");});
        Button b9=btn("✅ Informar itens resolvidos ou pendentes");root.addView(b9);b9.setOnClickListener(v->abrirPendenciasDialog());
        Button b10=btn("🆕 Nova conferência");root.addView(b10);b10.setOnClickListener(v->novaAnalise());

        dashboard=tv("🤖 Conferência Express aguardando análise.",14,Color.WHITE,Typeface.BOLD);dashboard.setBackground(bgCard());root.addView(dashboard,new LinearLayout.LayoutParams(-1,-2));
        TextView revTitle=tv("🔎 Revisão manual obrigatória — selecione somente os itens confirmados para o relatório final",16,dourado,Typeface.BOLD);root.addView(revTitle);
        reviewBox=new LinearLayout(this);reviewBox.setOrientation(LinearLayout.VERTICAL);reviewBox.setPadding(0,4,0,4);root.addView(reviewBox);
        status=tv("Pronto para conferência. Selecione Analítico, O.C., pasta de saída e escolha o módulo de auditoria.",15,Color.WHITE,Typeface.BOLD);root.addView(status);
        log=tv("",12,Color.LTGRAY,Typeface.NORMAL);log.setMovementMethod(new ScrollingMovementMethod());root.addView(log);
        TextView rod=tv("Conferência Express V4 • IA Auditável • By Maicon",12,Color.LTGRAY,Typeface.NORMAL);rod.setGravity(Gravity.RIGHT);root.addView(rod);
        setContentView(sv);
    }

    void abrirArquivo(int req){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/pdf");startActivityForResult(i,req);} 
    void abrirPasta(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);startActivityForResult(i,REQ_FOLDER);} 
    protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(c!=RESULT_OK||d==null)return;try{Uri u=d.getData();if(r==REQ_ANALITICO){analiticoUri=u;analiticoText=lerPdf(u);status.setText("Analítico carregado.");append("Analítico OK: "+u);sugerirDatas();}else if(r==REQ_OC){ocUri=u;ocText=lerPdf(u);status.setText("OC carregada.");append("OC OK: "+u);}else if(r==REQ_FOLDER){folderUri=u;try{getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);}catch(Exception ignored){}append("Pasta de saída OK: "+u);}}catch(Exception e){erro(e);}}

    String lerPdf(Uri uri)throws Exception{InputStream in=getContentResolver().openInputStream(uri);PDDocument doc=PDDocument.load(in);PDFTextStripper st=new PDFTextStripper();st.setSortByPosition(true);st.setShouldSeparateByBeads(false);st.setLineSeparator("\n");st.setWordSeparator(" ");String t=st.getText(doc);doc.close();in.close();return t==null?"":t;}
    void sugerirDatas(){ArrayList<String> ds=new ArrayList<>();Matcher m=Pattern.compile("(\\d{2}/\\d{2}/\\d{4})\\s+(segunda|ter[cç]a|quarta|quinta|sexta|s[áa]bado|domingo)",Pattern.CASE_INSENSITIVE).matcher(analiticoText);while(m.find())if(!ds.contains(m.group(1)))ds.add(m.group(1));if(ds.isEmpty()){for(String raw:analiticoText.split("\\n")){String line=raw.trim().replaceAll("\\s+"," ");String up=line.toUpperCase(Locale.ROOT);if(up.contains("EMISSÃO")||up.contains("EMISSAO")||up.contains("SOLICITAÇÃO")||up.contains("SOLICITACAO")||up.contains("PERÍODO DE ENTREGA")||up.contains("PERIODO DE ENTREGA")||up.contains("LIMITE DE ENTREGA"))continue;Matcher dm=Pattern.compile("(\\d{2}/\\d{2}/\\d{4})").matcher(line);if(dm.find()&&!ds.contains(dm.group(1)))ds.add(dm.group(1));}}if(ds.size()>0){dataIni.setText(ds.get(0));dataFim.setText(ds.get(ds.size()-1));}}

    void analisarIA(String modo,String filtro){
        modoAnaliseAtual=modo==null||modo.isEmpty()?"CONFERÊNCIA TOTAL":modo;
        filtroCategoriaAtual=filtro==null?"":filtro;
        new Thread(()->{
            try{
                runOnUiThread(()->{status.setText("Conferência Express analisando: "+modoAnaliseAtual+"...");append("Auditoria inteligente iniciou "+modoAnaliseAtual+" — Analítico x O.C. item por item.");reviewBox.removeAllViews();});
                validarBase();saveInfo="";
                itensConsumo=extrairAnalitico(analiticoText);
                if(itensConsumo.size()<20){salvarDebugTexto();throw new Exception("Poucos registros extraídos do analítico. Foi salvo debug_texto_extraido_conferencia_express.txt para auditoria.");}
                itensOC=extrairOC(ocText);
                if(itensOC.size()<3){salvarDebugTextoOC();throw new Exception("Poucos itens extraídos da O.C. Foi salvo debug_texto_extraido_oc_conferencia_express.txt para auditoria.");}
                Map<String,Item> cons=consolidar(itensConsumo), oc=consolidarOC(itensOC);
                ArrayList<CandidatoIA> todos=gerarCandidatosIA(cons,oc);
                candidatos=filtrarPorModulo(todos,filtroCategoriaAtual);
                ultimoAnalisados=contarConsumoPorModulo(cons,filtroCategoriaAtual);
                ultimoCandidatos=candidatos.size();
                ultimoOK=Math.max(0,ultimoAnalisados-ultimoCandidatos);
                ultimaEntrega=menorEntregaOC(itensOC);ultimoRisco=riscoIA(candidatos);salvarPrefs();
                db.inserirEvento("IA_ANALISE",modoAnaliseAtual,"Analisados no módulo: "+ultimoAnalisados+" | Faltantes: "+ultimoCandidatos+" | Risco: "+ultimoRisco+" | Alertas 24h/48h/72h atualizados.");
                runOnUiThread(()->{status.setText(modoAnaliseAtual+" finalizada. Revise e selecione o que entrará no relatório final.");montarListaRevisao();atualizarDashboard();append("Registros analítico: "+itensConsumo.size()+" | Itens analisados no módulo: "+ultimoAnalisados+" | Conferência Express: "+candidatos.size());});
            }catch(Exception e){runOnUiThread(()->erro(e));}
        }).start();
    }


    void baixarRelatorioRevisado(){
        new Thread(()->{try{ArrayList<CandidatoIA> sel=selecionados();if(sel.isEmpty())throw new Exception("Nenhum faltante selecionado para o relatório final.");saveInfo="";String entrega=ultimaEntrega==null||ultimaEntrega.isEmpty()?menorEntregaOC(itensOC):ultimaEntrega;String base="conferencia_express_v4_ia_auditavel_entrega_"+dataArquivo(entrega);salvarPdfRevisado(sel,base+".pdf",entrega);salvarXlsxRevisado(sel,base+".xlsx");salvarTxtRevisado(sel,"resumo_"+base+"_whatsapp.txt");for(CandidatoIA c:candidatos){db.aprender(c.chave(),c.nome,c.selecionado,c.faltante,entrega);if(c.selecionado)db.salvarFaltantePendente(c.chave(),c.nome,c.un,c.faltante,c.necessario,entrega,c.datasConsumo,c.categoria);}ultimoSelecionados=sel.size();ultimoCandidatos=candidatos.size();ultimoRisco=riscoIA(sel);salvarPrefs();db.inserirHistorico(dataIni.getText().toString(),dataFim.getText().toString(),entrega,ultimoAnalisados,ultimoCandidatos,ultimoSelecionados,ultimoOK,ultimoRisco);db.inserirEvento("RELATORIO_PREVISAO_FALTANTES","Relatório previsão de faltantes gerado",base+".pdf | Itens: "+sel.size()+" | Pendências salvas para alertas 24h/48h/72h");runOnUiThread(()->{status.setText("Relatório revisado baixado. A IA salvou as pendências para notificações.");atualizarDashboard();append("Relatório previsão de faltantes salvo: "+base+".pdf");append("Conferência Express: quando resolver algum item, toque em Informar itens resolvidos ou pendentes.");if(!saveInfo.isEmpty())append(saveInfo);});}catch(Exception e){runOnUiThread(()->erro(e));}}).start();
    }
    void montarListaRevisao(){
        reviewBox.removeAllViews();
        if(candidatos.isEmpty()){
            reviewBox.addView(tv("✅ A conferência analisou todos os itens e não encontrou faltantes no modo atual.",14,Color.WHITE,Typeface.BOLD));
            return;
        }
        String cat="";int pos=1;
        for(CandidatoIA c:candidatos){
            if(!c.categoria.equals(cat)){
                cat=c.categoria;TextView sec=tv("▸ "+cat,15,dourado,Typeface.BOLD);sec.setBackground(bgCard());reviewBox.addView(sec,new LinearLayout.LayoutParams(-1,-2));
            }
            LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(16,14,16,14);box.setBackground(bgCard());LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,8,0,8);reviewBox.addView(box,lp);
            CheckBox cb=new CheckBox(this);
            cb.setText(pos+". ["+c.categoria+"] "+c.nome+"\nStatus: "+c.status+" | Confiança IA: "+Math.round(c.confianca*100)+"% | Modo: "+nivelIA+"\nNecessário: "+fmt(c.necessario)+" "+c.un+" | O.C.: "+fmt(c.comprado)+" "+c.un+" | Faltante: "+fmt(c.faltante)+" "+c.un+"\nConsumo: "+c.consumo+" | Entrega O.C.: "+c.entrega+"\nMotivo: "+c.motivo+"\nItem O.C. vinculado: "+(c.itemOCNome==null||c.itemOCNome.isEmpty()?"não vinculado":c.itemOCNome));
            cb.setTextColor(Color.WHITE);cb.setTextSize(13);cb.setTypeface(Typeface.DEFAULT,Typeface.BOLD);cb.setButtonTintList(ColorStateList.valueOf(dourado));cb.setChecked(c.selecionado);cb.setOnCheckedChangeListener((buttonView,isChecked)->{c.selecionado=isChecked;atualizarDashboard();});box.addView(cb);
            LinearLayout acoes=new LinearLayout(this);acoes.setOrientation(LinearLayout.VERTICAL);box.addView(acoes);
            Button vinc=btn("🔁 Vincular com item da O.C.");acoes.addView(vinc);vinc.setOnClickListener(v->vincularItemOC(c));
            Button qtd=btn("✏️ Corrigir quantidade entregue");acoes.addView(qtd);qtd.setOnClickListener(v->corrigirQuantidade(c));
            Button motivo=btn("ℹ️ Ver motivo do faltante");acoes.addView(motivo);motivo.setOnClickListener(v->mostrarMotivo(c));
            Button rem=btn("🚫 Remover do relatório final");acoes.addView(rem);rem.setOnClickListener(v->{c.selecionado=false;montarListaRevisao();atualizarDashboard();append("Item removido da seleção final: "+c.nome);});
            pos++;
        }
    }

    void cicloModoIA(){
        if("RÁPIDA".equals(nivelIA))nivelIA="SEGURA"; else if("SEGURA".equals(nivelIA))nivelIA="AUDITORIA"; else nivelIA="RÁPIDA";
        append("Modo IA alterado para: "+nivelIA+". RÁPIDA prioriza faltas críticas; SEGURA exige revisão de todos os faltantes; AUDITORIA aumenta o rigor e manda dúvidas para revisão.");
        salvarPrefs();atualizarDashboard();
    }
    boolean selecionarAutomaticamente(CandidatoIA c){
        if(c==null)return false;
        if("RÁPIDA".equals(nivelIA))return c.confianca>=0.88 && c.faltante>=toleranciaUn(c.un)*2;
        if("AUDITORIA".equals(nivelIA))return c.confianca>=0.50;
        return c.confianca>=0.60 || c.faltante>=toleranciaUn(c.un)*2;
    }
    double toleranciaUn(String un){return "UN".equals(un)?0.5:0.050;}
    void vincularItemOC(CandidatoIA cand){
        try{
            if(cand==null)return;
            ArrayList<Item> poss=listarPossiveisOC(cand,12);
            ArrayList<String> labels=new ArrayList<>();
            for(Item it:poss)labels.add(it.nome+" | "+fmt(it.qtd)+" "+it.un+" | entrega "+it.entrega+" | utilização "+it.data+" | confiança "+Math.round(similaridade(cand.nome,it.nome)*100)+"%");
            labels.add("✏️ Digitar nome manualmente");
            String[] arr=labels.toArray(new String[0]);
            new AlertDialog.Builder(this).setTitle("Vincular item do analítico com item da O.C.")
                    .setItems(arr,(dialog,which)->{
                        if(which<poss.size())aplicarVinculoOC(cand,poss.get(which),true);
                        else digitarVinculoManual(cand);
                    }).setNegativeButton("Cancelar",null).show();
        }catch(Exception e){erro(e);} 
    }
    ArrayList<Item> listarPossiveisOC(CandidatoIA cand,int limite){
        ArrayList<Item> out=new ArrayList<>();
        for(Item it:itensOC){if(!cand.un.equals(it.un))continue;out.add(it.copy());}
        Collections.sort(out,(a,b)->Double.compare(similaridade(cand.nome,b.nome),similaridade(cand.nome,a.nome)));
        if(out.size()>limite)return new ArrayList<Item>(out.subList(0,limite));
        return out;
    }
    double similaridade(String a,String b){
        HashSet<String> ta=new HashSet<>(Arrays.asList(norm(a).split(" ")));HashSet<String> tb=new HashSet<>(Arrays.asList(norm(b).split(" ")));
        ta.remove("");tb.remove("");if(ta.isEmpty()||tb.isEmpty())return 0;
        int inter=0;for(String x:ta)if(tb.contains(x))inter++;
        int uni=ta.size()+tb.size()-inter;double j=uni==0?0:(inter*1.0/uni);
        if(norm(a).equals(norm(b)))j=1.0;return j;
    }
    void aplicarVinculoOC(CandidatoIA c,Item ocItem,boolean salvarRegra){
        if(c==null||ocItem==null)return;
        c.comprado=ocItem.qtd;c.entrega=ocItem.entrega;c.itemOCNome=ocItem.nome;c.chaveOC=key(ocItem);
        c.faltante=Math.max(0,c.necessario-c.comprado);c.confianca=Math.max(c.confianca,0.95);
        if(c.faltante<=toleranciaUn(c.un)){c.status="OK APÓS EQUIVALÊNCIA";c.motivo="Item equivalente encontrado na O.C.";c.selecionado=false;}
        else{c.status="FALTA PARCIAL APÓS EQUIVALÊNCIA";c.motivo="Falta parcial";c.selecionado=true;}
        if(salvarRegra)db.salvarEquivalencia(c.chave(),key(ocItem),c.nome,ocItem.nome,0.98);
        db.inserirEvento("EQUIVALENCIA","Equivalência confirmada",c.nome+" = "+ocItem.nome);
        montarListaRevisao();atualizarDashboard();append("Equivalência aplicada: "+c.nome+" = "+ocItem.nome);
    }
    void digitarVinculoManual(CandidatoIA c){
        final EditText input=new EditText(this);input.setHint("Digite parte do nome do item na O.C.");input.setTextColor(Color.WHITE);input.setHintTextColor(Color.LTGRAY);
        new AlertDialog.Builder(this).setTitle("Buscar item na O.C.").setView(input).setPositiveButton("Buscar",(d,w)->{
            String q=norm(input.getText().toString());ArrayList<Item> found=new ArrayList<>();for(Item it:itensOC)if(c.un.equals(it.un)&&norm(it.nome).contains(q))found.add(it);
            if(found.isEmpty()){Toast.makeText(this,"Nenhum item encontrado na O.C.",Toast.LENGTH_LONG).show();return;}
            String[] arr=new String[found.size()];for(int i=0;i<found.size();i++)arr[i]=found.get(i).nome+" | "+fmt(found.get(i).qtd)+" "+found.get(i).un+" | "+found.get(i).entrega;
            new AlertDialog.Builder(this).setTitle("Selecione o item correto").setItems(arr,(di,which)->aplicarVinculoOC(c,found.get(which),true)).show();
        }).setNegativeButton("Cancelar",null).show();
    }
    void corrigirQuantidade(CandidatoIA c){
        final EditText input=new EditText(this);input.setHint("Quantidade entregue na O.C. em "+c.un);input.setTextColor(Color.WHITE);input.setHintTextColor(Color.LTGRAY);input.setSingleLine(true);
        new AlertDialog.Builder(this).setTitle("Corrigir quantidade entregue").setMessage(c.nome+"\nNecessário: "+fmt(c.necessario)+" "+c.un).setView(input).setPositiveButton("Aplicar",(d,w)->{
            try{double q=num(input.getText().toString().replace('.',','));c.comprado=q;c.faltante=Math.max(0,c.necessario-c.comprado);c.confianca=0.99;if(c.faltante<=toleranciaUn(c.un)){c.status="OK CORRIGIDO MANUALMENTE";c.motivo="Quantidade corrigida manualmente";c.selecionado=false;}else{c.status="FALTA PARCIAL CORRIGIDA";c.motivo="Falta parcial";c.selecionado=true;}db.inserirEvento("CORRECAO_MANUAL","Quantidade corrigida",c.nome+" | O.C.: "+fmt(q)+" "+c.un);montarListaRevisao();atualizarDashboard();}catch(Exception e){erro(e);} 
        }).setNegativeButton("Cancelar",null).show();
    }
    void mostrarMotivo(CandidatoIA c){
        new AlertDialog.Builder(this).setTitle("Motivo da classificação").setMessage("Item: "+c.nome+"\nCategoria: "+c.categoria+"\nNecessário: "+fmt(c.necessario)+" "+c.un+"\nO.C.: "+fmt(c.comprado)+" "+c.un+"\nFaltante: "+fmt(c.faltante)+" "+c.un+"\nConsumo: "+c.consumo+"\nEntrega O.C.: "+c.entrega+"\nConfiança IA: "+Math.round(c.confianca*100)+"%\nStatus: "+c.status+"\nMotivo: "+c.motivo).setPositiveButton("OK",null).show();
    }
    void mostrarEquivalencias(){append("\n🧩 EQUIVALÊNCIAS APRENDIDAS\n"+db.listarEquivalenciasTexto());}

    ArrayList<CandidatoIA> selecionados(){ArrayList<CandidatoIA> out=new ArrayList<>();for(CandidatoIA c:candidatos)if(c.selecionado)out.add(c);ordenarPorTipoEAlfabeto(out);return out;}

    ArrayList<CandidatoIA> filtrarPorModulo(ArrayList<CandidatoIA> todos,String filtro){ArrayList<CandidatoIA> out=new ArrayList<>();for(CandidatoIA c:todos)if(filtro==null||filtro.isEmpty()||filtro.equals(c.categoria))out.add(c);ordenarPorTipoEAlfabeto(out);return out;}
    int contarConsumoPorModulo(Map<String,Item> cons,String filtro){int n=0;for(Item it:cons.values()){String cat=categoriaOperacional(it);if(filtro==null||filtro.isEmpty()||filtro.equals(cat))n++;}return n;}
    String modoArquivo(){String m=norm(modoAnaliseAtual).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","_").replaceAll("_+","_");if(m.startsWith("_"))m=m.substring(1);if(m.endsWith("_"))m=m.substring(0,m.length()-1);return m.isEmpty()?"conferencia_total":m;}

    ArrayList<CandidatoIA> gerarCandidatosIA(Map<String,Item> cons,Map<String,Item> oc){
        ArrayList<CandidatoIA> list=new ArrayList<>();HashSet<String> usados=new HashSet<>();
        for(Item a:cons.values()){
            if(a==null||a.qtd<=0||a.un==null||a.un.trim().isEmpty())continue;
            Item b=oc.get(key(a));
            if(b==null)b=acharPorEquivalenciaAprendida(a,oc,usados);
            if(b==null)b=acharSimilarSeguro(a,oc,usados);
            if(b!=null)usados.add(key(b));
            double comp=b==null?0:b.qtd;
            double dif=a.qtd-comp;
            if(dif>tolerancia(a)){
                CandidatoIA c=new CandidatoIA();
                c.codigo=a.codigo;c.nome=a.nome;c.un=a.un;c.necessario=a.qtd;c.comprado=comp;c.faltante=dif;
                c.datasConsumo=normalizarDatasConsumo(a.data);c.consumo=consumoDatas(c.datasConsumo);
                c.entrega=b!=null?b.entrega:menorEntregaOC(itensOC);
                c.itemOCNome=b!=null?b.nome:""; c.chaveOC=b!=null?key(b):"";
                c.motivo=b==null?"zerado na ordem de compra":"Falta parcial";
                c.status=b==null?"ZERADO NA O.C.":"FALTA PARCIAL";
                c.categoria=categoriaOperacional(a);c.confianca=scoreIA(c,a,b);
                // Configuração de IA conservadora: a IA sugere, mas a revisão manual continua obrigatória.
                c.selecionado=selecionarAutomaticamente(c);
                list.add(c);
            }
        }
        ordenarPorTipoEAlfabeto(list);return list;
    }



    double scoreIA(CandidatoIA c,Item a,Item b){double s=0.45;double hist=db.scoreHistorico(c.chave());s+=hist*0.25;if(b!=null&&a.codigo.equals(b.codigo))s+=0.18;if(c.faltante>=20)s+=0.10;if(c.faltante/a.qtd>0.40)s+=0.10;if(b==null)s-=0.06;if(nomePossuiRisco(a.nome))s+=0.08;if(s<0.05)s=0.05;if(s>0.99)s=0.99;return s;}
    String riscoIA(ArrayList<? extends Object> arr){int n=arr.size();double maior=0;for(Object o:arr){if(o instanceof CandidatoIA){maior=Math.max(maior,((CandidatoIA)o).faltante);}}if(n>=5||maior>=25)return "ALTO";if(n>=2||maior>=10)return "MÉDIO";return "BAIXO";}


    String categoriaOperacional(Item a){String n=norm(a.nome);String c=a.codigo==null?"":a.codigo;if(c.startsWith("1.01")||n.matches(".*(CARNE|FRANGO|BOVINA|SUINA|LINGUICA|LINGUIÇA|CALABRESA|HAMBURG|SASSAMI|BACON|KIBE|FEIJOADA|PEIXE|FIGADO|FÍGADO|PANCETA|BISTECA|COXA|SOBRECOXA|ALMONDEGA|ALMÔNDEGA|FRALDINHA|PATINHO|TRASEIRO|SALSICHA|COSTELA|PALETA).*"))return "PROTEÍNAS";if(c.startsWith("1.17")||n.matches(".*(ALFACE|TOMATE|CEBOLA|ALHO|BANANA|LARANJA|MACA|MAÇA|MAMAO|MAMÃO|MELAO|MELÃO|ABACAXI|ABOBRINHA|BETERRABA|CENOURA|CHUCHU|LIMAO|LIMÃO|REPOLHO|PIMENTAO|PIMENTÃO|RABANETE|TEMPERO VERDE|COUVE|MORANGA|BATATA DOCE|HORTI).*"))return "HORTIFRUTI";if(c.startsWith("3.")||n.matches(".*(DETERGENTE|DESINFETANTE|CLORO|SABONETE|SECANTE|DESENGORDURANTE|LIMPADOR|RINSE|FIBRA|PANO MULTIUSO|MIKRO|CHLORINE|GREASECUTTER|NO RINSE|SOLID POWER|QUIMIC|QUÍMIC|COPO PLASTICO|DESCARTAVEL|GUARDANAPO|MARMITEX|GAS GLP).*"))return "QUÍMICOS";return "PERECÍVEL";}
    int ordemCategoria(String c){if("PROTEÍNAS".equals(c))return 1;if("PERECÍVEL".equals(c))return 2;if("HORTIFRUTI".equals(c))return 3;if("QUÍMICOS".equals(c))return 4;return 9;}
    void ordenarPorTipoEAlfabeto(ArrayList<CandidatoIA> list){Collections.sort(list,(a,b)->{int ca=ordemCategoria(a.categoria),cb=ordemCategoria(b.categoria);if(ca!=cb)return Integer.compare(ca,cb);return norm(a.nome).compareTo(norm(b.nome));});}
    void validarBase()throws Exception{if(analiticoText.isEmpty())throw new Exception("Selecione o PDF analítico.");if(ocText.isEmpty())throw new Exception("Selecione o PDF da OC.");if(folderUri==null)throw new Exception("Escolha a pasta para salvar.");if(dataIni.getText().toString().trim().isEmpty()||dataFim.getText().toString().trim().isEmpty())throw new Exception("Informe início e fim da semana.");if(!analiticoText.toUpperCase(Locale.ROOT).contains("REQUISI")&&!analiticoText.toUpperCase(Locale.ROOT).contains("ANAL"))append("Atenção: o arquivo analítico não contém cabeçalho típico. A conferência seguirá, mas revise o debug se houver poucos registros.");}
    Date parse(String s)throws Exception{return br.parse(s.trim());}boolean entre(String d,Date a,Date b){try{Date x=parse(d);return !x.before(a)&&!x.after(b);}catch(Exception e){return false;}}

    ArrayList<Item> extrairAnalitico(String t)throws Exception{
        Date ini=parse(dataIni.getText().toString()),fim=parse(dataFim.getText().toString());
        String texto=t.replace('\u00A0',' ').replace("\r","\n").replaceAll("[ \t]+"," ");
        ArrayList<Item> out=extrairAnaliticoLinha(texto,ini,fim);
        if(out.size()<20){
            // Fallback controlado para PDFs cuja ordem do texto venha quebrada por colunas.
            out=extrairAnaliticoContexto(texto,ini,fim);
        }
        out=removerDatasInvalidas(out,ini,fim);
        if(out.size()<20){salvarDebugTexto();throw new Exception("Extração baixa do analítico dentro do período informado. O app bloqueou o relatório para evitar data incorreta; confira debug_texto_extraido_conferencia_express.txt.");}
        return out;
    }
    static class Contexto{String data,turno;Contexto(String d,String t){data=d;turno=t;}}

    String dataConsumoValidaNaLinha(String line,Date ini,Date fim){
        if(line==null)return "";String up=line.toUpperCase(Locale.ROOT);if(linhaDataNaoConsumo(line))return "";
        // Data de consumo válida precisa aparecer com dia da semana. Assim Emissão, Limite, Entrega e Solicitação nunca viram consumo.
        Matcher dm=Pattern.compile("(\\d{2}/\\d{2}/\\d{4})\\s+(segunda|ter[cç]a|quarta|quinta|sexta|s[áa]bado|domingo)",Pattern.CASE_INSENSITIVE).matcher(up);
        while(dm.find()){String d=dm.group(1);if(entre(d,ini,fim))return d;}
        return "";
    }

    Contexto contextoAntes(String texto,int pos,Date ini,Date fim){
        int start=Math.max(0,pos-30000);String ctx=texto.substring(start,pos);String data="";
        for(String raw:ctx.split("\\n")){String line=raw.trim().replaceAll("\\s+"," ");String d=dataConsumoValidaNaLinha(line,ini,fim);if(!d.isEmpty())data=d;}
        if(data.isEmpty())return null;String turno="";Matcher sm=Pattern.compile("(\\d{5})\\s+(ALMOCO|ALMOÇO|JANTAR|CEIA)\\b",Pattern.CASE_INSENSITIVE).matcher(ctx);while(sm.find()){String st=sm.group(2).toUpperCase(Locale.ROOT);if(st.contains("ALMO"))turno="A";else if(st.contains("JANT"))turno="B";else if(st.contains("CEIA"))turno="C";}return new Contexto(data,turno);
    }

    boolean linhaDataNaoConsumo(String line){String up=line.toUpperCase(Locale.ROOT);return up.contains("EMISSÃO")||up.contains("EMISSAO")||up.contains("VERSÃO")||up.contains("VERSAO")||up.contains("PÁGINA")||up.contains("PAGINA")||up.contains("SOLICITAÇÃO")||up.contains("SOLICITACAO")||up.contains("PERÍODO DE ENTREGA")||up.contains("PERIODO DE ENTREGA")||up.contains("LIMITE DE ENTREGA")||up.contains("ENTREGA PREVISTA")||up.contains("UTILIZAÇÃO")||up.contains("UTILIZACAO")||up.contains("RELATÓRIO:")||up.contains("RELATORIO:")||up.contains("DATA ");}

    ArrayList<Item> removerDatasInvalidas(ArrayList<Item> in,Date ini,Date fim){ArrayList<Item> out=new ArrayList<>();HashSet<String> visto=new HashSet<>();for(Item it:in){if(it.data==null||it.data.trim().isEmpty())continue;String limpas=normalizarDatasConsumo(it.data,ini,fim);if(!limpas.isEmpty()){it.data=limpas;String k=it.codigo+"|"+it.nome+"|"+it.un+"|"+it.data+"|"+String.format(Locale.US,"%.5f",it.qtd);if(!visto.contains(k)){visto.add(k);out.add(it);}}}return out;}

    String normalizarDatasConsumo(String data){try{return normalizarDatasConsumo(data,parse(dataIni.getText().toString()),parse(dataFim.getText().toString()));}catch(Exception e){return data==null?"":data;}}
    String normalizarDatasConsumo(String data,Date ini,Date fim){if(data==null)return "";StringBuilder limpas=new StringBuilder();HashSet<String> set=new HashSet<>();for(String d:data.split(",")){d=d.trim();if(d.length()>=10)d=d.substring(0,10);if(d.length()==10&&entre(d,ini,fim)&&!set.contains(d)){set.add(d);if(limpas.length()>0)limpas.append(", ");limpas.append(d);}}return limpas.toString();}

    ArrayList<Item> extrairAnaliticoLinha(String t,Date ini,Date fim){
        ArrayList<Item> out=new ArrayList<>();String data="",turno="",pend="";
        Pattern itemA=Pattern.compile("^(\\d\\.\\d{2}\\.\\d{2}\\.\\d{3}\\.\\d{2})\\s+(.+?)\\s+([0-9.]+,[0-9]{2,5})\\s+([0-9.]+,[0-9]{2,5})\\s+(KG|LT|UN|PC|CX)\\b.*$",Pattern.CASE_INSENSITIVE);
        Pattern itemB=Pattern.compile("^(\\d\\.\\d{2}\\.\\d{2}\\.\\d{3}\\.\\d{2})\\s+(.+?)\\s+([0-9.]+,[0-9]{2,5})\\s*(KG|LT|UN|PC|CX)\\s*([0-9.]+,[0-9]{2,5})\\b.*$",Pattern.CASE_INSENSITIVE);
        for(String raw:t.split("\\n")){
            String line=raw.trim().replaceAll("\\s+"," ");String d=dataConsumoValidaNaLinha(line,ini,fim);if(!d.isEmpty())data=d;
            String up=line.toUpperCase(Locale.ROOT);if(up.matches(".*\\b(ALMOCO|ALMOÇO)\\b.*"))turno="A";else if(up.matches(".*\\bJANTAR\\b.*"))turno="B";else if(up.matches(".*\\bCEIA\\b.*"))turno="C";
            if(line.matches("^\\d\\.\\d{2}\\.\\d{2}\\.\\d{3}\\.\\d{2}.*"))pend=line;else if(!pend.isEmpty()&&!linhaDataNaoConsumo(line))pend=pend+" "+line;else continue;
            Matcher im=itemA.matcher(pend);boolean ok=im.matches();
            if(ok&&!data.isEmpty()){Item it=new Item();it.codigo=im.group(1);it.nome=limparNome(im.group(2));it.qtd=num(im.group(3));it.un=im.group(5).toUpperCase(Locale.ROOT);it.data=data;it.turno=turno;out.add(it);pend="";continue;}
            im=itemB.matcher(pend);ok=im.matches();
            if(ok&&!data.isEmpty()){Item it=new Item();it.codigo=im.group(1);it.nome=limparNome(im.group(2));it.qtd=num(im.group(3));it.un=im.group(4).toUpperCase(Locale.ROOT);it.data=data;it.turno=turno;out.add(it);pend="";continue;}
            if(pend.length()>360)pend="";
        }
        return out;
    }

    ArrayList<Item> extrairAnaliticoContexto(String texto,Date ini,Date fim){
        ArrayList<Item> out=new ArrayList<>();
        Pattern itemA=Pattern.compile("(?is)\\b(\\d\\.\\d{2}\\.\\d{2}\\.\\d{3}\\.\\d{2})\\s+(.{3,160}?)\\s+([0-9.]+,[0-9]{2,5})\\s+([0-9.]+,[0-9]{2,5})\\s+(KG|LT|UN|PC|CX)\\b");
        Pattern itemB=Pattern.compile("(?is)\\b(\\d\\.\\d{2}\\.\\d{2}\\.\\d{3}\\.\\d{2})\\s+(.{3,160}?)\\s+([0-9.]+,[0-9]{2,5})\\s*(KG|LT|UN|PC|CX)\\s*([0-9.]+,[0-9]{2,5})\\b");
        Matcher im=itemA.matcher(texto);while(im.find()){Contexto ctx=contextoAntes(texto,im.start(),ini,fim);if(ctx==null)continue;String nome=limparNome(im.group(2));if(nome.length()<3||nome.contains("QTDE")||nome.contains("RELATORIO")||nome.contains("RELATÓRIO"))continue;Item it=new Item();it.codigo=im.group(1);it.nome=nome;it.qtd=num(im.group(3));it.un=im.group(5).toUpperCase(Locale.ROOT);it.data=ctx.data;it.turno=ctx.turno;out.add(it);}
        im=itemB.matcher(texto);while(im.find()){Contexto ctx=contextoAntes(texto,im.start(),ini,fim);if(ctx==null)continue;String nome=limparNome(im.group(2));if(nome.length()<3||nome.contains("QTDE")||nome.contains("RELATORIO")||nome.contains("RELATÓRIO"))continue;Item it=new Item();it.codigo=im.group(1);it.nome=nome;it.qtd=num(im.group(3));it.un=im.group(4).toUpperCase(Locale.ROOT);it.data=ctx.data;it.turno=ctx.turno;out.add(it);}
        return out;
    }

    ArrayList<Item> extrairOC(String t){ArrayList<Item> out=new ArrayList<>();String texto=t.replace('\u00A0',' ').replace("\r","\n");Pattern p=Pattern.compile("^(\\d\\.\\d{2}\\.\\d{2}\\.\\d{3}\\.\\d{2})\\s+(.+?)\\s+(KG|LT|UN|PC|CX)\\s+([0-9.]+,[0-9]{2,5})\\s+(\\d{2}/\\d{2}/\\d{4})\\s+(\\d{2}/\\d{2}/\\d{4}).*$",Pattern.CASE_INSENSITIVE);for(String raw:texto.split("\\n")){String line=raw.trim().replaceAll("\\s+"," ");Matcher m=p.matcher(line);if(m.matches()){Item it=new Item();it.codigo=m.group(1);it.nome=limparNomeOC(m.group(2));it.un=unBaseOC(m.group(2),m.group(3).toUpperCase(Locale.ROOT));it.qtd=num(m.group(4))*fatorOC(m.group(2),m.group(3).toUpperCase(Locale.ROOT));it.entrega=m.group(5);it.data=m.group(6);out.add(it);}}return out;}
    double fatorOC(String nome,String un){Matcher m=Pattern.compile("(\\d+)\\s*(KG|UN|LT)\\s*X\\s*(CX|PC|BJ)",Pattern.CASE_INSENSITIVE).matcher(nome);if(m.find())return Double.parseDouble(m.group(1));if(nome.toUpperCase(Locale.ROOT).contains("30 UN X 12"))return 360;return 1;}
    String unBaseOC(String nome,String un){Matcher m=Pattern.compile("(\\d+)\\s*(KG|UN|LT)\\s*X\\s*(CX|PC|BJ)",Pattern.CASE_INSENSITIVE).matcher(nome);if(m.find())return m.group(2).toUpperCase(Locale.ROOT);if(nome.toUpperCase(Locale.ROOT).contains("30 UN X 12"))return "UN";return un;}
    String limparNome(String s){String x=s.replace('\u00A0',' ').replaceAll("\\s+"," ").trim().toUpperCase(Locale.ROOT);x=x.replaceAll("\\s+[0-9.]+,[0-9]{2,5}\\s*$","");x=x.replaceAll("^(PRODUTO|TOTAL|UN)\\s+","");return x.trim();}
    String limparNomeOC(String s){return limparNome(s).replaceAll("\\b\\d+\\s*(KG|UN|LT|G)\\s*X\\s*(CX|PC|BJ)\\b","").replaceAll("\\s+"," ").trim();}
    double num(String s){return Double.parseDouble(s.replace(".","").replace(",","."));}
    String norm(String s){return s==null?"":s.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9 ]","").replaceAll("\\s+"," ").trim();}
    boolean nomePossuiRisco(String n){String x=norm(n);return x.matches(".*(CARNE|FRANGO|BOVINA|SUINA|LINGUICA|CALABRESA|HAMBURGUER|SASSAMI|KIBE|BACON|FEIJOADA|PEIXE|PESCADA|PANCETA|SOBREPALETA|PATINHO|TRASEIRO).* ")||x.matches(".*(CARNE|FRANGO|BOVINA|SUINA|LINGUICA|CALABRESA|HAMBURGUER|SASSAMI|KIBE|BACON|FEIJOADA|PEIXE|PESCADA|PANCETA|SOBREPALETA|PATINHO|TRASEIRO).*");}
    boolean excluido(String nome){String n=norm(nome);return n.matches(".*\\bOVO(S)?\\b.*")||n.contains("OVO BRANCO")||n.contains("QUEIJO RALADO")||n.contains("MISTURA ALIMENTICIA QUEIJO RALADO")||n.contains("MISTURA ALIMENTICIA QUEIJO");}
    boolean ehProteinaValida(Item a){if(a==null||excluido(a.nome))return false;String n=norm(a.nome),c=a.codigo==null?"":a.codigo;if(c.startsWith("1.01"))return true;if(nomePossuiRisco(n))return true;if(n.contains("QUEIJO MUSSARELA")||n.contains("MUSSARELA FATIADO"))return true;return false;}
    String key(Item it){return it.codigo+"|"+norm(it.nome)+"|"+it.un;}
    Map<String,Item> consolidar(ArrayList<Item> l){LinkedHashMap<String,Item> m=new LinkedHashMap<>();for(Item it:l){String k=key(it);Item x=m.get(k);if(x==null){x=it.copy();m.put(k,x);}else{x.qtd+=it.qtd;if(it.data!=null&&!it.data.isEmpty()&&!x.data.contains(it.data))x.data=x.data.isEmpty()?it.data:x.data+", "+it.data;}}return m;}
    Map<String,Item> consolidarOC(ArrayList<Item> l){LinkedHashMap<String,Item> m=new LinkedHashMap<>();for(Item it:l){String k=key(it);Item x=m.get(k);if(x==null){x=it.copy();m.put(k,x);}else{x.qtd+=it.qtd;x.entrega=menorDataBR(x.entrega,it.entrega);if(it.data!=null&&!it.data.isEmpty()&&!x.data.contains(it.data))x.data=x.data.isEmpty()?it.data:x.data+", "+it.data;}}return m;}
    Item acharPorEquivalenciaAprendida(Item a,Map<String,Item> oc,Set<String> usados){String alvo=db.buscarEquivalenciaOC(key(a));if(alvo==null||alvo.isEmpty())return null;for(Item b:oc.values()){if(usados.contains(key(b)))continue;if(key(b).equals(alvo)&&a.un.equals(b.un))return b;}return null;}
    Item acharSimilarSeguro(Item a,Map<String,Item> oc,Set<String> usados){for(Item b:oc.values()){if(usados.contains(key(b)))continue;if(!a.un.equals(b.un))continue;if(a.codigo.equals(b.codigo))return b;if(norm(a.nome).equals(norm(b.nome)))return b;}return null;}
    String menorDataBR(String a,String b){try{if(a==null||a.isEmpty())return b==null?"":b;if(b==null||b.isEmpty())return a;Date da=br.parse(a),db=br.parse(b);return da.after(db)?b:a;}catch(Exception e){return (a==null||a.isEmpty())?b:a;}}
    String menorEntregaOC(ArrayList<Item> l){String d="";for(Item it:l)if(it.entrega!=null&&!it.entrega.isEmpty())d=menorDataBR(d,it.entrega);return d;}
    double tolerancia(Item a){if(a==null)return 0.001;if("UN".equals(a.un))return 0.5;return 0.050;}
    String consumoDatas(String data){if(data==null||data.trim().isEmpty())return "DATA NÃO LOCALIZADA";StringBuilder sb=new StringBuilder();for(String d:data.split(",")){d=d.trim();if(d.length()>=5){if(sb.length()>0)sb.append(", ");sb.append(d.length()>=10?d.substring(0,5):d);}}return sb.length()==0?"DATA NÃO LOCALIZADA":sb.toString();}
    void salvarPdfRevisado(ArrayList<CandidatoIA> rows,String nome,String entrega)throws Exception{ordenarPorTipoEAlfabeto(rows);PdfDocument pdf=new PdfDocument();Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);int page=1;int i=0;int n=1;String catAtual="";while(i<rows.size()){PdfDocument.Page pg=pdf.startPage(new PdfDocument.PageInfo.Builder(842,595,page++).create());Canvas c=pg.getCanvas();drawHeaderPdf(c,p,entrega);int y=205;int rowH=42;drawTableHeader(c,p,y);y+=rowH;while(i<rows.size()&&y<510){CandidatoIA r=rows.get(i);if(!r.categoria.equals(catAtual)){if(y+24>510)break;catAtual=r.categoria;drawCategoria(c,p,y,catAtual);y+=24;}if(y+rowH>510)break;drawRow(c,p,y,rowH,n,r);y+=rowH;i++;n++;}p.setColor(Color.rgb(20,40,70));p.setStrokeWidth(1);c.drawLine(30,540,812,540,p);p.setTextSize(12);p.setFakeBoldText(false);p.setColor(Color.rgb(30,30,30));c.drawText(br.format(new Date())+" - By Maicon",650,570,p);pdf.finishPage(pg);}OutputStream os=out(nome,"application/pdf");pdf.writeTo(os);os.close();pdf.close();}
    void drawHeaderPdf(Canvas c,Paint p,String entrega){p.setColor(Color.WHITE);c.drawRect(0,0,842,595,p);p.setColor(Color.rgb(2,20,48));c.drawRect(0,0,842,130,p);p.setColor(dourado);c.drawRect(0,128,842,132,p);p.setTextSize(21);p.setFakeBoldText(true);p.setColor(Color.WHITE);c.drawText("RELATÓRIO PREVISÃO DE FALTANTES",390,68,p);p.setTextSize(13);p.setColor(Color.rgb(0,135,230));p.setFakeBoldText(true);c.drawText("EXPRESS-UNIDADE COLORADO",45,55,p);p.setTextSize(8);p.setColor(Color.rgb(90,180,255));c.drawText("CONFERÊNCIA REALIZADA COM BASE ANALITICO X O.C.",45,78,p);c.drawText("IA AUDITÁVEL • REVISÃO MANUAL OBRIGATÓRIA",45,96,p);p.setStrokeWidth(2);p.setColor(dourado);c.drawLine(350,32,350,108,p);p.setFakeBoldText(true);p.setTextSize(14);p.setColor(Color.rgb(15,25,45));c.drawText("ENTREGA PREVISTA: ",60,170,p);p.setColor(dourado);c.drawText(entrega==null||entrega.isEmpty()?"NÃO LOCALIZADA":entrega,205,170,p);}
    void drawTableHeader(Canvas c,Paint p,int y){int[] xs={35,65,245,335,425,515,600,695,820};p.setColor(Color.rgb(210,225,240));c.drawRect(xs[0],y,xs[8],y+42,p);p.setColor(Color.rgb(80,80,80));p.setStrokeWidth(1);for(int x:xs)c.drawLine(x,y,x,y+42,p);c.drawLine(xs[0],y,xs[8],y,p);c.drawLine(xs[0],y+42,xs[8],y+42,p);p.setColor(Color.rgb(10,20,40));p.setTextSize(10);p.setFakeBoldText(true);String[] h={"Nº","ITEM","NECESSÁRIO","ENTREGA","FALTANTE","CONSUMO","DATA ENTREGA","OBSERVAÇÕES"};int[] tx={43,118,258,350,438,528,612,705};for(int j=0;j<h.length;j++)c.drawText(h[j],tx[j],y+26,p);}
    void drawCategoria(Canvas c,Paint p,int y,String categoria){p.setColor(Color.rgb(2,20,48));c.drawRect(35,y,820,y+22,p);p.setColor(dourado);p.setTextSize(11);p.setFakeBoldText(true);c.drawText("TIPO: "+categoria,45,y+15,p);p.setFakeBoldText(false);}
    void drawRow(Canvas c,Paint p,int y,int h,int n,CandidatoIA r){int[] xs={35,65,245,335,425,515,600,695,820};p.setColor((n%2==0)?Color.rgb(245,248,252):Color.rgb(252,252,252));c.drawRect(xs[0],y,xs[8],y+h,p);p.setColor(Color.rgb(155,155,155));for(int x:xs)c.drawLine(x,y,x,y+h,p);c.drawLine(xs[0],y+h,xs[8],y+h,p);p.setTextSize(9);p.setFakeBoldText(false);p.setColor(Color.rgb(20,20,20));c.drawText(String.valueOf(n),45,y+25,p);drawWrapped(c,p,r.nome,75,y+15,160,10);p.setTextSize(9);c.drawText(fmt(r.necessario)+" "+r.un,255,y+25,p);c.drawText(fmt(r.comprado)+" "+r.un,345,y+25,p);p.setColor(Color.RED);p.setFakeBoldText(true);c.drawText(fmt(r.faltante)+" "+r.un,435,y+25,p);p.setFakeBoldText(false);p.setColor(Color.rgb(20,20,20));drawWrapped(c,p,r.consumo,525,y+15,68,9);c.drawText(r.entrega==null?"":r.entrega,610,y+25,p);drawWrapped(c,p,r.status+" • "+r.motivo+" • IA "+Math.round(r.confianca*100)+"%",705,y+15,110,8);}


    void drawWrapped(Canvas c,Paint p,String s,int x,int y,int width,int size){p.setTextSize(size);String[] parts=s.split(" ");String line="";int yy=y;for(String w:parts){String test=line.isEmpty()?w:line+" "+w;if(p.measureText(test)>width){c.drawText(line,x,yy,p);yy+=13;line=w;}else line=test;}if(!line.isEmpty())c.drawText(line,x,yy,p);}
    void salvarXlsxRevisado(ArrayList<CandidatoIA> rows,String nome)throws Exception{ordenarPorTipoEAlfabeto(rows);ByteArrayOutputStream baos=new ByteArrayOutputStream();ZipOutputStream z=new ZipOutputStream(baos);zip(z,"[Content_Types].xml","<?xml version=\"1.0\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>");zip(z,"_rels/.rels","<?xml version=\"1.0\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");zip(z,"xl/workbook.xml","<?xml version=\"1.0\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Previsao Faltantes\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");zip(z,"xl/_rels/workbook.xml.rels","<?xml version=\"1.0\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>");StringBuilder sbxml=new StringBuilder("<?xml version=\"1.0\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");int r=1;row(sbxml,r++,"Tipo","Nº","Item","Necessário","Entrega","Faltante","UN","Consumo","Datas completas","Data Entrega","Confiança IA","Observação","Status");int n=1;for(CandidatoIA c:rows)row(sbxml,r++,c.categoria,""+(n++),c.nome,fmt(c.necessario),fmt(c.comprado),fmt(c.faltante),c.un,c.consumo,c.datasConsumo,c.entrega,Math.round(c.confianca*100)+"%",c.status+" - "+c.motivo,"PENDENTE");sbxml.append("</sheetData></worksheet>");zip(z,"xl/worksheets/sheet1.xml",sbxml.toString());z.close();OutputStream os=out(nome,"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");os.write(baos.toByteArray());os.close();}
    void salvarTxtRevisado(ArrayList<CandidatoIA> rows,String nome)throws Exception{
        ordenarPorTipoEAlfabeto(rows);
        StringBuilder sb=new StringBuilder();
        sb.append("🤖 RELATÓRIO PREVISÃO DE FALTANTES\nEXPRESS-UNIDADE COLORADO\n")
          .append("CONFERÊNCIA REALIZADA COM BASE ANALITICO X O.C.\n")
          .append(dataIni.getText()).append(" a ").append(dataFim.getText())
          .append("\nEntrega: ").append(ultimaEntrega).append("\nModo IA: ").append(nivelIA).append("\n\n");
        String cat="";int n=1;
        for(CandidatoIA c:rows){
            if(!c.categoria.equals(cat)){cat=c.categoria;sb.append("\nTIPO: ").append(cat).append("\n");}
            sb.append(n++).append(". ").append(c.nome)
              .append(" | Necessário: ").append(fmt(c.necessario)).append(" ").append(c.un)
              .append(" | O.C.: ").append(fmt(c.comprado)).append(" ").append(c.un)
              .append(" | Faltante: ").append(fmt(c.faltante)).append(" ").append(c.un)
              .append(" | Consumo: ").append(c.consumo)
              .append(" | Data entrega OC: ").append(c.entrega)
              .append(" | Status IA: ").append(c.status)
              .append(" | Confiança: ").append(Math.round(c.confianca*100)).append("%")
              .append(" | ").append(c.motivo)
              .append(" | Status pendência: PENDENTE\n");
        }
        sb.append("\n").append(br.format(new Date())).append(" - By Maicon");
        OutputStream os=out(nome,"text/plain");os.write(sb.toString().getBytes("UTF-8"));os.close();
    }

    void zip(ZipOutputStream z,String name,String data)throws Exception{z.putNextEntry(new ZipEntry(name));z.write(data.getBytes("UTF-8"));z.closeEntry();}void row(StringBuilder s,int r,String...vals){s.append("<row r=\"").append(r).append("\">");for(String val:vals)s.append("<c t=\"inlineStr\"><is><t>").append(xml(val)).append("</t></is></c>");s.append("</row>");}String xml(String s){return s==null?"":s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");}

    Uri criarNaPastaEscolhida(String nome,String mime)throws Exception{String treeId=DocumentsContract.getTreeDocumentId(folderUri);Uri dirUri=DocumentsContract.buildDocumentUriUsingTree(folderUri,treeId);Uri novo=DocumentsContract.createDocument(getContentResolver(),dirUri,mime,nome);if(novo==null)throw new Exception("Não foi possível criar arquivo na pasta escolhida: "+nome);return novo;}
    Uri criarNoDownloads(String nome,String mime)throws Exception{if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q){ContentValues cv=new ContentValues();cv.put(MediaStore.MediaColumns.DISPLAY_NAME,nome);cv.put(MediaStore.MediaColumns.MIME_TYPE,mime);cv.put(MediaStore.MediaColumns.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/ConferenciaExpress");Uri uri=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,cv);if(uri==null)throw new Exception("Não foi possível criar arquivo em Downloads.");return uri;}else{File dir=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"ConferenciaExpress");dir.mkdirs();return Uri.fromFile(new File(dir,nome));}}
    OutputStream out(String nome,String mime)throws Exception{Exception erro=null;if(folderUri!=null){try{Uri uri=criarNaPastaEscolhida(nome,mime);OutputStream os=getContentResolver().openOutputStream(uri);if(os!=null)return os;}catch(Exception e){erro=e;}}Uri uri=criarNoDownloads(nome,mime);OutputStream os="file".equalsIgnoreCase(uri.getScheme())?new FileOutputStream(new File(uri.getPath())):getContentResolver().openOutputStream(uri);if(os==null)throw new Exception("Não foi possível salvar: "+nome);saveInfo+="\nArquivo salvo em Downloads/ConferenciaExpress: "+nome;if(erro!=null)saveInfo+="\nAviso pasta escolhida: "+erro.getMessage();return os;}
    void salvarDebugTexto(){try{OutputStream os=out("debug_texto_extraido_conferencia_express.txt","text/plain");String cab="DEBUG TEXTO EXTRAÍDO - Conferência Express\nCaracteres: "+analiticoText.length()+"\n\n";os.write(cab.getBytes("UTF-8"));os.write(analiticoText.getBytes("UTF-8"));os.close();}catch(Exception ignored){}}

    void salvarDebugTextoOC(){try{OutputStream os=out("debug_texto_extraido_oc_conferencia_express.txt","text/plain");String cab="DEBUG TEXTO EXTRAÍDO O.C. - Conferência Express\nCaracteres: "+ocText.length()+"\n\n";os.write(cab.getBytes("UTF-8"));os.write(ocText.getBytes("UTF-8"));os.close();}catch(Exception ignored){}}

    void novaAnalise(){analiticoUri=null;ocUri=null;analiticoText="";ocText="";itensConsumo.clear();itensOC.clear();candidatos.clear();reviewBox.removeAllViews();ultimoAnalisados=0;ultimoCandidatos=0;ultimoSelecionados=0;ultimoOK=0;ultimoRisco="BAIXO";ultimaEntrega="";modoAnaliseAtual="CONFERÊNCIA TOTAL";filtroCategoriaAtual="";dataIni.setText("");dataFim.setText("");status.setText("Nova conferência iniciada. Selecione novo Analítico e nova OC.");append("\n🆕 Nova conferência: PDFs anteriores limpos. A memória da IA, histórico e pendências foram mantidos para notificações.");atualizarDashboard();}
    void abrirPendenciasDialog(){try{ArrayList<DatabaseHelper.Pendente> pend=db.listarPendentes();if(pend.isEmpty()){Toast.makeText(this,"Nenhum faltante pendente na memória da IA.",Toast.LENGTH_LONG).show();append("Conferência Express: não existem pendências abertas.");return;}String[] nomes=new String[pend.size()];boolean[] marcados=new boolean[pend.size()];for(int i=0;i<pend.size();i++){DatabaseHelper.Pendente p=pend.get(i);nomes[i]=p.nome+" | "+fmt(p.faltante)+" "+p.un+" | consumo: "+consumoDatas(p.consumoDatas);marcados[i]=false;}new AlertDialog.Builder(this).setTitle("Qual faltante foi resolvido?").setMultiChoiceItems(nomes,marcados,(dialog,which,isChecked)->marcados[which]=isChecked).setPositiveButton("Marcar resolvido",(dialog,which)->{int qtd=0;for(int i=0;i<pend.size();i++)if(marcados[i]){db.marcarResolvido(pend.get(i).chave);qtd++;}db.inserirEvento("PENDENCIA","Itens resolvidos informados",qtd+" item(ns) marcado(s) como resolvido(s).");append("Conferência Express: "+qtd+" faltante(s) marcado(s) como resolvido(s). Os demais permanecem pendentes.");Toast.makeText(this,qtd+" item(ns) resolvido(s)",Toast.LENGTH_LONG).show();atualizarDashboard();}).setNegativeButton("Ainda pendente",(dialog,which)->{append("Conferência Express: pendências mantidas em aberto para próximos alertas.");}).show();}catch(Exception e){erro(e);}}
    void mostrarResumoIA(){append("\n🤖 RESUMO CONFERÊNCIA EXPRESS IA\n"+resumoTexto());}
    String resumoTexto(){int pend=0;try{pend=db.listarPendentes().size();}catch(Exception ignored){}return "Período: "+dataIni.getText()+" a "+dataFim.getText()+"\nEntrega OC: "+ultimaEntrega+"\nFaltantes detectados: "+ultimoCandidatos+"\nSelecionados para relatório final: "+selecionados().size()+"\nPendências na memória: "+pend+"\nItens OK estimados: "+ultimoOK+"\nRisco: "+ultimoRisco+"\nAlertas: 06:00 e 12:00 com janelas 24h, 48h e 72h.";}
    void mostrarHistorico(){append("\n📁 HISTÓRICO CONFERÊNCIA EXPRESS IA\n"+db.listarHistoricoTexto());}
    void atualizarDashboard(){int sel=selecionados().size();int pend=0;try{pend=db.listarPendentes().size();}catch(Exception ignored){}dashboard.setText("🛡️ CONFERÊNCIA EXPRESS IA — PAINEL DE SEGURANÇA\nRisco: "+ultimoRisco+" | Entrega: "+ultimaEntrega+"\nFaltantes detectados: "+ultimoCandidatos+" | Selecionados: "+sel+" | OK: "+ultimoOK+"\nPendências abertas: "+pend+" | Alertas preventivos 06:00 e 12:00\nRegra: nenhum relatório final é emitido sem revisão manual.");}
    void salvarPrefs(){getSharedPreferences("resumo",MODE_PRIVATE).edit().putInt("candidatos",ultimoCandidatos).putInt("selecionados",ultimoSelecionados).putInt("ok",ultimoOK).putString("ini",dataIni.getText().toString()).putString("fim",dataFim.getText().toString()).putString("entrega",ultimaEntrega).putString("risco",ultimoRisco).putString("modo",modoAnaliseAtual).putString("filtro",filtroCategoriaAtual).putString("nivelIA",nivelIA).apply();}
    void carregarPrefs(){android.content.SharedPreferences sp=getSharedPreferences("resumo",MODE_PRIVATE);ultimoCandidatos=sp.getInt("candidatos",0);ultimoSelecionados=sp.getInt("selecionados",0);ultimoOK=sp.getInt("ok",0);String ini=sp.getString("ini","");String fim=sp.getString("fim","");ultimaEntrega=sp.getString("entrega","");ultimoRisco=sp.getString("risco","BAIXO");modoAnaliseAtual=sp.getString("modo","CONFERÊNCIA TOTAL");filtroCategoriaAtual=sp.getString("filtro","");nivelIA=sp.getString("nivelIA","SEGURA");if(!ini.isEmpty())dataIni.setText(ini);if(!fim.isEmpty())dataFim.setText(fim);atualizarDashboard();}
    void pedirPermissaoNotificacao(){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},77);}
    void criarCanalNotificacao(){if(Build.VERSION.SDK_INT>=26){NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);NotificationChannel ch=new NotificationChannel("conferencia_express_alertas","Conferência Express",NotificationManager.IMPORTANCE_DEFAULT);ch.setDescription("Alertas detalhados 06:00 e 12:00 sobre faltantes nas próximas 24h, 48h e 72h");nm.createNotificationChannel(ch);}}
    void programarNotificacoesDiarias(){NotificationReceiver.schedule(this,6,0,6003);NotificationReceiver.schedule(this,12,0,1203);}

    String fmt(double d){return String.format(new Locale("pt","BR"),"%,.3f",d);}String dataArquivo(String d){try{return arquivoDt.format(br.parse(d));}catch(Exception e){return d==null||d.isEmpty()?"sem_data":d.replace('/','-');}}void append(String s){if(log!=null)log.append("\n"+s);}void erro(Exception e){status.setText("Erro: "+e.getMessage());append("ERRO: "+e.toString());}
    static class Item{String codigo="",nome="",un="",data="",turno="",entrega="";double qtd;Item copy(){Item i=new Item();i.codigo=codigo;i.nome=nome;i.un=un;i.data=data;i.turno=turno;i.entrega=entrega;i.qtd=qtd;return i;}}
    static class CandidatoIA{String codigo="",nome="",un="",consumo="",datasConsumo="",entrega="",motivo="",categoria="PERECÍVEL";double necessario,comprado,faltante,confianca;boolean selecionado;String itemOCNome="",chaveOC="",status="SUGERIDO";String chave(){return (codigo+"|"+nome+"|"+un).toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9| ]","").replaceAll("\\s+"," ").trim();}}
}
