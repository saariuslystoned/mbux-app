# AGENTS.md

This file is the durable contract for every human or coding agent working in this public repository. Read it before changing code, dependencies, manifests, architecture, or documentation. More specific `AGENTS.md` files may add constraints for a subtree but may not weaken this contract.

## Product mission

MBUX Companion is an Android phone application intended to expose a small, safe Android Auto front end for a private dispatcher/control plane. The eventual experience may:

- show one session list with provider filters for OpenAI-backed conversations and Claude Code tasks;
- create or resume a broker-owned unified session;
- capture an explicitly initiated push-to-talk utterance through Android Auto's supported in-app microphone path;
- stream that audio to a private broker, which uses OpenAI Realtime and returns a voice response; and
- let the broker map unified session IDs to provider-native session IDs.

The product is not a general-purpose car computer, vehicle assistant, autonomous agent, or replacement infotainment system.

## Non-negotiable product boundary

The app MUST NOT:

- read, command, automate, emulate, or modify vehicle systems;
- integrate with CAN, OBD-II, ECU, diagnostic, key, lock, alarm, climate, lighting, seat, drive-mode, steering, braking, throttle, charging, or telematics controls;
- request car-hardware data or permissions merely because an API is available;
- present provider credentials, broker credentials, raw auth tokens, or signing material to the car host;
- circumvent Android Auto app-category, template, interaction-count, distraction, trusted-store, or review restrictions;
- claim that an AI/task session is a human messaging, media, navigation, POI, IoT, weather, or other supported-category experience without documented policy and product review; or
- rely on Gemini or Google Assistant to understand and dispatch the user's private task request. System-owned Android Auto behavior can still exist around the app, but app microphone audio must be delivered to the app/broker path defined here.

No feature may be described as controlling “MBUX” or the vehicle. The working name identifies the companion experience only.

## Android Auto and driver-distraction gate

Android Auto accepts only supported app categories and host-constrained experiences. Category eligibility is a hard gate, not a later publishing detail.

Before adding `androidx.car.app`, a `CarAppService`, `com.google.android.gms.car.application` metadata, `automotive_app_desc.xml`, a car launcher category, or any other car declaration, a change must include:

1. the exact current Android Auto category and official policy supporting the feature;
2. the allowed templates and interaction model;
3. a distraction and parked/driving-state review;
4. a test plan using the Desktop Head Unit or an equivalent supported host; and
5. explicit maintainer acceptance of the category rationale.

If the product does not fit an allowed category, keep the feature phone-side or stop. Do not disguise the feature as another category and do not use sideloading as a production strategy.

For any future voice capture:

- use the Android for Cars App Library microphone API supported for the approved template/category;
- begin only after an intentional in-app microphone action;
- request `RECORD_AUDIO` at runtime on the phone/supported host flow before capture;
- acquire appropriate audio focus and stop immediately on release, cancel, timeout, host teardown, or focus loss;
- preserve Android Auto's visible recording indicator and never simulate or obscure it;
- avoid continuous listening, hotword capture, background capture, or covert recording;
- bound each utterance and network retry; and
- do not persist raw audio unless a separately reviewed feature makes retention explicit and user-controlled.

Touch interactions while driving must remain shallow and template-governed. Do not add free-form typing, arbitrary scrolling dashboards, code diffs, terminal views, long transcripts, confirmations requiring detailed reading, or destructive task controls to the car surface. Destructive, sensitive, ambiguous, or high-impact operations must move to the phone or another trusted screen.

## Architecture intent

Keep three trust domains distinct:

### Android client

The client is a constrained presentation and audio-streaming endpoint. It may hold a revocable, short-lived, device-bound broker session credential in Android-protected storage. It must not hold provider API keys or act as the source of truth for provider session mapping.

The current project is one small application module. Add module or package boundaries only when implementation requires them. Prefer these conceptual layers:

- `ui`: phone UI and, only after approval, car templates;
- `domain`: unified session and push-to-talk state machines;
- `broker`: authenticated transport DTOs and streaming client;
- `platform`: Android lifecycle, audio focus, connectivity, and secure storage.

UI code should depend on domain interfaces, not provider SDKs. Provider names may appear in the registry model and filters, but provider-specific transport behavior belongs behind the broker.

### Private broker/control plane

The broker is the security and routing boundary. It owns:

- OpenAI and Claude credentials;
- device/user authentication and revocation;
- unified session IDs and provider-session mappings;
- provider adapters for create, resume, input, cancellation, and response events;
- OpenAI Realtime connection establishment and lifecycle;
- authorization policy, rate limits, audit metadata, and redaction; and
- schema/version negotiation with the Android client.

The broker should return opaque client IDs. Do not make provider session IDs an Android routing primitive. Do not place broker implementation or deployment secrets in this public repository unless a later task explicitly adds a separately safe, secret-free contract or client fixture.

### Providers

OpenAI-backed conversations and Claude Code tasks remain provider-native resources behind broker adapters. The Android app never talks to provider APIs directly. A provider outage or unsupported capability must degrade one session/provider without corrupting the unified registry.

## Unified session model

The client-facing registry should remain provider-neutral. The initial model should cover:

- opaque unified `id`;
- `provider` (`openai` or `claude_code` initially);
- user-safe title/summary metadata;
- lifecycle `status` suitable for a short list;
- created/updated timestamps;
- capabilities such as text, voice, create, resume, or cancel; and
- an optional opaque continuation/version token for concurrency.

