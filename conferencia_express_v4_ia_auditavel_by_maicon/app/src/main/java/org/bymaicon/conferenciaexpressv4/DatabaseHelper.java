package org.bymaicon.conferenciaexpressv4;

import android.content.*;import android.database.*;import android.database.sqlite.*;import java.text.*;import java.util.*;

public class DatabaseHelper extends SQLiteOpenHelper{
    public static final String DB="conferencia_express.db";
    SimpleDateFormat dt=new SimpleDateFormat("dd/MM/yyyy HH:mm",new Locale("pt","BR"));
    public DatabaseHelper(Context c){super(c,DB,null,2);} 
    public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE IF NOT EXISTS historico(id INTEGER PRIMARY KEY AUTOINCREMENT, criado TEXT, inicio TEXT, fim TEXT, entrega TEXT, analisados INTEGER, candidatos INTEGER, selecionados INTEGER, ok INTEGER, risco TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS aprendizado(id INTEGER PRIMARY KEY AUTOINCREMENT, chave TEXT UNIQUE, nome TEXT, aprovado INTEGER DEFAULT 0, recusado INTEGER DEFAULT 0, total_faltante REAL DEFAULT 0, ultimo_faltante REAL DEFAULT 0, ultima_entrega TEXT, atualizado TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS notificacoes(id INTEGER PRIMARY KEY AUTOINCREMENT, criado TEXT, tipo TEXT, titulo TEXT, mensagem TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS equivalencias(id INTEGER PRIMARY KEY AUTOINCREMENT, chave_analitico TEXT, chave_oc TEXT, nome_analitico TEXT, nome_oc TEXT, confianca REAL, aprovado INTEGER DEFAULT 1, criado TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS faltantes_pendentes(id INTEGER PRIMARY KEY AUTOINCREMENT, chave TEXT UNIQUE, nome TEXT, categoria TEXT, un TEXT, faltante REAL, necessario REAL, entrega TEXT, consumo_datas TEXT, status TEXT DEFAULT 'PENDENTE', criado TEXT, resolvido_em TEXT, observacao TEXT)");
    }
    public void onUpgrade(SQLiteDatabase db,int oldV,int newV){onCreate(db);}
    String agora(){return dt.format(new Date());}
    public void inserirHistorico(String ini,String fim,String entrega,int analisados,int candidatos,int selecionados,int ok,String risco){
        SQLiteDatabase db=getWritableDatabase();ContentValues v=new ContentValues();v.put("criado",agora());v.put("inicio",ini);v.put("fim",fim);v.put("entrega",entrega);v.put("analisados",analisados);v.put("candidatos",candidatos);v.put("selecionados",selecionados);v.put("ok",ok);v.put("risco",risco);db.insert("historico",null,v);
    }
    public void inserirEvento(String tipo,String titulo,String msg){SQLiteDatabase db=getWritableDatabase();ContentValues v=new ContentValues();v.put("criado",agora());v.put("tipo",tipo);v.put("titulo",titulo);v.put("mensagem",msg);db.insert("notificacoes",null,v);} 
    public void aprender(String chave,String nome,boolean aprovado,double faltante,String entrega){
        SQLiteDatabase db=getWritableDatabase();
        Cursor c=db.rawQuery("SELECT aprovado,recusado,total_faltante FROM aprendizado WHERE chave=?",new String[]{chave});
        ContentValues v=new ContentValues();v.put("chave",chave);v.put("nome",nome);v.put("ultimo_faltante",faltante);v.put("ultima_entrega",entrega);v.put("atualizado",agora());
        if(c.moveToFirst()){
            int ap=c.getInt(0), rec=c.getInt(1); double total=c.getDouble(2); c.close();
            if(aprovado)ap++; else rec++;
            v.put("aprovado",ap);v.put("recusado",rec);v.put("total_faltante",total+(aprovado?faltante:0));
            db.update("aprendizado",v,"chave=?",new String[]{chave});
        }else{c.close();v.put("aprovado",aprovado?1:0);v.put("recusado",aprovado?0:1);v.put("total_faltante",aprovado?faltante:0);db.insert("aprendizado",null,v);}        
    }
    public double scoreHistorico(String chave){
        SQLiteDatabase db=getReadableDatabase();Cursor c=db.rawQuery("SELECT aprovado,recusado FROM aprendizado WHERE chave=?",new String[]{chave});double r=0.50;if(c.moveToFirst()){int ap=c.getInt(0), rec=c.getInt(1);r=(ap+1.0)/(ap+rec+2.0);}c.close();return r;
    }
    public void salvarFaltantePendente(String chave,String nome,String un,double faltante,double necessario,String entrega,String consumoDatas,String categoria){
        SQLiteDatabase db=getWritableDatabase();ContentValues v=new ContentValues();v.put("chave",chave);v.put("nome",nome);v.put("categoria",categoria);v.put("un",un);v.put("faltante",faltante);v.put("necessario",necessario);v.put("entrega",entrega);v.put("consumo_datas",consumoDatas);v.put("status","PENDENTE");v.put("criado",agora());v.put("observacao","");
        int upd=db.update("faltantes_pendentes",v,"chave=?",new String[]{chave});if(upd==0)db.insert("faltantes_pendentes",null,v);
    }

    public void salvarEquivalencia(String chaveAnalitico,String chaveOC,String nomeAnalitico,String nomeOC,double confianca){
        SQLiteDatabase db=getWritableDatabase();
        ContentValues v=new ContentValues();
        v.put("chave_analitico",chaveAnalitico);v.put("chave_oc",chaveOC);v.put("nome_analitico",nomeAnalitico);v.put("nome_oc",nomeOC);v.put("confianca",confianca);v.put("aprovado",1);v.put("criado",agora());
        db.delete("equivalencias","chave_analitico=?",new String[]{chaveAnalitico});
        db.insert("equivalencias",null,v);
    }
    public String buscarEquivalenciaOC(String chaveAnalitico){
        SQLiteDatabase db=getReadableDatabase();
        Cursor c=db.rawQuery("SELECT chave_oc FROM equivalencias WHERE chave_analitico=? AND aprovado=1 ORDER BY id DESC LIMIT 1",new String[]{chaveAnalitico});
        String r=""; if(c.moveToFirst())r=c.getString(0); c.close(); return r==null?"":r;
    }
    public String listarEquivalenciasTexto(){
        StringBuilder sb=new StringBuilder();SQLiteDatabase db=getReadableDatabase();Cursor c=db.rawQuery("SELECT criado,nome_analitico,nome_oc,confianca FROM equivalencias WHERE aprovado=1 ORDER BY id DESC LIMIT 50",null);
        while(c.moveToNext()){sb.append(c.getString(0)).append(" | ").append(c.getString(1)).append(" = ").append(c.getString(2)).append(" | confiança ").append(Math.round(c.getDouble(3)*100)).append("%\n");}
        c.close(); if(sb.length()==0)sb.append("Nenhuma equivalência salva ainda."); return sb.toString();
    }

    public void marcarResolvido(String chave){SQLiteDatabase db=getWritableDatabase();ContentValues v=new ContentValues();v.put("status","RESOLVIDO");v.put("resolvido_em",agora());db.update("faltantes_pendentes",v,"chave=?",new String[]{chave});}
    public void marcarPendente(String chave){SQLiteDatabase db=getWritableDatabase();ContentValues v=new ContentValues();v.put("status","PENDENTE");v.put("resolvido_em","");db.update("faltantes_pendentes",v,"chave=?",new String[]{chave});}
    public ArrayList<Pendente> listarPendentes(){ArrayList<Pendente> out=new ArrayList<>();SQLiteDatabase db=getReadableDatabase();Cursor c=db.rawQuery("SELECT chave,nome,categoria,un,faltante,necessario,entrega,consumo_datas,status FROM faltantes_pendentes WHERE status='PENDENTE' ORDER BY categoria ASC, faltante DESC",null);while(c.moveToNext()){Pendente p=new Pendente();p.chave=c.getString(0);p.nome=c.getString(1);p.categoria=c.getString(2);p.un=c.getString(3);p.faltante=c.getDouble(4);p.necessario=c.getDouble(5);p.entrega=c.getString(6);p.consumoDatas=c.getString(7);p.status=c.getString(8);out.add(p);}c.close();return out;}
    public String listarHistoricoTexto(){StringBuilder sb=new StringBuilder();SQLiteDatabase db=getReadableDatabase();Cursor c=db.rawQuery("SELECT criado,inicio,fim,entrega,analisados,candidatos,selecionados,ok,risco FROM historico ORDER BY id DESC LIMIT 30",null);while(c.moveToNext()){sb.append(c.getString(0)).append(" | ").append(c.getString(1)).append(" a ").append(c.getString(2)).append(" | Entrega ").append(c.getString(3)).append("\n").append("Analisados: ").append(c.getInt(4)).append(" | Candidatos: ").append(c.getInt(5)).append(" | Selecionados: ").append(c.getInt(6)).append(" | OK: ").append(c.getInt(7)).append(" | Risco: ").append(c.getString(8)).append("\n\n");}c.close();if(sb.length()==0)sb.append("Nenhuma análise IA salva ainda.");return sb.toString();}
    public static class Pendente{public String chave="",nome="",categoria="",un="",entrega="",consumoDatas="",status="";public double faltante,necessario;}
}
