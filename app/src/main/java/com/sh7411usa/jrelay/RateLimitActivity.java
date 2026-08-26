package com.sh7411usa.jrelay;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.sh7411usa.jrelay.db.DbHelper;
import com.sh7411usa.jrelay.util.Prefs;
import com.sh7411usa.jrelay.util.RateLimitSettings;

import java.util.Random;

public class RateLimitActivity extends Activity {

    private Prefs prefs;
    private EditText burstMinInput;
    private EditText burstMaxInput;
    private EditText minWaitInput;
    private EditText maxWaitInput;
    private CheckBox initialDelayCheckbox;
    private EditText systemMaxCountInput;
    private EditText systemIntervalInput;
    private TextView systemPermissionNotice;
    private Button systemSaveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rate_limit);
        prefs = new Prefs(this);

        burstMinInput = findViewById(R.id.edit_burst_min);
        burstMaxInput = findViewById(R.id.edit_burst_max);
        minWaitInput = findViewById(R.id.edit_min_wait);
        maxWaitInput = findViewById(R.id.edit_max_wait);
        initialDelayCheckbox = findViewById(R.id.checkbox_initial_delay);
        systemMaxCountInput = findViewById(R.id.edit_system_max_count);
        systemIntervalInput = findViewById(R.id.edit_system_interval);
        systemPermissionNotice = findViewById(R.id.text_system_permission_notice);
        systemSaveButton = findViewById(R.id.button_system_save);

        burstMinInput.setText(String.valueOf(prefs.getBurstMin()));
        burstMaxInput.setText(String.valueOf(prefs.getBurstMax()));
        minWaitInput.setText(String.valueOf(prefs.getMinWaitSeconds()));
        maxWaitInput.setText(String.valueOf(prefs.getMaxWaitSeconds()));
        initialDelayCheckbox.setChecked(prefs.isInitialDelayEnabled());

        findViewById(R.id.button_save).setOnClickListener(v -> saveAppSettings());
        systemSaveButton.setOnClickListener(v -> saveSystemSettings());
        findViewById(R.id.button_disband_group).setOnClickListener(v -> confirmDisbandGroup());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSystemSettings();
    }

    private void saveAppSettings() {
        int burstMin = parseOrDefault(burstMinInput, prefs.getBurstMin());
        int burstMax = parseOrDefault(burstMaxInput, prefs.getBurstMax());
        int min = parseOrDefault(minWaitInput, prefs.getMinWaitSeconds());
        int max = parseOrDefault(maxWaitInput, prefs.getMaxWaitSeconds());

        prefs.setBurstMin(Math.max(1, burstMin));
        prefs.setBurstMax(Math.max(prefs.getBurstMin(), burstMax));
        prefs.setMinWaitSeconds(Math.max(0, min));
        prefs.setMaxWaitSeconds(Math.max(prefs.getMinWaitSeconds(), max));
        prefs.setInitialDelayEnabled(initialDelayCheckbox.isChecked());
        finish();
    }

    private void loadSystemSettings() {
        boolean hasPermission = RateLimitSettings.hasPermission(this);
        int maxCount = RateLimitSettings.readMaxCount(this);
        long intervalMs = RateLimitSettings.readIntervalMs(this);

        systemMaxCountInput.setText(maxCount >= 0 ? String.valueOf(maxCount) : "");
        systemIntervalInput.setText(intervalMs >= 0 ? String.valueOf(intervalMs / 60000L) : "");
        systemMaxCountInput.setHint(maxCount >= 0 ? "" : getString(R.string.rate_limit_value_unavailable));
        systemIntervalInput.setHint(intervalMs >= 0 ? "" : getString(R.string.rate_limit_value_unavailable));

        systemMaxCountInput.setEnabled(hasPermission);
        systemIntervalInput.setEnabled(hasPermission);
        systemSaveButton.setEnabled(hasPermission);
        systemPermissionNotice.setVisibility(hasPermission ? View.GONE : View.VISIBLE);
        systemPermissionNotice.setText(getString(R.string.rate_limit_permission_missing) + "\n" + RateLimitSettings.ADB_GRANT_COMMAND);
    }

    private void saveSystemSettings() {
        int maxCount = parseOrDefault(systemMaxCountInput, -1);
        int intervalMinutes = parseOrDefault(systemIntervalInput, -1);
        if (maxCount >= 0) {
            RateLimitSettings.writeMaxCount(this, maxCount);
        }
        if (intervalMinutes >= 0) {
            RateLimitSettings.writeIntervalMs(this, intervalMinutes * 60000L);
        }
        loadSystemSettings();
    }

    private int parseOrDefault(EditText input, int defaultValue) {
        try {
            return Integer.parseInt(input.getText().toString().trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void confirmDisbandGroup() {
        int pin = 1000 + new Random().nextInt(9000);

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_disband_confirm, null);
        TextView pinView = dialogView.findViewById(R.id.text_disband_pin);
        EditText pinInput = dialogView.findViewById(R.id.edit_disband_pin);
        pinView.setText(String.valueOf(pin));

        new AlertDialog.Builder(this)
                .setTitle(R.string.disband_group_title)
                .setMessage(R.string.disband_group_warning)
                .setView(dialogView)
                .setPositiveButton(R.string.action_disband_group, (dialog, which) -> {
                    if (pinInput.getText().toString().trim().equals(String.valueOf(pin))) {
                        disbandGroup();
                    } else {
                        Toast.makeText(this, R.string.disband_pin_mismatch, Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void disbandGroup() {
        DbHelper.getInstance(this).wipeAllData();
        Toast.makeText(this, R.string.disband_group_done, Toast.LENGTH_LONG).show();

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
