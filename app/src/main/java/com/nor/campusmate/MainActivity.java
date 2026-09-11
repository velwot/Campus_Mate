package com.nor.campusmate;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class MainActivity extends Activity {
    static final String APP_DIR = "campusmate";
    static final int PURPLE = Color.rgb(79, 70, 229);
    static final int INK = Color.rgb(23, 32, 51);
    static final int MUTED = Color.rgb(102, 112, 133);
    static final int BG = Color.rgb(246, 247, 251);
    static final int LINE = Color.rgb(228, 231, 236);
    static final int SURFACE = Color.WHITE;

    static final int REQ_PICK_PDF = 3001;
    static final int REQ_PICK_IMAGE = 3002;
    static final int REQ_TAKE_PHOTO = 3003;
    static final int REQ_CAMERA_PERMISSION = 3004;

    DataStore store;
    LinearLayout root, content;
    TextView title;
    int currentTab = 0;
    String currentSubjectView = "notice";
    Map<String, Boolean> chapterExpanded = new HashMap<>();
    String pendingClassNoteSubject = "";
    String pendingClassNoteChapter = "";
    Uri pendingCameraUri = null;

    int dp(float v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }
    TextView tv(String text, float sp, int color) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }
    Button btn(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(PURPLE);
        return b;
    }
    EditText input(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextSize(15);
        e.setSingleLine(false);
        e.setPadding(dp(12), dp(10), dp(12), dp(10));
        return e;
    }
    LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }
    LinearLayout col() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }
    GradientDrawable bg(int color, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radius));
        g.setStroke(dp(1), LINE);
        return g;
    }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        store = new DataStore(this);
        if (b != null) {
            pendingClassNoteSubject = b.getString("pendingSubject", "");
            pendingClassNoteChapter = b.getString("pendingChapter", "");
            pendingCameraUri = b.getParcelable("pendingCameraUri");
            currentTab = b.getInt("currentTab", 0);
            currentSubjectView = b.getString("currentSubjectView", "notice");
        }
        buildShell();
        showHome();
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putString("pendingSubject", pendingClassNoteSubject);
        out.putString("pendingChapter", pendingClassNoteChapter);
        out.putParcelable("pendingCameraUri", pendingCameraUri);
        out.putInt("currentTab", currentTab);
        out.putString("currentSubjectView", currentSubjectView);
    }

    void buildShell() {
        root = col();
        root.setBackgroundColor(BG);
        setContentView(root);

        LinearLayout top = row();
        top.setPadding(dp(18), dp(16), dp(14), dp(8));
        title = tv("CampusMate", 22, INK);
        title.setTypeface(null, 1);
        top.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1));
        TextView menu = tv("⋮", 28, MUTED);
        menu.setGravity(Gravity.CENTER);
        top.addView(menu, new LinearLayout.LayoutParams(dp(44), dp(48)));
        menu.setOnClickListener(v -> showMenu(menu));
        root.addView(top);

        content = col();
        content.setPadding(dp(16), 0, dp(16), 0);
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(navbar());
    }

    LinearLayout navbar() {
        LinearLayout bar = row();
        bar.setPadding(dp(8), dp(5), dp(8), dp(8));
        bar.setBackgroundColor(SURFACE);
        String[] labels = {"Home", "Subjects", "Alerts", "Calendar", "Ideas"};
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            TextView x = tv(labels[i], 12, MUTED);
            x.setGravity(Gravity.CENTER);
            x.setPadding(0, dp(4), 0, dp(3));
            bar.addView(x, new LinearLayout.LayoutParams(0, dp(58), 1));
            x.setOnClickListener(v -> {
                currentTab = idx;
                switch (idx) {
                    case 0: showHome(); break;
                    case 1: showSubjects(); break;
                    case 2: showAlerts(); break;
                    case 3: showCalendar(); break;
                    default: showIdeas(); break;
                }
            });
        }
        return bar;
    }

    void clear(String newTitle) {
        content.removeAllViews();
        title.setText(newTitle);
    }

    TextView section(String s) {
        TextView t = tv(s, 17, INK);
        t.setTypeface(null, 1);
        t.setPadding(0, dp(12), 0, dp(8));
        content.addView(t);
        return t;
    }

    TextView small(String s) {
        TextView t = tv(s, 13, MUTED);
        t.setPadding(0, 0, 0, dp(6));
        return t;
    }

    View card(View v) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setPadding(dp(14), dp(12), dp(14), dp(12));
        wrap.setBackground(bg(SURFACE, 14));
        wrap.addView(v, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, 0, 0, dp(10));
        content.addView(wrap, p);
        return wrap;
    }

    void showHome() {
        clear("CampusMate");
        TextView hero = tv("Stay ahead of college work.", 25, INK);
        hero.setTypeface(null, 1);
        content.addView(hero);
        content.addView(small("One place for subjects, notes, notices, deadlines and exam countdowns."));

        LinearLayout stats = row();
        addStat(stats, "Subjects", store.subjects.length(), "Open notes & notices");
        addStat(stats, "Due soon", store.countDue(7), "Next 7 days");
        addStat(stats, "Countdowns", store.countdowns.length(), "Exams & events");
        content.addView(stats, new LinearLayout.LayoutParams(-1, dp(92)));

        section("Upcoming submissions");
        JSONArray due = store.upcoming(30);
        if (due.length() == 0) content.addView(small("No deadlines in the next 30 days."));
        for (int i = 0; i < Math.min(4, due.length()); i++) addDeadlineCard(due.optJSONObject(i), false);

        section("Exam countdowns");
        JSONArray cds = store.sortedCountdowns();
        if (cds.length() == 0) content.addView(small("Add a mid exam, end exam or any important date."));
        for (int i = 0; i < Math.min(3, cds.length()); i++) addCountdownCard(cds.optJSONObject(i));

        section("Quick capture");
        LinearLayout q = row();
        Button idea = btn("+ Idea");
        Button problem = btn("+ Problem");
        q.addView(idea, new LinearLayout.LayoutParams(0, dp(52), 1));
        q.addView(problem, new LinearLayout.LayoutParams(0, dp(52), 1));
        content.addView(q);
        idea.setOnClickListener(v -> addIdea("Idea"));
        problem.setOnClickListener(v -> addIdea("Problem"));
    }

    void addStat(LinearLayout p, String num, int value, String sub) {
        LinearLayout box = col();
        box.setPadding(dp(10), dp(8), dp(8), 0);
        box.setBackground(bg(SURFACE, 14));
        TextView a = tv(String.valueOf(value), 22, INK);
        a.setTypeface(null, 1);
        box.addView(a);
        box.addView(tv(num, 12, MUTED));
        p.addView(box, new LinearLayout.LayoutParams(0, -1, 1));
    }

    void showSubjects() {
        clear("Subjects");
        LinearLayout h = row();
        h.addView(small("Your subjects"), new LinearLayout.LayoutParams(0, dp(32), 1));
        Button add = btn("+ Subject");
        h.addView(add, new LinearLayout.LayoutParams(dp(110), dp(48)));
        content.addView(h);
        add.setOnClickListener(v -> addSubject());

        if (store.subjects.length() == 0) {
            TextView e = tv("No subjects yet. Add your first subject.", 15, MUTED);
            card(e);
            return;
        }

        for (int i = 0; i < store.subjects.length(); i++) {
            JSONObject s = store.subjects.optJSONObject(i);
            LinearLayout v = col();
            TextView n = tv(s.optString("name"), 18, INK);
            n.setTypeface(null, 1);
            v.addView(n);
            v.addView(tv(s.optString("code", ""), 12, MUTED));
            v.setPadding(dp(8), dp(6), dp(8), dp(6));
            View c = card(v);
            c.setOnClickListener(x -> showSubject(s));
        }
    }

    void showSubject(JSONObject s) {
        clear(s.optString("name"));
        Button back = btn("← Subjects");
        content.addView(back);
        back.setOnClickListener(v -> showSubjects());

        LinearLayout tabs = row();
        Button noticeBtn = btn("Notice");
        Button classBtn = btn("Class Notes");
        if ("classnotes".equals(currentSubjectView)) {
            noticeBtn.setTextColor(MUTED);
            classBtn.setTextColor(PURPLE);
        } else {
            noticeBtn.setTextColor(PURPLE);
            classBtn.setTextColor(MUTED);
        }
        tabs.addView(noticeBtn, new LinearLayout.LayoutParams(0, dp(48), 1));
        tabs.addView(classBtn, new LinearLayout.LayoutParams(0, dp(48), 1));
        noticeBtn.setOnClickListener(v -> { currentSubjectView = "notice"; showSubject(s); });
        classBtn.setOnClickListener(v -> { currentSubjectView = "classnotes"; showSubject(s); });
        content.addView(tabs);

        if ("classnotes".equals(currentSubjectView)) {
            showClassNotesSection(s);
        } else {
            showNoticeSection(s);
        }
    }

    void showNoticeSection(JSONObject s) {
        String sid = s.optString("id");
        section("Notice / remember");
        Button addNotice = btn("+ Add notice");
        content.addView(addNotice);
        addNotice.setOnClickListener(v -> addNotice(s));
        JSONArray ns = store.noticesFor(sid);
        for (int i = 0; i < ns.length(); i++) addNoticeCard(ns.optJSONObject(i));
        if (ns.length() == 0) content.addView(small("Write formulas, reminders, teacher instructions, or anything you must remember."));

        section("Uploaded notes");
        Button upload = btn("+ Upload note");
        content.addView(upload);
        upload.setOnClickListener(v -> pickFile(sid));
        JSONArray fs = store.filesFor(sid);
        for (int i = 0; i < fs.length(); i++) addFileCard(fs.optJSONObject(i));
        if (fs.length() == 0) content.addView(small("PDFs, slides, documents and other study files appear here."));
    }

    void showClassNotesSection(JSONObject s) {
        String sid = s.optString("id");
        section("Class Notes");
        Button addChapter = btn("+ Add Chapter");
        content.addView(addChapter);
        addChapter.setOnClickListener(v -> addChapter(sid));

        JSONArray chapters = store.chaptersForSubject(sid);
        if (chapters.length() == 0) {
            content.addView(small("Create chapters and keep lecture notes organized by topic."));
            return;
        }
        for (int i = 0; i < chapters.length(); i++) addChapterCard(s, chapters.optJSONObject(i));
    }

    void addChapterCard(JSONObject subject, JSONObject chapter) {
        String sid = subject.optString("id");
        String cid = chapter.optString("id");
        boolean expanded = chapterExpanded.containsKey(cid) ? chapterExpanded.get(cid) : true;

        LinearLayout block = col();
        LinearLayout titleRow = row();
        titleRow.setPadding(dp(8), dp(8), dp(8), dp(4));
        Button toggle = btn(expanded ? "▾" : "▸");
        toggle.setTextColor(MUTED);
        toggle.setPadding(dp(6), 0, dp(6), 0);
        titleRow.addView(toggle, new LinearLayout.LayoutParams(dp(36), dp(42)));

        TextView titleView = tv(chapter.optString("name", "Chapter") + " (" + store.notesForChapter(cid).length() + " notes)", 16, INK);
        titleView.setTypeface(null, 1);
        titleRow.addView(titleView, new LinearLayout.LayoutParams(0, -2, 1));
        titleRow.setOnClickListener(v -> { chapterExpanded.put(cid, !expanded); showSubject(subject); });
        block.addView(titleRow);

        LinearLayout actions = row();
        Button addNote = btn("+ note");
        Button rename = btn("Rename");
        Button delete = btn("Delete");
        actions.addView(addNote, new LinearLayout.LayoutParams(0, dp(48), 1));
        actions.addView(rename, new LinearLayout.LayoutParams(0, dp(48), 1));
        actions.addView(delete, new LinearLayout.LayoutParams(0, dp(48), 1));
        block.addView(actions);
        addNote.setOnClickListener(v -> showAddClassNoteDialog(sid, cid));
        rename.setOnClickListener(v -> renameChapterDialog(cid, subject));
        delete.setOnClickListener(v -> confirmDelete("Delete chapter and notes?", () -> { store.deleteChapter(cid); showSubject(subject); }));

        if (expanded) {
            JSONArray notes = store.notesForChapterSorted(cid);
            if (notes.length() == 0) {
                TextView empty = tv("No notes yet in this chapter.", 13, MUTED);
                empty.setPadding(dp(12), dp(8), 0, dp(4));
                block.addView(empty);
            } else {
                for (int i = 0; i < notes.length(); i++) addClassNoteCard(subject, notes.optJSONObject(i));
            }
        }
        card(block);
    }

    void addClassNoteCard(JSONObject subject, JSONObject note) {
        LinearLayout v = col();
        LinearLayout top = row();
        top.setPadding(0, 0, 0, dp(4));
        TextView icon = tv(noteTypeLabel(note.optString("type")), 13, PURPLE);
        icon.setTypeface(null, 1);
        icon.setPadding(dp(8), dp(8), dp(8), dp(8));
        icon.setBackground(bg(SURFACE, 10));
        top.addView(icon, new LinearLayout.LayoutParams(dp(54), dp(40)));

        LinearLayout meta = col();
        TextView name = tv(note.optString("name", "Untitled note"), 15, INK);
        name.setTypeface(null, 1);
        meta.addView(name);
        meta.addView(tv(noteTypeLabel(note.optString("type")) + " • " + fmtDate(note.optString("date")), 12, MUTED));
        top.addView(meta, new LinearLayout.LayoutParams(0, -2, 1));
        v.addView(top);

        if (!note.optString("description", "").trim().isEmpty()) {
            v.addView(tv(note.optString("description"), 13, INK));
        }

        LinearLayout actions = row();
        Button open = btn("Open");
        Button rename = btn("Rename");
        Button move = btn("Move");
        Button del = btn("Delete");
        actions.addView(open, new LinearLayout.LayoutParams(0, dp(42), 1));
        actions.addView(rename, new LinearLayout.LayoutParams(0, dp(42), 1));
        actions.addView(move, new LinearLayout.LayoutParams(0, dp(42), 1));
        actions.addView(del, new LinearLayout.LayoutParams(0, dp(42), 1));
        v.addView(actions);

        open.setOnClickListener(x -> openFile(note.optString("path")));
        rename.setOnClickListener(x -> renameNoteDialog(note));
        move.setOnClickListener(x -> moveNoteDialog(subject.optString("id"), note));
        del.setOnClickListener(x -> confirmDelete("Delete this note?", () -> { store.deleteClassNote(note.optString("id")); showSubject(subject); }));
        card(v);
    }

    void addChapter(String sid) {
        final EditText e = input("Chapter name");
        new AlertDialog.Builder(this)
            .setTitle("Add chapter")
            .setView(e)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", (d, w) -> {
                String name = e.getText().toString().trim();
                if (!name.isEmpty()) {
                    store.addChapter(sid, name);
                    showSubject(store.subjectById(sid));
                }
            })
            .show();
    }

    void renameChapterDialog(String chapterId, JSONObject subject) {
        JSONObject c = store.chapterById(chapterId);
        final EditText e = input("New chapter name");
        e.setText(c.optString("name"));
        new AlertDialog.Builder(this)
            .setTitle("Rename chapter")
            .setView(e)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", (d, w) -> {
                String name = e.getText().toString().trim();
                if (!name.isEmpty()) {
                    store.renameChapter(chapterId, name);
                    showSubject(subject);
                }
            })
            .show();
    }

    void renameNoteDialog(JSONObject note) {
        final EditText e = input("New note name");
        e.setText(note.optString("name"));
        new AlertDialog.Builder(this)
            .setTitle("Rename note")
            .setView(e)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", (d, w) -> {
                String name = e.getText().toString().trim();
                if (!name.isEmpty()) {
                    store.renameClassNote(note.optString("id"), name);
                    showSubject(store.subjectById(note.optString("subjectId")));
                }
            })
            .show();
    }

    void moveNoteDialog(String sid, JSONObject note) {
        JSONArray chapters = store.chaptersForSubject(sid);
        if (chapters.length() == 0) {
            toast("Add a chapter first.");
            return;
        }
        final String[] choices = new String[chapters.length()];
        final String[] ids = new String[chapters.length()];
        for (int i = 0; i < chapters.length(); i++) {
            JSONObject c = chapters.optJSONObject(i);
            ids[i] = c.optString("id");
            choices[i] = c.optString("name");
        }
        new AlertDialog.Builder(this)
            .setTitle("Move note to chapter")
            .setItems(choices, (d, idx) -> { store.moveClassNote(note.optString("id"), ids[idx]); showSubject(store.subjectById(sid)); })
            .show();
    }

    void showAddClassNoteDialog(String sid, String chapterId) {
        if (chapterId == null || chapterId.trim().isEmpty()) {
            JSONArray chapters = store.chaptersForSubject(sid);
            if (chapters.length() == 0) { addChapter(sid); return; }
            final String[] choices = new String[chapters.length()];
            final String[] ids = new String[chapters.length()];
            for (int i = 0; i < chapters.length(); i++) {
                JSONObject c = chapters.optJSONObject(i);
                ids[i] = c.optString("id");
                choices[i] = c.optString("name");
            }
            new AlertDialog.Builder(this)
                .setTitle("Select chapter")
                .setItems(choices, (d, idx) -> showAddClassNoteDialog(sid, ids[idx]))
                .show();
            return;
        }

        final String[] options = {"Take Photo", "Choose Image", "Choose PDF"};
        new AlertDialog.Builder(this)
            .setTitle("Add Class Note")
            .setItems(options, (d, idx) -> {
                if (idx == 0) requestCameraCapture(sid, chapterId);
                else if (idx == 1) pickClassNoteDocument(sid, chapterId, "image");
                else pickClassNoteDocument(sid, chapterId, "pdf");
            })
            .show();
    }

    void pickClassNoteDocument(String sid, String chapterId, String type) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        if ("pdf".equals(type)) {
            i.setType("application/pdf");
        } else {
            i.setType("image/*");
            i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/jpeg", "image/png", "image/webp", "image/gif", "image/bmp"});
        }
        pendingClassNoteSubject = sid;
        pendingClassNoteChapter = chapterId;
        startActivityForResult(i, "pdf".equals(type) ? REQ_PICK_PDF : REQ_PICK_IMAGE);
    }

    void requestCameraCapture(String sid, String chapterId) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            pendingClassNoteSubject = sid;
            pendingClassNoteChapter = chapterId;
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQ_CAMERA_PERMISSION);
            return;
        }
        launchCamera(sid, chapterId);
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera(pendingClassNoteSubject, pendingClassNoteChapter);
            } else {
                toast("Camera permission denied. You can still use gallery or PDF selection.");
            }
        }
    }

    void launchCamera(String sid, String chapterId) {
        File dir = new File(getFilesDir(), "campusmate/camera");
        dir.mkdirs();
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File photoFile = new File(dir, "classnote_" + stamp + ".jpg");
        try {
            pendingCameraUri = FileProvider.getUriForFile(this, "com.nor.campusmate.fileprovider", photoFile);
        } catch (Exception e) {
            toast("Could not create photo file.");
            return;
        }
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, pendingCameraUri);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        pendingClassNoteSubject = sid;
        pendingClassNoteChapter = chapterId;
        startActivityForResult(intent, REQ_TAKE_PHOTO);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == 9001 && result == RESULT_OK && data != null && data.getData() != null) {
            store.exportTo(data.getData());
            return;
        }
        if (request == 9002 && result == RESULT_OK && data != null && data.getData() != null) {
            store.importFrom(data.getData());
            return;
        }
        if (result != RESULT_OK) return;
        if (request >= 2000 && request < 3000 && data != null && data.getData() != null) {
            String sid = store.subjects.optJSONObject(Math.max(0, request - 2000) % Math.max(1, store.subjects.length())).optString("id");
            addPickedDocument(data.getData(), sid, "", "*/*", "file");
            return;
        }
        if (request == REQ_PICK_PDF && data != null && data.getData() != null) {
            addPickedDocument(data.getData(), pendingClassNoteSubject, pendingClassNoteChapter, "application/pdf", "pdf");
            return;
        }
        if (request == REQ_PICK_IMAGE && data != null && data.getData() != null) {
            addPickedDocument(data.getData(), pendingClassNoteSubject, pendingClassNoteChapter, "image/*", "image");
            return;
        }
        if (request == REQ_TAKE_PHOTO && pendingCameraUri != null) {
            addPickedDocument(pendingCameraUri, pendingClassNoteSubject, pendingClassNoteChapter, "image/jpeg", "image");
        }
    }

    void addPickedDocument(Uri source, String sid, String chapterId, String mimeFallback, String kind) {
        if (sid == null || sid.trim().isEmpty()) {
            toast("Select a subject first.");
            return;
        }

        String name = getName(source);
        String mime = queryMimeType(source, mimeFallback);
        String safeName = ensureFileExtension(name, mime, kind);
        File saved = copyUriToPrivateStorage(source, safeName);
        if (saved == null) {
            toast("Could not save note.");
            return;
        }

        if (chapterId == null || chapterId.trim().isEmpty()) {
            JSONArray chapters = store.chaptersForSubject(sid);
            if (chapters.length() == 0) {
                addChapter(sid);
                return;
            }
            final String[] choices = new String[chapters.length()];
            final String[] ids = new String[chapters.length()];
            for (int i = 0; i < chapters.length(); i++) {
                JSONObject c = chapters.optJSONObject(i);
                ids[i] = c.optString("id");
                choices[i] = c.optString("name");
            }
            new AlertDialog.Builder(this)
                .setTitle("Select chapter")
                .setItems(choices, (d, idx) -> saveClassNote(sid, ids[idx], safeName, saved.getAbsolutePath(), mime, ""))
                .show();
            return;
        }

        saveClassNote(sid, chapterId, safeName, saved.getAbsolutePath(), mime, "");
    }

    void saveClassNote(String sid, String chapterId, String name, String path, String type, String description) {
        JSONObject note = store.addClassNote(sid, chapterId, name, path, type, description);
        JSONObject subject = store.subjectById(sid);
        if (subject != null && subject.length() > 0) showSubject(subject); else toast("Saved.");
        renameNoteDialog(note);
    }

    String ensureFileExtension(String name, String mimeType, String kind) {
        String rootName = (name == null || name.trim().isEmpty()) ? ("class-note" + ("pdf".equals(kind) ? ".pdf" : ".jpg")) : name;
        String lower = rootName.toLowerCase(Locale.US);
        if (mimeType != null && mimeType.contains("pdf") && !lower.endsWith(".pdf")) rootName += ".pdf";
        if (mimeType != null && mimeType.startsWith("image/") && !lower.matches(".*\\.(jpg|jpeg|png|webp|gif|bmp)$")) {
            String ext = ".jpg";
            if ("image/png".equals(mimeType)) ext = ".png"; else if ("image/webp".equals(mimeType)) ext = ".webp"; else if ("image/gif".equals(mimeType)) ext = ".gif"; else if ("image/bmp".equals(mimeType)) ext = ".bmp";
            rootName += ext;
        }
        return rootName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    String getName(Uri u) {
        Cursor c = getContentResolver().query(u, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
        if (c != null) {
            try {
                if (c.moveToFirst()) return c.getString(0);
            } finally {
                c.close();
            }
        }
        return "class-note";
    }

    String queryMimeType(Uri u, String fallback) {
        String type = getContentResolver().getType(u);
        return (type != null && type.trim().length() > 0) ? type : fallback;
    }

    File copyUriToPrivateStorage(Uri source, String name) {
        File dir = store.filesDir();
        File out = new File(dir, uniqueFilename(name));
        InputStream in = null;
        OutputStream os = null;
        try {
            in = getContentResolver().openInputStream(source);
            if (in == null) return null;
            os = new FileOutputStream(out);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) os.write(buffer, 0, read);
            os.flush();
        } catch (Exception e) {
            if (out.exists()) out.delete();
            return null;
        } finally {
            try { if (in != null) in.close(); } catch (Exception ignored) {}
            try { if (os != null) os.close(); } catch (Exception ignored) {}
        }
        if (out.length() == 0) {
            out.delete();
            return null;
        }
        return out;
    }

    String uniqueFilename(String name) {
        String safe = (name == null || name.trim().isEmpty()) ? "class-note" : name;
        String base = safe.replaceAll("[^a-zA-Z0-9._-]", "_");
        File dir = store.filesDir();
        File candidate = new File(dir, base);
        int i = 1;
        while (candidate.exists()) {
            int dot = base.lastIndexOf('.');
            String a = (dot > 0) ? base.substring(0, dot) : base;
            String ext = (dot > 0) ? base.substring(dot) : "";
            candidate = new File(dir, a + "_" + i + ext);
            i++;
        }
        return candidate.getName();
    }

    String noteTypeLabel(String type) {
        if (type == null) return "File";
        if (type.contains("pdf")) return "PDF";
        if (type.startsWith("image/")) return "Image";
        return "File";
    }

    void addNoticeCard(JSONObject o) {
        LinearLayout v = col();
        v.addView(tv(o.optString("text"), 15, INK));
        v.addView(tv(fmtDate(o.optString("date")), 11, MUTED));
        Button del = btn("Delete");
        v.addView(del);
        del.setOnClickListener(x -> { store.delete("notices", o.optString("id")); showSubject(store.subjectById(o.optString("subjectId"))); });
        card(v);
    }

    void addFileCard(JSONObject o) {
        LinearLayout v = col();
        v.addView(tv("📎 " + o.optString("name"), 15, INK));
        v.addView(tv(fmtDate(o.optString("date")), 11, MUTED));
        Button open = btn("Open");
        v.addView(open);
        open.setOnClickListener(x -> openFile(o.optString("path")));
        card(v);
    }

    void pickFile(String sid) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        startActivityForResult(i, 2000 + findSubjectIndex(sid));
    }

    int findSubjectIndex(String sid) {
        for (int j = 0; j < store.subjects.length(); j++) {
            if (sid.equals(store.subjects.optJSONObject(j).optString("id"))) return j;
        }
        return 0;
    }

    void addSubject() {
        final EditText n = input("Subject name");
        final EditText c = input("Code (optional)");
        LinearLayout l = col();
        l.addView(n);
        l.addView(c);
        new AlertDialog.Builder(this)
            .setTitle("New subject")
            .setView(l)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", (d, w) -> {
                if (n.getText().toString().trim().isEmpty()) return;
                store.addSubject(n.getText().toString().trim(), c.getText().toString().trim());
                showSubjects();
            })
            .show();
    }

    void addNotice(JSONObject s) {
        final EditText t = input("What should you remember?");
        new AlertDialog.Builder(this)
            .setTitle("Add notice")
            .setView(t)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", (d, w) -> {
                String z = t.getText().toString().trim();
                if (!z.isEmpty()) {
                    store.addNotice(s.optString("id"), z);
                    showSubject(s);
                }
            })
            .show();
    }

    void showAlerts() {
        clear("Alerts");
        content.addView(small("Submission tracker • sorted by due date"));
        Button add = btn("+ Submission");
        content.addView(add);
        add.setOnClickListener(v -> addDeadline());
        JSONArray a = store.upcomingAll();
        if (a.length() == 0) content.addView(small("No submissions yet."));
        for (int i = 0; i < a.length(); i++) addDeadlineCard(a.optJSONObject(i), true);
    }

    void addDeadlineCard(JSONObject o, boolean actions) {
        LinearLayout v = col();
        LinearLayout top = row();
        TextView n = tv(o.optString("title"), 16, INK);
        n.setTypeface(null, 1);
        top.addView(n, new LinearLayout.LayoutParams(0, dp(32), 1));
        TextView d = tv(daysLabel(o.optString("date")), 13, statusColor(o.optString("date")));
        d.setTypeface(null, 1);
        top.addView(d);
        v.addView(top);
        v.addView(tv(o.optString("subject", "General") + " • " + fmtDate(o.optString("date")), 12, MUTED));
        if (!o.optString("details").isEmpty()) v.addView(tv(o.optString("details"), 14, INK));
        if (actions) {
            Button done = btn(store.isDone(o) ? "Mark active" : "Mark done");
            v.addView(done);
            done.setOnClickListener(x -> { store.toggleDeadline(o.optString("id")); showAlerts(); });
        }
        View c = card(v);
        c.setOnLongClickListener(x -> { confirmDelete("Delete submission?", () -> { store.delete("deadlines", o.optString("id")); showAlerts(); }); return true; });
    }

    void addDeadline() {
        LinearLayout l = col();
        EditText n = input("Submission title");
        EditText sub = input("Subject");
        EditText detail = input("Details / requirements (optional)");
        EditText date = input("Due date: YYYY-MM-DD");
        l.addView(n); l.addView(sub); l.addView(detail); l.addView(date);
        new AlertDialog.Builder(this)
            .setTitle("New submission")
            .setView(l)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", (d, w) -> {
                if (validDate(date.getText().toString()) && n.getText().toString().trim().length() > 0) {
                    store.addDeadline(n.getText().toString().trim(), sub.getText().toString().trim(), detail.getText().toString().trim(), date.getText().toString().trim());
                    showAlerts();
                } else {
                    toast("Use a valid date: YYYY-MM-DD");
                }
            })
            .show();
    }

    void showCalendar() {
        clear("Calendar");
        content.addView(small("Countdowns to exams, breaks, practicals and important dates."));
        Button add = btn("+ Countdown");
        content.addView(add);
        add.setOnClickListener(v -> addCountdown());
        JSONArray cds = store.sortedCountdowns();
        if (cds.length() == 0) content.addView(small("No countdowns yet."));
        for (int i = 0; i < cds.length(); i++) addCountdownCard(cds.optJSONObject(i));
    }

    void addCountdownCard(JSONObject o) {
        LinearLayout v = col();
        LinearLayout top = row();
        TextView n = tv(o.optString("title"), 16, INK);
        n.setTypeface(null, 1);
        top.addView(n, new LinearLayout.LayoutParams(0, dp(34), 1));
        TextView days = tv(daysLabel(o.optString("date")), 13, statusColor(o.optString("date")));
        days.setTypeface(null, 1);
        top.addView(days);
        v.addView(top);
        v.addView(tv(fmtDate(o.optString("date")), 12, MUTED));
        if (!o.optString("note").isEmpty()) v.addView(tv(o.optString("note"), 14, INK));
        Button del = btn("Delete");
        v.addView(del);
        del.setOnClickListener(x -> { store.delete("countdowns", o.optString("id")); showCalendar(); });
        card(v);
    }

    void addCountdown() {
        LinearLayout l = col();
        EditText n = input("Name (e.g. Mid Exam)");
        EditText date = input("Date: YYYY-MM-DD");
        EditText note = input("Note (optional)");
        l.addView(n); l.addView(date); l.addView(note);
        new AlertDialog.Builder(this)
            .setTitle("New countdown")
            .setView(l)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", (d, w) -> {
                if (validDate(date.getText().toString()) && n.getText().toString().trim().length() > 0) {
                    store.addCountdown(n.getText().toString().trim(), date.getText().toString().trim(), note.getText().toString().trim());
                    showCalendar();
                } else {
                    toast("Use a valid date: YYYY-MM-DD");
                }
            })
            .show();
    }

    void showIdeas() {
        clear("Ideas & Problems");
        content.addView(small("A scratchpad for the things you do not want to lose."));
        LinearLayout add = row();
        Button i = btn("+ Idea");
        Button p = btn("+ Problem");
        add.addView(i, new LinearLayout.LayoutParams(0, dp(50), 1));
        add.addView(p, new LinearLayout.LayoutParams(0, dp(50), 1));
        content.addView(add);
        i.setOnClickListener(v -> addIdea("Idea"));
        p.setOnClickListener(v -> addIdea("Problem"));

        JSONArray a = store.sortedIdeas();
        if (a.length() == 0) content.addView(small("Capture project ideas, doubts, assignment problems, things to ask a professor, etc."));
        for (int x = 0; x < a.length(); x++) {
            JSONObject o = a.optJSONObject(x);
            LinearLayout v = col();
            TextView t = tv(o.optString("type") + " • " + fmtDate(o.optString("date")), 12, MUTED);
            t.setTypeface(null, 1);
            v.addView(t);
            v.addView(tv(o.optString("text"), 15, INK));
            Button del = btn("Delete");
            v.addView(del);
            del.setOnClickListener(z -> { store.delete("ideas", o.optString("id")); showIdeas(); });
            card(v);
        }
    }

    void addIdea(String type) {
        final EditText e = input("Write your " + type.toLowerCase() + "...");
        new AlertDialog.Builder(this)
            .setTitle("Quick " + type)
            .setView(e)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", (d, w) -> {
                if (!e.getText().toString().trim().isEmpty()) {
                    store.addIdea(type, e.getText().toString().trim());
                    if (currentTab == 4) showIdeas();
                }
            })
            .show();
    }

    void showMenu(View anchor) {
        PopupMenu p = new PopupMenu(this, anchor);
        p.getMenu().add("Export backup");
        p.getMenu().add("Import backup");
        p.getMenu().add("About");
        p.setOnMenuItemClickListener(m -> {
            String x = m.getTitle().toString();
            if (x.startsWith("Export")) {
                startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/zip").putExtra(Intent.EXTRA_TITLE, "campusmate-backup.zip"), 9001);
            } else if (x.startsWith("Import")) {
                startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/zip"), 9002);
            } else {
                new AlertDialog.Builder(this)
                    .setTitle("CampusMate")
                    .setMessage("Offline college organizer\n\nYour subjects, notices, deadlines, countdowns, ideas and class notes stay on this device. Backup creates a ZIP you can move to another phone.")
                    .setPositiveButton("OK", null)
                    .show();
            }
            return true;
        });
        p.show();
    }

    void confirmDelete(String msg, final Runnable yes) {
        new AlertDialog.Builder(this)
            .setTitle(msg)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete", (d, w) -> yes.run())
            .show();
    }

    int statusColor(String date) {
        long days = store.daysUntil(date);
        return days < 0 ? Color.rgb(217, 45, 32) : (days <= 3 ? Color.rgb(181, 71, 8) : Color.rgb(2, 122, 72));
    }

    String daysLabel(String date) {
        long d = store.daysUntil(date);
        if (d < 0) return Math.abs(d) + "d overdue";
        if (d == 0) return "Today";
        if (d == 1) return "Tomorrow";
        return d + " days";
    }

    String fmtDate(String d) {
        try {
            return new SimpleDateFormat("dd MMM yyyy", Locale.US).format(new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(d));
        } catch (Exception e) {
            return d;
        }
    }

    boolean validDate(String d) {
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            f.setLenient(false);
            f.parse(d);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    void openFile(String path) {
        try {
            File f = new File(path);
            if (!f.exists()) {
                toast("This file is no longer available on this device.");
                return;
            }
            Uri uri = new Uri.Builder()
                .scheme("content")
                .authority("com.nor.campusmate.files")
                .path("/" + Uri.encode(f.getName()))
                .build();
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(uri, guessMimeType(f));
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(i);
        } catch (Exception e) {
            toast("No app available to open this file.");
        }
    }

    String guessMimeType(File f) {
        String name = f.getName().toLowerCase(Locale.US);
        if (name.endsWith(".pdf")) return "application/pdf";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".gif")) return "image/gif";
        if (name.endsWith(".bmp")) return "image/bmp";
        if (name.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }
}

