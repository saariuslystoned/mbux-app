# MBUX Companion vision

This document records future product direction. It does not authorize implementation, provider access, network connectivity, account changes, or deployment.

## Confirmed future target

Subject to separate design, policy, security, and implementation approval, the companion is intended to support:

- an in-app Gemini conversation through an approved provider boundary;
- user-directed task handoffs to ChatGPT and Claude Code running on the user's Mac; and
- a user-triggered ChatGPT Voice handoff for direct conversation.

Each capability is future and gated. Before implementation, its slice must define and validate the provider or platform capability, approved routing boundary, credentials and account authorization, user-visible consent and cancellation, data retention and redaction, failure behavior, and any required human gate. Task handoffs must remain user-directed, and actions that can send, spend, deploy, delete, expose data, alter an account or device, or affect a live remote task require explicit authorization for that action.

## Android Auto product constraint

The desired handoffs above do not make ChatGPT Voice, Claude Code, or ChatGPT Remote supported Android Auto experiences. The companion must not use UI automation, accessibility or overlay behavior, private components, reverse-engineered integration, category misrepresentation, or other policy workarounds to embed, control, or force those or arbitrary third-party apps onto the vehicle display.

The companion's legitimate role is a phone-side, driver-aware routing surface where the relevant platform capabilities and policies support it. A future car surface still requires its own approved Android Auto category, templates, interaction model, and distraction review. Any ChatGPT Voice or Mac-task handoff must remain a user-triggered routing action on an appropriate supported surface, not an embedded or remotely controlled third-party experience. This constraint reflects the current product and platform boundary; it does not speculate about any platform provider's motives.

## Personal-use phone S1 foundation

S1 starts as a personal, sideloaded phone session-board prototype. Three top-level phone destinations separate the Claude fixture board, Codex fixture board, and Dispatch review flow. The provider boards store only their selected provider and fixture state in app-private preferences and keep attach, archive, restore, delete, and simulated numbered decisions on the phone; they do not contain task creation controls. The app neither scans a Mac nor launches, contacts, embeds, or controls a real provider session. Real launch and attach routing remain future, gated capabilities.

This phone foundation is deliberately separate from Android Auto eligibility. It does not authorize car declarations or category metadata, and it does not claim that Claude, Codex, ChatGPT, or their remote applications run inside Android Auto.

## Explicit phone handoff foundation

The current phone prototype includes one narrow user-directed handoff, isolated under the top-level **Dispatch** destination. Its credential-free proof mode is constrained to the single public repository **`saariuslystoned/mbux-app`**; the repository is visible in review but cannot be selected or edited. A person selects Claude or Codex / ChatGPT, provides an editable task brief, reviews the complete plain-text payload, and deliberately chooses **Continue in…**. The shared lane is Android's documented text Sharesheet, so the operating system and user choose the destination. MBUX neither targets a hidden component nor observes or interprets a result. A handoff does not add a fixture or claim a real session under either provider board; future confirmed sessions would conceptually belong to their selected provider board only after separately approved status wiring exists.

Claude additionally offers its documented Claude Code draft-composer route as an optional fast path. That route opens a prefilled Claude-owned draft only; it does not submit the draft. Codex / ChatGPT has no app-specific route in this implementation and stays on the generic chooser. The project does not guess private parameters, automate either app, or claim that opening an app created a session.

On the physically tested Pixel Fold, choosing ChatGPT opened a regular chat, while the Claude route entered Claude's Code/Dispatch experience and attempted its own Mac continuation. This records observed destination-app behavior only; it is not a stable provider contract, task-creation proof, callback, or status channel available to MBUX.

