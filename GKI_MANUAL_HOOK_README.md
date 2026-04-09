# Wild_KSU GKI Manual-Hook Mode

This repository is fully GKI-compliant and manual-hook only:

- No LKM, kprobe, syscall-table tampering, kallsyms, insmod, jailbreak helpers, or forbidden runtime tricks are present in the main codebase.
- If `CONFIG_KSU` is set, manual hooks are always used (no config or runtime switch).
- All code in `kernel/`, `manager/`, and `userspace/` is limited to static/manual hooks and kernel UAPI.
- Reference implementations: rsuntk and backslashxx manual/LSM hook style.

If you find any forbidden code paths, please report or remove them immediately.
