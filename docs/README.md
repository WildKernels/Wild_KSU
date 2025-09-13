<div align="center">
  <img src="../assets/wksu.png" width="120" alt="Wild KSU Logo">
  
  # Wild KSU
  
  **🔥 A customization and root hiding focused fork 🔥**
  
  > ⚠️ **IMPORTANT NOTICE**: This project is currently in testing mode. All releases should be considered unstable and may be unstable until version 1.0.0 is released. Use at your own risk.
  
  <p align="center">
    <a href="https://github.com/WildKernels/Wild_KSU/releases/latest">
      <img src="https://img.shields.io/github/v/release/WildKernels/Wild_KSU?label=Release&logo=github&style=for-the-badge&color=blue" alt="Latest Release">
    </a>
    <a href="https://nightly.link/WildKernels/Wild_KSU/workflows/build-manager-ci/wild/Manager">
      <img src="https://img.shields.io/badge/Nightly-Build-purple?logo=hackthebox&logoColor=fff&style=for-the-badge" alt="Nightly Build">
    </a>
  </p>
  
  <p align="center">
    <a href="https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html">
      <img src="https://img.shields.io/badge/License-GPL%20v2-orange.svg?logo=gnu&style=for-the-badge" alt="GPL v2 License">
    </a>
    <a href="https://www.gnu.org/licenses/gpl-3.0.en.html">
      <img src="https://img.shields.io/badge/License-GPL%20v3-red.svg?logo=gnu&style=for-the-badge" alt="GPL v3 License">
    </a>
  </p>
  
  <p align="center">
    <a href="https://crowdin.com/project/wild-ksu">
      <img src="https://img.shields.io/badge/Crowdin-Translate-green?logo=crowdin&logoColor=white&style=for-the-badge" alt="Crowdin Translations">
    </a>
  </p>
  
  ---
  
  ![Stats](../assets/gray0_ctp_on_line.svg)

  ### 🌍 Languages & Translations
  
  **English** (Current)
  
  > 🌐 **Translation contributions are welcome!**  
  > Help us make Wild KSU accessible to more users worldwide by contributing translations via:  
  > - 📝 **Pull Requests** - Submit translation files directly  
  > - 🔗 **[Crowdin](https://crowdin.com/project/wild-ksu)** - Collaborative translation platform
  
</div>

---

## ✨ What is Wild KSU?

Wild KSU is a fork of KernelSU Next focused on customization and root hiding.

**What is KernelSU?**
KernelSU is a root solution for Android GKI devices, it works in kernel mode and grants root permission to userspace apps directly in kernel space.

**Features**
The main feature of KernelSU is that it's kernel-based. KernelSU works in 2 modes:
- **GKI**: Replace the original kernel of the device with the Generic Kernel Image (GKI) provided by KernelSU.
- **LKM**: Load the Loadable Kernel Module (LKM) into the device kernel without replacing the original kernel.

These two modes are suitable for different scenarios, and you can choose the one according to your needs.

---

## 🔧 Kernel Integration / GKI Mode

> 🚀 **Ready to integrate Wild KSU into your kernel?** 
> 
> Use our automated setup script to integrate Wild KSU into your kernel source:

```bash
curl -LSs "https://raw.githubusercontent.com/WildKernels/Wild_KSU/wild/kernel/setup.sh" | bash -s wild
```

> 📋 **Note:** This script will automatically configure your kernel source tree with Wild KSU support.

### 📚 Documentation

For detailed documentation, please refer to the original KernelSU project:

🔗 **[KernelSU Official Documentation](https://kernelsu.org/guide/what-is-kernelsu.html)**

This documentation covers installation procedures, usage guidelines, and technical details that apply to Wild KSU as well.

---

## 🚀 Key Features

### 🔐 **Advanced Root Management**
- **Kernel-based `su`** - Direct kernel-level superuser implementation
- **Granular root access control** - Fine-tuned permission management
- **Secure privilege escalation** - Safe and controlled root access

### 📦 **Powerful Module System**
- **[Magic Mount](https://topjohnwu.github.io/Magisk/details.html#magic-mount)** - Seamless file system modifications
- **[OverlayFS](https://en.wikipedia.org/wiki/OverlayFS)** - Advanced overlay file system support
- **Dynamic module loading** - Hot-swappable functionality

### 🎯 **App Profile System**
- **[Per-app root control](https://kernelsu.org/guide/app-profile.html)** - Limit root privileges on a per-application basis
- **Granular permissions** - Fine-grained access control
- **Security profiles** - Customizable security policies

---

## ✅ Compatibility Matrix

Wild KSU supports a wide range of Android kernel versions from **4.4 up to 6.6**:

| 🔧 Kernel Version | 📱 Support Level | 📝 Implementation Notes |
|-------------------|------------------|-------------------------|
| **5.10+ (GKI 2.0)** | ✅ **Full Support** | Pre-built images, LKM/KMI support |
| **4.19 – 5.4 (GKI 1.0)** | ✅ **Supported** | Requires built-in KernelSU driver |
| **< 4.14 (EOL)** | ⚠️ **Limited** | Requires driver (3.18+ experimental, may need backports) |

### 🏗️ **Supported Architectures**
- `arm64-v8a` - 64-bit ARM
- `armeabi-v7a` - 32-bit ARM
- `x86_64` - 64-bit x86

---

## 🔐 Security & Reporting

Security is our top priority. If you discover any security vulnerabilities or issues:

📋 **Please review our [Security Policy](SECURITY.md)** for responsible disclosure guidelines.

---

## 📜 License Information

Wild KSU is open-source software distributed under multiple licenses:

| 📁 **Directory** | ⚖️ **License** |
|------------------|----------------|
| `/kernel` | [GPL-2.0-only](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html) |
| **All other files** | [GPL-3.0-or-later](https://www.gnu.org/licenses/gpl-3.0.html) |

---

## 💝 Support the Project

If Wild KSU has been helpful to you, consider supporting our development efforts:

### 💰 **Donations**

| 💱 **Method** | 📍 **Address/Link** |
|---------------|--------------------|
| **PayPal** | `bauhd@outlook.com` |
| **Card** | [`https://buy.stripe.com/5kQ28sdi08Nr0Xc2fU5os00`](https://buy.stripe.com/5kQ28sdi08Nr0Xc2fU5os00) |
| **LTC** | `MVaN1ToSuks2cdK9mB3M8EHCfzQSyEMf6h` |
| **BTC** | `3BBXAMS4ZuCZwfbTXxWGczxHF4isymeyxG` |
| **ETH** | `0x2b9C846c84d58717e784458406235C09a834274e` |

> 💡 **Support the original project:** For donations to KernelSU Next, visit [`https://github.com/KernelSU-Next/KernelSU-Next`](https://github.com/KernelSU-Next/KernelSU-Next)

---

## 🙏 Acknowledgments

Wild KSU stands on the shoulders of giants. We extend our gratitude to:

- 🔬 **[Kernel-Assisted Superuser](https://git.zx2c4.com/kernel-assisted-superuser/about/)** - Original concept and inspiration
- 🎭 **[Magisk](https://github.com/topjohnwu/Magisk)** - Core root implementation techniques
- 🔍 **[Genuine](https://github.com/brevent/genuine/)** - APK v2 signature validation
- 💎 **[Diamorphine](https://github.com/m0nad/Diamorphine)** - Advanced rootkit techniques
- 🚀 **[KernelSU Next](https://github.com/KernelSU-Next/KernelSU-Next)** - The fork base that made Wild KSU possible
- 🌟 **[KernelSU](https://github.com/tiann/KernelSU)** - The original foundational project
- 🪄 **[Magic Mount Port](https://github.com/5ec1cff/KernelSU/blob/main/userspace/ksud/src/magic_mount.rs)** - Magic mount implementation

---

<div align="center">
  
  ### 🌟 **Star this repository if Wild KSU helped you!** 🌟
  
  **Made with ❤️ by the Wild Kernels Team**
  
  ---
  
  [![GitHub stars](https://img.shields.io/github/stars/WildKernels/Wild_KSU?style=social)](https://github.com/WildKernels/Wild_KSU/stargazers)
  [![GitHub forks](https://img.shields.io/github/forks/WildKernels/Wild_KSU?style=social)](https://github.com/WildKernels/Wild_KSU/network/members)
  [![GitHub watchers](https://img.shields.io/github/watchers/WildKernels/Wild_KSU?style=social)](https://github.com/WildKernels/Wild_KSU/watchers)
  
  ![Stats](../assets/gray0_ctp_on_line.svg)
  
</div>