class DataStore {
    final MainActivity a;
    final SharedPreferences p;
    final File root;

    JSONArray subjects, notices, files, deadlines, countdowns, ideas, chapters, classNotes;

    DataStore(MainActivity x) {
        a = x;
        p = x.getSharedPreferences("data", 0);
        root = new File(x.getFilesDir(), MainActivity.APP_DIR);
        root.mkdirs();
        load();
    }

    File filesDir() {
        File f = new File(root, "attachments");
        f.mkdirs();
        return f;
    }

    JSONArray arr(String k) {
        try {
            return new JSONArray(p.getString(k, "[]"));
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    void load() {
        subjects = arr("subjects");
        notices = arr("notices");
        files = arr("files");
        deadlines = arr("deadlines");
        countdowns = arr("countdowns");
        ideas = arr("ideas");
        chapters = arr("chapters");
        classNotes = arr("classNotes");
    }

    void save() {
        p.edit()
            .putString("subjects", subjects.toString())
            .putString("notices", notices.toString())
            .putString("files", files.toString())
            .putString("deadlines", deadlines.toString())
            .putString("countdowns", countdowns.toString())
            .putString("ideas", ideas.toString())
            .putString("chapters", chapters.toString())
            .putString("classNotes", classNotes.toString())
            .apply();
    }

    String id() { return UUID.randomUUID().toString(); }
    String today() { return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()); }
    long daysUntil(String d) {
        try {
            long t = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(d).getTime();
            long n = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(today()).getTime();
            return Math.round((t - n) / 86400000.0);
        } catch (Exception e) {
            return 9999;
        }
    }

    JSONObject o(String... kv) {
        JSONObject x = new JSONObject();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            try { x.put(kv[i], kv[i + 1]); } catch (Exception ignored) { }
        }
        return x;
    }

