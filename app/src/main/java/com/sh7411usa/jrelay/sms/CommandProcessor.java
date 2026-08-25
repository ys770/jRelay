package com.sh7411usa.jrelay.sms;

import android.content.Context;

import com.sh7411usa.jrelay.R;
import com.sh7411usa.jrelay.db.MemberRepository;
import com.sh7411usa.jrelay.db.MessageRepository;
import com.sh7411usa.jrelay.db.OutboxRepository;
import com.sh7411usa.jrelay.model.Member;
import com.sh7411usa.jrelay.util.NotificationHelper;
import com.sh7411usa.jrelay.util.Prefs;

import java.util.List;

public class CommandProcessor {

    private final Context context;
    private final MemberRepository memberRepository;
    private final MessageRepository messageRepository;
    private final OutboxRepository outboxRepository;
    private final Prefs prefs;

    public CommandProcessor(Context context) {
        this.context = context.getApplicationContext();
        memberRepository = new MemberRepository(this.context);
        messageRepository = new MessageRepository(this.context);
        outboxRepository = new OutboxRepository(this.context);
        prefs = new Prefs(this.context);
    }

    /** Entry point for an inbound SMS from an already-normalized sender number. */
    public void handleIncoming(String senderE164, String body) {
        Member sender = memberRepository.findByPhone(senderE164);
        if (sender == null || !sender.active) {
            return;
        }

        messageRepository.log(sender.id, "IN", "RELAY", body);

        String trimmed = body == null ? "" : body.trim();
        if (trimmed.startsWith("#")) {
            handleCommand(sender, trimmed);
        } else if (!sender.isMuted) {
            relayPlainMessage(sender, trimmed);
        }

        SmsSendService.start(context);
    }

    private void handleCommand(Member sender, String text) {
        String lower = text.toLowerCase();
        if (lower.equals("#commands")) {
            reply(sender, context.getString(R.string.tpl_commands_list));
        } else if (lower.equals("#mute")) {
            memberRepository.setMuted(sender.id, true);
            reply(sender, context.getString(R.string.tpl_muted_confirm));
        } else if (lower.equals("#unmute")) {
            memberRepository.setMuted(sender.id, false);
            reply(sender, context.getString(R.string.tpl_unmuted_confirm));
        } else if (lower.equals("#stop")) {
            handleStop(sender);
        } else if (lower.equals("#list")) {
            handleList(sender);
        } else if (lower.startsWith("#admin")) {
            handleAdminMessage(sender, text);
        } else if (lower.startsWith("#add")) {
            handleAdd(sender, text);
        } else if (lower.startsWith("#remove")) {
            handleRemove(sender, text);
        } else {
            reply(sender, context.getString(R.string.tpl_unknown_command));
        }
    }

    private void handleStop(Member sender) {
        memberRepository.softRemove(sender.id);
        String groupName = prefs.getGroupName();
        reply(sender, context.getString(R.string.tpl_removed_you, groupName));
        broadcastExcept(sender.id, context.getString(R.string.tpl_left_other, sender.nickname));
    }

