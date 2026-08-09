# MBUX Companion

An Android companion for a private dispatcher/control plane, intended to provide a safe, voice-first Android Auto surface for unified OpenAI-backed conversations and Claude Code tasks.

This repository currently contains **only a buildable phone-side scaffold and the product contracts**. It does not declare Android Auto support, request microphone or network permissions, connect to a broker, or control any vehicle capability.

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

- **Android client:** renders a constrained session list, provider filters, resume/create actions, and an explicit push-to-talk state machine.
- **Private broker/control plane:** authenticates the device, holds provider credentials, owns the unified registry and provider ID mappings, and terminates OpenAI Realtime connections.
- **Providers:** OpenAI-backed conversations and Claude Code tasks remain provider-native behind broker adapters.
- **Vehicle boundary:** Android Auto is a presentation and supported microphone/audio surface only. The product never reads or controls vehicle systems.

See [AGENTS.md](AGENTS.md) for the durable safety, security, architecture, and verification contract.

## Build

Prerequisites:

- JDK 17
- Android SDK Platform 36 and Build Tools 36.0.0

Then run:

```bash
./gradlew clean assembleDebug lint
```

The debug APK is generated under `app/build/outputs/apk/debug/`.

The wrapper pins Gradle 9.4.1 and the project pins Android Gradle Plugin 9.2.0. Do not commit `local.properties`; use it only for a developer machine's SDK path.

## Current layout

```text
app/              Minimal Android application module
gradle/wrapper/   Pinned Gradle wrapper
AGENTS.md         Product and contributor contract
README.md         Scope, proof target, and build entry point
```

## Safety references

- [Android for Cars overview and supported categories](https://developer.android.com/training/cars)
- [Android Auto overview](https://developer.android.com/training/cars/platforms/android-auto)
- [Record from the car microphone](https://developer.android.com/training/cars/apps/library/car-microphone)
- [Car app quality](https://developer.android.com/docs/quality-guidelines/car-app-quality)
