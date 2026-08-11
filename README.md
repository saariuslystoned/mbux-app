# MBUX Companion

An Android companion for a private dispatcher/control plane, starting as a personal-use phone app and retaining a separately gated Android Auto direction.

This repository currently contains **only a buildable phone-side Compose prototype and the product contracts**. It has a fixture-backed personal session board, an explicit OS-owned task handoff, and a separate push-to-talk screen for real, local microphone capture. The app declares no network permission, makes no direct broker or provider connection, declares no Android Auto support, and controls no vehicle capability.

## Personal-use phone session board

The S1 foundation is a local, sideload-oriented phone prototype. It intentionally uses representative public fixtures rather than discovering or contacting sessions on a Mac:

- three top-level phone destinations separate the Claude board, Codex board, and Dispatch review flow; the last selected provider board is retained in app-private preferences;
- only fixtures labeled as MBUX-launched or explicitly attached are shown;
- cards show provider, repository, short task title, one bounded state, and an artifact-ready marker when applicable;
- archive, restore, and delete are local phone controls, with fixture records retained in app-private preferences until deletion;
- `needs you` fixtures alone reveal a concise blocker and up to three numbered choices; choosing one changes only local simulated state and sends nothing; and
- attach existing presents at most three local fixture matches and requires an explicit tap. The Claude and Codex boards are display/fixture-management surfaces and do not contain task creation controls.

No provider session, account, credential, private task, captured audio, or Mac inventory is stored in the board. This foundation is separate from Android Auto category eligibility and does not imply that Claude or Codex runs in a vehicle surface.

## Explicit phone handoff

The top-level **Dispatch** destination is the only review-and-handoff surface. This credential-free proof mode is fixed to the public repository **`saariuslystoned/mbux-app`**: there is no repository selector or editable repository field. The user must choose Claude or Codex / ChatGPT, edit the task brief, and then deliberately continue. Nothing is automatically submitted, added to either board, or represented as a provider session.

- The shared supported lane uses Android's documented `ACTION_SEND` text flow and the system chooser. MBUX does not force a destination, inspect the chooser result, or claim that the receiving app created a task.
- Claude alone has an optional documented Claude Code draft-composer route. It opens a prefilled draft; Claude owns the final review and send.
- Codex / ChatGPT uses the generic chooser only. This project does not guess private deep-link parameters or infer task status from an app launch.
- A credential-backed repository catalog and repository authorization are deferred to a separately approved future slice; this proof makes no GitHub API or OAuth request.
- Voice transcription is not implemented. The screen's editable typed draft is the input boundary for this slice.

