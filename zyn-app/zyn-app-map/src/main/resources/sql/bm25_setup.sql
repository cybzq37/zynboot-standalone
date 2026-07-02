-- ============================================================
-- BM25 全文搜索扩展（ParadeDB pg_search）
-- 在 zyn_base 数据库中执行
-- ============================================================

-- 启用 pg_search 扩展（ParadeDB 镜像已内置）
CREATE EXTENSION IF NOT EXISTS pg_search;

-- ============================================================
-- 为 map_feature 表创建 BM25 索引
-- properties_json 中的文本字段支持全文搜索
-- ============================================================

-- 方式 1: 使用 ParadeDB 的 bm25 索引（推荐）
-- 适用于 properties_json 中存储的文本字段
CREATE INDEX IF NOT EXISTS idx_feature_bm25
    ON map_feature
    USING bm25 (id, properties)
    WITH (
        key_field = 'id',
        text_fields = '{
            "properties": {
                "tokenizer": {"type": "chinese"},
                "record": "position"
            }
        }'
    );

-- ============================================================
-- 使用示例
-- ============================================================

-- BM25 搜索：在 properties_json 中搜索关键词
-- SELECT id, properties, paradedb.score(id) AS relevance
-- FROM map_feature
-- WHERE properties @@@ '关键词'
-- ORDER BY relevance DESC
-- LIMIT 20;

-- BM25 + 空间联合查询：在指定区域内全文搜索
-- SELECT id, properties, paradedb.score(id) AS relevance,
--        ST_AsGeoJSON(geometry) AS geometry
-- FROM map_feature
-- WHERE layer_id = 'xxx'
--   AND properties @@@ '关键词'
--   AND geometry && ST_MakeEnvelope(116.0, 39.0, 117.0, 40.0, 4326)
-- ORDER BY relevance DESC
-- LIMIT 100;
