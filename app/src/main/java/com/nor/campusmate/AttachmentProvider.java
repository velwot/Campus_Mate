package com.nor.campusmate;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import java.io.File;

public class AttachmentProvider extends ContentProvider {
    private File root() { return new File(getContext().getFilesDir(), "campusmate/attachments"); }
    private File fileFor(Uri uri) throws java.io.FileNotFoundException {
        String name = uri.getLastPathSegment();
        if (name == null || name.contains("..") || name.contains("/")) throw new java.io.FileNotFoundException();
        File f = new File(root(), name);
        if (!f.exists() || !f.isFile()) throw new java.io.FileNotFoundException(name);
        return f;
    }
    @Override public boolean onCreate(){ root().mkdirs(); return true; }
    @Override public String getType(Uri uri){ return "application/octet-stream"; }
    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws java.io.FileNotFoundException {
        return ParcelFileDescriptor.open(fileFor(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sortOrder){
        File f; try { f=fileFor(uri); } catch(Exception e){ return null; }
        MatrixCursor c=new MatrixCursor(new String[]{"_display_name","_size"});
        c.addRow(new Object[]{f.getName(),f.length()});
        return c;
    }
    @Override public int delete(Uri uri, String selection, String[] args){ return 0; }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] args){ return 0; }
    @Override public Uri insert(Uri uri, ContentValues values){ return null; }
}
