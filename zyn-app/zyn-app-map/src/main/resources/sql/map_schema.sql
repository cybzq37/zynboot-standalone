-- ============================================================
-- Zyn 地图模块 - PostgreSQL + PostGIS Schema
-- 优化版：第三范式、字段精简、索引合理、命名规范
-- ============================================================

CREATE EXTENSION IF NOT EXISTS postgis;

-- 清理旧表（按依赖顺序反向删除）
DROP TABLE IF EXISTS map_async_task CASCADE;
DROP TABLE IF EXISTS map_layer_version CASCADE;
DROP TABLE IF EXISTS map_layer_feature CASCADE;
DROP TABLE IF EXISTS map_instance_layer CASCADE;
DROP TABLE IF EXISTS map_publish CASCADE;
DROP TABLE IF EXISTS map_layer_style CASCADE;
DROP TABLE IF EXISTS map_layer_field CASCADE;
DROP TABLE IF EXISTS map_source_proxy CASCADE;
DROP TABLE IF EXISTS map_source_tile CASCADE;
DROP TABLE IF EXISTS map_source_raster CASCADE;
DROP TABLE IF EXISTS map_layer_source CASCADE;
DROP TABLE IF EXISTS map_layer CASCADE;
DROP TABLE IF EXISTS map_layer_group CASCADE;
DROP TABLE IF EXISTS map_basemap CASCADE;
DROP TABLE IF EXISTS map_data_source CASCADE;
DROP TABLE IF EXISTS map_instance CASCADE;

-- ============================================================
-- 1. 图层分组（树形，邻接表模型）
-- 数据规模：~100 行
-- ============================================================
CREATE TABLE map_layer_group (
    id              VARCHAR(64)  PRIMARY KEY,
    parent_id       VARCHAR(64),                     -- NULL = 根节点
    name            VARCHAR(128) NOT NULL,
    description     VARCHAR(512),
    sort_order      SMALLINT     DEFAULT 0,
    icon            VARCHAR(256),
    color           VARCHAR(32),
    create_by       VARCHAR(64),
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(64),
    update_time     TIMESTAMP
);

CREATE INDEX idx_group_parent ON map_layer_group(parent_id);

COMMENT ON TABLE map_layer_group IS '图层分组（树形）';
COMMENT ON COLUMN map_layer_group.parent_id IS '父节点 ID，NULL 表示根节点';

-- ============================================================
-- 2. 底图配置
-- 数据规模：~20 行
-- ============================================================
CREATE TABLE map_basemap (
    id                  VARCHAR(64)    PRIMARY KEY,
    name                VARCHAR(128)   NOT NULL,
    description         VARCHAR(512),
    type                VARCHAR(16)    NOT NULL,       -- XYZ / WMS / WMTS / TMS / VECTOR_TILE
    url                 VARCHAR(1024)  NOT NULL,       -- 瓦片地址模板
    srid                SMALLINT       NOT NULL DEFAULT 3857,
    attribution         VARCHAR(512),
    min_zoom            SMALLINT       NOT NULL DEFAULT 0,
    max_zoom            SMALLINT       NOT NULL DEFAULT 24,
    thumbnail_url       VARCHAR(512),
    is_default          BOOLEAN        NOT NULL DEFAULT FALSE,
    sort_order          SMALLINT       DEFAULT 0,
    -- WMS/WMTS 专有（非该类型时为 NULL）
    wms_layers          VARCHAR(512),
    wmts_layer          VARCHAR(256),
    wmts_style          VARCHAR(256),
    wmts_matrix_set     VARCHAR(256),
    create_by           VARCHAR(64),
    create_time         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by           VARCHAR(64),
    update_time         TIMESTAMP
);

CREATE INDEX idx_basemap_default ON map_basemap(is_default) WHERE is_default = TRUE;

COMMENT ON TABLE map_basemap IS '底图配置';
COMMENT ON COLUMN map_basemap.type IS '底图类型：XYZ/WMS/WMTS/TMS/VECTOR_TILE';
COMMENT ON COLUMN map_basemap.srid IS 'EPSG 代码数字部分，如 3857、4326、4490';
COMMENT ON COLUMN map_basemap.url IS '瓦片地址模板，{z}/{x}/{y} 占位符';

