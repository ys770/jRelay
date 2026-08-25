package com.sh7411usa.jrelay.util;

import android.content.Context;
import android.content.SharedPreferences;

public class Prefs {

    private static final String PREFS_NAME = "jrelay_prefs";
    private static final String KEY_CONSENT_ACCEPTED = "consent_accepted";
    private static final String KEY_GROUP_NAME = "group_name";
    private static final String KEY_BURST_SIZE = "burst_size";
    private static final String KEY_MIN_WAIT = "min_wait_seconds";
    private static final String KEY_MAX_WAIT = "max_wait_seconds";

    private static final int DEFAULT_BURST_SIZE = 5;
    private static final int DEFAULT_MIN_WAIT = 3;
    private static final int DEFAULT_MAX_WAIT = 8;

    private final SharedPreferences prefs;

    public Prefs(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isConsentAccepted() {
        return prefs.getBoolean(KEY_CONSENT_ACCEPTED, false);
    }

    public void setConsentAccepted(boolean accepted) {
        prefs.edit().putBoolean(KEY_CONSENT_ACCEPTED, accepted).apply();
    }

    public String getGroupName() {
        return prefs.getString(KEY_GROUP_NAME, "jRelay");
    }

    public void setGroupName(String name) {
        prefs.edit().putString(KEY_GROUP_NAME, name).apply();
    }

    public int getBurstSize() {
        return prefs.getInt(KEY_BURST_SIZE, DEFAULT_BURST_SIZE);
    }

    public void setBurstSize(int size) {
        prefs.edit().putInt(KEY_BURST_SIZE, size).apply();
    }

    public int getMinWaitSeconds() {
        return prefs.getInt(KEY_MIN_WAIT, DEFAULT_MIN_WAIT);
    }

    public void setMinWaitSeconds(int seconds) {
        prefs.edit().putInt(KEY_MIN_WAIT, seconds).apply();
    }

    public int getMaxWaitSeconds() {
        return prefs.getInt(KEY_MAX_WAIT, DEFAULT_MAX_WAIT);
    }

    public void setMaxWaitSeconds(int seconds) {
        prefs.edit().putInt(KEY_MAX_WAIT, seconds).apply();
    }
}
