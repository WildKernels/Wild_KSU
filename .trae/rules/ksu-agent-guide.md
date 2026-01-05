# KernelSU Agent Guide

## Agent Quick Start

- For significant features or refactors, sketch an Plan first; keep it updated as you work.
- When unsure which path to take, favor minimal risk changes that keep kernel/userspace contracts intact.

## Project Overview

KernelSU is a kernel-based root solution for Android with a kernel module, Rust userspace daemons, a Kotlin Manager app, and docs/web assets.

## Repository Structure

```bash
/kernel/                      # Kernel module - C code for Linux kernel integration
/userspace/ksud/              # Userspace daemon - Rust binary for userspace-kernel communication
/manager/                     # Android manager app - Kotlin/Jetpack Compose UI
/website/                     # Documentation website - VitePress
/js/                          # JavaScript library for module WebUI
/.github/workflows/           # CI/CD workflows for building and testing
/scripts/                     # Build automation scripts (Python)
```

## Building the Project

- Never build locally, all build will happen on github after push!

## Git Commit

- Mirror existing history style: `<scope>: <summary>` with a short lowercase scope tied to the touched area (e.g., `kernel`, `ksud`, `manager`, `docs`, `scripts`). Keep the summary concise, sentence case, and avoid trailing period.
- Prefer one scope; if multiple areas change, pick the primary one rather than chaining scopes. For doc-only changes use `docs:`; for multi-lang string updates use `translations:` if that matches log history.
- Keep subject lines brief (target ≤72 chars), no body unless necessary. If referencing a PR/issue, append `(#1234)` at the end as seen in history.
- Before committing, glance at recent `git log --oneline` to stay consistent with current prefixes and capitalization used in this repo.