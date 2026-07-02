#!/usr/bin/env python3
"""
GeoJSON / Shapefile → 矢量瓦片 (目录结构) 预处理脚本

与 tile-vector.py 的区别：
  - tile-vector.py  → 输出 MBTiles 单文件（推荐）
  - tile-vector-dir.py → 输出 {z}/{x}/{y}.pbf 目录结构

用法:
    python tile-vector-dir.py <input> <output_dir> [--layer buildings] [--min-zoom 8] [--max-zoom 18]
"""
import argparse
import json
import os
import shutil
import sqlite3
import subprocess
import sys
import tempfile
from datetime import datetime
from pathlib import Path


def log(msg: str):
    print(f"[{datetime.now():%H:%M:%S}] {msg}")


def fail(msg: str):
    print(f"\033[0;31m✗ {msg}\033[0m")
    sys.exit(1)


def success(msg: str):
    print(f"\033[0;32m✓ {msg}\033[0m")


def find_executable(name: str, extra_paths: list[str] = None) -> str:
    path = shutil.which(name)
    if path:
        return path
    if extra_paths:
        for p in extra_paths:
            full = os.path.join(p, "bin", name)
            if os.path.exists(full):
                return full
    fail(f"{name} 未安装")


def main():
    parser = argparse.ArgumentParser(description="GeoJSON/Shapefile → {z}/{x}/{y}.pbf 瓦片目录")
    parser.add_argument("input", help="输入文件 (.geojson / .shp)")
    parser.add_argument("output", help="输出目录")
    parser.add_argument("--layer", default="features", help="图层名称")
    parser.add_argument("--min-zoom", type=int, default=8)
    parser.add_argument("--max-zoom", type=int, default=18)
    parser.add_argument("--gdal-path", default=os.environ.get("GDAL_PATH", "/usr"))
    args = parser.parse_args()

    input_path = Path(args.input)
    output_dir = Path(args.output)

    if not input_path.exists():
        fail(f"输入文件不存在: {input_path}")

    tippecanoe = find_executable("tippecanoe")
    ogr2ogr = find_executable("ogr2ogr", [args.gdal_path])

    # Shapefile → GeoJSON
    tmp_file = None
    ext = input_path.suffix.lower()
    if ext == ".shp":
        tmp_file = tempfile.mktemp(suffix=".geojson")
        log(f"Shapefile → GeoJSON")
        subprocess.run([ogr2ogr, "-f", "GeoJSON", tmp_file, str(input_path)],
                       capture_output=True, timeout=300)
        input_path = Path(tmp_file)

    # Step 1: tippecanoe → MBTiles（临时）
    output_dir.mkdir(parents=True, exist_ok=True)
    mbtiles_tmp = tempfile.mktemp(suffix=".mbtiles")
    log(f"生成 MBTiles...")

    proc = subprocess.run([
        tippecanoe,
        f"--output={mbtiles_tmp}",
        f"--layer={args.layer}",
        f"--minimum-zoom={args.min_zoom}",
        f"--maximum-zoom={args.max_zoom}",
        "--simplification=10",
        "--buffer=64",
        "--generate-ids",
        "--drop-densest-as-needed",
        "--force",
        str(input_path),
    ], capture_output=True, text=True, timeout=3600)

    if tmp_file and os.path.exists(tmp_file):
        os.remove(tmp_file)

    if proc.returncode != 0:
        fail(f"tippecanoe 失败: {proc.stderr[:500]}")

    # Step 2: MBTiles → {z}/{x}/{y}.pbf 目录结构
    log("解包 MBTiles 到目录结构...")
    tile_dir = output_dir / "tiles"
    tile_count = 0

    with sqlite3.connect(mbtiles_tmp) as conn:
        rows = conn.execute("SELECT zoom_level, tile_column, tile_row, tile_data FROM tiles").fetchall()
        for z, x, y_tms, data in rows:
            # TMS → XYZ Y轴翻转
            y_xyz = (1 << z) - 1 - y_tms
            tile_path = tile_dir / str(z) / str(x)
            tile_path.mkdir(parents=True, exist_ok=True)
            pbf_file = tile_path / f"{y_xyz}.pbf"
            pbf_file.write_bytes(data)
            tile_count += 1

    # 清理临时 MBTiles
    os.remove(mbtiles_tmp)

    # 生成 metadata.json
    metadata = {
        "format": "pbf",
        "minZoom": args.min_zoom,
        "maxZoom": args.max_zoom,
        "layerName": args.layer,
        "tileCount": tile_count,
        "generatedAt": datetime.now().isoformat(),
    }
    (output_dir / "metadata.json").write_text(json.dumps(metadata, indent=2))

    success(f"矢量瓦片生成完成")
    print(f"\n==============================")
    print(f"  目录: {tile_dir}")
    print(f"  瓦片数: {tile_count}")
    print(f"  缩放级别: {args.min_zoom} - {args.max_zoom}")
    print(f"==============================")


if __name__ == "__main__":
    main()
