# Block apps, websites, or feeds

Independent blocks work without a focus session. Ending a session does not remove them. The three-icon dock and profile/settings header remain unchanged.

## Three steps

1. Choose targets in **Rules**, then tap **Block selected apps/websites/feeds**. For a single target, use its clock-lock icon. Home's **Block apps** and **Block websites** cards are shortcuts.
2. Choose an ending and resistance mode. Expand **Choose targets** for select-all/clear or individual selection.
3. Tap **Block now**. If permissions need setup, open Settings, connect protection, then return with Back; the draft remains intact.

Home lists active groups, selected targets, their mode, and a precise ending. Longer permission, privacy, and coverage explanations are expandable. App cards use installed-app icons. Controls retain accessible labels and minimum touch sizes.

## Endings

| Choice | Meaning |
| --- | --- |
| Today | Until the next midnight in the device's timezone |
| 1 day | The same local time tomorrow |
| 1 week | The same local time seven calendar days later |
| Set minutes | A timer of 1–1,440 minutes |
| Until I unblock | Persistent block with no automatic expiry |

Calendar day/week options respect daylight-saving changes, so they do not always equal exactly 24/168 hours. Deadlines are saved as timestamps. Normal-phone clock changes can affect expiry; managed Strict also restricts clock configuration. Existing deadlines remain the same instant when the timezone changes.

## Modes and unblocking

- **Normal:** a compact popup with temporary visits. The Unblock control asks for confirmation.
- **Resistant:** a 30-second pause and reason for a one-minute visit. Removing the whole group also requires a 30-second pause and reason, checked by the repository.
- **Strict:** no temporary visits or early unblock inside Focus. Requires an automatic ending and explicit acknowledgement. Rules stay locked while Strict is active. Manual indefinite Strict is rejected by the backend.

Unblock controls are on Home. Expired groups are removed automatically. Manual blocks persist through normal app restarts and reboots. Starting the same target group with the same mode and period refreshes its existing block rather than duplicating it. Up to 100 groups are supported.

Independent groups capture their selected targets. Editing a focus profile or removing an item from the focus selection does not silently end an active group. **Always allowed** exceptions retain precedence. When multiple sessions, schedules, budgets, and groups apply, the strongest matching mode wins; equal-mode deadlines use the latest applicable ending. Strict ignores older temporary visits.

## Coverage

Whole-app protection uses the consented accessibility service. Website and selective feed detection remain experimental and depend on controls exposed by supported browsers/social apps; whole-app blocking is the stronger fallback. New lifetime choices do not turn this into DNS/VPN filtering.

Optional device-owner Strict app suspension supports independent whole-app groups, with exact-alarm expiry and journaled recovery. Managed website/feed-only groups cannot claim OS enforcement; choose the whole app or use ordinary accessibility protection. See [managed setup](MANAGED_DEVICE.md). Ordinary Android settings can still revoke accessibility or deactivate legacy administration.
