#!/bin/sh
set -eu

# Wild KSU auto patcher for manual hook integration.
# Applies predetermined .patch files for supported kernel versions.

KERNEL_ROOT=""
KERNEL_VERSION=""
PATCH_DIR=""

usage() {
    cat <<'EOF'
Usage: auto_patch.sh -p <kernel-path> -v <gki|ngki>

    -p, --path <kernel-path>:    Kernel source root path.
    -v, --version <gki|ngki>:    Patch variant to apply.
    -h, --help:                  Show this help.

Supported variants:
    gki : GKI kernels 5.10+ (64-bit core patch set)
    ngki: Not implemented yet.

Exit codes:
  0 success (all patches applied)
  1 one or more patches failed
  2 invalid usage
EOF
}

log() { printf '%s\n' "$*"; }
err() { printf '[ERROR] %s\n' "$*" >&2; }

validate_kernel_root() {
    root="$1"
    
    # Check for drivers directory structure (same as setup.sh logic)
    if [ -d "$root/common/drivers" ]; then
        return 0
    elif [ -d "$root/drivers" ]; then
        return 0
    else
        err "Invalid kernel root: neither $root/common/drivers nor $root/drivers found"
        return 1
    fi
}

get_kernel_src_base() {
    root="$1"
    
    # Detect where kernel source files are located
    # GKI kernels may have fs/ under common/ or at root
    if [ -d "$root/common/fs" ]; then
        printf '%s' "$root/common"
    elif [ -d "$root/fs" ]; then
        printf '%s' "$root"
    else
        err "Kernel source fs/ directory not found in $root or $root/common"
        return 1
    fi
}

PATCH_OK=0
PATCH_FAIL=0

report_ok() {
    PATCH_OK=$((PATCH_OK + 1))
    log "[OK] $1"
}

report_fail() {
    PATCH_FAIL=$((PATCH_FAIL + 1))
    err "[FAIL] $1"
}

apply_patch() {
    patch_file="$1"
    patch_name="$(basename "$patch_file" .patch)"
    kernel_src_base="$2"
    
    if [ ! -f "$patch_file" ]; then
        report_fail "$patch_name: patch file not found at $patch_file"
        return 1
    fi
    
    log "[TRY] applying $patch_name from $patch_file"

    cd "$kernel_src_base" || return 1

    dryrun_log="$(mktemp)"
    apply_log="$(mktemp)"

    if patch -p1 --dry-run < "$patch_file" >"$dryrun_log" 2>&1; then
        if patch -p1 < "$patch_file" >"$apply_log" 2>&1; then
            report_ok "$patch_name applied successfully"
            rm -f "$dryrun_log" "$apply_log"
            cd - > /dev/null
            return 0
        fi

        err "$patch_name apply step failed; output:"
        cat "$apply_log" >&2
    else
        err "$patch_name dry-run failed; output:"
        cat "$dryrun_log" >&2
    fi

    rm -f "$dryrun_log" "$apply_log"
    report_fail "$patch_name failed to apply"
    cd - > /dev/null
    return 1
}

while [ $# -gt 0 ]; do
    case "$1" in
        -p|--path|--kernel-root)
            shift
            [ $# -gt 0 ] || { err "-p|--path requires a value"; usage; exit 2; }
            KERNEL_ROOT="$1"
            ;;
        -v|--version)
            shift
            [ $# -gt 0 ] || { err "-v requires a value"; usage; exit 2; }
            KERNEL_VERSION="$1"
            ;;
        --help|-h)
            usage
            exit 0
            ;;
        *)
            err "unknown option: $1"
            usage
            exit 2
            ;;
    esac
    shift
done

if [ -z "$KERNEL_ROOT" ] || [ -z "$KERNEL_VERSION" ]; then
    err "missing required arguments"
    usage
    exit 2
fi

if [ ! -d "$KERNEL_ROOT" ]; then
    err "kernel root not found: $KERNEL_ROOT"
    exit 2
fi

case "$KERNEL_VERSION" in
    gki)
        ;;
    ngki)
        err "ngki patch variant is not implemented yet"
        exit 2
        ;;
    *)
        err "unsupported patch variant: $KERNEL_VERSION"
        usage
        exit 2
        ;;
esac

# Locate patch directory
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PATCH_DIR="$SCRIPT_DIR/../patches/$KERNEL_VERSION"

if [ ! -d "$PATCH_DIR" ]; then
    err "patch set not found for kernel version $KERNEL_VERSION"
    err "available patches: $SCRIPT_DIR/../patches/*"
    exit 2
fi

# Validate kernel root structure before patching
if ! validate_kernel_root "$KERNEL_ROOT"; then
    exit 1
fi

log "Applying patches for kernel $KERNEL_VERSION from $PATCH_DIR"

# Detect kernel source base directory
KERNEL_SRC_BASE=$(get_kernel_src_base "$KERNEL_ROOT")
if [ -z "$KERNEL_SRC_BASE" ]; then
    exit 1
fi
log "Using kernel source base: $KERNEL_SRC_BASE"

# Apply patches in order
apply_patch "$PATCH_DIR/exec.patch" "$KERNEL_SRC_BASE"
apply_patch "$PATCH_DIR/open.patch" "$KERNEL_SRC_BASE"
apply_patch "$PATCH_DIR/stat.patch" "$KERNEL_SRC_BASE"
apply_patch "$PATCH_DIR/stat_ret.patch" "$KERNEL_SRC_BASE"
apply_patch "$PATCH_DIR/reboot.patch" "$KERNEL_SRC_BASE"

# 32-bit patch files are kept in PATCH_DIR for later, but not applied by default.

log ""
log "=========================================="
log "Patch Summary: $PATCH_OK OK, $PATCH_FAIL FAILED"
log "=========================================="

if [ "$PATCH_FAIL" -gt 0 ]; then
    exit 1
fi

exit 0
