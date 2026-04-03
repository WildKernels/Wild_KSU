#include <linux/rcupdate.h>
#include <linux/slab.h>
#include <asm/current.h>
#include <linux/compat.h>
#include <linux/cred.h>
#include <linux/dcache.h>
#include <linux/err.h>
#include <linux/file.h>
#include <linux/fs.h>
#include <linux/input.h>
#include <linux/version.h>
#include <linux/input-event-codes.h>
#include <linux/printk.h>
#include <linux/types.h>
#include <linux/uaccess.h>
#include <linux/namei.h>
#include <linux/uio.h>

#include "manager.h"
#include "allowlist.h"
#include "arch.h"
#include "klog.h" // IWYU pragma: keep
#include "ksu.h"
#include "ksud.h"
#include "selinux/selinux.h"
#include "throne_tracker.h"
#include "sucompat.h"

bool ksu_boot_completed __read_mostly = false;

static const char KERNEL_SU_RC[] =
	"\n"

	"on post-fs-data\n"
	"    start logd\n"
	// We should wait for the post-fs-data finish
	"    exec u:r:" KERNEL_SU_DOMAIN ":s0 root -- " KSUD_PATH " post-fs-data\n"
	"\n"

	"on nonencrypted\n"
	"    exec u:r:" KERNEL_SU_DOMAIN ":s0 root -- " KSUD_PATH " services\n"
	"\n"

	"on property:vold.decrypt=trigger_restart_framework\n"
	"    exec u:r:" KERNEL_SU_DOMAIN ":s0 root -- " KSUD_PATH " services\n"
	"\n"

    "on property:sys.boot_completed=1\n"
    "    exec u:r:" KERNEL_SU_DOMAIN ":s0 root -- " KSUD_PATH
    " boot-completed\n"
    "\n"

	"\n";

static void stop_init_rc_hook();
static void stop_execve_hook();
static void stop_input_hook();

void on_post_fs_data(void)
{
	static bool done = false;
	if (done) {
		pr_info("on_post_fs_data already done\n");
		return;
	}
	done = true;
	pr_info("on_post_fs_data!\n");

	ksu_load_allow_list();
	ksu_observer_init();
	// sanity check, this may influence the performance
	stop_input_hook();
}

extern void ext4_unregister_sysfs(struct super_block *sb);
int nuke_ext4_sysfs(const char *mnt)
{
	struct path path;
	int err = kern_path(mnt, 0, &path);
	if (err) {
		pr_err("nuke path err: %d\n", err);
		return err;
	}

	struct super_block *sb = path.dentry->d_inode->i_sb;
	const char *name = sb->s_type->name;
	if (strcmp(name, "ext4") != 0) {
		pr_info("nuke but module aren't mounted\n");
		path_put(&path);
		return -EINVAL;
	}

	ext4_unregister_sysfs(sb);
	path_put(&path);
	return 0;
}

extern void ksu_avc_spoof_late_init();
void on_boot_completed(void)
{
    ksu_boot_completed = true;
    pr_info("on_boot_completed!\n");
    track_throne(true);
    ksu_avc_spoof_late_init();
}

#define MAX_ARG_STRINGS 0x7FFFFFFF
struct user_arg_ptr {
#ifdef CONFIG_COMPAT
	bool is_compat;
#endif
	union {
		const char __user *const __user *native;
#ifdef CONFIG_COMPAT
		const compat_uptr_t __user *compat;
#endif
	} ptr;
};

static const char __user *get_user_arg_ptr(struct user_arg_ptr argv, int nr)
{
	const char __user *native;

#ifdef CONFIG_COMPAT
	if (unlikely(argv.is_compat)) {
		compat_uptr_t compat;

		if (get_user(compat, argv.ptr.compat + nr))
			return ERR_PTR(-EFAULT);

		return compat_ptr(compat);
	}
#endif

	if (get_user(native, argv.ptr.native + nr))
		return ERR_PTR(-EFAULT);

	return native;
}

/*
 * count() counts the number of strings in array ARGV.
 */

/*
 * Make sure old GCC compiler can use __maybe_unused,
 * Test passed in 4.4.x ~ 4.9.x when use GCC.
 */

