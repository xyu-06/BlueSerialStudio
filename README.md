# BlueSerialStudio | 蓝牙串口实验室

[![Build APK](https://github.com/xyu-06/BlueSerialStudio/actions/workflows/build-apk.yml/badge.svg)](https://github.com/xyu-06/BlueSerialStudio/actions/workflows/build-apk.yml)

面向 STM32 和其他嵌入式设备的 **Android 原生 Java 蓝牙 SPP/RFCOMM 串口助手**。适用于 HC-05、HC-06 和其他支持蓝牙经典串口 SPP 的模块；仅支持 BLE 的设备不适用。

## 功能

- **蓝牙串口**：经典蓝牙发现/配对/连接，UTF-8 和 HEX 收发，TX/RX 数据日志、计数器、清空与导出。
- **自定义结束符**：无、\\n、\\r、\\r\\n，发送界面跟随设置更新；支持输入法键盘适配。
- **三种互斥小车控制模式**：360° 单摇杆、左右双摇杆（左前后/右转向）、四个方向按键（按住时间越长速度越大）。
- **统一遥控协议**：三种模式都产生转向 X、油门 Y，默认发送 `J,{x},{y}`，可自定义格式；摇杆松手自动回中，提供急停。
- **动态组件**：快捷按键与参数滑杆可添加、编辑、删除，并保存在本机。
- **实时曲线**：串口数值解析与多通道绘图。
- **STM32 示例**：标准外设库 USART 收发与差速混控，支持前进中平滑转向，减少左右轮控制冲突。

## 下载 APK

**[下载最新 v1.4 Debug APK](https://raw.githubusercontent.com/xyu-06/BlueSerialStudio/main/downloads/BlueSerialStudio-v1.4-debug.apk)**

自动构建记录：[GitHub Actions](https://github.com/xyu-06/BlueSerialStudio/actions/workflows/build-apk.yml)。每次 `main` 分支更新时会编译并上传 APK 工件，还会把 APK 发布到 `downloads/` 路径，便于手机直接下载。

> 当前为 **Debug 测试版**，不是商店签名发行版。首次从旧仓库 APK 切换至新仓库构建的 APK，可能因签名不同而无法覆盖安装，必要时备份设置后卸载旧版。

## 开发构建

环境：JDK 17、Android SDK Platform 35、Gradle 8.11.1、Android Gradle Plugin 8.9.2。

```bash
gradle --no-daemon assembleDebug
```

APK 默认输出 `app/build/outputs/apk/debug/app-debug.apk`。仓库没有完整 Gradle Wrapper 时，请使用系统 Gradle 或 Actions。

## 串口控制协议

X 表示转向（右为正），Y 表示前后速度（前进为正），各在 -100 至 100 之间。例如：

```text
J,0,80     # 直行
J,50,80    # 前进中右转
J,0,0      # 停车
```

三种控制模式仅改变人机操作，不改变上述发送格式。STM32 端必须实施失联/超时停车机制，运动控制优先保护人员和电机。参考 `stm32_sample/blue_serial_drive_mixer.h` 与 `stm32_sample/blue_serial_protocol.c`。

## 目录结构

| 目录 | 说明 |
|---|---|
| `app/` | Android 原生应用 |
| `stm32_sample/` | STM32 USART 通信和差速混控示例 |
| `qa/` | 界面源码静态回归检查 |
| `tests/` | Java 协议与 C 混控测试 |
| `.github/workflows/` | APK 自动测试与编译 |
| `downloads/` | 自动生成的 Debug APK |

## 测试说明

- 旧仓库中的 v1.4 已成功完成 GitHub Actions Android APK 构建。
- 新仓库的 CI 会重新验证协议测试、源码静态检查、C 语言混控测试和 APK 编译。
- **实体手机、SPP 硬件连接、真实电机运动、双指触控和输入法兼容性尚未实测**。第一次操作小车请先架空车轮。
