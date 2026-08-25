# jRelay

jRelay turns a single Android phone into a relay hub for a group SMS conversation. Members text the host device's phone number; the host fans each message back out to every other active member. Group membership, admin messages, and send-pacing are all managed from commands sent by text or from the app itself.

Built as a vanilla Android Java app (no Kotlin, no Jetpack/androidx) with a local SQLite database — see `CLAUDE.md` for the project's coding constraints.

## How it works

There is no group MMS thread and no third-party service involved. jRelay only uses standard SMS:

1. A member texts the host device's number.
2. jRelay reads the message, checks the sender against its member list, and either runs a command or relays the message.
3. A relayed message is queued and sent out individually to every other active, unmuted member, prefixed with the sender's nickname (e.g. `Alex: on my way`).

Because everything rides on regular carrier SMS, standard messaging and data rates apply to every message sent and received, and you are responsible for complying with all applicable laws and carrier policies around automated/bulk messaging. This is disclosed and must be accepted the first time the app is opened.

## First run

On first launch, jRelay shows a disclosure screen covering:

- Carrier messaging/data rates apply to all relayed messages.
- You are responsible for legal and carrier-policy compliance for automated messaging.
- The app is provided as-is with no warranty.

You must tap **I Agree** to proceed — tapping **Decline** closes the app, and the same screen reappears next launch until it's accepted. Agreeing triggers the runtime permission prompts for SMS send/receive (and notifications, on Android 13+).

## Screens

### Dashboard (Main screen)

- Group name, with an **Edit** button to rename the group.
- Live stats: member count, admin count, muted count, messages sent today, and messages sent all-time.
- **Messages in Queue** — how many outbox messages are waiting to go out or mid-send right now.
- **Next burst in Xs (N messages)** — a live countdown (updates every second) to when the send service will fire its next burst, and how many messages that burst will contain. Hidden when nothing is scheduled (queue empty, or a burst is actively sending).
- A scrollable feed of the most recent activity across the whole group.
- **Add Member** shortcut.
- Buttons to open the **Membership** and **Rate Limiting** screens, and to open **Send to Group**.

### Membership

- A scrollable list of all active members, showing each nickname with an `(admin)` badge and/or a `(muted)` badge where applicable.
- Tap any member to open their detail screen.
- **Add Member** shortcut.

### Member Detail

Tapping a member shows:

- Nickname and phone number.
- Member since date, last activity timestamp, messages sent/received counts, and an activity level (Very active / Active / Quiet / Inactive) based on their message volume over the last 7 days.
- A scrollable feed of that member's recent activity.

Actions available:

