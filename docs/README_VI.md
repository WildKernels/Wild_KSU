[English](README.md) | [简体中文](README_CN.md) | [繁體中文](README_TW.md) | [Türkçe](README_TR.md) | [Português (Brasil)](README_PT-BR.md) | [한국어](README_KO.md) | [Français](README_FR.md) | [Bahasa Indonesia](README_ID.md) | [Русский](README_RU.md) | [Українська](README_UA.md) | [ภาษาไทย](README_TH.md) | **Tiếng Việt** | [Italiano](README_IT.md) | [Polski](README_PL.md) | [Български](README_BG.md) | [日本語](README_JA.md) | [Español](README_ES.md)

# Wild KSU

<img src="/assets/wksu.png" style="width: 96px;" alt="logo">

Một giải pháp root từ nhân linux dành cho các thiết bị chạy Android

[![Phiên bản mới nhất](https://img.shields.io/github/v/release/Wild-KSU/Wild-KSU?label=Release&logo=github)](https://github.com/Wild-KSU/Wild-KSU/releases/latest)
[![CI build mới nhất](https://img.shields.io/badge/Nightly%20Release-gray?logo=hackthebox&logoColor=fff)](https://nightly.link/Wild-KSU/Wild-KSU/workflows/build-manager-ci/next/Manager)
[![Gíây pháp: GPL v2](https://img.shields.io/badge/License-GPL%20v2-orange.svg?logo=gnu)](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html)
[![Gíây phép GITHUB](https://img.shields.io/github/license/Wild-KSU/Wild-KSU?logo=gnu)](/LICENSE)

## Tính năng

1. Quản lý quyền truy cập SU dựa trên kernel android.
2. Hệ thống mount module dựa trên 1 trong 2 cơ chế mount [Magic Mount](https://topjohnwu.github.io/Magisk/details.html#magic-mount) / [OverlayFS](https://en.wikipedia.org/wiki/OverlayFS).
3. [App Profile](https://kernelsu.org/guide/app-profile.html): Quản lý quyền truy cập root 1 cách chặt chẽ

## Danh sách tương thích

Wild KSU hỗ trợ chính thức các kernel Android từ phiên bản 4.4 đến 6.6
 - GKI 2.0 (5.10+) kernels có thể cài đặt qua những .img/.zip đã được build sẵn và LKM/KMI hoặc tự vá qua manager (nếu được)
 - GKI 1.0 (4.19 - 5.4) kernels cần dược build lại với các nhân Wild KSU
 - EOL (<4.14) kernels cần dược build lại với các nhân Wild KSU (các kernels 3.18+ đang dược thử nghiệm và có thể cần backports 1 vài thứ ).

Hiện tại Wild KSU chỉ hỗ trợ những cpu có `arm64-v8a`, `armeabi-v7a` & `x86_64` 

> [!CAUTION]
> Các phiên bản kernel gần đây đã áp dụng một thay đổi lớn có thể khiến Wild KSU gặp lỗi và có khả năng gây ra kernel panic trên `x86_64`! Hãy kiểm tra trang web để biết thêm thông tin!

## Sử dụng

- [Hướng dẫn vá Wild KSU vào Kernel của bạn (yêu cầu kernel source)](https://wksu.org/pages/installation.html)

## Bảo mật

Để biết thêm thông tin về việc báo cáo lỗ hổng bảo mật trong Wild KSU vui lòng đọc (Thông tin sẽ dược gửi về KernelSU)[SECURITY.md](/SECURITY.md).

## Gíây phép

- Những thư mục/tập tin trong `kernel` là giấy phép [GPL-2.0-only](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html).
- Những thư mục/tập tin ngoài `kernel` là giấy phép [GPL-3.0-or-later](https://www.gnu.org/licenses/gpl-3.0.html).

## Quyên góp/Hỗ trợ

- 0x12b5224b7aca0121c2f003240a901e1d064371c1 [ USDT BEP20 ]

- TYUVMWGTcnR5svnDoX85DWHyqUAeyQcdjh [ USDT TRC20 ]

- 0x12b5224b7aca0121c2f003240a901e1d064371c1 [ USDT ERC20 ]

- 0x12b5224b7aca0121c2f003240a901e1d064371c1 [ ETH ERC20 ]

- Ld238uYBuRQdZB5YwdbkuU6ektBAAUByoL [ LTC ]

- 19QgifcjMjSr1wB2DJcea5cxitvWVcXMT6 [ BTC ]

## Lời cảm ơn tới...

- [Kernel-Assisted Superuser](https://git.zx2c4.com/kernel-assisted-superuser/about/): Ý tưởng cho sự ra đời của KernelSU.
- [Magisk](https://github.com/topjohnwu/Magisk): Công cụ root mạnh mẽ, quen thuộc và tương thích cao cho các thiết bị chạy Android.
- [genuine](https://github.com/brevent/genuine/): Chữ kí apk v2.
- [Diamorphine](https://github.com/m0nad/Diamorphine): Một vài kỹ năng rootkit.
- [KernelSU](https://github.com/tiann/KernelSU): Nguồn gốc của Wild KSU, thanks to tiann.
- [Magic Mount Port](https://github.com/5ec1cff/KernelSU/blob/main/userspace/ksud/src/magic_mount.rs): 5ec1cff - người đã cứu lấy KernelSU💜 !
