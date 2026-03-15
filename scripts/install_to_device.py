#!/usr/bin/env python3
"""
安装 APK 到连接的 Android 设备
支持安装 Debug 或 Release 版本
"""

import subprocess
import sys
import argparse
from pathlib import Path


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


def check_adb():
    """检查 ADB 是否可用"""
    result = run_command("adb version")
    if result.returncode != 0:
        print("错误: 未找到 adb 命令，请确保 Android SDK 已正确安装并配置到 PATH", file=sys.stderr)
        return False
    return True


def list_devices():
    """列出连接的设备"""
    result = run_command("adb devices -l")
    if result.returncode != 0:
        print("错误: 无法列出设备", file=sys.stderr)
        return None

    lines = result.stdout.strip().split('\n')[1:]  # 跳过第一行 "List of devices attached"
    devices = [line for line in lines if line.strip()]

    return devices


def install_apk(apk_path, device_id=None):
    """安装 APK 到设备"""
    cmd = "adb"
    if device_id:
        cmd += f" -s {device_id}"
    cmd += f" install -r -d \"{apk_path}\""

    print(f"正在安装: {apk_path}")
    result = run_command(cmd, check=False, capture_output=False)

    if result.returncode == 0:
        print("安装成功！")
        return True
    else:
        print("安装失败！", file=sys.stderr)
        return False


def launch_app(package_name, activity_name=None, device_id=None):
    """启动应用"""
    cmd = "adb"
    if device_id:
        cmd += f" -s {device_id}"

    if activity_name:
        # 启动指定的 Activity
        cmd += f" shell am start -n {package_name}/{activity_name}"
    else:
        # 启动默认的 Launcher Activity
        cmd += f" shell monkey -p {package_name} -c android.intent.category.LAUNCHER 1"

    result = run_command(cmd, check=False)
    return result.returncode == 0


def main():
    parser = argparse.ArgumentParser(description="安装 APK 到 Android 设备")
    parser.add_argument(
        "--variant", "-v",
        choices=["debug", "release"],
        default="debug",
        help="APK 变体 (默认: debug)"
    )
    parser.add_argument(
        "--device", "-d",
        help="指定设备 ID (可选，默认使用第一个设备)"
    )
    parser.add_argument(
        "--launch", "-l",
        action="store_true",
        help="安装后启动应用"
    )
    parser.add_argument(
        "--apk", "-a",
        help="直接指定 APK 文件路径"
    )

    args = parser.parse_args()

    print("=" * 60)
    print("Android APK 安装脚本")
    print("=" * 60)

    # 检查 ADB
    if not check_adb():
        return 1

    # 检查设备
    devices = list_devices()
    if not devices:
        print("错误: 未检测到连接的设备，请连接设备或启动模拟器", file=sys.stderr)
        return 1

    print(f"\n检测到 {len(devices)} 个设备:")
    for i, device in enumerate(devices, 1):
        print(f"  {i}. {device}")

    # 确定 APK 路径
    if args.apk:
        apk_path = Path(args.apk)
    else:
        project_root = Path(__file__).parent.parent
        apk_dir = project_root / "app" / "build" / "outputs" / "apk" / args.variant

        # 查找 APK 文件
        apks = list(apk_dir.glob(f"*-{args.variant}.apk"))
        if not apks:
            print(f"错误: 在 {apk_dir} 中未找到 {args.variant} APK", file=sys.stderr)
            print("请先运行 build_apk.py 构建 APK", file=sys.stderr)
            return 1

        apk_path = apks[0]

    if not apk_path.exists():
        print(f"错误: APK 文件不存在: {apk_path}", file=sys.stderr)
        return 1

    # 安装 APK
    print(f"\nAPK: {apk_path}")
    print(f"大小: {apk_path.stat().st_size / 1024 / 1024:.2f} MB\n")

    if not install_apk(apk_path, args.device):
        return 1

    # 可选：启动应用
    if args.launch:
        print("\n正在启动应用...")
        if launch_app("com.example.myapplication", device_id=args.device):
            print("应用已启动！")
        else:
            print("启动应用失败", file=sys.stderr)

    return 0


if __name__ == "__main__":
    sys.exit(main())