    void addSubject(String n, String c) { subjects.put(o("id", id(), "name", n, "code", c)); save(); }
    void addNotice(String sid, String text) { notices.put(o("id", id(), "subjectId", sid, "text", text, "date", today())); save(); }
    void addFile(String sid, String name, String path) { files.put(o("id", id(), "subjectId", sid, "name", name, "path", path, "date", today())); save(); }
    void addDeadline(String title, String subject, String details, String date) {
        try {
            JSONObject d = new JSONObject();
            d.put("id", id());
            d.put("title", title);
            d.put("subject", subject);
            d.put("details", details);
            d.put("date", date);
            d.put("done", false);
            deadlines.put(d);
        } catch (JSONException e) {
            // Fallback: use string-based helper (should not happen)
            deadlines.put(o("id", id(), "title", title, "subject", subject, "details", details, "date", date));
        }
        save();
    }
    void addCountdown(String title, String date, String note) { countdowns.put(o("id", id(), "title", title, "date", date, "note", note)); save(); }
    void addIdea(String type, String text) { ideas.put(o("id", id(), "type", type, "text", text, "date", today())); save(); }

    void addChapter(String sid, String name) { chapters.put(o("id", id(), "subjectId", sid, "name", name, "date", today())); save(); }
    void renameChapter(String id, String name) {
    try {
        for (int i = 0; i < chapters.length(); i++) {
            JSONObject c = chapters.optJSONObject(i);
            if (c != null && id.equals(c.optString("id"))) {
                c.put("name", name);
                break;
            }
        }
        save();
    } catch (JSONException e) {
        e.printStackTrace();
    }
}
    void deleteChapter(String id) {
        JSONArray next = new JSONArray();
        for (int i = 0; i < chapters.length(); i++) { JSONObject c = chapters.optJSONObject(i); if (!id.equals(c.optString("id"))) next.put(c); }
        chapters = next;
        JSONArray nextNotes = new JSONArray();
        for (int i = 0; i < classNotes.length(); i++) {
            JSONObject n = classNotes.optJSONObject(i);
            if (id.equals(n.optString("chapterId"))) {
                deleteAttachmentIfUnreferenced(n.optString("path"), n.optString("id"));
            } else {
                nextNotes.put(n);
            }
        }
        classNotes = nextNotes;
        save();
    }
    JSONObject chapterById(String id) { for (int i = 0; i < chapters.length(); i++) { JSONObject c = chapters.optJSONObject(i); if (id.equals(c.optString("id"))) return c; } return new JSONObject(); }
    JSONArray chaptersForSubject(String sid) { JSONArray out = new JSONArray(); for (int i = 0; i < chapters.length(); i++) { JSONObject c = chapters.optJSONObject(i); if (sid.equals(c.optString("subjectId"))) out.put(c); } return out; }