This is an explicit handoff to another phone app, not provider integration. There is no result callback, real status tracking, Mac relay, provider SDK, credential, network transport, or captured-audio transfer. The supported boundary follows the [Android Sharesheet guidance](https://developer.android.com/develop/ui/compose/sharing/send) and Anthropic's [documented Claude mobile link behavior](https://support.claude.com/en/articles/14898120-open-the-claude-mobile-app-with-a-link).

On the physically tested Pixel Fold, the receiving apps behaved differently: the generic ChatGPT destination opened a regular chat, while Claude entered its Code/Dispatch experience and attempted its own Mac continuation. This is observed destination behavior only—not a provider API contract, task-creation confirmation, callback, or status signal to MBUX.

## Voice entry to Dispatch

The app declares the public App Actions built-in intent `actions.intent.OPEN_APP_FEATURE` with one inline feature named **Dispatch**. Its fulfillment opens the exported `mbux://dispatch` route, which navigates directly to the fixed-repository Dispatch review screen. A normal launcher start still opens the existing session board.

The intended unambiguous request is **“Open Dispatch in MBUX”** (or the app's full published invocation name). The feature is deliberately titled **Dispatch**, so the shorter **“Open Dispatch”** is as discoverable as the platform permits when MBUX is already in context or the resolver can identify it. MBUX cannot override another installed app or system ambiguity, so the short phrase is not guaranteed.

This integration is bounded navigation, not dictation or automation. Assistant/Gemini does not pass a task brief, select a provider, create a session, invoke ChatGPT Voice, or submit anything. MBUX opens its review screen; any later handoff still requires the existing deliberate action, and the destination app owns final review/send.

Google's current documentation makes two deployment limits important:

- feature-specific App Actions are documented for Google Assistant and require Google Play publication plus App Actions review for production, or a same-account App Actions Test Tool preview during development; a sideloaded debug APK alone does not make the voice phrase available; and
- Gemini's Utilities documentation guarantees generic **Open [app name]** behavior, but does not guarantee that Gemini resolves a third-party `OPEN_APP_FEATURE` capability. Gemini may hand compatible requests to the Android/Assistant resolver on some configurations, but this project does not claim that behavior as universal.

See the official [Open app feature BII](https://developer.android.com/reference/app-actions/built-in-intents/common/open-app-feature), [App Actions build and deployment requirements](https://developer.android.com/develop/devices/assistant/get-started), and [Gemini Utilities capabilities](https://support.google.com/gemini/answer/15235441).

## Current microphone scope

The implemented capability is intentionally limited to the phone microphone:

- the app asks for `RECORD_AUDIO` only after the user taps **Enable microphone**;
- denial and permanent denial remain explicit, fail-closed UI states;
- recording begins only on an intentional push-to-talk press and stops on release, cancel, audio-focus loss, a 30-second limit, activity backgrounding, or teardown;
- captured audio is read in short PCM slices, immediately overwritten, and never exposed outside the local capture component; and
- there is no transcription, playback, session routing, provider call, broker connection, analytics, or upload.

A physical-device microphone smoke test is a separate verification step. The local unit tests use a capture boundary and never access host or device audio.

## Initial proof target

The first end-to-end proof is deliberately narrow:

```text
supported Android Auto microphone action
  -> user-held push-to-talk capture
  -> authenticated private broker
  -> broker-owned OpenAI Realtime session
  -> streamed voice response through the supported car audio path
```

The app must receive microphone audio through Android Auto's supported in-app path, such as `CarAudioRecord`, only after an explicit user action and permission grant. Audio goes to the private broker for OpenAI processing; Gemini is not the request interpreter in this product flow. The app must acquire audio focus, show the host-provided recording indication, stop on release/cancel/focus loss, and avoid retaining raw audio by default.

That proof is **not implemented yet**. Before adding a `CarAppService` or Android Auto manifest declaration, the product must establish that the experience belongs to a currently supported Android Auto app category and satisfies the applicable quality and distraction rules. A private AI/task console is not assumed to qualify as messaging, media, navigation, POI, IoT, weather, or another supported category.

## Unified session registry

The broker is the authority for one list spanning provider-backed work. A client-facing record is expected to resemble:

```json
{
  "id": "session_demo_01",
  "provider": "openai",
  "title": "Plan the weekend drive",
  "status": "ready",
  "updatedAt": "2026-08-09T12:00:00Z",
  "capabilities": ["text", "voice", "resume"]
}
```

`provider` will initially distinguish `openai` and `claude_code`. The broker maps this public opaque ID to provider session identifiers, owns create/resume routing, and returns only the metadata the Android client needs. Provider credentials and raw provider-session mappings never belong in the APK.

## Architecture intent

- **Android client:** renders separate Claude/Codex fixture boards, a reviewed Dispatch handoff, and an explicit push-to-talk state machine.
- **Private broker/control plane:** authenticates the device, holds provider credentials, owns the unified registry and provider ID mappings, and terminates OpenAI Realtime connections.
- **Providers:** OpenAI-backed conversations and Claude Code tasks remain provider-native behind broker adapters.
- **Vehicle boundary:** Android Auto is a presentation and supported microphone/audio surface only. The product never reads or controls vehicle systems.

The client now also contains a credential-free, read-only CP-1 status contract and an action-free status panel. It accepts only normalized, opaque session summaries and distinguishes not enrolled, unknown, live, stale, and disconnected state. This build deliberately installs a no-I/O `Not enrolled` feed: it has no endpoint, network permission, embedded credential, live adapter, or fixture fallback posing as remote truth.

See [AGENTS.md](AGENTS.md) for the durable safety, security, architecture, and verification contract.

## Build

Prerequisites:

- JDK 17
- Android SDK Platform 36 and Build Tools 36.0.0

Then run:

```bash
./gradlew clean testDebugUnitTest assembleDebug lint
```

The debug APK is generated under `app/build/outputs/apk/debug/`.

The wrapper pins Gradle 9.4.1 and the project pins Android Gradle Plugin 9.2.0. Do not commit `local.properties`; use it only for a developer machine's SDK path.

## Current layout

```text
app/              Minimal Android application module
gradle/wrapper/   Pinned Gradle wrapper
AGENTS.md         Product and contributor contract
README.md         Scope, proof target, and build entry point
VISION.md         Future product direction and approval gates
```

The phone UI models permission setup, ready, recording, cancellation, teardown, and error states. `sending` and `playing` remain inert domain placeholders for separately approved future slices; the current capture controller never enters them. Focused local unit tests also cover board fixture bounds, selection persistence, explicit attachment, archive/restore/delete, local-only numbered decisions, handoff brief construction, endpoint selection, and Claude draft URL encoding.

## Safety references

- [Android for Cars overview and supported categories](https://developer.android.com/training/cars)
- [Android Auto overview](https://developer.android.com/training/cars/platforms/android-auto)
- [Record from the car microphone](https://developer.android.com/training/cars/apps/library/car-microphone)
- [Car app quality](https://developer.android.com/docs/quality-guidelines/car-app-quality)