| Action | Effect |
|---|---|
| Make Admin / Revoke Admin | Toggles admin status |
| Mute / Unmute | Toggles mute (see [Mute](#mute-behavior) below) |
| Send Direct Message | Sends a one-off `[Admin]: ...` text to just this member |
| Edit Nickname | Renames the member |
| Edit Phone Number | Updates their stored number |
| Export Contact | Opens the Contacts app pre-filled with this member's name/number to save |
| Call | Opens the dialer with this member's number |
| Text (Default App) | Opens your default SMS app with this member's number, for an off-the-record text outside the relay |
| Remove from Group | Removes the member (with confirmation) — see [Removal](#removal) below |

### Add Member

A simple form (phone number + nickname) for adding a member from within the app. Members added this way are recorded as added by **"An Admin"** in all broadcasts, rather than a specific admin's nickname.

### Rate Limiting

Two independent controls:

- **jRelay Send Pacing**:
  - **Minimum/maximum messages per burst** — each burst's size is chosen at random from this range, rather than being fixed.
  - **Minimum/maximum wait between bursts (seconds)** — jRelay waits a random duration in this range before sending the next burst.
  - **Wait before sending the first burst** (checkbox, checked by default) — when checked, jRelay waits a random duration (drawn from the same wait range above) before sending the very first burst of a new send cycle, instead of relaying the moment a message comes in. Uncheck it to relay immediately.
  - This is jRelay's own throttle, always in effect, and needs no special permission. Every field has a small label above it so its purpose stays visible even once you've typed a value.
- **Android Outgoing SMS Limit** — Android itself has a built-in threshold (`sms_outgoing_check_max_count` / `sms_outgoing_check_interval_ms`) that warns/blocks an app sending too many texts too fast. jRelay can read and display the device's current values always. Editing them requires the `WRITE_SECURE_SETTINGS` permission, which apps cannot be granted through a normal permission prompt — if it's missing, the screen shows the current values (read-only) plus the exact command to run:

  ```
  adb shell pm grant com.sh7411usa.jrelay android.permission.WRITE_SECURE_SETTINGS
  ```

  Run that from a computer with the device connected over ADB, then reopen the screen to edit the system values.

### Send to Group

A dedicated screen (opened from the dashboard's **Send to Group** button) for sending a one-off admin message to every active member at once — regardless of anyone's mute state — formatted the same way as a direct message (`[Admin]: ...`). Useful for group-wide announcements that shouldn't wait on someone muting/unmuting.

## Commands (sent by text from any member)

| Command | Who can use it | What it does |
|---|---|---|
| `#commands` | anyone | Replies with the list of available commands |
| `#mute` | anyone | Pauses messages for you (see below) |
| `#unmute` | anyone | Resumes messages |
| `#stop` | anyone | Leaves the group |
| `#list` | anyone | Replies with a newline list of member nicknames only — no phone numbers. Shows `(You)` next to your own entry and `(admin)` next to admins |
| `#admin <message>` | anyone | Sends `<message>` to admins only, and pops up a notification on the host device. Non-admins can use this to reach admins directly |
| `#add <number> <nickname>` | admins only | Adds a new member |
| `#remove <nickname or number>` | admins only | Removes a member |

Non-admins attempting `#add` or `#remove` get back: `"Only admins can use this command."` An unrecognized `#` command gets: `"Unknown command. Reply #commands for a list of commands."`

### Adding a member by text

```
#add +12345678910 Alex
#add (234) 567-8910 Alex Smith
#add +1 (234) 567-8910 Alex Smith
```

Nicknames can contain spaces. jRelay accepts common US phone formats: `+12345678910`, `+1 (234) 567-8910`, `1-234-567-8910`, `234-567-8910`, `2345678910`, and `(234) 567-8910`.

When a member is added (by text or from the app), they receive:
> `<admin> added you to <group name> Group. Reply #stop at anytime to opt out.`

Everyone else in the group receives:
> `<admin> added <nickname> to the group.`

### Removing a member by text

```
#remove Alex
#remove +12345678910
```

The removed member receives:
> `You have been removed from <group name> Group. You will no longer receive messages from this Group.`

Everyone else receives:
> `<admin> removed <nickname> from the group.`

Removal is a soft delete — the member's history and stats are preserved (visible again if you're troubleshooting), but they're excluded from relaying, `#list`, and the Membership screen. They can be re-added later with `#add`/Add Member using the same number, which reactivates their original record rather than creating a duplicate.

### Mute behavior

Muting pauses messages in **both directions**: while muted, you neither receive relayed or admin messages, nor does anything you send in plain text get relayed to the group. Commands (`#unmute`, `#stop`, `#commands`, `#list`) still work normally while muted, so you're never stuck.

### Leaving the group (`#stop`)

Sending `#stop` removes you from the group immediately (same soft-delete as an admin removal). You get a confirmation, and everyone else is notified that you left:
> `<nickname> left the group.`

## Permissions

| Permission | Why | How it's granted |
|---|---|---|
| `SEND_SMS` | Sending relayed/command messages | Runtime prompt at first run |
| `RECEIVE_SMS` | Reading incoming messages to relay | Runtime prompt at first run |
| `POST_NOTIFICATIONS` (Android 13+) | Notifying you of `#admin` messages | Runtime prompt at first run |
| `WRITE_SECURE_SETTINGS` | Editing Android's built-in SMS throttle | Not requestable through a dialog — grant manually via `adb` (see [Rate Limiting](#rate-limiting)) |

jRelay does not need to be set as your default SMS app, and does not request contacts or call permissions — exporting a contact, calling, and texting outside the relay all hand off to your Contacts/Phone/Messages apps via intents instead.

## Data storage

Everything is stored locally in a SQLite database on the device — there is no server or cloud component. Three tables back the app: members, a full message log (used for dashboard/member stats), and an outbox queue that `SmsSendService` drains according to your rate-limiting settings.

## Building

```
./gradlew assembleDebug
```

Requires Android SDK with `compileSdk 36` / `minSdk 24`. See `VERSION.md` for the full change history.