This boundary is supported by the public [Android Sharesheet contract](https://developer.android.com/develop/ui/compose/sharing/send) and Anthropic's [Claude mobile link documentation](https://support.claude.com/en/articles/14898120-open-the-claude-mobile-app-with-a-link). It is distinct from the future Mac relay: real launch/resume routing, cross-session status, account authorization, and broker-owned provider mappings remain separately gated work. A credential-backed repository catalog, GitHub authorization, and any expansion beyond the one public proof repository are also a separate future slice. Voice transcription is a later device slice; the current handoff accepts only an editable typed draft and never transfers captured microphone audio.

## Voice entry foundation

The current Android app declares one public App Action: `actions.intent.OPEN_APP_FEATURE` for the inline feature **Dispatch**. Its route opens MBUX directly on the fixed-public-repository Dispatch review screen; ordinary app launch continues to open the session board. This is navigation only. It does not extract a task brief or provider from speech, create or control a Claude/Codex session, start ChatGPT Voice, or bypass the existing review and destination-owned send.

The strongest intended request is **“Open Dispatch in MBUX”** using the app's published invocation name. The feature title also permits the natural **“Open Dispatch”** where the platform already has MBUX in context or can resolve it unambiguously, but the app cannot claim or override a generic phrase when another app or system feature conflicts.

The support boundary remains platform-dependent. Google's [App Actions documentation](https://developer.android.com/develop/devices/assistant/get-started) requires Play publication and App Actions review for production availability, or a same-account Assistant test preview during development. Google's [Gemini Utilities documentation](https://support.google.com/gemini/answer/15235441) documents generic app opening but does not promise third-party `OPEN_APP_FEATURE` fulfillment in every Gemini configuration. Production publication/review and an actual Assistant/Gemini voice-resolution proof are therefore separate gated work, not claims of this sideloaded implementation.

## Future Gemini Live driver flow

The desired future driver experience uses Gemini Live as the system-owned, continuous two-way conversational layer and as a supported entry point that opens MBUX Dispatch. After explicit review and authorization, MBUX and its private control plane—not Gemini Live—would create or attach Claude or Codex work, remain the source of truth for those sessions, and return only concise, provider-neutral state. The desired display pairs an eligible MBUX state surface on Android Auto with the driver's separate conversation in Gemini Live.

This is a gated product target, not a claim about current platform behavior. Whether Gemini Live can receive or read MBUX state, consume content displayed by Android Auto, or maintain a supported state-feedback loop with MBUX must be validated against public platform capabilities before implementation. Until then, Gemini Live must not be assumed to understand or dispatch private task content beyond supported app navigation.

This direction does not authorize UI automation, accessibility or overlay workarounds, private or reverse-engineered components, third-party app embedding or control, or Android Auto category misrepresentation. Any MBUX car display remains independently subject to the category, template, distraction, host-testing, and maintainer-acceptance gates below; provider actions remain behind the approved control plane and applicable human gates.

## S1 Android Auto feasibility decision

Reviewed against the official Android for Cars documentation on 2026-08-09, the requested driver-aware session board does **not** currently fit an Android Auto category. S1 remains a product requirement, but it is blocked at category eligibility; this repository must not add a `CarAppService`, car category, template capability, or other Android Auto declaration until that gate changes or the platform owner confirms an honest category fit.

The requested board would show only tracked Claude/Codex sessions: provider, explicitly named repository, short task title, `running` / `done` / `needs-you` / `failed` state, a small artifact-ready indicator, and existing provider human-gate choices 1/2/3. It would not host, embed, or control ChatGPT Voice, ChatGPT Remote, Claude Code, or another app.

### Exact category mismatch

- **Communication — messaging** requires incoming, replyable, short-form peer-to-peer messages. Agent session status rows and task-gate choices are not conversations or message replies.
- **IoT** is limited to connected-device state, simple on/off actions, and related events. Coding sessions are not connected devices, and task-gate choices are not simple device controls.
- **Media, calling, navigation, POI, and weather** require their named core functions, none of which the session board provides.
- **Parked apps on Android Auto** currently cover games. A productivity/session board cannot honestly declare itself a game; video and browser parked categories are not Android Auto categories for this surface.

`ListTemplate` is structurally close to a compact board, but a template does not grant category eligibility. While driving, list secondary text is truncated to two lines, which also pressures the requested provider/repository/title/state/artifact density. A `PaneTemplate` permits only two buttons, so it cannot present human-gate choices 1/2/3 as three direct buttons. More importantly, a gate choice can affect a live remote coding task and needs a separate distraction and human-gate review even if a future category becomes available.

### Compliant next route

1. Keep the session board and task decisions phone-side, with the current microphone capability unchanged.
2. Seek explicit Android Auto platform/partner confirmation or a future supported productivity/status category for this exact surface; do not relabel it as messaging or IoT.
3. If eligibility is established, write a fresh category and distraction rationale, define the driving-versus-parked interaction model, and prototype with fake public fixtures in a non-production build.
4. Before acceptance, validate row limits, truncation, host-provided restrictions, disconnect/lifecycle behavior, and every human-gate choice using the Desktop Head Unit or another supported host, with private data excluded from proof.

Official evidence reviewed:

- [Android for Cars supported app categories](https://developer.android.com/training/cars#supported-app-categories)
- [Car app quality requirements](https://developer.android.com/docs/quality-guidelines/car-app-quality)
- [Communication apps and messaging requirements](https://developer.android.com/training/cars/communication)
- [List template constraints](https://developer.android.com/design/ui/cars/guides/templates/list-template)
- [Pane template constraints](https://developer.android.com/design/ui/cars/guides/templates/pane-template)
- [Parked app categories](https://developer.android.com/training/cars/parked)

## Current boundary

The current application implements a phone-side, fixture-backed session board, a single-public-repository proof handoff, one App Action route that only opens Dispatch, and separate permission-gated microphone capture. Board state uses app-private preferences; audio remains ephemeral in memory and is discarded locally. The handoff opens only a reviewed chooser or Claude-owned draft and provides no submission or status result. A client-only normalized CP-1 status contract and fail-closed panel are present, but the installed feed is explicitly **not enrolled** and performs no I/O; no fixture is presented as remote truth. The app has no repository catalog or authorization, voice transcription, direct provider integration, network permission or connection, broker connection, real Mac session discovery or live relay, ChatGPT Voice handoff, Android Auto declaration, or vehicle-control capability.

Nothing in this vision changes the Android Auto category gate or the prohibition on reading, commanding, automating, emulating, or modifying vehicle systems.
