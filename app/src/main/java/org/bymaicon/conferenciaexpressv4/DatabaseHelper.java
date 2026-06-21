package org.bymaicon.conferenciaexpressv4;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import java.util.*;

public class DatabaseHelper extends SQLiteOpenHelper {
    public DatabaseHelper(Context c){ super(c,"conferencia_express_ia.db",null,4); }
    @Override public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE IF NOT EXISTS equivalencias(id INTEGER PRIMARY KEY AUTOINCREMENT, analitico TEXT UNIQUE, oc TEXT, criado_em INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS pendencias(id INTEGER PRIMARY KEY AUTOINCREMENT, item TEXT, categoria TEXT, faltante REAL, unidade TEXT, consumo TEXT, entrega TEXT, status TEXT, criado_em INTEGER, resolvido_em INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS eventos(id INTEGER PRIMARY KEY AUTOINCREMENT, tipo TEXT, titulo TEXT, detalhe TEXT, criado_em INTEGER)");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldV,int newV){ onCreate(db); }
    public void salvarEquivalencia(String analitico,String oc){
        ContentValues v=new ContentValues(); v.put("analitico",analitico); v.put("oc",oc); v.put("criado_em",System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("equivalencias",null,v,SQLiteDatabase.CONFLICT_REPLACE);
    }
    public String equivalencia(String analitico){
        Cursor c=getReadableDatabase().rawQuery("SELECT oc FROM equivalencias WHERE analitico=?",new String[]{analitico});
        try{ if(c.moveToFirst()) return c.getString(0); return ""; } finally { c.close(); }
    }
    public void salvarPendencia(String item,String categoria,double faltante,String unidade,String consumo,String entrega){
        ContentValues v=new ContentValues(); v.put("item",item); v.put("categoria",categoria); v.put("faltante",faltante); v.put("unidade",unidade); v.put("consumo",consumo); v.put("entrega",entrega); v.put("status","PENDENTE"); v.put("criado_em",System.currentTimeMillis());
        getWritableDatabase().insert("pendencias",null,v);
    }
    public ArrayList<String> pendenciasAbertas(){
        ArrayList<String> out=new ArrayList<>();
        Cursor c=getReadableDatabase().rawQuery("SELECT item,categoria,faltante,unidade,consumo,entrega FROM pendencias WHERE status='PENDENTE' ORDER BY consumo,item",null);
        try{ while(c.moveToNext()) out.add(c.getString(1)+" | "+c.getString(0)+" | falta "+c.getDouble(2)+" "+c.getString(3)+" | consumo "+c.getString(4)+" | entrega "+c.getString(5)); } finally { c.close(); }
        return out;
    }
    public void inserirEvento(String tipo,String titulo,String detalhe){
        ContentValues v=new ContentValues(); v.put("tipo",tipo); v.put("titulo",titulo); v.put("detalhe",detalhe); v.put("criado_em",System.currentTimeMillis());
        getWritableDatabase().insert("eventos",null,v);
    }
}
