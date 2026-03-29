[English](README.md) | [简体中文](README_CN.md) | [繁體中文](README_TW.md) | [Türkçe](README_TR.md) | [Português (Brasil)](README_PT-BR.md) | [한국어](README_KO.md) | [Français](README_FR.md) | [Bahasa Indonesia](README_ID.md) | [Русский](README_RU.md) | [Українська](README_UA.md) | [ภาษาไทย](README_TH.md) | [Tiếng Việt](README_VI.md) | [Italiano](README_IT.md) | [Polski](README_PL.md) | **Български** | [日本語](README_JA.md) | [Español](README_ES.md)

---

<div align="center">
  <img src="/assets/wksu.png" width="96" alt="Wild KSU Logo">

  <h2>Wild KSU</h2>
  <p><strong>Решение за root достъп, базирано на ядрото, за устройства с Android.</strong></p>

  <p>
    <a href="https://github.com/Wild-KSU/Wild-KSU/releases/latest">
      <img src="https://img.shields.io/github/v/release/Wild-KSU/Wild-KSU?label=Release&logo=github" alt="Latest Release">
    </a>
    <a href="https://nightly.link/Wild-KSU/Wild-KSU/workflows/build-manager-ci/next/Manager">
      <img src="https://img.shields.io/badge/Nightly%20Release-gray?logo=hackthebox&logoColor=fff" alt="Nightly Build">
    </a>
    <a href="https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html">
      <img src="https://img.shields.io/badge/License-GPL%20v2-orange.svg?logo=gnu" alt="License: GPL v2">
    </a>
    <a href="/LICENSE">
      <img src="https://img.shields.io/github/license/Wild-KSU/Wild-KSU?logo=gnu" alt="GitHub License">
    </a>
    <a title="Crowdin" target="_blank" href="https://crowdin.com/project/wild-ksu"><img src="https://badges.crowdin.net/wild-ksu/localized.svg"></a>
  </p>
</div>

---

## 🚀 Възможности

- Управление на `su` и root достъп на ядрено ниво.
- Система за модули базирана на [Magic Mount](https://topjohnwu.github.io/Magisk/details.html#magic-mount) / [OverlayFS](https://en.wikipedia.org/wiki/OverlayFS).
- [Профили за приложения](https://kernelsu.org/guide/app-profile.html): Ограничаване на root права за конкретни приложения.

---

## ✅ Съвместимост

Wild KSU официално поддържа повечето Android ядра от версия **4.4 до 6.6**.

| Версия на ядрото     | Бележки относно поддръжката                                                                  |
|----------------------|----------------------------------------------------------------------------------------------|
| 5.10+ (GKI 2.0)      | Могат да използват предварително компилирани изображения и LKM/KMI                           |
| 4.19 – 5.4 (GKI 1.0) | Изисква вграден драйвер на KernelSU                                                          |
| < 4.14 (EOL)         | Изисква драйвер на KernelSU (3.18+ е експериментален и може да се нуждае от обратни портове) |

**Поддържани архитектури:** `arm64-v8a`, `armeabi-v7a` и `x86_64`

---

## 📦 Инсталация

Моля, вижте ръководството за [Инсталация](https://wild-ksu.github.io/webpage/pages/installation.html) за инструкции за инсталация.

---

## 🏅 Сигурност

- За докладване на уязвимости вижте [SECURITY.md](/SECURITY.md).

---

## 📜 Лиценз

- **Файловете в директорията `kernel`:** [GPL-2.0-only](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html).
- **Всички останали файлове:** [GPL-3.0-or-later](https://www.gnu.org/licenses/gpl-3.0.html).

---

## 💸 Дарения

Ако искате да подкрепите проекта:

- **USDT (BEP20, ERC20)**: `0x12b5224b7aca0121c2f003240a901e1d064371c1`
- **USDT (TRC20)**: `TYUVMWGTcnR5svnDoX85DWHyqUAeyQcdjh`
- **USDT (SOL)**: `A4wqBXYd6Ey4nK4SJ2bmjeMgGyaLKT9TwDLh8BEo8Zu6`
- **ETH (ERC20)**: `0x12b5224b7aca0121c2f003240a901e1d064371c1`
- **LTC**: `Ld238uYBuRQdZB5YwdbkuU6ektBAAUByoL`
- **BTC**: `19QgifcjMjSr1wB2DJcea5cxitvWVcXMT6`

---

## 🙏 Благодарности

- [Kernel-Assisted Superuser](https://git.zx2c4.com/kernel-assisted-superuser/about/) – Идеята за KernelSU
- [Magisk](https://github.com/topjohnwu/Magisk) – Мощният root инструмент
- [Genuine](https://github.com/brevent/genuine/) – Валидация на APK подписи v2
- [Diamorphine](https://github.com/m0nad/Diamorphine) – Rootkit техники
- [KernelSU](https://github.com/tiann/KernelSU) – Благодарности към tiann за създаването на KernelSU
- [Magic Mount Port](https://github.com/5ec1cff/KernelSU/blob/main/userspace/ksud/src/magic_mount.rs) – 💜 5ec1cff за спасяването на KernelSU
