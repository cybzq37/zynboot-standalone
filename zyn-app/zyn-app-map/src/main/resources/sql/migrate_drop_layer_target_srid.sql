-- ============================================================
-- 迁移脚本：删除 map_layer.target_srid 列
-- ============================================================
-- 背景：
--   要素存储已统一为 geography(4326)，map_layer.target_srid 不再用于坐标转换。
--   该字段被移除，map_layer_source.target_srid 保留作为导入元数据记录。
-- ============================================================

-- 1. 删除列
ALTER TABLE map_layer DROP COLUMN IF EXISTS target_srid;

-- 2. 校验
SELECT 'target_srid_exists' AS check_name, COUNT(*) AS cnt
FROM information_schema.columns
WHERE table_name = 'map_layer' AND column_name = 'target_srid';

-- 期望：cnt = 0
