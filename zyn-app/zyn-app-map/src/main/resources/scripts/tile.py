#!/usr/bin/env python3
"""
GeoTIFF → XYZ 栅格瓦片切片脚本

用法:
    python tile.py <input.tif> <output_dir> [--min-zoom 8] [--max-zoom 18] [--gdal-path /usr]

依赖:
    - GDAL (gdal2tiles.py, gdaladdo, gdal_translate)
    - Windows: D:/OSGeo4W/bin/
"""
import argparse
import os
import shutil
import subprocess
import sys
from datetime import datetime
from pathlib import Path


def log(msg: str):
    print(f"[{datetime.now():%H:%M:%S}] {msg}")


def fail(msg: str):
    print(f"\033[0;31m✗ {msg}\033[0m")
    sys.exit(1)


def success(msg: str):
    print(f"\033[0;32m✓ {msg}\033[0m")


def find_executable(gdal_path: str, name: str) -> str:
    """在 GDAL 安装目录中查找可执行文件"""
    candidates = [
        os.path.join(gdal_path, "bin", name),
        os.path.join(gdal_path, name),
        shutil.which(name),
    ]
    for c in candidates:
        if c and os.path.exists(c):
            return c
    fail(f"{name} 未找到（检查 --gdal-path: {gdal_path}）")


def main():
    parser = argparse.ArgumentParser(description="GeoTIFF → XYZ 栅格瓦片")
    parser.add_argument("input", help="输入栅格文件路径")
    parser.add_argument("output", help="输出目录")
    parser.add_argument("--min-zoom", type=int, default=8, help="最小缩放级别 (默认 8)")
    parser.add_argument("--max-zoom", type=int, default=18, help="最大缩放级别 (默认 18)")
    parser.add_argument("--gdal-path", default=os.environ.get("GDAL_PATH", "/usr"), help="GDAL 安装路径")
    args = parser.parse_args()

    input_path = Path(args.input)
    output_dir = Path(args.output)

    if not input_path.exists():
        fail(f"输入文件不存在: {input_path}")

    gdal_translate = find_executable(args.gdal_path, "gdal_translate")
    gdaladdo = find_executable(args.gdal_path, "gdaladdo")
    gdal2tiles = find_executable(args.gdal_path, "gdal2tiles.py")

    output_dir.mkdir(parents=True, exist_ok=True)

    log(f"输入: {input_path}")
    log(f"输出: {output_dir}")
    log(f"缩放级别: {args.min_zoom} - {args.max_zoom}")

    # Step 1: 检查源坐标系
    log("检查源坐标系...")
    try:
        result = subprocess.run([gdal_translate, "-of", "VRT", str(input_path), "/dev/stdout"],
                                capture_output=True, text=True, timeout=30)
        log(f"源文件: {input_path.name}")
    except Exception:
        log("无法检测源坐标系，跳过")

    # Step 2: 生成概览金字塔
    log("生成概览金字塔...")
    subprocess.run([gdaladdo, "-r", "average", str(input_path),
                    "2", "4", "8", "16", "32", "64", "128"],
                   capture_output=True, timeout=300)
    success("概览金字塔已生成")

    # Step 3: 切片
    log("开始切片 (gdal2tiles)...")
    proc = subprocess.run([
        gdal2tiles,
        "--profile=mercator",
        f"--zoom={args.min_zoom}-{args.max_zoom}",
        "--webviewer=none",
        "--resampling=bilinear",
        "--processes=4",
        "--xyz",
        str(input_path),
        str(output_dir),
    ], capture_output=True, text=True, timeout=3600)

    if proc.returncode != 0:
        fail(f"gdal2tiles 失败: {proc.stderr[:500]}")

    # 统计
    tile_count = sum(1 for _ in output_dir.rglob("*.png"))
    z_levels = sum(1 for d in output_dir.iterdir() if d.is_dir() and d.name.isdigit())

    success(f"切片完成: {tile_count} 个瓦片, {z_levels} 个缩放级别")
    print(f"\n==============================")
    print(f"  目录: {output_dir}")
    print(f"  瓦片数: {tile_count}")
    print(f"  缩放级别: {args.min_zoom} - {args.max_zoom}")
    print(f"==============================")


if __name__ == "__main__":
    main()
