#!/usr/bin/env python3
"""
Android APK 构建脚本
用于构建 Debug 和 Release 版本的 APK
"""

import subprocess
import sys
import os
from pathlib import Path


def run_command(cmd, cwd=None):
    """运行命令并返回输出"""
    try:
        result = subprocess.run(
            cmd,
            shell=True,
            cwd=cwd,
            check=True,
            capture_output=True,
            text=True
        )
        print(result.stdout)
        if result.stderr:
            print(result.stderr, file=sys.stderr)
        return True
    except subprocess.CalledProcessError as e:
        print(f"命令执行失败: {e.cmd}", file=sys.stderr)
        print(f"输出: {e.stdout}", file=sys.stderr)
        print(f"错误: {e.stderr}", file=sys.stderr)
        return False


def main():
    # 获取项目根目录
    project_root = Path(__file__).parent.parent

    print("=" * 60)
    print("Android APK 构建脚本")
    print("=" * 60)

    # 清理之前的构建
    print("\n[1/3] 清理构建目录...")
    if not run_command("./gradlew clean", cwd=project_root):
        print("清理失败！", file=sys.stderr)
        return 1

    # 构建 Debug APK
    print("\n[2/3] 构建 Debug APK...")
    if not run_command("./gradlew assembleDebug", cwd=project_root):
        print("Debug APK 构建失败！", file=sys.stderr)
        return 1

    # 构建 Release APK
    print("\n[3/3] 构建 Release APK...")
    if not run_command("./gradlew assembleRelease", cwd=project_root):
        print("Release APK 构建失败！", file=sys.stderr)
        return 1

    print("\n" + "=" * 60)
    print("构建完成！")
    print("=" * 60)

    # 显示 APK 位置
    debug_apk = project_root / "app" / "build" / "outputs" / "apk" / "debug" / "app-debug.apk"
    release_apk = project_root / "app" / "build" / "outputs" / "apk" / "release" / "app-release.apk"

    if debug_apk.exists():
        print(f"\nDebug APK: {debug_apk}")
        print(f"  大小: {debug_apk.stat().st_size / 1024 / 1024:.2f} MB")

    if release_apk.exists():
        print(f"\nRelease APK: {release_apk}")
        print(f"  大小: {release_apk.stat().st_size / 1024 / 1024:.2f} MB")

    return 0


if __name__ == "__main__":
    sys.exit(main())