static int __maybe_unused count(struct user_arg_ptr argv, int max)
{
	int i = 0;

	if (argv.ptr.native != NULL) {
		for (;;) {
			const char __user *p = get_user_arg_ptr(argv, i);

			if (!p)
				break;

			if (IS_ERR(p))
				return -EFAULT;

			if (i >= max)
				return -E2BIG;
			++i;

			if (fatal_signal_pending(current))
				return -ERESTARTNOHAND;
		}
	}
	return i;
}

static bool check_argv(struct user_arg_ptr argv, int index,
			const char *expected, char *buf, size_t buf_len)
{
	const char __user *p;
	int argc;

	argc = count(argv, MAX_ARG_STRINGS);
	if (argc <= index)
		return false;

	p = get_user_arg_ptr(argv, index);
	if (!p || IS_ERR(p))
		goto fail;

	if (strncpy_from_user_nofault(buf, p, buf_len) <= 0)
		goto fail;

	buf[buf_len - 1] = '\0';
	return !strcmp(buf, expected);

fail:
	pr_err("check_argv failed\n");
	return false;
}

static void ksu_handle_execveat_ksud_path(const char *path,
					  struct user_arg_ptr *argv)
{
	static const char app_process[] = "/system/bin/app_process";
	static bool first_zygote = true;

	/* This applies to versions Android 10+ */
	static const char system_bin_init[] = "/system/bin/init";
	static bool init_second_stage_executed = false;

	// https://cs.android.com/android/platform/superproject/+/android-16.0.0_r2:system/core/init/main.cpp;l=77
	if (unlikely(!memcmp(path, system_bin_init, sizeof(system_bin_init) - 1) &&
			argv)) {
		char buf[16];
		if (!init_second_stage_executed &&
			check_argv(*argv, 1, "second_stage", buf, sizeof(buf))) {
			pr_info("/system/bin/init second_stage executed\n");
			apply_kernelsu_rules();
			cache_sid();
			setup_ksu_cred();
			init_second_stage_executed = true;
		}
	}

	if (unlikely(first_zygote &&
			!memcmp(path, app_process, sizeof(app_process) - 1) && argv)) {
		char buf[16];
		if (check_argv(*argv, 1, "-Xzygote", buf, sizeof(buf))) {
			pr_info("exec zygote, /data prepared, second_stage: %d\n",
				init_second_stage_executed);
			on_post_fs_data();
			first_zygote = false;
			ksu_stop_ksud_execve_hook();
		}
	}
}

int ksu_handle_execveat_ksud(int *fd, struct filename **filename_ptr,
				     void *argv, void *envp, int *flags)
{
	struct filename *filename;
	struct user_arg_ptr argv_ptr = { 0 };

	if (!filename_ptr)
		return 0;

	filename = *filename_ptr;
	if (IS_ERR(filename) || !filename->name)
		return 0;

	if (argv) {
		argv_ptr.ptr.native = (const char __user *const __user *)argv;
	}

	ksu_handle_execveat_ksud_path(filename->name, argv ? &argv_ptr : NULL);
	return 0;
}

int ksu_handle_execveat(int *fd, struct filename **filename_ptr,
			      void *argv, void *envp, int *flags)
{
	ksu_handle_execveat_ksud(fd, filename_ptr, argv, envp, flags);
	return ksu_handle_execveat_sucompat(fd, filename_ptr, argv, envp, flags);
}

void ksu_stop_ksud_execve_hook()
{
	pr_info("ksud: manual mode execve hook stop requested\n");
}

static ssize_t (*orig_read)(struct file *, char __user *, size_t, loff_t *);
static ssize_t (*orig_read_iter)(struct kiocb *, struct iov_iter *);
static struct file_operations fops_proxy;
static ssize_t ksu_rc_pos = 0;
const size_t ksu_rc_len = sizeof(KERNEL_SU_RC) - 1;

// https://cs.android.com/android/platform/superproject/main/+/main:system/core/init/parser.cpp;l=144;drc=61197364367c9e404c7da6900658f1b16c42d0da
// https://cs.android.com/android/platform/superproject/main/+/main:system/libbase/file.cpp;l=241-243;drc=61197364367c9e404c7da6900658f1b16c42d0da
// The system will read init.rc file until EOF, whenever read() returns 0,
// so we begin append ksu rc when we meet EOF.

