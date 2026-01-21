# Wild KSU Agent Guide

## Agent Quick Start

- For significant features or refactors, sketch an Plan first; keep it updated as you work.
- Use Context7 to pull library/API docs when you touch unfamiliar crates, Android APIs, or JS deps.
- Default to `rg` for searching and keep edits ASCII unless the file already uses non-ASCII.
- Run the component-specific checks below before handing work off; do not skip failing steps.
- When unsure which path to take, favor minimal risk changes that keep kernel/userspace contracts intact.

## Project Overview

Wild KSU is a fork of KernelSU, a kernel-based root solution for Android with a kernel module, Rust userspace daemons, a Kotlin Manager app, and docs/web assets.

## Repository Structure

```bash
/kernel/                      # Kernel module - C code for Linux kernel integration
/userspace/ksud/              # Userspace daemon - Rust binary for userspace-kernel communication
/manager/                     # Android manager app - Kotlin/Jetpack Compose UI
/.github/workflows/           # CI/CD workflows for building and testing
```

## Component Workflows

### Kernel (`kernel/`)

- Kernel changes are C-only; keep interfaces aligned with supercall and allowlist expectations in userspace/Manager.
- If you alter IOCTLs or profiles, update the corresponding wrappers in ksud (`ksucalls.rs`) and Manager JNI (`manager/app/src/main/cpp/ksu.cc`).

### Userspace Rust (`userspace/ksud`)

For Rust projects in `userspace/ksud`, ALWAYS run these commands in sequence after making code changes:

1. `cargo ndk -t arm64-v8a check` (verify compilation)
2. `cargo ndk -t arm64-v8a clippy` (lints and warnings)
3. `cargo fmt` (format)
4. Fix any errors or warnings before considering the task complete.

### Android Manager App (`manager/`)

```bash
cd manager
# Must have ksud binaries first!
mkdir -p app/src/main/jniLibs/arm64-v8a
cp ../userspace/ksud/target/aarch64-linux-android/release/ksud app/src/main/jniLibs/arm64-v8a/libksud.so

# Then build
./gradlew clean assembleRelease
```

Important: Manager build REQUIRES ksud binaries to be present in `jniLibs` before building.

## Common Pitfalls

- Manager JNI mirrors every supercall; kernel or ksud API changes must be reflected there to avoid runtime drift.
- Do not skip the `cargo ndk` steps; plain `cargo check` will not validate Android targets.
- Manager builds fail if `libksud.so` is missing; create it before any Gradle command.

## Git Commit

- Mirror existing history style: `<scope>: <summary>` with a short lowercase scope tied to the touched area (e.g., `kernel`, `ksud`, `manager`, `docs`). Keep the summary concise, sentence case, and avoid trailing period.
- Prefer one scope; if multiple areas change, pick the primary one rather than chaining scopes. For doc-only changes use `docs:`; for multi-lang string updates use `translations:` if that matches log history.
- Keep subject lines brief (target ≤72 chars), no body unless necessary. If referencing a PR/issue, append `(#1234)` at the end as seen in history.
- Before committing, glance at recent `git log --oneline` to stay consistent with current prefixes and capitalization used in this repo.