    JSONObject addClassNote(String sid, String chapterId, String name, String path, String type, String description) {
        JSONObject note = o("id", id(), "subjectId", sid, "chapterId", chapterId, "name", name, "path", path, "type", type, "description", description, "date", today());
        classNotes.put(note);
        save();
        return note;
    }
    JSONArray notesForChapter(String chapterId) { JSONArray out = new JSONArray(); for (int i = 0; i < classNotes.length(); i++) { JSONObject n = classNotes.optJSONObject(i); if (chapterId.equals(n.optString("chapterId"))) out.put(n); } return out; }
    JSONArray notesForChapterSorted(String chapterId) { JSONArray out = notesForChapter(chapterId); ArrayList<JSONObject> list = new ArrayList<>(); for (int i = 0; i < out.length(); i++) list.add(out.optJSONObject(i)); Collections.sort(list, (u, v) -> v.optString("date", "0000-00-00").compareTo(u.optString("date", "0000-00-00"))); JSONArray z = new JSONArray(); for (JSONObject item : list) z.put(item); return z; }
    void renameClassNote(String id, String name) {
    try {
        for (int i = 0; i < classNotes.length(); i++) {
            JSONObject n = classNotes.optJSONObject(i);
            if (n != null && id.equals(n.optString("id"))) {
                n.put("name", name);
                break;
            }
        }
        save();
    } catch (JSONException e) {
        e.printStackTrace();
    }
}
    void moveClassNote(String id, String chapterId) {
    try {
        for (int i = 0; i < classNotes.length(); i++) {
            JSONObject n = classNotes.optJSONObject(i);
            if (n != null && id.equals(n.optString("id"))) {
                n.put("chapterId", chapterId);
                break;
            }
        }
        save();
    } catch (JSONException e) {
        e.printStackTrace();
    }
}
    void deleteClassNote(String id) {
        String pathToDelete = null;
        JSONArray next = new JSONArray();
        for (int i = 0; i < classNotes.length(); i++) {
            JSONObject n = classNotes.optJSONObject(i);
            if (id.equals(n.optString("id"))) {
                pathToDelete = n.optString("path");
            } else {
                next.put(n);
            }
        }
        classNotes = next;
        if (pathToDelete != null && !pathToDelete.isEmpty()) {
            deleteAttachmentIfUnreferenced(pathToDelete, id);
        }
        save();
    }