static ssize_t read_proxy(struct file *file, char __user *buf, size_t count,
				loff_t *pos)
{
	ssize_t ret = 0;
	size_t append_count;
	if (ksu_rc_pos && ksu_rc_pos < ksu_rc_len)
		goto append_ksu_rc;

	ret = orig_read(file, buf, count, pos);
	if (ret != 0 || ksu_rc_pos >= ksu_rc_len) {
		return ret;
	} else {
		pr_info("read_proxy: orig read finished, start append rc\n");
	}
append_ksu_rc:
	append_count = ksu_rc_len - ksu_rc_pos;
	if (append_count > count - ret)
		append_count = count - ret;
	// copy_to_user returns the number of not copied
	if (copy_to_user(buf + ret, KERNEL_SU_RC + ksu_rc_pos, append_count)) {
		pr_info("read_proxy: append error, totally appended %ld\n", ksu_rc_pos);
	} else {
		pr_info("read_proxy: append %ld\n", append_count);

		ksu_rc_pos += append_count;
		if (ksu_rc_pos == ksu_rc_len) {
			pr_info("read_proxy: append done\n");
		}
		ret += append_count;
	}

	return ret;
}

static ssize_t read_iter_proxy(struct kiocb *iocb, struct iov_iter *to)
{
	ssize_t ret = 0;
	size_t append_count;
	if (ksu_rc_pos && ksu_rc_pos < ksu_rc_len)
		goto append_ksu_rc;

	ret = orig_read_iter(iocb, to);
	if (ret != 0 || ksu_rc_pos >= ksu_rc_len) {
		return ret;
	} else {
		pr_info("read_iter_proxy: orig read finished, start append rc\n");
	}
append_ksu_rc:
	// copy_to_iter returns the number of copied bytes
	append_count =
		copy_to_iter(KERNEL_SU_RC + ksu_rc_pos, ksu_rc_len - ksu_rc_pos, to);
	if (!append_count) {
		pr_info("read_iter_proxy: append error, totally appended %ld\n",
			ksu_rc_pos);
	} else {
		pr_info("read_iter_proxy: append %ld\n", append_count);

		ksu_rc_pos += append_count;
		if (ksu_rc_pos == ksu_rc_len) {
			pr_info("read_iter_proxy: append done\n");
		}
		ret += append_count;
	}
	return ret;
}

static bool is_init_rc(struct file *fp)
{
	if (strcmp(current->comm, "init")) {
		// we are only interest in `init` process
		return false;
	}

	if (!d_is_reg(fp->f_path.dentry)) {
		return false;
	}

	const char *short_name = fp->f_path.dentry->d_name.name;
	if (strcmp(short_name, "init.rc")) {
		// we are only interest `init.rc` file name file
		return false;
	}
	char path[256];
	char *dpath = d_path(&fp->f_path, path, sizeof(path));

	if (IS_ERR(dpath)) {
		return false;
	}

	if (strcmp(dpath, "/system/etc/init/hw/init.rc")) {
		return false;
	}

	return true;
}

static void ksu_install_rc_hook(struct file *file)
{
	if (!is_init_rc(file)) {
		return;
	}

	// we only process the first read
	static bool rc_hooked = false;
	if (rc_hooked) {
		// we don't need these hooks, unregister it!

		return;
	}
	rc_hooked = true;

	// now we can sure that the init process is reading
	// `/system/etc/init/init.rc`

	pr_info("read init.rc, comm: %s, rc_count: %zu\n", current->comm,
		ksu_rc_len);

	// Now we need to proxy the read and modify the result!
	// But, we can not modify the file_operations directly, because it's in read-only memory.
	// We just replace the whole file_operations with a proxy one.
	memcpy(&fops_proxy, file->f_op, sizeof(struct file_operations));
	orig_read = file->f_op->read;
	if (orig_read) {
		fops_proxy.read = read_proxy;
	}
	orig_read_iter = file->f_op->read_iter;
	if (orig_read_iter) {
		fops_proxy.read_iter = read_iter_proxy;
	}
	// replace the file_operations
	file->f_op = &fops_proxy;
}

static void ksu_handle_sys_read(unsigned int fd, char __user **buf_ptr,
				size_t *count_ptr)
{
	struct file *file = fget(fd);
	if (!file) {
		return;
	}
	ksu_install_rc_hook(file);
	fput(file);
}

/* Compatibility stub for legacy manual patch sets. */
int __maybe_unused ksu_handle_input_handle_event(unsigned int *type,
						  unsigned int *code,
						  int *value)
{
	return 0;
}

static bool safe_mode_flag = false;
#define VOLUME_PRESS_THRESHOLD_COUNT 3