    private void handleList(Member requester) {
        List<Member> members = memberRepository.getActiveMembers();
        StringBuilder sb = new StringBuilder();
        for (Member m : members) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(m.nickname);
            if (m.id == requester.id) {
                sb.append(" ").append(context.getString(R.string.tpl_list_you));
            }
            if (m.isAdmin) {
                sb.append(" ").append(context.getString(R.string.tpl_list_admin));
            }
        }
        reply(requester, sb.toString());
    }

    private void handleAdminMessage(Member sender, String text) {
        String message = stripLeadingWord(text).trim();
        if (message.isEmpty()) {
            return;
        }
        String formatted = context.getString(R.string.tpl_admin_relay_prefix, sender.nickname, message);
        List<Member> admins = memberRepository.getActiveAdmins();
        for (Member admin : admins) {
            if (admin.id == sender.id || admin.isMuted) {
                continue;
            }
            enqueue(admin, formatted, "ADMIN");
        }
        NotificationHelper.showAdminMessage(context, formatted);
    }

    private void handleAdd(Member sender, String text) {
        if (!sender.isAdmin) {
            reply(sender, context.getString(R.string.tpl_unauthorized));
            return;
        }
        String rest = stripLeadingWord(text).trim();
        String[] parts = PhoneNumberUtils.splitLeadingNumberAndRest(rest);
        if (parts == null) {
            reply(sender, context.getString(R.string.tpl_invalid_number));
            return;
        }
        String normalized = PhoneNumberUtils.normalize(parts[0]);
        String nickname = parts[1].trim();
        if (normalized == null) {
            reply(sender, context.getString(R.string.tpl_invalid_number));
            return;
        }
        if (nickname.isEmpty()) {
            reply(sender, context.getString(R.string.error_empty_nickname));
            return;
        }
        Member existing = memberRepository.findByPhone(normalized);
        if (existing != null && existing.active) {
            reply(sender, context.getString(R.string.error_duplicate_number));
            return;
        }
        addMember(normalized, nickname, sender.nickname);
    }

    /** Adds a member and sends the standard welcome/broadcast messages. addedByLabel is either an admin's nickname or "An Admin". */
    public void addMember(String normalizedPhone, String nickname, String addedByLabel) {
        long id = memberRepository.insert(normalizedPhone, nickname, false, addedByLabel);
        Member newMember = memberRepository.findById(id);
        String groupName = prefs.getGroupName();
        enqueue(newMember, context.getString(R.string.tpl_added_you, addedByLabel, groupName), "SYSTEM");
        broadcastExcept(id, context.getString(R.string.tpl_added_other, addedByLabel, nickname));
        SmsSendService.start(context);
    }

    private void handleRemove(Member sender, String text) {
        if (!sender.isAdmin) {
            reply(sender, context.getString(R.string.tpl_unauthorized));
            return;
        }
        String rest = stripLeadingWord(text).trim();
        if (rest.isEmpty()) {
            return;
        }
        Member target = memberRepository.findActiveByNickname(rest);
        if (target == null) {
            String normalized = PhoneNumberUtils.normalize(rest);
            if (normalized != null) {
                Member byPhone = memberRepository.findByPhone(normalized);
                if (byPhone != null && byPhone.active) {
                    target = byPhone;
                }
            }
        }
        if (target == null) {
            return;
        }
        removeMember(target, sender.nickname);
    }

    /** Removes a member and sends the standard notice/broadcast messages. removedByLabel is either an admin's nickname or "An Admin". */
    public void removeMember(Member target, String removedByLabel) {
        memberRepository.softRemove(target.id);
        String groupName = prefs.getGroupName();
        enqueue(target, context.getString(R.string.tpl_removed_you, groupName), "SYSTEM");
        broadcastExcept(target.id, context.getString(R.string.tpl_removed_other, removedByLabel, target.nickname));
        SmsSendService.start(context);
    }

    /** Sends a one-off admin direct message to a member, regardless of their mute state. */
    public void sendDirectMessage(Member target, String body) {
        String formatted = context.getString(R.string.tpl_dm_prefix, body);
        enqueue(target, formatted, "DM");
        SmsSendService.start(context);
    }

    private void relayPlainMessage(Member sender, String body) {
        String formatted = context.getString(R.string.tpl_relay_prefix, sender.nickname, body);
        broadcastExcept(sender.id, formatted, "RELAY");
    }

    private void broadcastExcept(long excludeId, String message) {
        broadcastExcept(excludeId, message, "SYSTEM");
    }

    private void broadcastExcept(long excludeId, String message, String category) {
        List<Member> recipients = memberRepository.getActiveRecipientsExcept(excludeId);
        for (Member m : recipients) {
            enqueue(m, message, category);
        }
    }

    private void reply(Member recipient, String message) {
        enqueue(recipient, message, "COMMAND");
    }

    private void enqueue(Member recipient, String message, String category) {
        outboxRepository.enqueue(recipient.id, recipient.phoneE164, message);
        messageRepository.log(recipient.id, "OUT", category, message);
    }

    private String stripLeadingWord(String text) {
        int spaceIdx = text.indexOf(' ');
        if (spaceIdx < 0) {
            return "";
        }
        return text.substring(spaceIdx + 1);
    }
}
