# Physical-device session-board smoke

Date: 2026-08-09
Target: attached Pixel, `com.saariuslystoned.mbux` version `0.1.0` (`versionCode` 1, target SDK 36)
Scope: source-blind visual behavior check using public fixtures only

## Contract results

| Clause | Result | Observable evidence |
| --- | --- | --- |
| Install and launch the verified debug APK | Pass | Device install returned `Success`; cold launch returned `Status: ok`; `MainActivity` was the top resumed activity. |
| Open on the personal phone session board | Pass | Launch screen shows `Phone session board` and `PHONE-SIDE PERSONAL PROTOTYPE • Local fixtures • No provider or car connection`. See `01-board-launch.png` and `01-board-launch.xml`. |
| Provider picker changes visible fixture content | Pass | Selecting Codex unchecked Claude, checked Codex, and displayed the public `pixel-fold` / `done` and `stellarai` / `failed` fixtures. See `02-codex-selected.png` and `02-codex-selected.xml`. |
| Selected provider persists across reopen | Pass | After a force-stop and cold reopen, Codex remained checked and the Codex fixtures remained visible. See `03-board-reopen.png` and `03-board-reopen.xml`. |
| Microphone route is visible without capture | Pass | `Open private microphone` opened the mic-only screen with `No files • No network`, `Ready`, and `HOLD TO TALK`. The capture control was not touched. See `04-microphone-route.png` and `04-microphone-route.xml`. |
| In-app navigation returns to the board | Pass | `Open session board` returned to the board with Codex still checked. See `05-board-return.png` and `05-board-return.xml`. |
| Avoid real/external effects | Pass | No provider choice, attach/archive/delete control, account surface, network surface, Android Auto surface, or vehicle setting was exercised. |

## Permission boundary

The app already had `RECORD_AUDIO` granted in the device's existing app state. This smoke did not invoke a permission prompt, run a permission-grant command, press the hold-to-talk control, or start microphone capture.

## Out of scope

- microphone recording or audio validation;
- modifying fixture sessions beyond the local provider filter;
- provider, Mac, Cloudflare, network, Android Auto, account, or vehicle integration;
- commit, push, or deployment outside this explicitly approved attached-device install.