-- ============================================================
-- 3. 外部数据库连接配置（多 source 共享）
-- 数据规模：~20 行
-- ============================================================
CREATE TABLE map_data_source (
    id                  VARCHAR(64)    PRIMARY KEY,
    name                VARCHAR(128)   NOT NULL,
    type                VARCHAR(16)    NOT NULL DEFAULT 'POSTGIS',
    url                 VARCHAR(1024)  NOT NULL,        -- jdbc:postgresql://host:port/db
    username            VARCHAR(128),
    password            VARCHAR(512),                  -- AES-GCM 加密存储
    schema_name         VARCHAR(128)   NOT NULL DEFAULT 'public',
    driver_class        VARCHAR(256),
    test_query          VARCHAR(256),
    status              VARCHAR(16)    NOT NULL DEFAULT 'ACTIVE', -- ACTIVE / INACTIVE
    create_by           VARCHAR(64),
    create_time         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by           VARCHAR(64),
    update_time         TIMESTAMP,
    CONSTRAINT uk_data_source_name UNIQUE (name)
);

COMMENT ON TABLE map_data_source IS '外部数据库连接配置';
COMMENT ON COLUMN map_data_source.password IS 'AES-GCM 加密存储';

-- ============================================================
-- 4. 图层
-- 数据规模：~500 行
-- ============================================================
CREATE TABLE map_layer (
    id              VARCHAR(64)     PRIMARY KEY,
    group_id        VARCHAR(64),                        -- 关联 map_layer_group
    name            VARCHAR(128)    NOT NULL,
    title           VARCHAR(256),
    description     VARCHAR(512),
    type            VARCHAR(16)     NOT NULL,           -- RASTER / VECTOR
    target_srid     SMALLINT        NOT NULL DEFAULT 4326,
    geometry_type   VARCHAR(32),                        -- POINT/LINESTRING/POLYGON/MULTIPOINT 等
    extent          GEOMETRY(Polygon, 4326),            -- 图层范围（所有 source 并集）
    feature_count   INTEGER         NOT NULL DEFAULT 0,
    source_count    SMALLINT        NOT NULL DEFAULT 0,
    render_order    SMALLINT        NOT NULL DEFAULT 0,
    visible         BOOLEAN         NOT NULL DEFAULT TRUE,
    selectable      BOOLEAN         NOT NULL DEFAULT TRUE,
    editable        BOOLEAN         NOT NULL DEFAULT TRUE,
    is_public       BOOLEAN         NOT NULL DEFAULT FALSE,
    min_zoom        SMALLINT        NOT NULL DEFAULT 0,
    max_zoom        SMALLINT        NOT NULL DEFAULT 24,
    opacity         NUMERIC(3,2)    NOT NULL DEFAULT 1.00,
    style_id        VARCHAR(64),
    metadata        JSONB,
    create_by       VARCHAR(64),
    create_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(64),
    update_time     TIMESTAMP
);

CREATE INDEX idx_layer_group ON map_layer(group_id);
CREATE INDEX idx_layer_type ON map_layer(type);
CREATE INDEX idx_layer_render ON map_layer(group_id, render_order);

COMMENT ON TABLE map_layer IS '图层';
COMMENT ON COLUMN map_layer.type IS '图层类型：RASTER / VECTOR';
COMMENT ON COLUMN map_layer.target_srid IS '目标坐标系 EPSG 代码，所有导入数据统一转换到此';
COMMENT ON COLUMN map_layer.extent IS '图层空间范围（所有数据源并集），SRID = target_srid';
COMMENT ON COLUMN map_layer.opacity IS '默认透明度 0.00 ~ 1.00';

