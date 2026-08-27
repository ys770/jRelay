package com.sh7411usa.jrelay;

import android.app.Activity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sh7411usa.jrelay.db.MemberRepository;
import com.sh7411usa.jrelay.db.OutboxRepository;
import com.sh7411usa.jrelay.model.Member;
import com.sh7411usa.jrelay.util.UiUtil;

import java.util.List;

public class DeliveryStatusActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery_status);
        render();
    }

    private void render() {
        LinearLayout container = findViewById(R.id.container_delivery_status);
        OutboxRepository outbox = new OutboxRepository(this);
        MemberRepository members = new MemberRepository(this);
        List<OutboxRepository.OutboxItem> recent = outbox.getRecent(100);
        if (recent.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.delivery_status_empty);
            container.addView(empty);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < recent.size(); i++) {
            OutboxRepository.OutboxItem item = recent.get(i);
            View row = inflater.inflate(R.layout.row_message, container, false);
            TextView body = row.findViewById(R.id.text_message_body);
            TextView meta = row.findViewById(R.id.text_message_meta);
            body.setText(item.body);
            Member member = item.memberId == null ? null : members.findById(item.memberId);
            String recipient = member == null ? maskedNumber(item.phoneE164) : member.nickname;
            String status = statusLabel(item.status, item.errorCode);
            meta.setText(getString(R.string.delivery_status_meta, recipient, status,
                    DateFormat.format("MMM d, h:mm a", item.enqueuedAt)));
            container.addView(row);
            if (i < recent.size() - 1) {
                container.addView(UiUtil.createDivider(this, R.color.divider));
            }
        }
    }

    private String statusLabel(String status, Integer errorCode) {
        int label;
        switch (status) {
            case "PENDING": label = R.string.status_pending; break;
            case "SENDING": label = R.string.status_sending; break;
            case "SENT": label = R.string.status_sent; break;
            case "DELIVERED": label = R.string.status_delivered; break;
            case "DELIVERY_FAILED": label = R.string.status_delivery_failed; break;
            default: label = R.string.status_failed; break;
        }
        String text = getString(label);
        return errorCode == null ? text : getString(R.string.delivery_status_error, text, errorCode);
    }

    private String maskedNumber(String phone) {
        if (phone == null || phone.length() < 4) {
            return "Unknown recipient";
        }
        return "Number ending " + phone.substring(phone.length() - 4);
    }
}
