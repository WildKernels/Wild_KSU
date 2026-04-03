# Integrate Wild KSU (Advanced Manual Hook Guide)

This guide is intentionally laid out in the same practical patching style used
by advanced non-GKI integration pages, but targets this Wild_KSU tree running
manual mode only.

Scope:
- GKI/manual mode
- No kprobes dependency for core hooks
- No syscall-table tampering backend

## Hook Profiles

Use one of these profiles depending on how much compatibility you need.

### Profile A: Minimum Manual Hooks

Required hooks:
1. `execve/execveat`
2. `faccessat`
3. `newfstatat` (or nearest stat syscall path)
4. `reboot`

This is enough for core sucompat + ksud + supercall flow.

### Profile B: Compatibility Manual Hooks

Profile A plus:
1. `newfstat` return hook
2. `fstat64` return hook (32-bit compatibility)

Use this when rc injection compatibility is required on older/fragmented trees.

## Exported Handler API

Core manual handlers:
- `ksu_handle_execveat(int *fd, struct filename **filename_ptr, void *argv, void *envp, int *flags)`
- `ksu_handle_faccessat(int *dfd, const char __user **filename_user, int *mode, int *__unused_flags)`
- `ksu_handle_stat(int *dfd, const char __user **filename_user, int *flags)`
- `ksu_handle_sys_reboot(int magic1, int magic2, unsigned int cmd, void __user **arg)`

Compatibility handlers:
- `ksu_handle_newfstat_ret(unsigned int *fd, struct stat __user **statbuf_ptr)`
- `ksu_handle_fstat64_ret(unsigned long *fd, struct stat64 __user **statbuf_ptr)`

Headers typically needed near patch points:
- `#include <linux/ksu.h>`
- `#include <linux/ksud.h>`
- `#include <linux/sucompat.h>`
- `#include <linux/supercalls.h>`

## Patch Targets (Version-Aware)

Because OEM trees vary, patch the closest available function from each group.

### 1) exec path

Preferred target:
- `do_execveat_common(...)` in `fs/exec.c`

Fallback targets:
- `do_execve(...)`
- `compat_do_execve(...)` for 32-bit compat path
- `do_execve_common(...)` on older trees

Patch snippet:

```c
#ifdef CONFIG_KSU
ksu_handle_execveat(&fd, &filename, &argv, &envp, &flags);
#endif
```

If the target function has no local `fd/flags`, adapt by creating local values
matching the function ABI.

### 2) faccessat path

Preferred target:
- `do_faccessat(...)`

Fallback target:
- `SYSCALL_DEFINE3(faccessat, ...)`

Patch snippet:

```c
#ifdef CONFIG_KSU
ksu_handle_faccessat(&dfd, &filename, &mode, NULL);
#endif
```

### 3) stat path

Preferred target (modern syscall entry style):
- `SYSCALL_DEFINE4(newfstatat, ...)`

Fallback targets:
- `vfs_statx(...)`
- `vfs_fstatat(...)`
- `SYSCALL_DEFINE4(fstatat64, ...)` for 32-bit compatibility

Patch snippet:

```c
#ifdef CONFIG_KSU
ksu_handle_stat(&dfd, &filename, &flags);
#endif
```

If the local flag variable is named `flag`, pass `&flag`.

### 4) optional stat return hooks (compat profile)

Targets:
- return path of `SYSCALL_DEFINE2(newfstat, ...)`
- return path of `SYSCALL_DEFINE2(fstat64, ...)` (optional 32-bit)

Add just before function returns:

```c
#ifdef CONFIG_KSU
ksu_handle_newfstat_ret(&fd, &statbuf);
#endif
```

```c
#ifdef CONFIG_KSU
ksu_handle_fstat64_ret(&fd, &statbuf);
#endif
```

### 5) reboot path

Target:
- `SYSCALL_DEFINE4(reboot, ...)` in `kernel/reboot.c`

Patch snippet:

```c
#ifdef CONFIG_KSU
if (ksu_handle_sys_reboot(magic1, magic2, cmd, &arg))
        return 0;
#endif
```

If your local argument type differs:

```c
void __user *argp = (void __user *)arg;
if (ksu_handle_sys_reboot(magic1, magic2, cmd, &argp))
        return 0;
```

## Safe Mode Behavior

This Wild_KSU tree does not require manual `input.c` patching for safe mode in
the default path. It uses internal ksud input handler registration.

If your kernel blocks input handler registration in this context, add a custom
manual input patch as a local vendor workaround.

## Practical Checklist

1. Enable `CONFIG_KSU=y`.
2. Apply Profile A hooks first.
3. Boot test su, manager attach, and reboot supercall flow.
4. If rc/stat behavior is incomplete, apply Profile B return hooks.
5. Keep 32-bit hooks only if your device/userspace requires them.

## Compatibility Notes

- Use the nearest syscall entry layer available in your kernel branch.
- Do not rely on one exact symbol name across all vendor trees.
- Keep pointer types and signedness exactly aligned with handler signatures.
- Avoid mixing old kprobe patches with this manual-only path.