-- ============================================================
-- 5. 图层字段定义
-- 数据规模：~2000 行（每图层 ~5 字段 × 500 图层）
-- ============================================================
CREATE TABLE map_layer_field (
    id              VARCHAR(64)  PRIMARY KEY,
    layer_id        VARCHAR(64)  NOT NULL,
    name            VARCHAR(128) NOT NULL,             -- 字段名
    alias           VARCHAR(256),                      -- 显示别名
    type            VARCHAR(16)  NOT NULL,             -- STRING / INTEGER / DOUBLE / DATE / BOOLEAN
    visible         BOOLEAN      NOT NULL DEFAULT TRUE,
    sortable        BOOLEAN      NOT NULL DEFAULT FALSE,
    searchable      BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order      SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT uk_layer_field UNIQUE (layer_id, name)
);

COMMENT ON TABLE map_layer_field IS '图层字段定义（合并所有数据源的字段）';

-- ============================================================
-- 6. 图层样式
-- 数据规模：~1000 行（每图层 ~2 样式）
-- ============================================================
CREATE TABLE map_layer_style (
    id              VARCHAR(64)  PRIMARY KEY,
    layer_id        VARCHAR(64)  NOT NULL,
    name            VARCHAR(128) NOT NULL,
    type            VARCHAR(16)  NOT NULL,             -- SINGLE / CATEGORIZED / GRADUATED / RULE / RASTER
    style_json      JSONB        NOT NULL,
    is_default      BOOLEAN      NOT NULL DEFAULT FALSE,
    create_by       VARCHAR(64),
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(64),
    update_time     TIMESTAMP
);

CREATE INDEX idx_style_layer ON map_layer_style(layer_id);
CREATE INDEX idx_style_default ON map_layer_style(layer_id, is_default) WHERE is_default = TRUE;

COMMENT ON TABLE map_layer_style IS '图层样式';

-- ============================================================
-- 7. 数据源（一图层多数据源）
-- 数据规模：~2000 行
-- ============================================================
CREATE TABLE map_layer_source (
    id                  VARCHAR(64)   PRIMARY KEY,
    layer_id            VARCHAR(64)   NOT NULL,
    name                VARCHAR(256),
    type                VARCHAR(16)   NOT NULL,       -- FILE / POSTGIS / WMS / WFS / WMTS / TMS / XYZ
    format              VARCHAR(16),                  -- CSV / GEOJSON / SHP / GEOTIFF / XYZ
    source_srid         SMALLINT,                     -- 源数据 EPSG 代码
    target_srid         SMALLINT,                     -- 目标 EPSG 代码
    storage_key         VARCHAR(512),                 -- 文件存储路径（FILE 时）
    geometry_type       VARCHAR(32),
    feature_count       INTEGER       NOT NULL DEFAULT 0,
    extent              GEOMETRY(Polygon),            -- 本次导入空间范围
    field_mapping       JSONB,                        -- 字段映射关系
    status              VARCHAR(16)   NOT NULL DEFAULT 'PENDING', -- PENDING / PROCESSING / COMPLETED / FAILED
    message             JSONB,                        -- 导入校验结果
    -- PostGIS 直查字段（type=POSTGIS 时必填）
    data_source_id      VARCHAR(64),
    external_schema     VARCHAR(128),
    external_table      VARCHAR(256),
    external_geom_col   VARCHAR(128)  NOT NULL DEFAULT 'geom',
    external_id_col     VARCHAR(128)  NOT NULL DEFAULT 'gid',
    create_by           VARCHAR(64),
    create_time         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by           VARCHAR(64),
    update_time         TIMESTAMP
);

CREATE INDEX idx_source_layer ON map_layer_source(layer_id);
CREATE INDEX idx_source_status ON map_layer_source(status);
CREATE INDEX idx_source_data_source ON map_layer_source(data_source_id) WHERE data_source_id IS NOT NULL;

COMMENT ON TABLE map_layer_source IS '数据源（每次导入/接入一条记录）';
COMMENT ON COLUMN map_layer_source.type IS '数据源类型：FILE/POSTGIS/WMS/WFS/WMTS/TMS/XYZ';
COMMENT ON COLUMN map_layer_source.format IS '文件格式：CSV/GEOJSON/SHP/GEOTIFF/XYZ';
COMMENT ON COLUMN map_layer_source.status IS '导入状态：PENDING/PROCESSING/COMPLETED/FAILED';
COMMENT ON COLUMN map_layer_source.extent IS '本次导入的空间范围';

