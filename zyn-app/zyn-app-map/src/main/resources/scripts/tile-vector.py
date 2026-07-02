#!/usr/bin/env python3
"""
GeoJSON / Shapefile → 矢量瓦片 (MBTiles) 预处理脚本

用法:
    python tile-vector.py <input> <output.mbtiles> [--layer buildings] [--min-zoom 8] [--max-zoom 18]

依赖:
    - tippecanoe (https://github.com/felt/tippecanoe)
    - ogr2ogr (GDAL, 仅 Shapefile 需要)
"""
import argparse
import json
import os
import shutil
import subprocess
import sys
import tempfile
from datetime import datetime, timezone
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
            full = os.path.join(p, name)
            if os.path.exists(full):
                return full
    fail(f"{name} 未安装。tippecanoe: https://github.com/felt/tippecanoe")


def convert_shp_to_geojson(shp_path: str, ogr2ogr: str) -> str:
    """Shapefile → GeoJSON 临时文件"""
    tmp = tempfile.mktemp(suffix=".geojson")
    log(f"Shapefile → GeoJSON: {shp_path}")
    proc = subprocess.run([ogr2ogr, "-f", "GeoJSON", tmp, shp_path],
                          capture_output=True, text=True, timeout=300)
    if proc.returncode != 0:
        fail(f"ogr2ogr 转换失败: {proc.stderr[:300]}")
    success(f"转换完成: {tmp}")
    return tmp


def count_features(geojson_path: str) -> int:
    """快速统计要素数量"""
    try:
        with open(geojson_path, "r", encoding="utf-8") as f:
            data = json.load(f)
        if data.get("type") == "FeatureCollection":
            return len(data.get("features", []))
        return 1
    except Exception:
        return -1


def main():
    parser = argparse.ArgumentParser(description="GeoJSON/Shapefile → MBTiles 矢量瓦片")
    parser.add_argument("input", help="输入文件 (.geojson / .json / .shp)")
    parser.add_argument("output", help="输出 MBTiles 文件路径")
    parser.add_argument("--layer", default="features", help="图层名称 (默认 features)")
    parser.add_argument("--min-zoom", type=int, default=8, help="最小缩放级别 (默认 8)")
    parser.add_argument("--max-zoom", type=int, default=18, help="最大缩放级别 (默认 18)")
    parser.add_argument("--gdal-path", default=os.environ.get("GDAL_PATH", "/usr"), help="GDAL 路径")
    args = parser.parse_args()

    input_path = Path(args.input)
    output_path = Path(args.output)

    if not input_path.exists():
        fail(f"输入文件不存在: {input_path}")

    tippecanoe = find_executable("tippecanoe")
    ogr2ogr = find_executable("ogr2ogr", [args.gdal_path])

    # Shapefile 转 GeoJSON
    tmp_file = None
    ext = input_path.suffix.lower()
    if ext == ".shp":
        tmp_file = convert_shp_to_geojson(str(input_path), ogr2ogr)
        input_path = Path(tmp_file)
    elif ext not in (".geojson", ".json"):
        fail(f"不支持的格式: {ext}（支持 .geojson / .json / .shp）")

    # 统计要素数
    feature_count = count_features(str(input_path))
    if feature_count > 0:
        log(f"要素数量: {feature_count}")

    # 生成 MBTiles
    output_path.parent.mkdir(parents=True, exist_ok=True)
    log(f"切片: {input_path.name} → {output_path}")
    log(f"图层: {args.layer} | 缩放: {args.min_zoom}-{args.max_zoom}")

    proc = subprocess.run([
        tippecanoe,
        f"--output={output_path}",
        f"--layer={args.layer}",
        f"--minimum-zoom={args.min_zoom}",
        f"--maximum-zoom={args.max_zoom}",
        "--simplification=10",
        "--buffer=64",
        "--generate-ids",
        "--drop-densest-as-needed",
        "--limit-low-zooms=1000",
        "--force",
        str(input_path),
    ], capture_output=True, text=True, timeout=3600)

    # 清理临时文件
    if tmp_file and os.path.exists(tmp_file):
        os.remove(tmp_file)

    if proc.returncode != 0:
        fail(f"tippecanoe 失败: {proc.stderr[:500]}")

    if not output_path.exists():
        fail("MBTiles 生成失败")

    # 统计瓦片数
    size_mb = output_path.stat().st_size / (1024 * 1024)
    try:
        import sqlite3
        with sqlite3.connect(str(output_path)) as conn:
            tile_count = conn.execute("SELECT COUNT(*) FROM tiles").fetchone()[0]
    except Exception:
        tile_count = "?"

    success(f"矢量瓦片生成完成")
    print(f"\n==============================")
    print(f"  文件: {output_path} ({size_mb:.1f} MB)")
    print(f"  瓦片数: {tile_count}")
    print(f"  缩放级别: {args.min_zoom} - {args.max_zoom}")
    print(f"  前端: /v1/map/mvt/mbtiles/{{layerId}}/{{z}}/{{x}}/{{y}}.pbf")
    print(f"==============================")


if __name__ == "__main__":
    main()
