package com.sh7411usa.jrelay;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sh7411usa.jrelay.db.MemberRepository;
import com.sh7411usa.jrelay.db.MessageRepository;
import com.sh7411usa.jrelay.model.Member;
import com.sh7411usa.jrelay.model.MessageRecord;
import com.sh7411usa.jrelay.util.Prefs;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {

    private Prefs prefs;
    private MemberRepository memberRepository;
    private MessageRepository messageRepository;

    private TextView groupNameView;
    private TextView statsMembersView;
    private TextView statsAdminsView;
    private TextView statsMutedView;
    private TextView statsMessagesTodayView;
    private TextView statsMessagesTotalView;
    private LinearLayout recentActivityContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new Prefs(this);
        if (!prefs.isConsentAccepted()) {
            startActivity(new Intent(this, ConsentActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        memberRepository = new MemberRepository(this);
        messageRepository = new MessageRepository(this);

        groupNameView = findViewById(R.id.text_group_name);
        statsMembersView = findViewById(R.id.text_stats_members);
        statsAdminsView = findViewById(R.id.text_stats_admins);
        statsMutedView = findViewById(R.id.text_stats_muted);
        statsMessagesTodayView = findViewById(R.id.text_stats_messages_today);
        statsMessagesTotalView = findViewById(R.id.text_stats_messages_total);
        recentActivityContainer = findViewById(R.id.container_recent_activity);

        findViewById(R.id.button_edit_group_name).setOnClickListener(v -> showRenameDialog());
        findViewById(R.id.button_add_member).setOnClickListener(v ->
                startActivity(new Intent(this, AddMemberActivity.class)));
        findViewById(R.id.button_membership).setOnClickListener(v ->
                startActivity(new Intent(this, MembershipActivity.class)));
        findViewById(R.id.button_rate_limit).setOnClickListener(v ->
                startActivity(new Intent(this, RateLimitActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!prefs.isConsentAccepted()) {
            return;
        }
        refresh();
    }

    private void refresh() {
        groupNameView.setText(prefs.getGroupName());

        List<Member> members = memberRepository.getActiveMembers();
        int adminCount = 0;
        int mutedCount = 0;
        for (Member m : members) {
            if (m.isAdmin) {
                adminCount++;
            }
            if (m.isMuted) {
                mutedCount++;
            }
        }
        statsMembersView.setText(getString(R.string.stats_members, members.size()));
        statsAdminsView.setText(getString(R.string.stats_admins, adminCount));
        statsMutedView.setText(getString(R.string.stats_muted, mutedCount));

        long startOfDay = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1);
        statsMessagesTodayView.setText(getString(R.string.stats_messages_today, messageRepository.countSince(startOfDay)));
        statsMessagesTotalView.setText(getString(R.string.stats_messages_total, messageRepository.countAll()));

        renderRecentActivity();
    }

    private void renderRecentActivity() {
        recentActivityContainer.removeAllViews();
        List<MessageRecord> recent = messageRepository.getRecent(20);
        if (recent.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.no_activity_yet);
            recentActivityContainer.addView(empty);
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(this);
        for (MessageRecord record : recent) {
            View row = inflater.inflate(R.layout.row_message, recentActivityContainer, false);
            TextView bodyView = row.findViewById(R.id.text_message_body);
            TextView metaView = row.findViewById(R.id.text_message_meta);
            bodyView.setText(record.body);
            String memberLabel = "";
            if (record.memberId != null) {
                Member m = memberRepository.findById(record.memberId);
                if (m != null) {
                    memberLabel = m.nickname + " • ";
                }
            }
            metaView.setText(memberLabel + DateFormat.format("MMM d, h:mm a", record.timestamp));
            recentActivityContainer.addView(row);
        }
    }

    private void showRenameDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_text_input, null);
        EditText input = dialogView.findViewById(R.id.edit_text_input);
        input.setText(prefs.getGroupName());
        new AlertDialog.Builder(this)
                .setTitle(R.string.edit_group_name)
                .setView(dialogView)
                .setPositiveButton(R.string.action_save, (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        prefs.setGroupName(newName);
                        refresh();
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }
}