-- ============================================================
-- 8. 栅格专有字段（一对一 map_layer_source）
-- 数据规模：~500 行
-- ============================================================
CREATE TABLE map_source_raster (
    source_id           VARCHAR(64) PRIMARY KEY,
    path                VARCHAR(512) NOT NULL,        -- 栅格数据相对路径
    width               INTEGER,                      -- 像素宽度
    height              INTEGER,                      -- 像素高度
    bands               SMALLINT,                     -- 波段数
    data_type           VARCHAR(32),                  -- Byte / Int16 / Float32 等
    nodata              DOUBLE PRECISION,
    pixel_size_x        DOUBLE PRECISION,             -- X 方向像素分辨率（地理单位）
    pixel_size_y        DOUBLE PRECISION,
    compressed_bytes    BIGINT,
    uncompressed_bytes  BIGINT
);

COMMENT ON TABLE map_source_raster IS '栅格数据源元数据';

-- ============================================================
-- 9. 瓦片专有字段（一对一 map_layer_source）
-- 数据规模：~300 行
-- ============================================================
CREATE TABLE map_source_tile (
    source_id           VARCHAR(64) PRIMARY KEY,
    status              VARCHAR(16)  NOT NULL DEFAULT 'NONE', -- NONE/PENDING/TILING/COMPLETED/FAILED
    path                VARCHAR(512),                 -- 切片目录相对路径
    min_zoom            SMALLINT,
    max_zoom            SMALLINT,
    format              VARCHAR(16)  NOT NULL DEFAULT 'png',
    tile_size           SMALLINT     NOT NULL DEFAULT 256,
    tile_count          INTEGER      NOT NULL DEFAULT 0,
    progress            SMALLINT     NOT NULL DEFAULT 0  -- 0-100
);

COMMENT ON TABLE map_source_tile IS '瓦片切片元数据';

-- ============================================================
-- 10. 代理专有字段（一对一 map_layer_source）
-- 数据规模：~200 行
-- ============================================================
CREATE TABLE map_source_proxy (
    source_id               VARCHAR(64)  PRIMARY KEY,
    url                     VARCHAR(1024) NOT NULL,    -- 外部服务基地址
    wmts_layer              VARCHAR(256),
    wmts_style              VARCHAR(256),
    wmts_matrix_set     VARCHAR(256),
    wmts_format             VARCHAR(64),
    auth_type               VARCHAR(16)  NOT NULL DEFAULT 'NONE', -- NONE/BASIC/TOKEN/API_KEY
    auth_header             VARCHAR(128),
    auth_value              VARCHAR(512),              -- AES-GCM 加密
    cache_ttl               INTEGER      NOT NULL DEFAULT 86400,
    health_status           VARCHAR(16)  NOT NULL DEFAULT 'UNKNOWN', -- UNKNOWN/HEALTHY/DEGRADED/DOWN
    health_message          VARCHAR(512),
    last_check_at           TIMESTAMP,
    fail_count              SMALLINT     NOT NULL DEFAULT 0
);

COMMENT ON TABLE map_source_proxy IS '外部服务代理配置';

-- ============================================================
-- 11. 矢量要素（FILE 模式，BIGINT Snowflake ID + 哈希分区）
-- 数据规模：千万级（最大表）
-- id 为 BIGINT Snowflake（8 字节，大致有序，减少 B-tree 页分裂）
-- 按 layer_id 哈希分区，查询单图层只扫描 1/8 数据
-- ============================================================
CREATE TABLE map_layer_feature (
    id                BIGINT       NOT NULL,           -- Snowflake ID
    layer_id          VARCHAR(64)  NOT NULL,           -- 分区键
    source_id         VARCHAR(64),                     -- 数据源 ID；手动新增要素时为 NULL
    properties        JSONB,                           -- 属性键值对
    geometry          GEOMETRY     NOT NULL,           -- PostGIS 几何（统一为 target_srid）
    PRIMARY KEY (id, layer_id)
) PARTITION BY HASH (layer_id);