    /** Delete the physical attachment file only if no other record references it */
    void deleteAttachmentIfUnreferenced(String path, String excludeId) {
        if (path == null || path.isEmpty()) return;
        // Check if any other classNote or file record references this path
        for (int i = 0; i < classNotes.length(); i++) {
            JSONObject n = classNotes.optJSONObject(i);
            if (!excludeId.equals(n.optString("id")) && path.equals(n.optString("path"))) return;
        }
        for (int i = 0; i < files.length(); i++) {
            JSONObject f = files.optJSONObject(i);
            if (path.equals(f.optString("path"))) return;
        }
        File file = new File(path);
        if (file.exists()) file.delete();
    }

    int countDue(int n) { int c = 0; for (int i = 0; i < deadlines.length(); i++) { JSONObject o = deadlines.optJSONObject(i); long d = daysUntil(o.optString("date")); if (!isDone(o) && d <= n && d >= 0) c++; } return c; }
    JSONArray upcoming(int days) { JSONArray out = new JSONArray(); for (int i = 0; i < deadlines.length(); i++) { JSONObject o = deadlines.optJSONObject(i); long d = daysUntil(o.optString("date")); if (!isDone(o) && d >= 0 && d <= days) out.put(o); } sortDate(out); return out; }
    JSONArray upcomingAll() { JSONArray out = new JSONArray(); for (int i = 0; i < deadlines.length(); i++) { JSONObject o = deadlines.optJSONObject(i); if (!isDone(o)) out.put(o); } sortDate(out); for (int i = 0; i < deadlines.length(); i++) { JSONObject o = deadlines.optJSONObject(i); if (isDone(o)) out.put(o); } return out; }
    JSONArray sortedCountdowns() { JSONArray out = new JSONArray(); for (int i = 0; i < countdowns.length(); i++) out.put(countdowns.optJSONObject(i)); sortDate(out); return out; }
    JSONArray sortedIdeas() { JSONArray out = new JSONArray(); for (int i = ideas.length() - 1; i >= 0; i--) out.put(ideas.optJSONObject(i)); return out; }
    JSONArray noticesFor(String sid) { JSONArray out = new JSONArray(); for (int i = 0; i < notices.length(); i++) if (sid.equals(notices.optJSONObject(i).optString("subjectId"))) out.put(notices.optJSONObject(i)); return out; }
    JSONArray filesFor(String sid) { JSONArray out = new JSONArray(); for (int i = 0; i < files.length(); i++) if (sid.equals(files.optJSONObject(i).optString("subjectId"))) out.put(files.optJSONObject(i)); return out; }
    JSONObject subjectById(String sid) { for (int i = 0; i < subjects.length(); i++) if (sid.equals(subjects.optJSONObject(i).optString("id"))) return subjects.optJSONObject(i); return new JSONObject(); }
    void sortDate(JSONArray x) { ArrayList<JSONObject> l = new ArrayList<>(); for (int i = 0; i < x.length(); i++) l.add(x.optJSONObject(i)); Collections.sort(l, (u, v) -> u.optString("date", "9999-12-31").compareTo(v.optString("date", "9999-12-31"))); JSONArray z = new JSONArray(); for (JSONObject item : l) z.put(item); for (int i = 0; i < z.length(); i++) { try { x.put(i, z.get(i)); } catch (Exception ignored) { } } }
    void toggleDeadline(String id) {
        for (int i = 0; i < deadlines.length(); i++) {
            if (id.equals(deadlines.optJSONObject(i).optString("id"))) {
                try {
                    JSONObject o = deadlines.getJSONObject(i);
                    boolean current = isDone(o);
                    o.put("done", !current);
                } catch (Exception ignored) { }
                break;
            }
        }
        save();
    }
    /** Read "done" safely: handles both boolean true/false and string "true"/"false" */
    boolean isDone(JSONObject o) {
        Object v = o.opt("done");
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof String) return "true".equalsIgnoreCase((String) v);
        return false;
    }
    void delete(String k, String id) { JSONArray x = arr(k); JSONArray z = new JSONArray(); for (int i = 0; i < x.length(); i++) if (!id.equals(x.optJSONObject(i).optString("id"))) z.put(x.optJSONObject(i)); try { p.edit().putString(k, z.toString()).apply(); load(); } catch (Exception ignored) { } }

    void exportTo(Uri dest) {
        try {
            File tmp = new File(root, "export.zip");
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tmp));

            // Build a map from absolute path → safe zip filename for portability
            Map<String, String> pathToZipName = new HashMap<>();
            Set<String> usedZipNames = new HashSet<>();
            collectAttachmentPaths(files, pathToZipName, usedZipNames);
            collectAttachmentPaths(classNotes, pathToZipName, usedZipNames);

            // Deep-copy JSON arrays and replace absolute paths with portable filenames
            JSONObject data = new JSONObject();
            data.put("version", 2);
            data.put("subjects", subjects);
            data.put("notices", notices);
            data.put("files", replacePathsForExport(files, pathToZipName));
            data.put("deadlines", deadlines);
            data.put("countdowns", countdowns);
            data.put("ideas", ideas);
            data.put("chapters", chapters);
            data.put("classNotes", replacePathsForExport(classNotes, pathToZipName));
            byte[] bytes = data.toString(2).getBytes("UTF-8");
            zos.putNextEntry(new ZipEntry("data.json"));
            zos.write(bytes);
            zos.closeEntry();

            // Write attachment files into zip
            for (Map.Entry<String, String> entry : pathToZipName.entrySet()) {
                File src = new File(entry.getKey());
                if (src.exists()) {
                    addZipEntry(zos, "attachments/" + entry.getValue(), src);
                }
            }
            zos.close();

            OutputStream out = a.getContentResolver().openOutputStream(dest);
            InputStream in = new FileInputStream(tmp);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            in.close();
            out.close();
            tmp.delete();
            a.toast("Backup exported");
        } catch (Exception e) {
            a.toast("Export failed");
        }
    }

    /** Collect attachment paths and generate unique safe zip filenames */
    void collectAttachmentPaths(JSONArray arr, Map<String, String> pathToZipName, Set<String> usedZipNames) {
        for (int i = 0; i < arr.length(); i++) {
            String path = arr.optJSONObject(i).optString("path");
            if (path == null || path.isEmpty() || pathToZipName.containsKey(path)) continue;
            File f = new File(path);
            String safeName = f.getName().replaceAll("[^a-zA-Z0-9._-]", "_");
            if (safeName.isEmpty()) safeName = "file_" + i;
            String candidate = safeName;
            int counter = 1;
            while (usedZipNames.contains(candidate)) {
                int dot = safeName.lastIndexOf('.');
                String base = (dot > 0) ? safeName.substring(0, dot) : safeName;
                String ext = (dot > 0) ? safeName.substring(dot) : "";
                candidate = base + "_" + counter + ext;
                counter++;
            }
            usedZipNames.add(candidate);
            pathToZipName.put(path, candidate);
        }
    }

    /** Deep-copy a JSON array, replacing "path" with portable zip filename */
    JSONArray replacePathsForExport(JSONArray src, Map<String, String> pathToZipName) throws JSONException {
        JSONArray out = new JSONArray(src.toString());
        for (int i = 0; i < out.length(); i++) {
            JSONObject o = out.getJSONObject(i);
            String path = o.optString("path");
            if (pathToZipName.containsKey(path)) {
                o.put("path", pathToZipName.get(path));
            }
        }
        return out;
    }

    void addZipEntry(ZipOutputStream zos, String entryName, File src) throws Exception {
        zos.putNextEntry(new ZipEntry(entryName));
        FileInputStream in = new FileInputStream(src);
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) zos.write(buf, 0, n);
        in.close();
        zos.closeEntry();
    }

    void importFrom(Uri src) {
        try {
            File tmp = new File(root, "import.zip");
            InputStream in = a.getContentResolver().openInputStream(src);
            if (in == null) { a.toast("Import failed"); return; }
            FileOutputStream out = new FileOutputStream(tmp);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            in.close();
            out.close();

            ZipInputStream zin = new ZipInputStream(new FileInputStream(tmp));
            ZipEntry e;
            JSONObject data = null;
            File att = filesDir();
            String attCanonical = att.getCanonicalPath();
            while ((e = zin.getNextEntry()) != null) {
                if (e.getName().equals("data.json")) {
                    ByteArrayOutputStream bo = new ByteArrayOutputStream();
                    while ((n = zin.read(buf)) > 0) bo.write(buf, 0, n);
                    data = new JSONObject(new String(bo.toByteArray(), "UTF-8"));
                } else if (e.getName().startsWith("attachments/") && !e.isDirectory()) {
                    // Extract only the filename, reject path traversal
                    String entryFileName = new File(e.getName()).getName();
                    if (entryFileName.contains("..") || entryFileName.contains("/") || entryFileName.contains("\\")) {
                        continue; // Skip unsafe entry
                    }
                    File f = new File(att, entryFileName);
                    // Verify the resolved path is inside the attachment directory
                    if (!f.getCanonicalPath().startsWith(attCanonical)) {
                        continue; // Path traversal attempt
                    }
                    FileOutputStream fo = new FileOutputStream(f);
                    while ((n = zin.read(buf)) > 0) fo.write(buf, 0, n);
                    fo.close();
                }
            }
            zin.close();
            tmp.delete();
            if (data == null) { a.toast("Import failed: no data found"); return; }

            subjects = data.optJSONArray("subjects");
            notices = data.optJSONArray("notices");
            files = data.optJSONArray("files");
            deadlines = data.optJSONArray("deadlines");
            countdowns = data.optJSONArray("countdowns");
            ideas = data.optJSONArray("ideas");
            chapters = data.optJSONArray("chapters");
            classNotes = data.optJSONArray("classNotes");

            if (subjects == null) subjects = new JSONArray();
            if (notices == null) notices = new JSONArray();
            if (files == null) files = new JSONArray();
            if (deadlines == null) deadlines = new JSONArray();
            if (countdowns == null) countdowns = new JSONArray();
            if (ideas == null) ideas = new JSONArray();
            if (chapters == null) chapters = new JSONArray();
            if (classNotes == null) classNotes = new JSONArray();

            // Resolve portable paths to local absolute paths
            resolveImportedPaths(files, att);
            resolveImportedPaths(classNotes, att);

            // Normalize deadline "done" fields from string to boolean
            normalizeDeadlineBooleans();

            save();
            a.toast("Backup imported");
            a.currentTab = 0;
            a.showHome();
        } catch (Exception e) {
            a.toast("Import failed");
        }
    }

    /** Resolve imported paths: handles both old absolute paths and new portable filenames */
    void resolveImportedPaths(JSONArray arr, File attachDir) {
        for (int i = 0; i < arr.length(); i++) {
            JSONObject f = arr.optJSONObject(i);
            if (f == null) continue;
            String path = f.optString("path");
            if (path == null || path.trim().isEmpty()) continue;
            // If it's already an absolute path that exists, keep it (same-device restore)
            File existing = new File(path);
            if (existing.isAbsolute() && existing.exists()) continue;
            // Otherwise resolve from the attachment directory (portable filename or old path basename)
            String baseName = new File(path).getName();
            File target = new File(attachDir, baseName);
            if (target.exists()) {
                try { f.put("path", target.getAbsolutePath()); } catch (JSONException ignored) {}
            }
        }
    }

    /** Normalize deadline "done" from string "true"/"false" to actual boolean */
    void normalizeDeadlineBooleans() {
        for (int i = 0; i < deadlines.length(); i++) {
            JSONObject d = deadlines.optJSONObject(i);
            if (d == null) continue;
            try { d.put("done", isDone(d)); } catch (JSONException ignored) {}
        }
    }
}
