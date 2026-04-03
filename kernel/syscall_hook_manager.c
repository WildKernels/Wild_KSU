#include "klog.h" // IWYU pragma: keep
#include "syscall_hook_manager.h"
#include "sucompat.h"
#include "setuid_hook.h"

void ksu_syscall_hook_manager_init(void)
{
	pr_info("hook_manager: manual mode init\n");
	ksu_setuid_hook_init();
	ksu_sucompat_init();
}

void ksu_syscall_hook_manager_exit(void)
{
	pr_info("hook_manager: manual mode exit\n");
	ksu_sucompat_exit();
	ksu_setuid_hook_exit();
}
