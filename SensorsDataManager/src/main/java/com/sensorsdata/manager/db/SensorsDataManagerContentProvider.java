package com.sensorsdata.manager.db;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sensorsdata.manager.SensorsDataManagerAPI;
import com.sensorsdata.manager.utils.SALogger;

public class SensorsDataManagerContentProvider extends ContentProvider {
    private static final String TAG = "SensorsDataManagerContentProvider";
    private final static int EVENTS = 1;
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private SensorsDataManagerDbHelper mDbHelper;
    private boolean isDbWritable = true;
    static {
        uriMatcher.addURI(DBConstant.AUTHORITY, DBConstant.TABLE_EVENTS, EVENTS);
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new SensorsDataManagerDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        if (!isDbWritable) {
            return null;
        }
        Cursor cursor = null;
        try {
            int code = uriMatcher.match(uri);
            if (code == EVENTS) {
                cursor = queryByTable(DBConstant.TABLE_EVENTS, projection, selection, selectionArgs, sortOrder);
            }
        } catch (Exception e) {
            SALogger.printStackTrace(e);
        }
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        // 不处理 values = null 或者 values 为空的情况
        if (!isDbWritable || contentValues == null || contentValues.size() == 0) {
            return uri;
        }
        try {
            if (DBUtil.getInstance(getContext()).deleteDataLowMemory(uri) != 0) {
                return uri;
            }
            int code = uriMatcher.match(uri);
            if (code == EVENTS) {
                return insertEvent(uri, contentValues);
            }
        } catch (Exception e) {
            SALogger.printStackTrace(e);
        } finally {
            // 如果超过缓存数据条数，则直接 flush 数据
            if (DBUtil.getInstance(getContext()).isOutOfFlushSize(uri)) {
                SensorsDataManagerAPI.sharedInstance().flush();
            } else {
                SensorsDataManagerAPI.sharedInstance().flushDelay();
            }
        }
        return uri;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        if (!isDbWritable) {
            return 0;
        }
        int deletedCounts = 0;
        try {
            int code = uriMatcher.match(uri);
            if (EVENTS == code) {
                try {
                    SQLiteDatabase database = mDbHelper.getWritableDatabase();
                    if (selectionArgs != null) {
                        deletedCounts = database.delete(DBConstant.TABLE_EVENTS, selection, selectionArgs);
                    } else {
                        database.execSQL(selection);
                    }
                } catch (SQLiteException e) {
                    isDbWritable = false;
                    SALogger.printStackTrace(e);
                }
            }
            //目前逻辑不处理其他 Code
        } catch (Exception e) {
            SALogger.printStackTrace(e);
        }
        return deletedCounts;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String s, @Nullable String[] strings) {
        return 0;
    }

    private Uri insertEvent(Uri uri, ContentValues values) {
        SQLiteDatabase database;
        try {
            database = mDbHelper.getWritableDatabase();
        } catch (SQLiteException e) {
            isDbWritable = false;
            SALogger.printStackTrace(e);
            return uri;
        }
        if (!values.containsKey(DBConstant.KEY_DATA) || !values.containsKey(DBConstant.KEY_CREATED_AT)) {
            return uri;
        }
        long d = database.insert(DBConstant.TABLE_EVENTS, "_id", values);
        return ContentUris.withAppendedId(uri, d);
    }

    private Cursor queryByTable(String tableName, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor cursor = null;
        try {
            cursor = mDbHelper.getWritableDatabase().query(tableName, projection, selection, selectionArgs, null, null, sortOrder);
        } catch (SQLiteException e) {
            isDbWritable = false;
            SALogger.printStackTrace(e);
        }
        return cursor;
    }
}
