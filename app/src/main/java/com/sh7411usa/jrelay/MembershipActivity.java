package com.sh7411usa.jrelay;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sh7411usa.jrelay.db.MemberRepository;
import com.sh7411usa.jrelay.model.Member;

import java.util.List;

public class MembershipActivity extends Activity {

    private MemberRepository memberRepository;
    private LinearLayout container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_membership);
        memberRepository = new MemberRepository(this);
        container = findViewById(R.id.container_members);
        findViewById(R.id.button_add_member).setOnClickListener(v ->
                startActivity(new Intent(this, AddMemberActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderMembers();
    }

    private void renderMembers() {
        container.removeAllViews();
        List<Member> members = memberRepository.getActiveMembers();
        if (members.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.no_members_yet);
            container.addView(empty);
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(this);
        for (Member member : members) {
            View row = inflater.inflate(R.layout.row_member, container, false);
            TextView nicknameView = row.findViewById(R.id.text_member_nickname);
            TextView badgeView = row.findViewById(R.id.text_member_badge);

            StringBuilder badge = new StringBuilder();
            if (member.isAdmin) {
                badge.append(getString(R.string.admin_badge));
            }
            if (member.isMuted) {
                if (badge.length() > 0) {
                    badge.append(" ");
                }
                badge.append(getString(R.string.muted_badge));
            }
            nicknameView.setText(member.nickname);
            badgeView.setText(badge.toString());

            long memberId = member.id;
            row.setOnClickListener(v -> {
                Intent intent = new Intent(MembershipActivity.this, MemberDetailActivity.class);
                intent.putExtra(MemberDetailActivity.EXTRA_MEMBER_ID, memberId);
                startActivity(intent);
            });
            container.addView(row);
        }
    }
}
