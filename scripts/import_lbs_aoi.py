#!/usr/bin/env python3
"""
将 AOI JSON 数据导入 PostgreSQL / PostGIS 的 lbs_aoi 表。

说明：
1. 所有数据库连接参数、表名、源文件路径都在脚本内配置。
2. 不通过命令行参数传入。
3. 采用流式解析 features 数组，避免一次性加载超大 JSON 文件。

依赖：
    pip install psycopg2-binary
"""

from __future__ import annotations

import json
import re
import sys
import time
from datetime import datetime
from pathlib import Path
from typing import Iterator

try:
    import psycopg2
    from psycopg2.extras import execute_values
except ImportError as exc:  # pragma: no cover - import guard
    raise SystemExit("缺少依赖 psycopg2-binary，请先执行: pip install psycopg2-binary") from exc


# ==================== 固定配置区 ====================

# PostgreSQL / PostGIS 连接配置
DB_CONFIG = {
    "host": "10.130.20.24",
    "port": 5432,
    "dbname": "zyn_base",
    "user": "postgres",
    "password": "Superman@2021",
}

# 目标表配置
DB_SCHEMA = "public"
TABLE_NAME = "lbs_aoi"

# 源文件配置
AOI_BP_FILE = Path(r"D:\GitRepo\zynboot-standalone\data\AOI_BP.json")
AOI_BUILDING_FILE = Path(r"D:\GitRepo\zynboot-standalone\data\AOI_building.json")

SOURCE_FILES = [
    {
        "path": AOI_BP_FILE,
        "aoi_type": "BP",
    },
    # {
    #     "path": AOI_BUILDING_FILE,
    #     "aoi_type": "BUILDING",
    # },
]

# 导入行为配置
TRUNCATE_BEFORE_LOAD = False
UPSERT_ON_CONFLICT = True
BATCH_SIZE = 500
CHUNK_SIZE = 1024 * 1024
PROGRESS_EVERY = 5000


# ==================== 通用工具 ====================

def log(message: str) -> None:
    print(f"[{datetime.now():%H:%M:%S}] {message}")


def ensure_identifier(value: str, label: str) -> str:
    if not re.fullmatch(r"[A-Za-z_][A-Za-z0-9_]*", value):
        raise ValueError(f"{label} 非法: {value}")
    return value


def parse_float(value: object) -> float | None:
    if value is None:
        return None
    text = str(value).strip()
    if not text:
        return None
    try:
        return float(text)
    except ValueError:
        return None


def compact_json(value: object) -> str:
    return json.dumps(value, ensure_ascii=False, separators=(",", ":"))


# ==================== JSON 流式解析 ====================

def iter_features(json_file: Path) -> Iterator[dict]:
    decoder = json.JSONDecoder()
    buffer = ""
    position = 0
    features_started = False

    with json_file.open("r", encoding="utf-8") as handle:
        while True:
            if position > CHUNK_SIZE:
                buffer = buffer[position:]
                position = 0

            if not features_started:
                idx = buffer.find('"features"', position)
                if idx == -1:
                    chunk = handle.read(CHUNK_SIZE)
                    if not chunk:
                        raise RuntimeError(f"未在文件中找到 features 数组: {json_file}")
                    buffer += chunk
                    continue

                position = idx + len('"features"')
                while True:
                    if position >= len(buffer):
                        chunk = handle.read(CHUNK_SIZE)
                        if not chunk:
                            raise RuntimeError(f"features 数组起始不完整: {json_file}")
                        buffer += chunk
                        continue
                    current = buffer[position]
                    if current in " \r\n\t:":
                        position += 1
                        continue
                    if current == "[":
                        position += 1
                        features_started = True
                        break
                    raise RuntimeError(f"非法 JSON 结构，features 后未找到 [: {json_file}")

            while True:
                if position >= len(buffer):
                    chunk = handle.read(CHUNK_SIZE)
                    if not chunk:
                        raise RuntimeError(f"features 数组未正常结束: {json_file}")
                    buffer += chunk
                    continue

                current = buffer[position]
                if current in " \r\n\t,":
                    position += 1
                    continue
                if current == "]":
                    return
                break

            while True:
                try:
                    feature, next_position = decoder.raw_decode(buffer, position)
                    position = next_position
                    yield feature
                    break
                except json.JSONDecodeError:
                    chunk = handle.read(CHUNK_SIZE)
                    if not chunk:
                        raise RuntimeError(f"JSON 解析失败，文件可能截断: {json_file}")
                    buffer += chunk


# ==================== 数据库写入 ====================

def build_insert_sql(table_name: str) -> str:
    if UPSERT_ON_CONFLICT:
        return f"""
            INSERT INTO {table_name} (
                id,
                aoi_type,
                aoi_name,
                address,
                gb_code,
                kind,
                longitude,
                latitude,
                geometry,
                center_point,
                properties,
                geojson
            ) VALUES %s
            ON CONFLICT (id) DO UPDATE SET
                aoi_type = EXCLUDED.aoi_type,
                aoi_name = EXCLUDED.aoi_name,
                address = EXCLUDED.address,
                gb_code = EXCLUDED.gb_code,
                kind = EXCLUDED.kind,
                longitude = EXCLUDED.longitude,
                latitude = EXCLUDED.latitude,
                geometry = EXCLUDED.geometry,
                center_point = EXCLUDED.center_point,
                properties = EXCLUDED.properties,
                geojson = EXCLUDED.geojson
        """

    return f"""
        INSERT INTO {table_name} (
            id,
            aoi_type,
            aoi_name,
            address,
            gb_code,
            kind,
            longitude,
            latitude,
            geometry,
            center_point,
            properties,
            geojson
        ) VALUES %s
    """


