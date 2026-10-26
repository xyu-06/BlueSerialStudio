# 测试与验证状态（v1.4）

- GitHub Actions：运行 Python 源码回归检查、Java 串口协议测试、C 语言轮速混控测试，然后执行 Android Gradle Debug APK 构建。
- 迁移来源：原 `xyu-06/javaProject` 的 `blue-serial-studio` 分支，最新已成功编译版本 v1.4.0。
- 源码内容：SPP/RFCOMM 通信；单摇杆、双摇杆、四方向按键共用控制格式；清空 TX/RX 日志；动态按键、滑杆和图形曲线。
- **尚未执行**：真实手机蓝牙配对/断线测试、软键盘适配真机测试、蓝牙模块 HC-05/HC-06 与实际小车行驶测试。
- 源码与 APK 构建通过不保证车辆行驶安全；需要 STM32 端正确解析和通信看门狗，首次测试请架空驱动轮。
