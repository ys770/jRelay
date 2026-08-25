package com.sh7411usa.jrelay.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;

public class SmsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            return;
        }
        SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (messages == null || messages.length == 0) {
            return;
        }
        String sender = messages[0].getOriginatingAddress();
        if (sender == null) {
            return;
        }
        String normalized = PhoneNumberUtils.normalize(sender);
        if (normalized == null) {
            return;
        }

        StringBuilder bodyBuilder = new StringBuilder();
        for (SmsMessage message : messages) {
            if (message.getMessageBody() != null) {
                bodyBuilder.append(message.getMessageBody());
            }
        }

        new CommandProcessor(context).handleIncoming(normalized, bodyBuilder.toString());
    }
}
