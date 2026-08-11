# MBUX Control-Plane Handoff

This document is the durable architecture handoff for continuing MBUX control-plane design from the `x-api` repository. It records product boundaries, not live infrastructure state.

## Canonical vocabulary

- **CP-1** is the committed control-plane host and normal doorway into the swarm. It owns routing, proof, dashboard truth, human gates, and future MBUX status normalization.
- **Operator terminals** are human-owned travel MacBooks used to steer, review, and approve work. They are not control-plane replicas or default worker capacity.
- **Spark nodes** are specialist shared services: Spark-1 is the advisory model shelf, while Spark-2 is the evaluation, sandbox, and review service. They are not independent orchestration brains.
- **AIWorker Macs** are execution and proof capacity. Routed Claude or Codex work runs there in owned lanes; they do not own control-plane authority.

“Swarm base” is not a canonical role. Use **control plane** for the logical authority and **CP-1** for its committed host.

## Two distinct lanes

### Provider-managed remote lane

MBUX Dispatch can hand a reviewed brief to Claude or ChatGPT through an explicit phone action. The provider may continue that work in its own remote Mac session, but the provider owns the continuation, review, and send. MBUX must not infer session creation, completion, identity, or status from opening the provider app.

### MBUX-owned status lane

The intended MBUX status path is Pixel over WireGuard to a CP-1-owned, normalized, read-only status service. CP-1 may observe registered worker lanes and return a minimal safe projection. The phone does not connect directly to worker hosts or provider sessions and receives no command authority.

## Current product state

- The phone-side Claude/Codex session board and reviewed Dispatch flow exist.
- Dispatch supports the generic Android chooser and an optional Claude-owned draft handoff; neither path reports a provider result.
- An App Actions entry can route into Dispatch, but Google preview-tool compatibility, account eligibility, review, and production availability remain separate validation gates.
- The CP-1 relay panel and client contract exist in a fail-closed **not enrolled** state. They perform no network I/O and never present local fixtures as remote truth.

## Real relay blocker

The existing SwarmPocket raw feed is not an acceptable MBUX source. It requires shared bearer authentication and exposes internal host and session details. MBUX must not embed that credential model or consume the raw response.

## Required CP-1 capability

Before a live adapter is added, CP-1 must provide both:

1. a fixed, normalized, status-only wrapper; and
2. enrolled, revocable, device-bound authentication suitable for the Pixel.

The phone response must contain only an opaque session ID, provider, safe repository label, safe short title, bounded state, freshness, artifact-ready flag, and opaque schema or snapshot version. Unknown, stale, disconnected, and not-enrolled states must remain explicit and must never be upgraded optimistically.

## Non-negotiable boundaries

- No phone SSH or mosh access.
- No raw CP-1 paths, panes, commands, host details, provider-native IDs, or terminal output.
- No shared bearer token, provider key, VPN material, or other secret in the APK.
- No provider UI automation, accessibility workaround, private component, or inferred callback.
- No Android Auto declaration, category misrepresentation, or policy workaround.
- No vehicle-system access or control.

## Architecture sentence

MBUX has two distinct paths: provider-owned Claude or ChatGPT Remote may continue a user-reviewed handoff on an operator MacBook, while MBUX status travels over WireGuard only to a CP-1-owned normalized read-only service that observes registered worker lanes and grants the Pixel no provider or command authority.
