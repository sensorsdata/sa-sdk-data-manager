package com.sensorsdata.manager.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.sensorsdata.manager.utils.SALogger;

class SensorsDataManagerDbHelper extends SQLiteOpenHelper {
    private static final String TAG = "SQLiteOpenHelper";
    private static final String CREATE_EVENTS_TABLE =
            String.format("CREATE TABLE %s (_id INTEGER PRIMARY KEY AUTOINCREMENT, %s TEXT NOT NULL, %s INTEGER NOT NULL);", DBConstant.TABLE_EVENTS, DBConstant.KEY_DATA, DBConstant.KEY_CREATED_AT);
    private static final String EVENTS_TIME_INDEX =
            String.format("CREATE INDEX IF NOT EXISTS time_idx ON %s (%s);", DBConstant.TABLE_EVENTS, DBConstant.KEY_CREATED_AT);

    SensorsDataManagerDbHelper(Context context) {
        super(context, DBConstant.DATABASE_NAME, null, DBConstant.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        SALogger.i(TAG, "Creating a new Sensors Analytics DB");

        db.execSQL(CREATE_EVENTS_TABLE);
        db.execSQL(EVENTS_TIME_INDEX);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        SALogger.i(TAG, "Upgrading app, replacing Sensors Analytics DB");
        db.execSQL(String.format("DROP TABLE IF EXISTS %s", DBConstant.TABLE_EVENTS));
        db.execSQL(CREATE_EVENTS_TABLE);
        db.execSQL(EVENTS_TIME_INDEX);
    }
}
