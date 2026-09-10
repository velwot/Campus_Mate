package com.nor.campusmate;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.zip.*;
import org.json.*;

public class MainActivity extends Activity {
    static final String APP_DIR = "campusmate";
    static final int PURPLE = Color.rgb(79,70,229);
    static final int INK = Color.rgb(23,32,51);
    static final int MUTED = Color.rgb(102,112,133);
    static final int BG = Color.rgb(246,247,251);
    static final int LINE = Color.rgb(228,231,236);
    static final int SURFACE = Color.WHITE;

    DataStore store;
    LinearLayout root, content;
    TextView title;
    int currentTab = 0;

    int dp(float v){ return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
    TextView tv(String text, float sp, int color){ TextView t=new TextView(this); t.setText(text); t.setTextSize(sp); t.setTextColor(color); t.setGravity(Gravity.CENTER_VERTICAL); return t; }
    Button btn(String text){ Button b=new Button(this); b.setText(text); b.setAllCaps(false); b.setTextColor(PURPLE); return b; }
    EditText input(String hint){ EditText e=new EditText(this); e.setHint(hint); e.setTextSize(15); e.setSingleLine(false); e.setPadding(dp(12),dp(10),dp(12),dp(10)); return e; }
    LinearLayout row(){ LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    LinearLayout col(){ LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    GradientDrawable bg(int color, float radius){ GradientDrawable g=new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(radius)); g.setStroke(dp(1), LINE); return g; }

    @Override public void onCreate(Bundle b){ super.onCreate(b); store=new DataStore(this); buildShell(); showHome(); }

    void buildShell(){
        root=col(); root.setBackgroundColor(BG); setContentView(root);
        LinearLayout top=row(); top.setPadding(dp(18),dp(16),dp(14),dp(8));
        title=tv("CampusMate",22,INK); title.setTypeface(null,1);
        top.addView(title,new LinearLayout.LayoutParams(0,dp(48),1));
        TextView menu=tv("⋮",28,MUTED); menu.setGravity(Gravity.CENTER); top.addView(menu,new LinearLayout.LayoutParams(dp(44),dp(48)));
        menu.setOnClickListener(v->showMenu(menu)); root.addView(top);
        content=col(); content.setPadding(dp(16),0,dp(16),0);
        root.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        root.addView(navbar());
    }
    LinearLayout navbar(){
        LinearLayout bar=row(); bar.setPadding(dp(8),dp(5),dp(8),dp(8)); bar.setBackgroundColor(SURFACE);
        String[] labels={"Home","Subjects","Alerts","Calendar","Ideas"};
        for(int i=0;i<labels.length;i++){ final int idx=i; TextView x=tv(labels[i],12,MUTED); x.setGravity(Gravity.CENTER); x.setPadding(0,dp(4),0,dp(3)); bar.addView(x,new LinearLayout.LayoutParams(0,dp(58),1)); x.setOnClickListener(v->{currentTab=idx; switch(idx){case 0:showHome();break;case 1:showSubjects();break;case 2:showAlerts();break;case 3:showCalendar();break;default:showIdeas();}}); }
        return bar;
    }
    void clear(String newTitle){ content.removeAllViews(); title.setText(newTitle); }
    TextView section(String s){ TextView t=tv(s,17,INK); t.setTypeface(null,1); t.setPadding(0,dp(12),0,dp(8)); content.addView(t); return t; }
    TextView small(String s){ TextView t=tv(s,13,MUTED); t.setPadding(0,0,0,dp(6)); return t; }
    View card(View v){ LinearLayout wrap=new LinearLayout(this); wrap.setPadding(dp(14),dp(12),dp(14),dp(12)); wrap.setBackground(bg(SURFACE,14)); wrap.addView(v,new LinearLayout.LayoutParams(-1,-2)); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,0,0,dp(10)); content.addView(wrap,p); return wrap; }
    TextView label(String s){ TextView t=tv(s,12,MUTED); t.setTypeface(null,1); return t; }

    void showHome(){
        clear("CampusMate");
        TextView hero=tv("Stay ahead of college work.",25,INK); hero.setTypeface(null,1); content.addView(hero); content.addView(small("One place for subjects, notes, notices, deadlines and exam countdowns."));
        LinearLayout stats=row();
        addStat(stats,"Subjects",store.subjects.length(), "Open notes & notices");
        addStat(stats,"Due soon",store.countDue(7), "Next 7 days");
        addStat(stats,"Countdowns",store.countdowns.length(), "Exams & events");
        content.addView(stats,new LinearLayout.LayoutParams(-1,dp(92)));
        section("Upcoming submissions");
        JSONArray due=store.upcoming(30); if(due.length()==0) content.addView(small("No deadlines in the next 30 days."));
        for(int i=0;i<Math.min(4,due.length());i++) addDeadlineCard(due.optJSONObject(i),false);
        section("Exam countdowns");
        JSONArray cds=store.sortedCountdowns(); if(cds.length()==0) content.addView(small("Add a mid exam, end exam or any important date."));
        for(int i=0;i<Math.min(3,cds.length());i++) addCountdownCard(cds.optJSONObject(i));
        section("Quick capture");
        LinearLayout q=row(); Button idea=btn("+ Idea"); Button problem=btn("+ Problem"); q.addView(idea,new LinearLayout.LayoutParams(0,dp(52),1)); q.addView(problem,new LinearLayout.LayoutParams(0,dp(52),1)); content.addView(q); idea.setOnClickListener(v->addIdea("Idea")); problem.setOnClickListener(v->addIdea("Problem"));
    }
    void addStat(LinearLayout p,String num,int value,String sub){ LinearLayout box=col(); box.setPadding(dp(10),dp(8),dp(8),0); box.setBackground(bg(SURFACE,14)); TextView a=tv(String.valueOf(value),22,INK); a.setTypeface(null,1); box.addView(a); box.addView(tv(num,12,MUTED)); p.addView(box,new LinearLayout.LayoutParams(0,-1,1)); }

    void showSubjects(){
        clear("Subjects");
        LinearLayout h=row(); h.addView(small("Your subjects"),new LinearLayout.LayoutParams(0,dp(32),1)); Button add=btn("+ Subject"); h.addView(add,new LinearLayout.LayoutParams(dp(110),dp(48))); content.addView(h); add.setOnClickListener(v->addSubject());
        if(store.subjects.length()==0){ TextView e=tv("No subjects yet. Add your first subject.",15,MUTED); card(e); return; }
        for(int i=0;i<store.subjects.length();i++){ JSONObject s=store.subjects.optJSONObject(i); LinearLayout v=col(); TextView n=tv(s.optString("name"),18,INK); n.setTypeface(null,1); v.addView(n); v.addView(tv(s.optString("code",""),12,MUTED)); v.setPadding(dp(8),dp(6),dp(8),dp(6)); View c=card(v); c.setOnClickListener(x->showSubject(s)); }
    }
    void showSubject(JSONObject s){
        clear(s.optString("name")); Button back=btn("← Subjects"); content.addView(back); back.setOnClickListener(v->showSubjects());
        section("Notice / remember");
        Button addNotice=btn("+ Add notice"); content.addView(addNotice); addNotice.setOnClickListener(v->addNotice(s));
        String sid=s.optString("id"); JSONArray ns=store.noticesFor(sid); for(int i=0;i<ns.length();i++) addNoticeCard(ns.optJSONObject(i)); if(ns.length()==0) content.addView(small("Write formulas, reminders, teacher instructions, or anything you must remember."));
        section("Uploaded notes");
        Button upload=btn("+ Upload note"); content.addView(upload); upload.setOnClickListener(v->pickFile(sid));
        JSONArray fs=store.filesFor(sid); for(int i=0;i<fs.length();i++) addFileCard(fs.optJSONObject(i)); if(fs.length()==0) content.addView(small("PDFs, slides, documents and other study files appear here."));
    }

    void showAlerts(){
        clear("Alerts"); content.addView(small("Submission tracker • sorted by due date")); Button add=btn("+ Submission"); content.addView(add); add.setOnClickListener(v->addDeadline());
        JSONArray a=store.upcomingAll(); if(a.length()==0) content.addView(small("No submissions yet."));
        for(int i=0;i<a.length();i++) addDeadlineCard(a.optJSONObject(i),true);
    }
    void addDeadlineCard(JSONObject o, boolean actions){
        LinearLayout v=col(); row().getChildCount();
        LinearLayout top=row(); TextView n=tv(o.optString("title"),16,INK); n.setTypeface(null,1); top.addView(n,new LinearLayout.LayoutParams(0,dp(32),1)); TextView d=tv(daysLabel(o.optString("date")),13,statusColor(o.optString("date"))); d.setTypeface(null,1); top.addView(d); v.addView(top);
        v.addView(tv(o.optString("subject","General")+" • "+fmtDate(o.optString("date")),12,MUTED));
        if(!o.optString("details").isEmpty()) v.addView(tv(o.optString("details"),14,INK));
        if(actions){ Button done=btn(o.optBoolean("done")?"Mark active":"Mark done"); v.addView(done); done.setOnClickListener(x->{store.toggleDeadline(o.optString("id")); showAlerts();}); }
        View c=card(v); c.setOnLongClickListener(x->{confirmDelete("Delete submission?",()->{store.delete("deadlines",o.optString("id"));showAlerts();});return true;});
    }

    void showCalendar(){
        clear("Calendar"); content.addView(small("Countdowns to exams, breaks, practicals and important dates.")); Button add=btn("+ Countdown"); content.addView(add); add.setOnClickListener(v->addCountdown());
        JSONArray cds=store.sortedCountdowns(); if(cds.length()==0) content.addView(small("No countdowns yet.")); for(int i=0;i<cds.length();i++) addCountdownCard(cds.optJSONObject(i));
    }
    void addCountdownCard(JSONObject o){
        LinearLayout v=col(); LinearLayout top=row(); TextView n=tv(o.optString("title"),16,INK); n.setTypeface(null,1); top.addView(n,new LinearLayout.LayoutParams(0,dp(34),1)); TextView days=tv(daysLabel(o.optString("date")),13,statusColor(o.optString("date"))); days.setTypeface(null,1); top.addView(days); v.addView(top); v.addView(tv(fmtDate(o.optString("date")),12,MUTED));
        if(!o.optString("note").isEmpty()) v.addView(tv(o.optString("note"),14,INK)); Button del=btn("Delete"); v.addView(del); del.setOnClickListener(x->{store.delete("countdowns",o.optString("id"));showCalendar();}); card(v);
    }

    void showIdeas(){
        clear("Ideas & Problems"); content.addView(small("A scratchpad for the things you do not want to lose."));
        LinearLayout add=row(); Button i=btn("+ Idea"); Button p=btn("+ Problem"); add.addView(i,new LinearLayout.LayoutParams(0,dp(50),1));add.addView(p,new LinearLayout.LayoutParams(0,dp(50),1));content.addView(add);i.setOnClickListener(v->addIdea("Idea"));p.setOnClickListener(v->addIdea("Problem"));
        JSONArray a=store.sortedIdeas(); if(a.length()==0) content.addView(small("Capture project ideas, doubts, assignment problems, things to ask a professor, etc."));
        for(int x=0;x<a.length();x++){ JSONObject o=a.optJSONObject(x); LinearLayout v=col(); TextView t=tv(o.optString("type")+" • "+fmtDate(o.optString("date")),12,MUTED); t.setTypeface(null,1); v.addView(t); v.addView(tv(o.optString("text"),15,INK)); Button del=btn("Delete");v.addView(del);del.setOnClickListener(z->{store.delete("ideas",o.optString("id"));showIdeas();});card(v); }
    }

    void addSubject(){ final EditText n=input("Subject name"); final EditText c=input("Code (optional)"); LinearLayout l=col();l.addView(n);l.addView(c); new AlertDialog.Builder(this).setTitle("New subject").setView(l).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{if(n.getText().toString().trim().isEmpty())return;store.addSubject(n.getText().toString().trim(),c.getText().toString().trim());showSubjects();}).show(); }
    void addNotice(JSONObject s){ final EditText t=input("What should you remember?"); new AlertDialog.Builder(this).setTitle("Add notice").setView(t).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{String z=t.getText().toString().trim();if(!z.isEmpty()){store.addNotice(s.optString("id"),z);showSubject(s);}}).show(); }
    void addDeadline(){
        LinearLayout l=col(); EditText n=input("Submission title"); EditText sub=input("Subject"); EditText detail=input("Details / requirements (optional)"); EditText date=input("Due date: YYYY-MM-DD"); l.addView(n);l.addView(sub);l.addView(detail);l.addView(date);
        new AlertDialog.Builder(this).setTitle("New submission").setView(l).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{if(validDate(date.getText().toString())&&n.getText().toString().trim().length()>0){store.addDeadline(n.getText().toString().trim(),sub.getText().toString().trim(),detail.getText().toString().trim(),date.getText().toString().trim());showAlerts();}else toast("Use a valid date: YYYY-MM-DD");}).show();
    }
    void addCountdown(){ LinearLayout l=col(); EditText n=input("Name (e.g. Mid Exam)"); EditText date=input("Date: YYYY-MM-DD"); EditText note=input("Note (optional)");l.addView(n);l.addView(date);l.addView(note); new AlertDialog.Builder(this).setTitle("New countdown").setView(l).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{if(validDate(date.getText().toString())&&n.getText().toString().trim().length()>0){store.addCountdown(n.getText().toString().trim(),date.getText().toString().trim(),note.getText().toString().trim());showCalendar();}else toast("Use a valid date: YYYY-MM-DD");}).show(); }
    void addIdea(String type){ final EditText e=input("Write your "+type.toLowerCase()+"..."); new AlertDialog.Builder(this).setTitle("Quick "+type).setView(e).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{if(!e.getText().toString().trim().isEmpty()){store.addIdea(type,e.getText().toString().trim()); if(currentTab==4)showIdeas();}}).show(); }

    void addNoticeCard(JSONObject o){ LinearLayout v=col(); v.addView(tv(o.optString("text"),15,INK)); v.addView(tv(fmtDate(o.optString("date")),11,MUTED)); Button del=btn("Delete");v.addView(del);del.setOnClickListener(x->{store.delete("notices",o.optString("id"));showSubject(store.subjectById(o.optString("subjectId")));});card(v); }
    void addFileCard(JSONObject o){ LinearLayout v=col(); TextView t=tv("📎 "+o.optString("name"),15,INK); v.addView(t); v.addView(tv(fmtDate(o.optString("date")),11,MUTED)); Button open=btn("Open");v.addView(open);open.setOnClickListener(x->openFile(o.optString("path")));card(v); }
    void pickFile(String sid){ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");int idx=0;for(int j=0;j<store.subjects.length();j++){if(sid.equals(store.subjects.optJSONObject(j).optString("id"))){idx=j;break;}}startActivityForResult(i,2000+idx); }
    String activeFileSubject="";
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result==RESULT_OK&&data!=null&&data.getData()!=null&&request>=2000){int idx=request-2000; if(idx>=0&&idx<store.subjects.length()){activeFileSubject=store.subjects.optJSONObject(idx).optString("id");} Uri u=data.getData();try{String name=getName(u);File f=new File(store.filesDir(),"attachments");f.mkdirs();File out=new File(f,System.currentTimeMillis()+"_"+name.replaceAll("[^a-zA-Z0-9._-]","_"));copy(u,out);store.addFile(activeFileSubject,name,out.getAbsolutePath());showSubject(store.subjectById(activeFileSubject));}catch(Exception e){toast("Could not copy file");}}else if(request==9001&&result==RESULT_OK&&data!=null&&data.getData()!=null) store.exportTo(data.getData());else if(request==9002&&result==RESULT_OK&&data!=null&&data.getData()!=null) store.importFrom(data.getData());}
    String getName(Uri u){Cursor c=getContentResolver().query(u,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null);if(c!=null){try{if(c.moveToFirst())return c.getString(0);}finally{c.close();}}return "note";}
    void copy(Uri u,File out)throws Exception{InputStream in=getContentResolver().openInputStream(u);OutputStream os=new FileOutputStream(out);byte[] b=new byte[8192];int n;while((n=in.read(b))>0)os.write(b,0,n);in.close();os.close();}
    void openFile(String path){try{File f=new File(path);Uri u=Uri.parse("content://com.nor.campusmate.files/"+Uri.encode(f.getName()));Intent i=new Intent(Intent.ACTION_VIEW);i.setDataAndType(u,"*/*");i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(i);}catch(Exception e){toast("No app available to open this file");}}

    void showMenu(View anchor){ PopupMenu p=new PopupMenu(this,anchor);p.getMenu().add("Export backup");p.getMenu().add("Import backup");p.getMenu().add("About");p.setOnMenuItemClickListener(m->{String x=m.getTitle().toString();if(x.startsWith("Export")){startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/zip").putExtra(Intent.EXTRA_TITLE,"campusmate-backup.zip"),9001);}else if(x.startsWith("Import")){startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/zip"),9002);}else{new AlertDialog.Builder(this).setTitle("CampusMate").setMessage("Offline college organizer\n\nYour subjects, notices, deadlines, countdowns and quick notes stay on this device. Backup creates a ZIP you can move to another phone.").setPositiveButton("OK",null).show();}return true;});p.show();}
    void confirmDelete(String msg,final Runnable yes){new AlertDialog.Builder(this).setTitle(msg).setNegativeButton("Cancel",null).setPositiveButton("Delete",(d,w)->yes.run()).show();}
    int statusColor(String date){long days=store.daysUntil(date);return days<0?Color.rgb(217,45,32):(days<=3?Color.rgb(181,71,8):Color.rgb(2,122,72));}
    String daysLabel(String date){long d=store.daysUntil(date);if(d<0)return Math.abs(d)+"d overdue";if(d==0)return "Today";if(d==1)return "Tomorrow";return d+" days";}
    String fmtDate(String d){try{return new SimpleDateFormat("dd MMM yyyy",Locale.US).format(new SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(d));}catch(Exception e){return d;}}
    boolean validDate(String d){try{SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd",Locale.US);f.setLenient(false);f.parse(d);return true;}catch(Exception e){return false;}}
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}

class DataStore {
    final MainActivity a; final android.content.SharedPreferences p; final File root;
    JSONArray subjects,notices,files,deadlines,countdowns,ideas;
    DataStore(MainActivity x){a=x;p=x.getSharedPreferences("data",0);root=new File(x.getFilesDir(),MainActivity.APP_DIR);root.mkdirs();load();}
    File filesDir(){File f=new File(root,"attachments");f.mkdirs();return f;}
    JSONArray arr(String k){try{return new JSONArray(p.getString(k,"[]"));}catch(Exception e){return new JSONArray();}}
    void load(){subjects=arr("subjects");notices=arr("notices");files=arr("files");deadlines=arr("deadlines");countdowns=arr("countdowns");ideas=arr("ideas");}
    void save(){p.edit().putString("subjects",subjects.toString()).putString("notices",notices.toString()).putString("files",files.toString()).putString("deadlines",deadlines.toString()).putString("countdowns",countdowns.toString()).putString("ideas",ideas.toString()).apply();}
    String id(){return UUID.randomUUID().toString();}
    void addSubject(String n,String c){subjects.put(o("id",id(),"name",n,"code",c));save();}
    void addNotice(String sid,String text){notices.put(o("id",id(),"subjectId",sid,"text",text,"date",today()));save();}
    void addFile(String sid,String name,String path){files.put(o("id",id(),"subjectId",sid,"name",name,"path",path,"date",today()));save();}
    void addDeadline(String t, String s, String det, String d) {
    deadlines.put(
        o("id", id(),
          "title", t,
          "subject", s,
          "details", det,
          "date", d,
          "done", false)
    );
    save();
}

JSONObject o(Object... kv) {
    JSONObject x = new JSONObject();

    for (int i = 0; i + 1 < kv.length; i += 2) {
        try {
            x.put(String.valueOf(kv[i]), kv[i + 1]);
        } catch (Exception e) {
            // Ignore malformed entry
        }
    }

    return x;
}

    void addCountdown(String t,String d,String n){countdowns.put(o("id",id(),"title",t,"date",d,"note",n));save();}
    void addIdea(String type,String text){ideas.put(o("id",id(),"type",type,"text",text,"date",today()));save();}
    JSONObject o(String... kv){JSONObject x=new JSONObject();for(int i=0;i+1<kv.length;i+=2)try{x.put(kv[i],kv[i+1]);}catch(Exception e){}return x;}
    String today(){return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());}
    long daysUntil(String d){try{long t=new SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(d).getTime();long n=new SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(today()).getTime();return Math.round((t-n)/86400000.0);}catch(Exception e){return 9999;}}
    int countDue(int n){int c=0;for(int i=0;i<deadlines.length();i++){JSONObject o=deadlines.optJSONObject(i);if(!o.optBoolean("done")&&daysUntil(o.optString("date"))<=n&&daysUntil(o.optString("date"))>=0)c++;}return c;}
    JSONArray upcoming(int days){JSONArray out=new JSONArray();for(int i=0;i<deadlines.length();i++){JSONObject o=deadlines.optJSONObject(i);long d=daysUntil(o.optString("date"));if(!o.optBoolean("done")&&d>=0&&d<=days)out.put(o);}sortDate(out);return out;}
    JSONArray upcomingAll(){JSONArray out=new JSONArray();for(int i=0;i<deadlines.length();i++){JSONObject o=deadlines.optJSONObject(i);if(!o.optBoolean("done"))out.put(o);}sortDate(out);for(int i=0;i<deadlines.length();i++){JSONObject o=deadlines.optJSONObject(i);if(o.optBoolean("done"))out.put(o);}return out;}
    JSONArray sortedCountdowns(){JSONArray out=new JSONArray();for(int i=0;i<countdowns.length();i++)out.put(countdowns.optJSONObject(i));sortDate(out);return out;}
    JSONArray sortedIdeas(){JSONArray out=new JSONArray();for(int i=ideas.length()-1;i>=0;i--)out.put(ideas.optJSONObject(i));return out;}
    JSONArray noticesFor(String sid){JSONArray out=new JSONArray();for(int i=0;i<notices.length();i++)if(sid.equals(notices.optJSONObject(i).optString("subjectId")))out.put(notices.optJSONObject(i));return out;}
    JSONArray filesFor(String sid){JSONArray out=new JSONArray();for(int i=0;i<files.length();i++)if(sid.equals(files.optJSONObject(i).optString("subjectId")))out.put(files.optJSONObject(i));return out;}
    JSONObject subjectById(String sid){for(int i=0;i<subjects.length();i++)if(sid.equals(subjects.optJSONObject(i).optString("id")))return subjects.optJSONObject(i);return new JSONObject();}
    void sortDate(JSONArray x){ArrayList<JSONObject> l=new ArrayList<>();for(int i=0;i<x.length();i++)l.add(x.optJSONObject(i));Collections.sort(l,(u,v)->u.optString("date").compareTo(v.optString("date")));JSONArray z=new JSONArray();for(JSONObject o:l)z.put(o);for(int i=0;i<z.length();i++)try{x.put(i,z.get(i));}catch(Exception e){}}
    void toggleDeadline(String id){for(int i=0;i<deadlines.length();i++)if(id.equals(deadlines.optJSONObject(i).optString("id"))){try{JSONObject o=deadlines.getJSONObject(i);o.put("done",!o.optBoolean("done"));}catch(Exception e){}break;}save();}
    void delete(String k,String id){JSONArray x=arr(k);JSONArray z=new JSONArray();for(int i=0;i<x.length();i++)if(!id.equals(x.optJSONObject(i).optString("id")))z.put(x.optJSONObject(i));try{p.edit().putString(k,z.toString()).apply();load();}catch(Exception e){}}
    void exportTo(Uri dest){try{File tmp=new File(root,"export.zip");ZipOutputStream zos=new ZipOutputStream(new FileOutputStream(tmp));JSONObject data=new JSONObject();data.put("version",1);data.put("subjects",subjects);data.put("notices",notices);data.put("files",files);data.put("deadlines",deadlines);data.put("countdowns",countdowns);data.put("ideas",ideas);byte[] b=data.toString(2).getBytes("UTF-8");zos.putNextEntry(new ZipEntry("data.json"));zos.write(b);zos.closeEntry();for(int i=0;i<files.length();i++){JSONObject f=files.optJSONObject(i);File src=new File(f.optString("path"));if(src.exists()){zos.putNextEntry(new ZipEntry("attachments/"+src.getName()));FileInputStream in=new FileInputStream(src);byte[] buf=new byte[8192];int n;while((n=in.read(buf))>0)zos.write(buf,0,n);in.close();zos.closeEntry();}}zos.close();OutputStream out=a.getContentResolver().openOutputStream(dest);InputStream in=new FileInputStream(tmp);byte[] buf=new byte[8192];int n;while((n=in.read(buf))>0)out.write(buf,0,n);in.close();out.close();a.toast("Backup exported");}catch(Exception e){a.toast("Export failed");}}
    void importFrom(Uri src){try{File tmp=new File(root,"import.zip");InputStream in=a.getContentResolver().openInputStream(src);FileOutputStream out=new FileOutputStream(tmp);byte[] b=new byte[8192];int n;while((n=in.read(b))>0)out.write(b,0,n);in.close();out.close();ZipInputStream zin=new ZipInputStream(new FileInputStream(tmp));ZipEntry e;JSONObject data=null;File att=filesDir();while((e=zin.getNextEntry())!=null){if(e.getName().equals("data.json")){ByteArrayOutputStream bo=new ByteArrayOutputStream();while((n=zin.read(b))>0)bo.write(b,0,n);data=new JSONObject(new String(bo.toByteArray(),"UTF-8"));}else if(e.getName().startsWith("attachments/")&&!e.isDirectory()){File f=new File(att,new File(e.getName()).getName());FileOutputStream fo=new FileOutputStream(f);while((n=zin.read(b))>0)fo.write(b,0,n);fo.close();}}zin.close();if(data==null)throw new Exception();subjects=data.optJSONArray("subjects");notices=data.optJSONArray("notices");files=data.optJSONArray("files");deadlines=data.optJSONArray("deadlines");countdowns=data.optJSONArray("countdowns");ideas=data.optJSONArray("ideas");if(subjects==null)subjects=new JSONArray();if(notices==null)notices=new JSONArray();if(files==null)files=new JSONArray();if(deadlines==null)deadlines=new JSONArray();if(countdowns==null)countdowns=new JSONArray();if(ideas==null)ideas=new JSONArray();for(int i=0;i<files.length();i++){JSONObject f=files.optJSONObject(i);String old=f.optString("path");File target=new File(att,new File(old).getName());f.put("path",target.getAbsolutePath());}save();a.toast("Backup imported");a.currentTab=0;a.showHome();}catch(Exception ex){a.toast("Import failed");}}
}
