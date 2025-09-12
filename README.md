<div align="center">
  <img src="./assets/kernelsu_next.png" width="120" alt="Wild KSU Logo">
  
  # Wild KSU
  
  **🔥 A powerful kernel-based root solution for Android devices 🔥**
  
  [![Latest Release](https://img.shields.io/github/v/release/WildKernels/Wild_KSU?label=Release&logo=github&style=for-the-badge)](https://github.com/WildKernels/Wild_KSU/releases/latest)
  [![Nightly Build](https://img.shields.io/badge/Nightly%20Release-gray?logo=hackthebox&logoColor=fff&style=for-the-badge)](https://nightly.link/WildKernels/Wild_KSU/workflows/build-manager-ci/wild/Manager)
  [![License: GPL v2](https://img.shields.io/badge/License-GPL%20v2-orange.svg?logo=gnu&style=for-the-badge)](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html)
  [![Crowdin](https://badges.crowdin.net/wild-ksu/localized.svg)](https://crowdin.com/project/wild-ksu)
  
  ---
  
  ### 🌍 Languages & Translations
  
  **English** (Current)
  
  > 🌐 **Translation contributions are welcome!**  
  > Help us make Wild KSU accessible to more users worldwide by contributing translations via:  
  > - 📝 **Pull Requests** - Submit translation files directly  
  > - 🔗 **[Crowdin](https://crowdin.com/project/wild-ksu)** - Collaborative translation platform
  
</div>

---

## ✨ What is Wild KSU?

Wild KSU is an advanced, kernel-based root solution that provides seamless superuser access management for Android devices. Built upon the foundation of KernelSU, Wild KSU offers enhanced features, improved compatibility, and a more robust architecture for power users and developers.

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

## 🔧 Kernel Integration

> 🚀 **Ready to integrate Wild KSU into your kernel?** 
> 
> Use our automated setup script to integrate Wild KSU into your kernel source:

```bash
curl -LSs "https://raw.githubusercontent.com/WildKernels/Wild_KSU/wild/kernel/setup.sh" | bash -s wild
```

> 📋 **Note:** This script will automatically configure your kernel source tree with Wild KSU support. Make sure you have the necessary build dependencies installed before running the integration script.

---

## 🔐 Security & Reporting

Security is our top priority. If you discover any security vulnerabilities or issues:

📋 **Please review our [Security Policy](SECURITY.md)** for responsible disclosure guidelines.

---

## 📜 License Information

Wild KSU is open-source software distributed under multiple licenses:

| 📁 **Directory** | ⚖️ **License** | 📄 **Details** |
|------------------|----------------|----------------|
| `/kernel` | [GPL-2.0-only](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html) | Kernel components |
| **All other files** | [GPL-3.0-or-later](https://www.gnu.org/licenses/gpl-3.0.html) | Userspace components |

---

## 💝 Support the Project

If Wild KSU has been helpful to you, consider supporting our development efforts:

### 💰 **Cryptocurrency Donations**

| 💱 **Currency** | 🏦 **Network** | 📍 **Address** |
|-----------------|----------------|----------------|
| **USDT** | BEP20, ERC20 | `0x12b5224b7aca0121c2f003240a901e1d064371c1` |
| **USDT** | TRC20 | `TYUVMWGTcnR5svnDoX85DWHyqUAeyQcdjh` |
| **USDT** | Solana | `A4wqBXYd6Ey4nK4SJ2bmjeMgGyaLKT9TwDLh8BEo8Zu6` |
| **ETH** | ERC20 | `0x12b5224b7aca0121c2f003240a901e1d064371c1` |
| **LTC** | Litecoin | `Ld238uYBuRQdZB5YwdbkuU6ektBAAUByoL` |
| **BTC** | Bitcoin | `19QgifcjMjSr1wB2DJcea5cxitvWVcXMT6` |

---

## 🙏 Acknowledgments

Wild KSU stands on the shoulders of giants. We extend our gratitude to:

- 🔬 **[Kernel-Assisted Superuser](https://git.zx2c4.com/kernel-assisted-superuser/about/)** - Original concept and inspiration
- 🎭 **[Magisk](https://github.com/topjohnwu/Magisk)** - Core root implementation techniques
- 🔍 **[Genuine](https://github.com/brevent/genuine/)** - APK v2 signature validation
- 💎 **[Diamorphine](https://github.com/m0nad/Diamorphine)** - Advanced rootkit techniques
- 🌟 **[KernelSU](https://github.com/tiann/KernelSU)** - The foundational base that made Wild KSU possible
- 🪄 **[Magic Mount Port](https://github.com/5ec1cff/KernelSU/blob/main/userspace/ksud/src/magic_mount.rs)** - Magic mount implementation

---

<div align="center">
  
  ### 🌟 **Star this repository if Wild KSU helped you!** 🌟
  
  **Made with ❤️ by the Wild Kernels Team**
  
  ---
  
  [![GitHub stars](https://img.shields.io/github/stars/WildKernels/Wild_KSU?style=social)](https://github.com/WildKernels/Wild_KSU/stargazers)
  [![GitHub forks](https://img.shields.io/github/forks/WildKernels/Wild_KSU?style=social)](https://github.com/WildKernels/Wild_KSU/network/members)
  [![GitHub watchers](https://img.shields.io/github/watchers/WildKernels/Wild_KSU?style=social)](https://github.com/WildKernels/Wild_KSU/watchers)
  
</div>