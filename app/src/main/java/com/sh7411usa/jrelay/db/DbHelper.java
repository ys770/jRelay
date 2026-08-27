package com.sh7411usa.jrelay.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "jrelay.db";
    private static final int DB_VERSION = 3;

    public static final String TABLE_MEMBERS = "members";
    public static final String TABLE_MESSAGE_LOG = "message_log";
    public static final String TABLE_OUTBOX = "outbox";

    private static DbHelper instance;

    public static synchronized DbHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DbHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_MEMBERS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "phone_e164 TEXT UNIQUE NOT NULL," +
                "nickname TEXT NOT NULL," +
                "is_admin INTEGER NOT NULL DEFAULT 0," +
                "is_muted INTEGER NOT NULL DEFAULT 0," +
                "active INTEGER NOT NULL DEFAULT 1," +
                "added_by TEXT," +
                "created_at INTEGER NOT NULL," +
                "removed_at INTEGER" +
                ")");

        db.execSQL("CREATE TABLE " + TABLE_MESSAGE_LOG + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "member_id INTEGER," +
                "direction TEXT NOT NULL," +
                "category TEXT NOT NULL," +
                "body TEXT NOT NULL," +
                "timestamp INTEGER NOT NULL" +
                ")");

        db.execSQL("CREATE TABLE " + TABLE_OUTBOX + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "member_id INTEGER," +
                "message_log_id INTEGER," +
                "phone_e164 TEXT NOT NULL," +
                "body TEXT NOT NULL," +
                "enqueued_at INTEGER NOT NULL," +
                "status TEXT NOT NULL DEFAULT 'PENDING'," +
                "parts_total INTEGER NOT NULL DEFAULT 1," +
                "parts_sent INTEGER NOT NULL DEFAULT 0," +
                "parts_delivered INTEGER NOT NULL DEFAULT 0," +
                "submitted_at INTEGER," +
                "delivered_at INTEGER," +
                "error_code INTEGER" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_OUTBOX + " ADD COLUMN parts_total INTEGER NOT NULL DEFAULT 1");
            db.execSQL("ALTER TABLE " + TABLE_OUTBOX + " ADD COLUMN parts_sent INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE_OUTBOX + " ADD COLUMN parts_delivered INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE_OUTBOX + " ADD COLUMN submitted_at INTEGER");
            db.execSQL("ALTER TABLE " + TABLE_OUTBOX + " ADD COLUMN delivered_at INTEGER");
            db.execSQL("ALTER TABLE " + TABLE_OUTBOX + " ADD COLUMN error_code INTEGER");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_OUTBOX + " ADD COLUMN message_log_id INTEGER");
        }
    }

    /** Permanently erases every member, message, and queued outbound message. Used only by "Disband Group". */
    public void wipeAllData() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_OUTBOX, null, null);
        db.delete(TABLE_MESSAGE_LOG, null, null);
        db.delete(TABLE_MEMBERS, null, null);
    }
}