static void vol_detector_event(struct input_handle *handle, unsigned int type,
			       unsigned int code, int value)
{
	static int vol_up_cnt = 0;
	static int vol_down_cnt = 0;

	if (!value || type != EV_KEY)
		return;

	if (code == KEY_VOLUMEDOWN) {
		vol_down_cnt++;
		pr_info("KEY_VOLUMEDOWN press detected!\n");
	}

	if (code == KEY_VOLUMEUP) {
		vol_up_cnt++;
		pr_info("KEY_VOLUMEUP press detected!\n");
	}

	pr_info("volume_pressed_count: vol_up: %d vol_down: %d\n",
		vol_up_cnt, vol_down_cnt);

	if (vol_up_cnt >= VOLUME_PRESS_THRESHOLD_COUNT ||
	    vol_down_cnt >= VOLUME_PRESS_THRESHOLD_COUNT) {
		pr_info("volume keys pressed max times, safe mode detected!\n");
		safe_mode_flag = true;
	}
}

static int vol_detector_connect(struct input_handler *handler,
				 struct input_dev *dev,
				 const struct input_device_id *id)
{
	struct input_handle *handle;
	int error;

	handle = kzalloc(sizeof(struct input_handle), GFP_KERNEL);
	if (!handle)
		return -ENOMEM;

	handle->dev = dev;
	handle->handler = handler;
	handle->name = "ksu_handle_input";

	error = input_register_handle(handle);
	if (error)
		goto err_free_handle;

	error = input_open_device(handle);
	if (error)
		goto err_unregister_handle;

	return 0;

err_unregister_handle:
	input_unregister_handle(handle);
err_free_handle:
	kfree(handle);
	return error;
}

static const struct input_device_id vol_detector_ids[] = {
	{
		.flags = INPUT_DEVICE_ID_MATCH_EVBIT | INPUT_DEVICE_ID_MATCH_KEYBIT,
		.evbit = { BIT_MASK(EV_KEY) },
		.keybit = { [BIT_WORD(KEY_VOLUMEUP)] = BIT_MASK(KEY_VOLUMEUP) },
	},
	{
		.flags = INPUT_DEVICE_ID_MATCH_EVBIT | INPUT_DEVICE_ID_MATCH_KEYBIT,
		.evbit = { BIT_MASK(EV_KEY) },
		.keybit = { [BIT_WORD(KEY_VOLUMEDOWN)] = BIT_MASK(KEY_VOLUMEDOWN) },
	},
	{}
};

static void vol_detector_disconnect(struct input_handle *handle)
{
	input_close_device(handle);
	input_unregister_handle(handle);
	kfree(handle);
}

static struct input_handler vol_detector_handler = {
	.event = vol_detector_event,
	.connect = vol_detector_connect,
	.disconnect = vol_detector_disconnect,
	.name = "ksu",
	.id_table = vol_detector_ids,
};

static int vol_detector_init(void)
{
	pr_info("vol_detector: init\n");
	return input_register_handler(&vol_detector_handler);
}

static int vol_detector_exit(void)
{
	pr_info("vol_detector: exit\n");
	input_unregister_handler(&vol_detector_handler);
	return 0;
}

bool ksu_is_safe_mode()
{
	static bool already_checked = false;
	if (already_checked) {
		// don't need to check again, userspace may call multiple times
		return true;
	}

	// stop hook first!
	stop_input_hook();

	if (!safe_mode_flag)
		return false;

	pr_info("volume keys pressed max times, safe mode detected!\n");
	already_checked = true;
	return true;
}

void ksu_execve_hook_ksud(const struct pt_regs *regs)
{
	const char __user **filename_user = (const char **)&PT_REGS_PARM1(regs);
	const char __user *const __user *__argv =
		(const char __user *const __user *)PT_REGS_PARM2(regs);
	struct user_arg_ptr argv = { .ptr.native = __argv };
	char path[32];
	long ret;
	unsigned long addr;
	const char __user *fn;

	if (!filename_user)
		return;

	addr = untagged_addr((unsigned long)*filename_user);
	fn = (const char __user *)addr;

	memset(path, 0, sizeof(path));
	ret = strncpy_from_user(path, fn, 32);
	if (ret < 0) {
		pr_err("Access filename failed for execve_handler_pre\n");
		return;
	}

	ksu_handle_execveat_ksud_path(path, &argv);
}

