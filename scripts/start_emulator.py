#!/usr/bin/env python3
"""
管理 Android 模拟器
列出、启动和停止 Android 模拟器
"""

import subprocess
import sys
import argparse
import time
import os


def run_command(cmd, check=True, capture_output=True):
    """运行命令"""
    try:
        result = subprocess.run(
            cmd,
            shell=True,
            check=check,
            capture_output=capture_output,
            text=True
        )
        return result
    except subprocess.CalledProcessError as e:
        return e


def find_emulator():
    """查找 emulator 可执行文件"""
    # 尝试常用的 Android SDK 路径
    sdk_paths = []

    # 从环境变量获取
    if os.environ.get("ANDROID_HOME"):
        sdk_paths.append(os.environ["ANDROID_HOME"])
    if os.environ.get("ANDROID_SDK_ROOT"):
        sdk_paths.append(os.environ["ANDROID_SDK_ROOT"])

    # Windows 默认路径
    if sys.platform == "win32":
        sdk_paths.extend([
            os.path.expanduser("~\\AppData\\Local\\Android\\Sdk"),
        ])
    # macOS/Linux 默认路径
    else:
        sdk_paths.extend([
            os.path.expanduser("~/Library/Android/sdk"),
            os.path.expanduser("~/Android/Sdk"),
        ])

    for sdk_path in sdk_paths:
        emulator_path = os.path.join(sdk_path, "emulator", "emulator")
        if sys.platform == "win32":
            emulator_path += ".exe"
        if os.path.exists(emulator_path):
            return emulator_path

    # 尝试直接运行
    result = run_command("emulator -version", check=False)
    if result.returncode == 0:
        return "emulator"

    return None


def list_avds(emulator_cmd):
    """列出可用的 AVD"""
    result = run_command(f'"{emulator_cmd}" -list-avds')
    if result.returncode != 0:
        return None

    avds = [line.strip() for line in result.stdout.strip().split('\n') if line.strip()]
    return avds


def list_running_devices():
    """列出正在运行的设备"""
    result = run_command("adb devices")
    if result.returncode != 0:
        return []

    lines = result.stdout.strip().split('\n')[1:]
    devices = []
    for line in lines:
        parts = line.split()
        if parts and parts[1] == "device":
            devices.append(parts[0])
    return devices


def start_emulator(emulator_cmd, avd_name, detach=True, wipe_data=False, no_window=False):
    """启动模拟器"""
    cmd = f'"{emulator_cmd}" -avd "{avd_name}"'

    if wipe_data:
        cmd += " -wipe-data"
    if no_window:
        cmd += " -no-window"

    print(f"正在启动模拟器: {avd_name}")
    print(f"命令: {cmd}")

    if detach:
        # 后台启动
        if sys.platform == "win32":
            # Windows: 使用 start 命令
            subprocess.Popen(cmd, shell=True, creationflags=subprocess.DETACHED_PROCESS)
        else:
            # macOS/Linux: 使用 &
            subprocess.Popen(f"{cmd} &", shell=True)
        print("模拟器正在后台启动...")
        return True
    else:
        # 前台启动
        result = run_command(cmd, check=False, capture_output=False)
        return result.returncode == 0


def wait_for_device(timeout=120):
    """等待设备上线"""
    print("等待设备上线...", end="", flush=True)
    start_time = time.time()

    while time.time() - start_time < timeout:
        devices = list_running_devices()
        if devices:
            print(f"\n设备已连接: {devices[0]}")
            return devices[0]
        print(".", end="", flush=True)
        time.sleep(2)

    print("\n等待超时！", file=sys.stderr)
    return None


def stop_emulator(device_id=None):
    """停止模拟器"""
    devices = list_running_devices()

    if not devices:
        print("没有正在运行的设备")
        return True

    if device_id:
        if device_id not in devices:
            print(f"设备未找到: {device_id}", file=sys.stderr)
            return False
        target_device = device_id
    else:
        target_device = devices[0]

    print(f"正在停止设备: {target_device}")
    result = run_command(f"adb -s {target_device} emu kill", check=False)
    return result.returncode == 0


def main():
    parser = argparse.ArgumentParser(description="管理 Android 模拟器")
    subparsers = parser.add_subparsers(dest="command", help="可用命令")

    # 列表命令
    list_parser = subparsers.add_parser("list", help="列出可用的 AVD")

    # 启动命令
    start_parser = subparsers.add_parser("start", help="启动模拟器")
    start_parser.add_argument("avd", nargs="?", help="AVD 名称")
    start_parser.add_argument("--wipe", "-w", action="store_true", help="清除数据启动")
    start_parser.add_argument("--no-window", action="store_true", help="无窗口模式（用于 CI）")
    start_parser.add_argument("--wait", action="store_true", help="等待设备上线")
    start_parser.add_argument("--foreground", "-f", action="store_true", help="前台运行")

    # 停止命令
    stop_parser = subparsers.add_parser("stop", help="停止模拟器")
    stop_parser.add_argument("device", nargs="?", help="设备 ID")

    # 状态命令
    status_parser = subparsers.add_parser("status", help="显示设备状态")

    args = parser.parse_args()

    print("=" * 60)
    print("Android 模拟器管理脚本")
    print("=" * 60)

    # 查找 emulator
    emulator_cmd = find_emulator()
    if not emulator_cmd:
        print("错误: 未找到 emulator 命令", file=sys.stderr)
        print("请设置 ANDROID_HOME 环境变量", file=sys.stderr)
        return 1

    print(f"\nEmulator: {emulator_cmd}")

    # 执行命令
    if args.command == "list":
        avds = list_avds(emulator_cmd)
        if avds:
            print(f"\n可用的 AVD ({len(avds)}):")
            for i, avd in enumerate(avds, 1):
                print(f"  {i}. {avd}")
        else:
            print("\n没有找到可用的 AVD")
            print("请使用 Android Studio 的 AVD Manager 创建模拟器")

    elif args.command == "start":
        avds = list_avds(emulator_cmd)
        if not avds:
            print("\n错误: 没有可用的 AVD", file=sys.stderr)
            return 1

        # 选择 AVD
        if not args.avd:
            if len(avds) == 1:
                args.avd = avds[0]
            else:
                print(f"\n可用的 AVD:")
                for i, avd in enumerate(avds, 1):
                    print(f"  {i}. {avd}")
                choice = input("\n请选择 AVD (1-{}): ".format(len(avds)))
                try:
                    idx = int(choice) - 1
                    if 0 <= idx < len(avds):
                        args.avd = avds[idx]
                    else:
                        print("无效的选择", file=sys.stderr)
                        return 1
                except ValueError:
                    print("无效的输入", file=sys.stderr)
                    return 1

        # 启动
        if start_emulator(emulator_cmd, args.avd, detach=not args.foreground,
                         wipe_data=args.wipe, no_window=args.no_window):
            if args.wait:
                device = wait_for_device()
                if device:
                    print("\n模拟器已就绪！")
                else:
                    return 1
            return 0
        else:
            return 1

    elif args.command == "stop":
        if stop_emulator(args.device):
            print("已发送停止命令")
            return 0
        else:
            return 1

    elif args.command == "status":
        devices = list_running_devices()
        if devices:
            print(f"\n正在运行的设备 ({len(devices)}):")
            for device in devices:
                print(f"  - {device}")
        else:
            print("\n没有正在运行的设备")

    else:
        parser.print_help()

    return 0


if __name__ == "__main__":
    sys.exit(main())
