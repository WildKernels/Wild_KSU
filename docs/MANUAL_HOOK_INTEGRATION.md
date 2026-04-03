# Wild KSU Manual Hook Integration

This tree now runs in manual hook mode and no longer uses syscall-table hook objects.

## Exported manual handlers

Use these handlers from kernel call sites:

- `ksu_handle_execveat(int *fd, struct filename **filename_ptr, void *argv, void *envp, int *flags)`
- `ksu_handle_faccessat(int *dfd, const char __user **filename_user, int *mode, int *__unused_flags)`
- `ksu_handle_stat(int *dfd, const char __user **filename_user, int *flags)`
- `ksu_handle_sys_reboot(int magic1, int magic2, unsigned int cmd, void __user **arg)`

Headers:

- `#include <linux/ksu.h>` (or your integrated path)
- `#include <linux/ksud.h>`
- `#include <linux/sucompat.h>`
- `#include <linux/supercalls.h>`

## Example patch points

### 1) execve / execveat path

Patch `fs/exec.c` in `do_execveat_common` after `struct filename *filename` is valid:

```c
#ifdef CONFIG_KSU
ksu_handle_execveat(&fd, &filename, &argv, &envp, &flags);
#endif
```

### 2) faccessat path

Patch `fs/open.c` syscall handler path (where `dfd`, `filename`, and `mode` are available):

```c
#ifdef CONFIG_KSU
ksu_handle_faccessat(&dfd, &filename, &mode, NULL);
#endif
```

### 3) stat/newfstatat path

Patch `fs/stat.c` syscall handler path (where `dfd`, `filename`, and `flags` are available):

```c
#ifdef CONFIG_KSU
ksu_handle_stat(&dfd, &filename, &flags);
#endif
```

### 4) reboot path

Patch `kernel/reboot.c` syscall handler before default reboot logic:

```c
#ifdef CONFIG_KSU
if (ksu_handle_sys_reboot(magic1, magic2, cmd, &arg))
    return 0;
#endif
```

If your reboot syscall argument is not a `void __user *arg`, adapt by creating a local variable:

```c
void __user *argp = (void __user *)arg;
ksu_handle_sys_reboot(magic1, magic2, cmd, &argp);
```

## Notes

- Manual hooks must be integrated in the kernel source tree where syscalls are implemented.
- This avoids syscall-table tampering and avoids kprobe-based interception for the listed syscall paths.
- Keep argument pointer types exactly as expected by the handlers to avoid ABI mismatches.