def insert_batch(cursor, table_name: str, rows: list[tuple]) -> None:
    sql = build_insert_sql(table_name)
    template = """
        (
            %s, %s, %s, %s, %s, %s, %s, %s,
            ST_SetSRID(ST_GeomFromGeoJSON(%s), 4326),
            CASE
                WHEN %s IS NOT NULL AND %s IS NOT NULL
                    THEN ST_SetSRID(ST_Point(%s, %s), 4326)
                ELSE ST_PointOnSurface(ST_SetSRID(ST_GeomFromGeoJSON(%s), 4326))
            END,
            %s::jsonb,
            %s::jsonb
        )
    """
    execute_values(cursor, sql, rows, template=template, page_size=len(rows))


def fallback_insert_one_by_one(cursor, table_name: str, rows: list[tuple]) -> tuple[int, int]:
    inserted = 0
    skipped = 0
    for row in rows:
        try:
            insert_batch(cursor, table_name, [row])
            inserted += 1
        except Exception as exc:  # pragma: no cover - runtime safety
            skipped += 1
            log(f"跳过坏数据 id={row[0]}，原因: {exc}")
    return inserted, skipped


# ==================== 业务映射 ====================

def map_feature_to_row(feature: dict, aoi_type: str) -> tuple | None:
    geometry = feature.get("geometry")
    properties = feature.get("properties") or {}

    if not geometry or geometry.get("type") != "Polygon":
        return None

    aoi_id = str(properties.get("linknid", "")).strip()
    if not aoi_id:
        return None

    longitude = parse_float(properties.get("longitude"))
    latitude = parse_float(properties.get("latitude"))
    geometry_json = compact_json(geometry)
    properties_json = compact_json(properties)

    return (
        aoi_id,
        aoi_type,
        properties.get("aoiname"),
        properties.get("address"),
        properties.get("gbcode"),
        properties.get("kind"),
        longitude,
        latitude,
        geometry_json,
        longitude,
        latitude,
        longitude,
        latitude,
        geometry_json,
        properties_json,
        geometry_json,
    )


# ==================== 主流程 ====================

def import_file(connection, source_file: Path, aoi_type: str, table_name: str) -> tuple[int, int, int, int]:
    inserted = 0
    skipped = 0
    duplicates = 0
    seen = 0
    seen_ids: set[str] = set()
    batch: list[tuple] = []

    log(f"开始导入 {source_file.name} -> {table_name}，类型={aoi_type}")
    start = time.time()

    with connection.cursor() as cursor:
        for feature in iter_features(source_file):
            seen += 1
            row = map_feature_to_row(feature, aoi_type)
            if row is None:
                skipped += 1
                continue

            aoi_id = row[0]
            if aoi_id in seen_ids:
                duplicates += 1
                continue
            seen_ids.add(aoi_id)

            batch.append(row)
            if len(batch) >= BATCH_SIZE:
                try:
                    insert_batch(cursor, table_name, batch)
                    connection.commit()
                    inserted += len(batch)
                except Exception as exc:  # pragma: no cover - runtime safety
                    connection.rollback()
                    log(f"批量写入失败，回退逐条重试，原因: {exc}")
                    ok_count, skip_count = fallback_insert_one_by_one(cursor, table_name, batch)
                    connection.commit()
                    inserted += ok_count
                    skipped += skip_count
                batch.clear()

            if seen % PROGRESS_EVERY == 0:
                elapsed = time.time() - start
                log(f"{source_file.name}: 已扫描 {seen} 条，已写入 {inserted} 条，已跳过 {skipped} 条，重复 {duplicates} 条，用时 {elapsed:.1f}s")

        if batch:
            try:
                insert_batch(cursor, table_name, batch)
                connection.commit()
                inserted += len(batch)
            except Exception as exc:  # pragma: no cover - runtime safety
                connection.rollback()
                log(f"尾批写入失败，回退逐条重试，原因: {exc}")
                ok_count, skip_count = fallback_insert_one_by_one(cursor, table_name, batch)
                connection.commit()
                inserted += ok_count
                skipped += skip_count

    elapsed = time.time() - start
    log(f"完成 {source_file.name}：扫描 {seen} 条，写入 {inserted} 条，跳过 {skipped} 条，重复 {duplicates} 条，用时 {elapsed:.1f}s")
    return seen, inserted, skipped, duplicates


def main() -> None:
    schema_name = ensure_identifier(DB_SCHEMA, "schema")
    table_name = ensure_identifier(TABLE_NAME, "table")
    full_table_name = f"{schema_name}.{table_name}"

    for source in SOURCE_FILES:
        if not Path(source["path"]).exists():
            raise FileNotFoundError(f"源文件不存在: {source['path']}")

    connection = psycopg2.connect(**DB_CONFIG)
    connection.autocommit = False

    try:
        if TRUNCATE_BEFORE_LOAD:
            with connection.cursor() as cursor:
                log(f"清空表 {full_table_name}")
                cursor.execute(f"TRUNCATE TABLE {full_table_name}")
                connection.commit()

        total_seen = 0
        total_inserted = 0
        total_skipped = 0
        total_duplicates = 0

        for source in SOURCE_FILES:
            seen, inserted, skipped, duplicates = import_file(
                connection=connection,
                source_file=Path(source["path"]),
                aoi_type=source["aoi_type"],
                table_name=full_table_name,
            )
            total_seen += seen
            total_inserted += inserted
            total_skipped += skipped
            total_duplicates += duplicates

        log(f"全部完成：扫描 {total_seen} 条，写入 {total_inserted} 条，跳过 {total_skipped} 条，重复 {total_duplicates} 条")
    finally:
        connection.close()


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n中断执行")
        sys.exit(130)