CREATE TABLE map_layer_feature_p0 PARTITION OF map_layer_feature FOR VALUES WITH (MODULUS 8, REMAINDER 0);
CREATE TABLE map_layer_feature_p1 PARTITION OF map_layer_feature FOR VALUES WITH (MODULUS 8, REMAINDER 1);
CREATE TABLE map_layer_feature_p2 PARTITION OF map_layer_feature FOR VALUES WITH (MODULUS 8, REMAINDER 2);
CREATE TABLE map_layer_feature_p3 PARTITION OF map_layer_feature FOR VALUES WITH (MODULUS 8, REMAINDER 3);
CREATE TABLE map_layer_feature_p4 PARTITION OF map_layer_feature FOR VALUES WITH (MODULUS 8, REMAINDER 4);
CREATE TABLE map_layer_feature_p5 PARTITION OF map_layer_feature FOR VALUES WITH (MODULUS 8, REMAINDER 5);
CREATE TABLE map_layer_feature_p6 PARTITION OF map_layer_feature FOR VALUES WITH (MODULUS 8, REMAINDER 6);
CREATE TABLE map_layer_feature_p7 PARTITION OF map_layer_feature FOR VALUES WITH (MODULUS 8, REMAINDER 7);

CREATE INDEX idx_layer_feature_layer ON map_layer_feature(layer_id);
CREATE INDEX idx_layer_feature_geom ON map_layer_feature USING GIST(geometry);
-- properties JSONB GIN 索引：支持按任意属性键值过滤（properties @> '{"code":"A001"}'）
CREATE INDEX idx_layer_feature_props_gin ON map_layer_feature USING GIN (properties jsonb_path_ops);

COMMENT ON TABLE map_layer_feature IS '图层矢量要素（千万级，哈希分区）';
COMMENT ON COLUMN map_layer_feature.id IS 'Snowflake ID（BIGINT，8 字节，大致有序）';
COMMENT ON COLUMN map_layer_feature.geometry IS 'PostGIS 几何，SRID = 所属图层 target_srid';

-- ============================================================
-- 12. 图层版本
-- 数据规模：~5000 行（100 图层 × 每月 2 次 × 2 年）
-- ============================================================
CREATE TABLE map_layer_version (
    id                  VARCHAR(64)  PRIMARY KEY,
    layer_id            VARCHAR(64)  NOT NULL,
    version             INTEGER      NOT NULL,
    name                VARCHAR(256),
    type                VARCHAR(16)  NOT NULL,        -- IMPORT / EDIT / MANUAL
    source_snapshot     JSONB,                        -- 导入前所有 source 快照
    feature_count       INTEGER      NOT NULL DEFAULT 0,
    extent              GEOMETRY(Polygon),
    source_count        SMALLINT     NOT NULL DEFAULT 0,
    created_by          VARCHAR(64),
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_version_layer_ver UNIQUE (layer_id, version)
);

CREATE INDEX idx_version_layer ON map_layer_version(layer_id, version DESC);

COMMENT ON TABLE map_layer_version IS '图层版本快照';

