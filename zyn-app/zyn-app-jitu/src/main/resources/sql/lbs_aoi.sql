CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE IF NOT EXISTS lbs_aoi (
    id            VARCHAR(64) PRIMARY KEY,
    aoi_type      VARCHAR(32)  NOT NULL,
    aoi_name      VARCHAR(256),
    address       VARCHAR(512),
    gb_code       VARCHAR(64),
    kind          VARCHAR(64),
    longitude     DOUBLE PRECISION,
    latitude      DOUBLE PRECISION,
    geometry      GEOMETRY(Polygon, 4326) NOT NULL,
    center_point  GEOMETRY(Point, 4326),
    properties    JSONB,
    geojson       JSONB,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    version       INTEGER      NOT NULL DEFAULT 1,
    create_time   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP,
    CONSTRAINT chk_lbs_aoi_type CHECK (aoi_type IN ('BP', 'BUILDING'))
);

-- 通用过滤索引：适合 enabled + aoi_type + 主键/分页类查询
CREATE INDEX IF NOT EXISTS idx_lbs_aoi_type_enabled_id
    ON lbs_aoi(aoi_type, enabled, id);

-- 行政区过滤索引
CREATE INDEX IF NOT EXISTS idx_lbs_aoi_gb_code_enabled
    ON lbs_aoi(gb_code, enabled);

-- 通用空间索引：适合未区分类型的 bbox / 点查面
CREATE INDEX IF NOT EXISTS idx_lbs_aoi_geom_enabled
    ON lbs_aoi USING GIST(geometry)
    WHERE enabled = TRUE;

-- 按类型拆开的空间索引：适合 AOI_BP / AOI_BUILDING 图层独立查询
CREATE INDEX IF NOT EXISTS idx_lbs_aoi_bp_geom_enabled
    ON lbs_aoi USING GIST(geometry)
    WHERE enabled = TRUE AND aoi_type = 'BP';

CREATE INDEX IF NOT EXISTS idx_lbs_aoi_building_geom_enabled
    ON lbs_aoi USING GIST(geometry)
    WHERE enabled = TRUE AND aoi_type = 'BUILDING';

-- 中心点索引：适合后续按中心点做轻量范围/近邻查询
CREATE INDEX IF NOT EXISTS idx_lbs_aoi_center_point_enabled
    ON lbs_aoi USING GIST(center_point)
    WHERE enabled = TRUE AND center_point IS NOT NULL;

COMMENT ON TABLE lbs_aoi IS '极兔 AOI 数据';
COMMENT ON COLUMN lbs_aoi.id IS 'AOI 主键，优先直接使用 linknid';
COMMENT ON COLUMN lbs_aoi.aoi_type IS 'AOI 类型，例如 BP / BUILDING';
COMMENT ON COLUMN lbs_aoi.geometry IS 'AOI Polygon，SRID = 4326';
COMMENT ON COLUMN lbs_aoi.center_point IS '预计算中心点，SRID = 4326';
COMMENT ON COLUMN lbs_aoi.properties IS '原始属性 JSONB';
COMMENT ON COLUMN lbs_aoi.geojson IS '预生成 GeoJSON';