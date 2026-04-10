#include <linux/export.h>
#include <linux/fs.h>
#include <linux/kobject.h>
#include <linux/module.h>
#include <linux/rcupdate.h>
#include <linux/sched.h>
#include <linux/workqueue.h>

#include "allowlist.h"
#include "app_profile.h"
#include "feature.h"
#include "klog.h" // IWYU pragma: keep
#include "manager.h"
#include "throne_tracker.h"
#include <linux/security.h>
#include <linux/key.h>
#include "hook/lsm_hook.c"
#include "ksud.h"
#include "supercalls.h"
#include "ksu.h"
#include "file_wrapper.h"
#include "selinux/selinux.h"

#if defined(__x86_64__)
#include <asm/cpufeature.h>


#include "feature.c"
#include "file_wrapper.c"
#include "seccomp_cache.c"
#include "hook/lsm_hook.c"
#include "allowlist.c"
#include "throne_tracker.c"
#include "apk_sign.c"
#include "app_profile.c"
#include "kernel_umount.c"
#include "manager.c"
#include "pkg_observer.c"
#include "setuid_hook.c"
#include "su_mount_ns.c"
#include "sucompat.c"
#include "supercalls.c"
#include "tiny_sulog.c"
#include "tp_marker.c"
#include "ksud.c"
#include "selinux/selinux.c"
#include "selinux/sepolicy.c"
        return -ENOSYS;
