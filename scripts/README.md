# Android 开发脚本

本目录包含用于 Android 开发的常用 uv run python 脚本。

## 前置要求

- uv 
- Android SDK（配置好环境变量 `ANDROID_HOME` 或 `ANDROID_SDK_ROOT`）
- ADB（添加到 PATH）

## 脚本列表

### 1. build_apk.py - 构建 APK

构建 Debug 和 Release 版本的 APK。

**用法:**
```bash
uv run python scripts/build_apk.py
```

**功能:**
- 清理之前的构建
- 构建 Debug APK
- 构建 Release APK
- 显示生成的 APK 路径和大小

---

### 2. install_to_device.py - 安装到设备

将 APK 安装到连接的 Android 设备或模拟器。

**用法:**
```bash
# 安装 Debug APK 到第一个设备
uv run python scripts/install_to_device.py

# 安装 Release APK
uv run python scripts/install_to_device.py --variant release

# 安装后启动应用
uv run python scripts/install_to_device.py --launch

# 指定设备
uv run python scripts/install_to_device.py --device <device-id>

# 直接指定 APK 文件
uv run python scripts/install_to_device.py --apk /path/to/app.apk
```

**参数:**
- `--variant, -v`: APK 变体 (debug/release, 默认: debug)
- `--device, -d`: 指定设备 ID
- `--launch, -l`: 安装后启动应用
- `--apk, -a`: 直接指定 APK 文件路径

---

### 3. start_emulator.py - 管理模拟器

列出、启动和停止 Android 模拟器。

**用法:**
```bash
# 列出可用的 AVD
uv run python scripts/start_emulator.py list

# 启动模拟器（交互选择）
uv run python scripts/start_emulator.py start

# 启动指定 AVD
uv run python scripts/start_emulator.py start <avd-name>

# 启动并等待设备上线
uv run python scripts/start_emulator.py start --wait

# 清除数据启动
uv run python scripts/start_emulator.py start --wipe

# 停止模拟器
uv run python scripts/start_emulator.py stop

# 查看设备状态
uv run python scripts/start_emulator.py status
```

**命令:**
- `list`: 列出可用的 AVD
- `start`: 启动模拟器
- `stop`: 停止模拟器
- `status`: 显示设备状态

**start 子命令参数:**
- `--wipe, -w`: 清除数据启动
- `--no-window`: 无窗口模式（用于 CI）
- `--wait`: 等待设备上线
- `--foreground, -f`: 前台运行

---

## 工作流程示例

### 完整的构建-安装-运行流程

```bash
# 1. 构建 APK
uv run python scripts/build_apk.py

# 2. 启动模拟器（如果需要）
uv run python scripts/start_emulator.py start --wait

# 3. 安装并启动应用
uv run python scripts/install_to_device.py --launch
```

### 快速开发迭代

```bash
# 构建 Debug 版本并直接安装
./gradlew assembleDebug && uv run python scripts/install_to_device.py --launch
```

---

## 故障排除

### ADB 未找到
确保 Android SDK 的 `platform-tools` 目录已添加到 PATH。

### Emulator 未找到
设置 `ANDROID_HOME` 环境变量指向 Android SDK 目录。

### 设备未检测到
- 检查 USB 调试是否已启用
- 尝试重新连接 USB 线
- 运行 `adb devices` 确认连接状态

### Gradle 构建失败
- 检查 JDK 版本（需要 JDK 17）
- 确保网络连接正常（需要下载依赖）
- 尝试运行 `./gradlew clean` 清理构建
