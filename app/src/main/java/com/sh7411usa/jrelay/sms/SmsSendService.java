package com.sh7411usa.jrelay.sms;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;

import com.sh7411usa.jrelay.db.OutboxRepository;
import com.sh7411usa.jrelay.util.Prefs;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SmsSendService extends Service {

    private static final String TAG = "SmsSendService";
    private static final int MAX_PART_LENGTH = 160;

    public static void start(Context context) {
        context.startService(new Intent(context, SmsSendService.class));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(() -> {
            drainOutbox();
            stopSelf(startId);
        }).start();
        return START_NOT_STICKY;
    }

    private void drainOutbox() {
        OutboxRepository outbox = new OutboxRepository(this);
        Prefs prefs = new Prefs(this);
        Random random = new Random();
        SmsManager smsManager = SmsManager.getDefault();

        try {
            if (outbox.countPending() <= 0) {
                return;
            }

            int burstSize = randomBurstSize(prefs, random);
            if (prefs.isInitialDelayEnabled()) {
                waitBeforeNextBurst(outbox, prefs, random, burstSize);
            }

            List<OutboxRepository.OutboxItem> burst = outbox.takeBurst(burstSize);
            while (!burst.isEmpty()) {
                for (OutboxRepository.OutboxItem item : burst) {
                    sendOne(smsManager, outbox, item);
                }
                if (outbox.countPending() <= 0) {
                    break;
                }
                int nextBurstSize = randomBurstSize(prefs, random);
                waitBeforeNextBurst(outbox, prefs, random, nextBurstSize);
                burst = outbox.takeBurst(nextBurstSize);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            SendQueueStatus.clear();
        }
    }

    private int randomBurstSize(Prefs prefs, Random random) {
        int min = Math.max(1, prefs.getBurstMin());
        int max = Math.max(min, prefs.getBurstMax());
        return min + (max > min ? random.nextInt(max - min + 1) : 0);
    }

    private void waitBeforeNextBurst(OutboxRepository outbox, Prefs prefs, Random random, int upcomingBurstSize)
            throws InterruptedException {
        int min = Math.max(0, prefs.getMinWaitSeconds());
        int max = Math.max(min, prefs.getMaxWaitSeconds());
        int waitSeconds = min + (max > min ? random.nextInt(max - min + 1) : 0);

        int pending = outbox.countPending();
        int displaySize = pending > 0 ? Math.min(upcomingBurstSize, pending) : upcomingBurstSize;
        SendQueueStatus.setNextBurst(System.currentTimeMillis() + waitSeconds * 1000L, displaySize);

        Thread.sleep(waitSeconds * 1000L);
    }

    private void sendOne(SmsManager smsManager, OutboxRepository outbox, OutboxRepository.OutboxItem item) {
        try {
            if (item.body.length() > MAX_PART_LENGTH) {
                ArrayList<String> parts = smsManager.divideMessage(item.body);
                smsManager.sendMultipartTextMessage(item.phoneE164, null, parts, null, null);
            } else {
                smsManager.sendTextMessage(item.phoneE164, null, item.body, null, null);
            }
            outbox.markSent(item.id);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send SMS to " + item.phoneE164, e);
            outbox.markFailed(item.id);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