static long (*orig_sys_read)(const struct pt_regs *regs);
static long ksu_sys_read(const struct pt_regs *regs)
{
	unsigned int fd = PT_REGS_PARM1(regs);
	char __user **buf_ptr = (char __user **)&PT_REGS_PARM2(regs);
	size_t *count_ptr = (size_t *)&PT_REGS_PARM3(regs);

	ksu_handle_sys_read(fd, buf_ptr, count_ptr);
	return orig_sys_read(regs);
}

static long (*orig_sys_fstat)(const struct pt_regs *regs);
static long ksu_sys_fstat(const struct pt_regs *regs)
{
	unsigned int fd = PT_REGS_PARM1(regs);
	void __user *statbuf = (void __user *)PT_REGS_PARM2(regs);
	bool is_rc = false;
	long ret;

	struct file *file = fget(fd);
	if (file) {
		if (is_init_rc(file)) {
			pr_info("stat init.rc");
			is_rc = true;
		}
		fput(file);
	}

	ret = orig_sys_fstat(regs);

	if (is_rc) {
		void __user *st_size_ptr = statbuf + offsetof(struct stat, st_size);
		long size, new_size;
		if (!copy_from_user_nofault(&size, st_size_ptr, sizeof(long))) {
			new_size = size + ksu_rc_len;
			pr_info("adding ksu_rc_len: %ld -> %ld", size, new_size);
			if (!copy_to_user_nofault(st_size_ptr, &new_size, sizeof(long))) {
				pr_info("added ksu_rc_len");
			} else {
				pr_err("add ksu_rc_len failed: statbuf 0x%lx",
					(unsigned long)st_size_ptr);
			}
		} else {
			pr_err("read statbuf 0x%lx failed", (unsigned long)st_size_ptr);
		}
	}

	return ret;
}

static void ksu_common_newfstat_ret(unsigned int fd, void __user *statbuf,
				     bool is_stat64, const char *syscall_name)
{
	struct file *file;
	void __user *st_size_ptr;
	long size;
	long new_size;
	size_t len = sizeof(long);

	if (!statbuf)
		return;

	file = fget(fd);
	if (!file)
		return;

	if (!is_init_rc(file)) {
		fput(file);
		return;
	}
	fput(file);

	/* WARNING: this keeps compatibility with legacy behavior for stat64 layouts. */
	if (is_stat64)
		st_size_ptr = (char __user *)statbuf + 44;
	else
		st_size_ptr = (char __user *)statbuf + offsetof(struct stat, st_size);

	if (copy_from_user_nofault(&size, st_size_ptr, len)) {
		pr_err("%s: read statbuf failed: 0x%lx\n", syscall_name,
		       (unsigned long)st_size_ptr);
		return;
	}

	new_size = size + ksu_rc_len;
	pr_info("%s: adding ksu_rc_len: %ld -> %ld\n", syscall_name, size,
		new_size);

	if (!copy_to_user_nofault(st_size_ptr, &new_size, len))
		pr_info("%s: added ksu_rc_len\n", syscall_name);
	else
		pr_err("%s: add ksu_rc_len failed: 0x%lx\n", syscall_name,
		       (unsigned long)st_size_ptr);
}

void ksu_handle_newfstat_ret(unsigned int *fd, struct stat __user **statbuf_ptr)
{
	if (!fd || !statbuf_ptr)
		return;

	ksu_common_newfstat_ret(*fd, *statbuf_ptr, false, "sys_newfstat");
}

#if defined(__ARCH_WANT_STAT64) || defined(__ARCH_WANT_COMPAT_STAT64)
void ksu_handle_fstat64_ret(unsigned long *fd, struct stat64 __user **statbuf_ptr)
{
	if (!fd || !statbuf_ptr)
		return;

	ksu_common_newfstat_ret((unsigned int)*fd, *statbuf_ptr, true,
			       "sys_fstat64");
}
#endif

static void stop_init_rc_hook()
{
	pr_info("manual mode: init_rc runtime hook disabled\n");
}

static void stop_input_hook()
{
	static bool input_hook_stopped = false;

	if (input_hook_stopped)
		return;

	input_hook_stopped = true;
	pr_info("stop input_hook\n");
	vol_detector_exit();
}

// ksud: module support
void ksu_ksud_init()
{
	vol_detector_init();
}

void ksu_ksud_exit()
{
	stop_input_hook();
	pr_info("ksud: manual mode exit\n");
}
