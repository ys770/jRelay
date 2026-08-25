package com.sh7411usa.jrelay.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.sh7411usa.jrelay.model.MessageRecord;

import java.util.ArrayList;
import java.util.List;

public class MessageRepository {

    private final DbHelper dbHelper;

    public MessageRepository(Context context) {
        dbHelper = DbHelper.getInstance(context);
    }

    public void log(Long memberId, String direction, String category, String body) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        if (memberId != null) {
            cv.put("member_id", memberId);
        }
        cv.put("direction", direction);
        cv.put("category", category);
        cv.put("body", body);
        cv.put("timestamp", System.currentTimeMillis());
        db.insert(DbHelper.TABLE_MESSAGE_LOG, null, cv);
    }

    public List<MessageRecord> getRecent(int limit) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(DbHelper.TABLE_MESSAGE_LOG, null, null, null, null, null,
                "timestamp DESC", String.valueOf(limit));
        List<MessageRecord> list = new ArrayList<>();
        while (c.moveToNext()) {
            list.add(fromCursor(c));
        }
        c.close();
        return list;
    }

    public List<MessageRecord> getRecentForMember(long memberId, int limit) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(DbHelper.TABLE_MESSAGE_LOG, null, "member_id = ?",
                new String[]{String.valueOf(memberId)}, null, null, "timestamp DESC", String.valueOf(limit));
        List<MessageRecord> list = new ArrayList<>();
        while (c.moveToNext()) {
            list.add(fromCursor(c));
        }
        c.close();
        return list;
    }

    public int countForMember(long memberId, String direction) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + DbHelper.TABLE_MESSAGE_LOG +
                " WHERE member_id = ? AND direction = ?", new String[]{String.valueOf(memberId), direction});
        int count = 0;
        if (c.moveToFirst()) {
            count = c.getInt(0);
        }
        c.close();
        return count;
    }

    public int countForMemberSince(long memberId, long sinceTimestamp) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + DbHelper.TABLE_MESSAGE_LOG +
                " WHERE member_id = ? AND timestamp >= ?", new String[]{String.valueOf(memberId), String.valueOf(sinceTimestamp)});
        int count = 0;
        if (c.moveToFirst()) {
            count = c.getInt(0);
        }
        c.close();
        return count;
    }

    public long lastActivityForMember(long memberId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT MAX(timestamp) FROM " + DbHelper.TABLE_MESSAGE_LOG +
                " WHERE member_id = ?", new String[]{String.valueOf(memberId)});
        long ts = 0;
        if (c.moveToFirst() && !c.isNull(0)) {
            ts = c.getLong(0);
        }
        c.close();
        return ts;
    }

    public int countSince(long sinceTimestamp) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + DbHelper.TABLE_MESSAGE_LOG +
                " WHERE timestamp >= ?", new String[]{String.valueOf(sinceTimestamp)});
        int count = 0;
        if (c.moveToFirst()) {
            count = c.getInt(0);
        }
        c.close();
        return count;
    }

    public int countAll() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + DbHelper.TABLE_MESSAGE_LOG, null);
        int count = 0;
        if (c.moveToFirst()) {
            count = c.getInt(0);
        }
        c.close();
        return count;
    }

    private MessageRecord fromCursor(Cursor c) {
        MessageRecord r = new MessageRecord();
        r.id = c.getLong(c.getColumnIndexOrThrow("id"));
        int memberIdx = c.getColumnIndexOrThrow("member_id");
        r.memberId = c.isNull(memberIdx) ? null : c.getLong(memberIdx);
        r.direction = c.getString(c.getColumnIndexOrThrow("direction"));
        r.category = c.getString(c.getColumnIndexOrThrow("category"));
        r.body = c.getString(c.getColumnIndexOrThrow("body"));
        r.timestamp = c.getLong(c.getColumnIndexOrThrow("timestamp"));
        return r;
    }
}