-- ============================================================
-- 13. 异步任务
-- 数据规模：~10000 行（定期清理已完成任务）
-- ============================================================
CREATE TABLE map_async_task (
    id                  VARCHAR(64)  PRIMARY KEY,
    type                VARCHAR(32)  NOT NULL,        -- TILE / RETILE / IMPORT_RASTER
    source_id           VARCHAR(64),
    layer_id            VARCHAR(64),
    status              VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    progress            SMALLINT     NOT NULL DEFAULT 0,
    total_count         INTEGER      NOT NULL DEFAULT 0,
    processed_count     INTEGER      NOT NULL DEFAULT 0,
    error_count         INTEGER      NOT NULL DEFAULT 0,
    error_message       TEXT,
    started_at          TIMESTAMP,
    finished_at         TIMESTAMP,
    created_by          VARCHAR(64),
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_task_status ON map_async_task(status);
CREATE INDEX idx_task_source ON map_async_task(source_id) WHERE source_id IS NOT NULL;
CREATE INDEX idx_task_layer ON map_async_task(layer_id) WHERE layer_id IS NOT NULL;

COMMENT ON TABLE map_async_task IS '异步任务管理';

-- ============================================================
-- 14. 地图实例
-- 数据规模：~100 行
-- ============================================================
CREATE TABLE map_instance (
    id              VARCHAR(64)     PRIMARY KEY,
    name            VARCHAR(128)    NOT NULL,
    description     VARCHAR(512),
    center_lng      DOUBLE PRECISION,
    center_lat      DOUBLE PRECISION,
    zoom            SMALLINT        NOT NULL DEFAULT 10,
    basemap_id      VARCHAR(64),
    max_extent      GEOMETRY(Polygon),
    is_public       BOOLEAN         NOT NULL DEFAULT FALSE,
    create_by       VARCHAR(64),
    create_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(64),
    update_time     TIMESTAMP
);

CREATE INDEX idx_instance_basemap ON map_instance(basemap_id) WHERE basemap_id IS NOT NULL;

COMMENT ON TABLE map_instance IS '地图实例';

-- ============================================================
-- 15. 地图发布
-- 数据规模：~50 行
-- ============================================================
CREATE TABLE map_publish (
    id              VARCHAR(64) PRIMARY KEY,
    instance_id     VARCHAR(64) NOT NULL,
    type            VARCHAR(16) NOT NULL DEFAULT 'PUBLIC',
    is_active       BOOLEAN     NOT NULL DEFAULT TRUE,
    create_by       VARCHAR(64),
    create_time     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(64),
    update_time     TIMESTAMP
);

CREATE INDEX idx_publish_instance ON map_publish(instance_id);

COMMENT ON TABLE map_publish IS '地图发布记录';

-- ============================================================
-- 16. 实例-图层关联（支持树形分组）
-- 数据规模：~2000 行
-- ============================================================
CREATE TABLE map_instance_layer (
    id              VARCHAR(64)  PRIMARY KEY,
    instance_id     VARCHAR(64)  NOT NULL,
    parent_id       VARCHAR(64),                      -- NULL = 顶层
    is_group        BOOLEAN      NOT NULL DEFAULT FALSE,
    layer_id        VARCHAR(64),                      -- is_group=FALSE 时必填
    name            VARCHAR(256),
    visible         BOOLEAN      NOT NULL DEFAULT TRUE,
    opacity         NUMERIC(3,2),
    render_order    SMALLINT     NOT NULL DEFAULT 0
);

-- 叶子节点：同一实例下 layer_id 唯一
CREATE UNIQUE INDEX uk_inst_layer_leaf ON map_instance_layer(instance_id, layer_id) WHERE layer_id IS NOT NULL;
CREATE INDEX idx_inst_layer_instance ON map_instance_layer(instance_id);
CREATE INDEX idx_inst_layer_parent ON map_instance_layer(instance_id, parent_id) WHERE parent_id IS NOT NULL;

COMMENT ON TABLE map_instance_layer IS '实例-图层关联（支持树形分组）';
COMMENT ON COLUMN map_instance_layer.parent_id IS '父节点 ID，NULL 表示顶层';
COMMENT ON COLUMN map_instance_layer.is_group IS 'TRUE=分组节点（layer_id 为 NULL），FALSE=图层叶子';

-- ============================================================
-- 17. 操作审计日志
-- 数据规模：~50000 行（定期清理）
-- ============================================================
CREATE TABLE map_operation_log (
    id              VARCHAR(64)  PRIMARY KEY,
    target_type     VARCHAR(16)  NOT NULL,        -- LAYER/SOURCE/FEATURE/GROUP/STYLE/BASEMAP
    target_id       VARCHAR(64)  NOT NULL,
    action          VARCHAR(16)  NOT NULL,        -- CREATE/UPDATE/DELETE/IMPORT/EXPORT/RETILE
    operator_id     VARCHAR(64),
    operator_name   VARCHAR(128),
    detail_json     JSONB,
    ip              VARCHAR(45),
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_target ON map_operation_log(target_type, target_id);
CREATE INDEX idx_audit_operator ON map_operation_log(operator_id);
CREATE INDEX idx_audit_time ON map_operation_log(create_time);

COMMENT ON TABLE map_operation_log IS '操作审计日志';
