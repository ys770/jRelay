# Version History

## 1.3 - Send Queue Visibility, Burst Randomization, Initial Delay, Group Broadcast

- Dashboard now shows live queue depth ("Messages in Queue") and a live countdown to the next burst ("Next burst in Xs/Xm Ys (N messages)"), updated every second while the screen is visible. Backed by a new in-memory `SendQueueStatus` holder that `SmsSendService` publishes to as it schedules each wait.
- Rate Limiting screen's single "messages per burst" field is now a random range: **Minimum** and **Maximum** messages per burst (`Prefs.burstMin`/`burstMax`), matching the existing min/max wait-time randomization. `SmsSendService` picks a random burst size in that range before each burst.
- Added an **initial delay** checkbox (checked by default) on Rate Limiting: when enabled, jRelay waits a random duration (drawn from the same min/max wait-between-bursts range) before sending the very first burst of a new send cycle, instead of relaying immediately. Unchecked sends right away.
- Every EditText on the Rate Limiting screen now has a small bold header label above it (in addition to its hint) so the field's purpose stays visible once text is entered.
- Added **Send to Group**: a dashboard button opens a dedicated `SendGroupMessageActivity` where an admin composes a message that's sent (as `[Admin]: ...`) to every active member regardless of mute state, via `CommandProcessor.broadcastToGroup`.
- Added `OutboxRepository.countUnsent()`/`countPending()` to back the new queue-depth display.

## 1.2 - Documentation

- Added `README.md` documenting all features (dashboard, membership, member detail, add member, rate limiting), the full command list with exact syntax and message templates, mute/removal semantics, permissions, and build instructions.

## 1.1 - Bug Fix

- Fixed a crash when adding a member whose phone number belonged to a previously removed (soft-deleted) member. `phone_e164` is UNIQUE in the `members` table, and soft-deleted rows keep their number, so re-adding it via `#add` or the Add Member screen hit `insertOrThrow` and threw an uncaught `SQLiteConstraintException`. `CommandProcessor.addMember` now reactivates the existing row (`MemberRepository.reactivate`) instead of inserting a duplicate when the number belongs to an inactive member.

## 1.0 - Initial Build

- Stripped all androidx/Jetpack dependencies from the generated template (AppCompatActivity, ConstraintLayout, Material Components, activity-ktx) in favor of vanilla `android.app.Activity` and framework widgets/themes, per project instructions.
- Added SQLite schema (`DbHelper`) with `members`, `message_log`, and `outbox` tables.
- Implemented phone number normalization (`PhoneNumberUtils`) supporting common US formats (+1XXXXXXXXXX, +1 (XXX) XXX-XXXX, (XXX) XXX-XXXX, XXX-XXX-XXXX, 1-XXX-XXX-XXXX, 10-digit, etc.), including numbers with internal spaces when parsing `#add <number> <nickname>`.
- Implemented the SMS relay pipeline: `SmsReceiver` (incoming SMS) -> `CommandProcessor` (parsing/business logic) -> `outbox` table -> `SmsSendService` (rate-limited/staggered sending in configurable bursts with randomized wait between bursts).
- Implemented all member commands: `#commands`, `#mute`, `#unmute`, `#stop`, `#list`, `#admin <message>` (admin-only relay + host notification), `#add <number> <nickname>` and `#remove <nickname|number>` (admin-only, with an "Only admins can use this command." reply for non-admins).
- Implemented all add/remove/mute/stop message templates and broadcasts to the rest of the group.
- Implemented soft-delete for removed/`#stop`'d members so message history and stats are preserved, while excluding them from relay, `#list`, and the membership screen.
- Muting pauses both directions: a muted member neither receives relayed/admin broadcasts nor has their own plain-text messages relayed, while slash-commands still work.
- Added first-run `ConsentActivity`: SMS/data-rate and legal-liability disclaimer that must be accepted before use (re-shown every launch until accepted), followed by runtime permission requests (`SEND_SMS`, `RECEIVE_SMS`, `POST_NOTIFICATIONS`).
- Added `MainActivity` dashboard: group name + rename, member/admin/muted counts, today/total message counts, scrollable recent activity feed, Add Member shortcut, navigation to Membership and Rate Limiting screens.
- Added `MembershipActivity`: scrollable list of active members with an admin badge, tap-through to member detail.
- Added `MemberDetailActivity`: per-member stats (messages sent/received, member since, last activity, 7-day activity level) and recent activity; actions to toggle admin/mute, remove, send a direct message, edit nickname/number, export contact, call, and text via the default SMS app.
- Added `AddMemberActivity` for adding members from the UI (recorded as "An Admin" in broadcasts/templates).
- Added `RateLimitActivity`: configurable burst size and randomized min/max wait between bursts for jRelay's own send pacing, plus a section to view/edit Android's built-in outgoing-SMS throttle (`sms_outgoing_check_max_count` / `sms_outgoing_check_interval_ms`) when `WRITE_SECURE_SETTINGS` is granted, or the exact `adb shell pm grant` command to grant it when it isn't.
- Added a notification channel/alert on the host device whenever a `#admin` message is relayed.
- Added DPAD-focus highlighting on all interactive rows/buttons and made every screen a `ScrollView`-rooted vertical `LinearLayout` per UI guidelines.
- Removed the default instrumented test (`androidTest`) along with its androidx.test dependencies, keeping only plain JUnit for unit tests.