Provider session IDs, credential references, internal routes, hostnames, filesystem paths, raw command output, and sensitive task payloads are not list metadata. The broker decides how create/resume maps to a provider. Updates must define idempotency and conflict behavior before implementation.

Treat session titles, transcripts, audio, and task outputs as private user data. Minimize what is cached on device, provide a clear sign-out/revocation story, and redact logs by default.

## Secrets and authentication

This is a public repository. Assume every committed byte, artifact, log excerpt, screenshot, and fixture is permanently public.

Never commit or paste:

- provider or broker API keys, OAuth tokens, cookies, authorization headers, refresh tokens, device enrollment secrets, or signed URLs;
- `.env` files, `local.properties`, keystores, signing passwords, service-account material, private certificates, or credential-store exports;
- production broker hostnames when they are intended to remain private;
- real provider/session identifiers, private transcripts, raw audio, customer/user data, or auth-bearing logs; or
- secrets disguised as examples, encoded blobs, test fixtures, screenshots, recordings, Gradle properties, build constants, or Git history.

Local development may use untracked `local.properties` only for the Android SDK path. Future non-secret endpoint selection should use a checked-in placeholder plus an untracked local override. Secrets must be injected at runtime from an approved local credential store or remote secret manager, never compiled into the APK or supplied as command-line values likely to enter shell history.

The Android client should authenticate only to the broker. Prefer short-lived, revocable, least-privilege, device-bound credentials stored using Android security APIs. Never mint or forward provider credentials to the device. TLS verification must remain enabled; debug trust overrides and cleartext traffic require explicit, local-only design and must never ship.

Logs and telemetry must exclude raw audio, transcript content, authorization data, provider IDs, private prompts, and broker payloads by default. Debug logging must be removable, bounded, and safe for screenshots or public CI logs.

If a secret enters a commit or output, stop. Do not merely delete the file in a follow-up commit. Report the exposure privately, rotate/revoke the credential, and clean history only with explicit maintainer coordination.

## Implementation rules

- Keep the scaffold small and buildable; do not introduce architecture for hypothetical features.
- Use supported, stable Android and AndroidX releases and pin build-tool versions intentionally.
- Keep the minimum SDK at Android 9 / API 28 or higher unless a documented compatibility decision changes it.
- Do not add microphone, network, notification, foreground-service, car-hardware, accessibility, overlay, SMS, contacts, or location permissions until the implementing change explains and tests the need.
- Do not add analytics, crash reporting, ad, tracking, remote-config, or third-party voice SDKs without explicit review of data flow and public-repository configuration.
- Use fake, obviously non-routable fixtures. Tests must not require live provider credentials or a live broker.
- Model push-to-talk as an explicit state machine with at least idle, requesting permission, ready, recording, sending, playing, cancelled, and error behavior before connecting audio.
- Cancellation must propagate from UI to capture, transport, broker, and playback. Lifecycle teardown must release audio and network resources.
- Fail closed on auth, TLS, category, permission, or schema uncertainty. Never silently fall back to direct provider access or a system assistant.
- Keep user-visible copy clear that broker actions can affect remote coding sessions; the car surface must not expose unsafe execution controls.

## Verification expectations

Every non-trivial change needs evidence proportional to risk. At minimum, before committing:

```bash
./gradlew clean assembleDebug lint
git status --short
git diff --check
```

Add focused unit tests for new domain or broker-client behavior. Run instrumentation or UI tests when lifecycle, permissions, audio, secure storage, or Android surfaces change. Network tests must use a fake broker and verify timeouts, cancellation, malformed events, auth expiry, and redaction without contacting live providers.

Any Android Auto change additionally requires:

- a cited category/policy rationale;
- manifest and permission review;
- Desktop Head Unit or supported-host evidence for visible states;
- driving/parked restriction tests as applicable;
- microphone permission, audio-focus loss, cancel, disconnect, and lifecycle tests;
- screenshots or recordings that contain no private data; and
- a documented skip/failure rationale for any case that cannot be exercised.

Before publishing from this public repository, inspect the staged diff and tracked files for credentials, private endpoints, real session IDs, local paths, generated APKs, logs, and screenshots. Do not stage unrelated files or machine-local configuration. CI must use secret references, avoid fork-secret exposure, and keep public logs redacted.

## Change and review discipline

- Keep each change bounded to one product slice with explicit acceptance criteria.
- Preserve a clean ownership trail: one branch/worktree owner for source-changing work.
- Reviewers should actively look for a hidden safety, lifecycle, cancellation, privacy, or distraction bug rather than assuming the happy path is sufficient.
- Record accepted and rejected review findings and why when a review is requested.
- Any change that can send externally, spend money, deploy, delete data, expose a secret, alter an account/device, or affect a live remote task requires an explicit human gate unless the task already authorizes that exact action.
- Never test vehicle behavior on public roads. Prefer local tests and the Desktop Head Unit; real-vehicle checks must be stationary, legal, supervised, and limited to Android Auto presentation/audio behavior.

## Definition of done

A feature is done only when it stays inside the product and vehicle boundary, uses the broker trust model, contains no secrets, handles obvious failure/cancellation/lifecycle cases, passes the relevant build/tests/lint, and includes visible proof for UI or car-host work. Stop when the acceptance criteria are satisfied; leave deeper work as a bounded follow-up instead of widening the change.
