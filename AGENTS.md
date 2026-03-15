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

## Common Pitfalls

- Manager JNI mirrors every supercall; kernel or ksud API changes must be reflected there to avoid runtime drift.

## Git Commit

- Mirror existing history style: `<scope>: <summary>` with a short lowercase scope tied to the touched area (e.g., `kernel`, `ksud`, `manager`, `docs`). Keep the summary concise, sentence case, and avoid trailing period.
- Prefer one scope; if multiple areas change, pick the primary one rather than chaining scopes. For doc-only changes use `docs:`; for multi-lang string updates use `translations:` if that matches log history.
- Keep subject lines brief (target ≤72 chars), no body unless necessary. If referencing a PR/issue, append `(#1234)` at the end as seen in history.
- Before committing, glance at recent `git log --oneline` to stay consistent with current prefixes and capitalization used in this repo.
