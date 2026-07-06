# 极兔 Geo 对外接口需求与实施计划

## 1. 上游地址解析示例

当前拿到的地图服务示例接口如下：

请求接口：`/service/lbs/search/v1/geo?address=北京市海淀区永旭南路四维图新大厦`

响应示例：

```json
{
  "count": 1,
  "geocodes": [
    {
      "adcode": "110108",
      "city": "北京市",
      "confidence": 1,
      "country": "中国",
      "district": "海淀区",
      "fid": "478903246",
      "formatted_address": "北京市海淀区四维图新大厦",
      "level": "大厦/写字楼",
      "location": "116.245583,40.072503",
      "number": "",
      "province": "北京市",
      "similarity_score": 1,
      "street": ""
    }
  ],
  "message": "success",
  "requestId": "3124835abd574281a8bce8c85b6cf560",
  "status": "0",
  "totalCostTime": "12ms"
}
```

说明：该接口更像“地址解析/地理编码”的上游能力示例，不应直接作为最终对第三方开放的业务接口协议。

## 2. 原始业务需求

新增一个对外部用户提供的接口。

请求参数：

- `type`：`1` 为取件，`2` 为收件
- `address`：结构化取件/收件地址

响应参数：

- `lnglat`：对应四段码的中心经纬度坐标
- `code`：绑定的四段码
- `address`：解析后的标准化地址
- `aoi`：AOI 数据几何
- `fenceInfo`：JSON 格式，包含图层要素 ID、GeoJSON、绑定的 AOI

处理流程：

1. 根据 `address` 调用地图服务，获取经纬度坐标和标准化地址。
2. 根据经纬度坐标，从极兔图层中获取命中的面数据，并且按 `type` 区分图层。
3. 根据命中的区域面数据计算中心点，拼装结果返回。

## 3. 当前阶段范围

当前阶段只考虑功能实现，不纳入以下内容：

- 第三方鉴权
- 限流与配额
- 调用审计
- SLA / 可用性承诺

当前目标收敛为：

- 能接收 `type` 和 `address`
- 能调用上游地图服务完成地址解析
- 能根据解析点位命中极兔业务围栏
- 能返回 `lnglat`、`code`、`address`、`aoi`、`fenceInfo`
- 设计上保持独立模块，便于后续再补鉴权和治理能力

## 4. 需求理解确认

基于当前说明，我对需求的理解如下：

1. 这是一个面向第三方用户的开放接口，但当前阶段只先落功能链路，不处理开放平台治理问题。
2. 该接口的本质是“地址解析 + 点查面 + 结果组装”的同步查询接口，核心链路不是通用 GIS 管理，而是高频只读查询。
3. `type` 实际上决定查询哪一套业务围栏，至少存在“取件围栏”和“收件围栏”两类数据视图。
4. 返回的 `lnglat` 应对应命中面数据的业务中心点，而不是上游地理编码返回的原始点位。
5. `aoi` 和 `fenceInfo` 都带几何信息，意味着响应体可能偏大，必须提前考虑 GeoJSON 预生成和裁剪策略。
6. 百万级数据规模和高并发要求下，真正的瓶颈不仅是空间查询，还包括第一跳外部地图服务调用，因此必须设计缓存和降级。

## 5. 方案建议

### 5.1 接口形态

虽然最终是对第三方开放，但当前阶段先只定义业务接口本身，不引入签名等开放平台协议。

建议最终业务接口不要继续沿用上游 GET 示例，而是定义独立业务接口：

- 建议路径：`POST /openapi/lbs/v1/geo/resolve`
- 建议请求体：

```json
{
  "type": 1,
  "address": "北京市海淀区永旭南路四维图新大厦"
}
```

原因：

- 便于后续扩展字段，例如城市、渠道、租户、幂等号、是否返回完整几何等。
- 即使当前不做鉴权，POST 结构也更适合后续扩展。

### 5.2 模块拆分

建议按独立模块建设，而不是直接塞进现有 `zyn-app-map` 的地图管理接口中。

推荐拆分方式：

- `zyn-app-jitu`
  - 单独部署的 Spring Boot 应用，包名建议 `com.zynboot.jitu`。
  - 只承载第三方 LBS 查询能力，不承载地图后台管理能力。
- 依赖复用现有基础设施：
  - `zyn-infra-web`：统一响应、异常处理
  - `zyn-infra-exchange`：对接外部地图服务
  - `zyn-infra-redis`：缓存
  - `zyn-infra-mybatis`：数据访问
  - `zyn-infra-geo`：几何处理工具

这样设计的原因：

- 对外接口边界更清晰，便于单独扩容和发布。
- 避免第三方访问流量影响地图后台。
- 后续如果不止极兔一个合作方，可以继续在该模块内扩展租户/合作方维度。

同时明确一条边界：

- 不在 `zyn-app-map` 中为 AOI 新增专属 Controller / Service / 查询接口
- AOI 进入 `map` 的方式是“注册为通用 PostGIS 数据源”，复用现有数据源、图层、要素查询能力
- `zyn-app-map` 继续保持通用 GIS 能力，不承载极兔专属 AOI 协议

### 5.3 查询链路

1. 根据 `address` 调上游地图服务获取经纬度和标准化地址。
2. 根据 `type` 在 `map_feature` 中做点查面，命中极兔围栏。
3. 从命中的 `map_feature.properties.aoi_id` 读取绑定的 AOI 标识。
4. 按 `aoi_id` 到 AOI 独立表 `lbs_aoi` 查询 AOI 几何和属性。
5. 拼装返回：
   - `lnglat`：围栏中心点
   - `code`：四段码
   - `address`：标准化地址
   - `aoi`：AOI 几何
   - `fenceInfo`：极兔围栏信息

说明：

- 第三方 LBS 查询服务可以直接查询 `map_feature` 和 `lbs_aoi`。
- 页面侧如果要浏览、点选、绑定 AOI，不走 AOI 专属接口，而是把 `lbs_aoi` 注册到 `map` 后，直接复用现有通用接口。
- 当前仓库中已有的数据源/挂源能力可以直接复用，例如：
  - `POST /map/datasource`
  - `POST /map/import/postgis`
  - `GET /map/layer/{layerId}/source`
  - `GET /map/layer/{layerId}/feature`
  - `GET /map/layer/{layerId}/feature/geojson`

空间查询建议使用：

```sql
WHERE biz_type = :type
  AND enabled = true
  AND geometry && ST_SetSRID(ST_Point(:lng, :lat), 4326)
  AND ST_Covers(geometry, ST_SetSRID(ST_Point(:lng, :lat), 4326))
LIMIT 1
```

说明：

- `geometry && point` 先走包围盒粗过滤。
- `ST_Covers` 比 `ST_Contains` 更适合边界点，避免点刚好落边界时查不到。
- `lnglat` 建议返回 `ST_PointOnSurface(geometry)` 的结果，避免凹多边形 `ST_Centroid` 落在面外。

### 5.4 数据模型建议

当前已明确有两部分数据：

- AOI 数据：约 `200` 万，独立导入
- 极兔图层数据：存放在 `map_feature`

基于这个前提，AOI 应单独建表，不建议把 AOI 也混放进 `map_feature`，也不建议为了 AOI 在 `zyn-app-map` 单独再造一套专属接口。

原因：

- AOI 是基础空间数据，导入量大、更新节奏和极兔图层不同。
- 当前查询链路是“先查围栏，再按关系查 AOI”，两类数据职责不同。
- 如果 AOI 也塞进 `map_feature`，后续导入、发布、查询、维护都会耦合在一起。

推荐采用“三层模型”：

1. `lbs_aoi`
   - 存 AOI 原始和发布态数据
2. `map_feature`
   - 存极兔图层要素
3. `lbs_geo_fence`
   - 可选的对外发布态聚合表，后续如果要进一步提速再建设

并且当前已经确认：

- `lbs_aoi` 不只是 LBS 查询使用，也要注册到 `map`
- 注册方式按现有 `map` 模块的 PostGIS 数据源模式处理
- 页面绑定时通过 `map` 图层展示 AOI，完成点选/查看后，再把绑定结果写回 `map_feature.properties.aoi_id`

#### AOI 独立表建议

建议新增表：`lbs_aoi`

DDL 存放建议：

- 单独放在独立 SQL 文件中，例如：`zyn-app/zyn-app-jitu/src/main/resources/sql/lbs_aoi.sql`
- 不并入 `zyn-app/zyn-app-map/src/main/resources/sql/map_schema.sql`

这样处理的原因：

- `map_schema.sql` 应继续只维护地图通用模型
- `lbs_aoi` 属于极兔任务数据模型，独立 SQL 更容易维护和演进
- 后续如果 `zyn-app-jitu` 独立部署或迁移，DDL 可以跟随业务模块走

建议字段：

- `id`：AOI 主键，建议直接使用源数据中的 `linknid`
- `aoi_type`：`BP` / `BUILDING`
- `aoi_name`：对应 `aoiname`
- `address`
- `gb_code`：对应 `gbcode`
- `kind`
- `longitude`
- `latitude`
- `geometry`：AOI Polygon，SRID = `4326`
- `center_point`：可由源数据经纬度或几何中心预生成
- `properties`：保留原始 `properties` JSONB
- `geojson`：预生成 AOI GeoJSON
- `enabled`
- `version`
- `create_time` / `update_time`

索引建议：

- `PRIMARY KEY (id)`
- `BTREE(aoi_type, enabled)`
- `BTREE(gb_code)`
- `GIST(geometry)`

说明：

- `AOI_BP` 和 `AOI_building` 结构基本一致，建议先共用一张表，用 `aoi_type` 区分。
- 如果后续发现两类数据生命周期完全不同，再考虑拆成两张物理表。
- 数据落库后，需要把 `lbs_aoi` 通过现有 PostGIS 数据源注册方式接入 `map`，作为通用图层供页面绑定使用。
- 页面上建议至少拆成两个图层视图：
  - `AOI_BP`
  - `AOI_BUILDING`

#### `map_feature` 与 AOI 的关系

当前约束是：`map_feature` 与 AOI 的绑定关系放在 `map_feature.properties.aoi_id` 中。

这个方案功能上可行，当前阶段可以直接采用，原因是：

- 对外查询是先通过空间条件命中单条或少量 `map_feature`
- 然后从命中记录的 `properties` 中取 `aoi_id`
- 最后按主键查询 `lbs_aoi`

也就是说，`aoi_id` 不参与大范围关联扫描时，放在 `properties` 里不会成为当前阶段的主要瓶颈。

但这里有两个实现建议：

1. `aoi_id` 要和 AOI 表主键保持同一口径
   - 既然 AOI 原始数据天然有 `linknid`，建议直接把 `linknid` 作为 `lbs_aoi.id`
   - 那么 `map_feature.properties.aoi_id` 也存这个值，避免再做一层 ID 映射

2. 给 `properties.aoi_id` 建表达式索引
   - 方便后续排查、反查和批量修复
   - PostgreSQL 可考虑：

```sql
CREATE INDEX idx_map_feature_aoi_id
ON map_feature ((properties ->> 'aoi_id'));
```

#### 页面绑定方式建议

既然已经确定 AOI 要注册到 `map`，页面绑定建议直接复用现有地图能力，而不是单独做一套 AOI 查询 UI，也不要在 `zyn-app-map` 中增加 AOI 专属接口。

建议流程：

1. 在 `map` 中注册 AOI 数据源
2. 创建 AOI 图层并在页面展示
3. 用户在页面点选 AOI 面要素
4. 前端拿到 AOI 要素主键和属性
5. 调用绑定接口，把 `aoi_id` 回写到目标 `map_feature.properties`

这样做的好处：

- 复用现有地图渲染、空间浏览、图层管理能力
- 绑定动作对业务人员更直观
- AOI 后续如果还要做查询、校验、可视化，也不需要再额外接一套前端数据能力
- `zyn-app-map` 保持通用能力，不被极兔业务细节侵入

#### 发布态聚合表建议

如果后续发现接口 QPS 很高，或者不希望请求时再查两张表，可以增加发布态聚合表：`lbs_geo_fence`

该表不是当前功能必需，但适合作为第二阶段性能优化方案。

关键字段建议：

- `id`
- `partner_code`：合作方编码，当前可先固定为 `JITU`
- `biz_type`：`1` 取件，`2` 收件
- `code`：四段码
- `std_address`：标准地址
- `fence_id`：围栏要素 ID
- `aoi_id`
- `geometry`：围栏面 geometry
- `center_point`：预计算中心点
- `aoi_geojson`：预生成 AOI GeoJSON
- `fence_geojson`：预生成围栏 GeoJSON
- `fence_info`：业务补充信息 JSONB
- `enabled`
- `version`
- `create_time` / `update_time`

索引建议：

- `BTREE(partner_code, biz_type, enabled)`
- `BTREE(code)`
- `GIST(geometry)`

设计重点：

- 当前阶段先允许“`map_feature` + `lbs_aoi`”两段查询。
- 如果后续性能不够，再把 AOI 和围栏信息预聚合到 `lbs_geo_fence`。
- `center_point` 不要在查询时实时计算，入库或发布时预计算。
- `aoi_geojson`、`fence_geojson` 不要在查询时实时 `ST_AsGeoJSON`，避免高并发下 CPU 消耗过高。
- 如果后续增长到千万级，可再升级为按 `biz_type` 分区或结合发布版本做分表；当前百万级在只读表 + GIST + 缓存条件下可先满足。

### 5.5 功能阶段的性能策略

为满足“100 万数据规模 + 高并发”，建议默认采用以下策略：

1. 读写分离
   - 当前阶段至少保证 AOI 独立入库，不和后台编辑逻辑混写。
   - 如果后续上发布态表，则第三方查询只读发布态表。

2. 双层缓存
   - 地址解析缓存：`normalizedAddress -> geocode`
   - 最终结果缓存：`type + normalizedAddress -> response`

3. 预计算
   - 预计算中心点
   - 预生成 AOI 和围栏 GeoJSON
   - 预绑定 `code`、`aoi`、`fenceInfo`

4. 降级能力
   - 外部地图服务超时或失败时，优先返回缓存结果。
   - 对上游地理编码服务设置超时、重试上限和熔断，避免级联放大。

5. 水平扩展
   - `zyn-app-jitu` 作为独立应用部署，可按实例横向扩容。
   - 接口应设计成无状态，状态落 Redis。

### 5.6 与现有仓库能力的关系

仓库现状可直接复用的能力：

- `zyn-infra-geo`：几何格式转换、坐标系处理
- `zyn-app-map`：PostGIS 空间查询思路、GeoJSON 输出、地图数据管理
- `map_feature`：已具备大表和空间索引设计经验

建议的边界划分：

- `zyn-app-map`：继续承担地图数据导入、图层管理、发布管理
- `zyn-app-jitu`：对第三方暴露稳定查询 API
- AOI 数据单独导入到 `lbs_aoi`
- `lbs_aoi` 作为 PostGIS 数据源注册到 `map`
- 极兔围栏继续保留在 `map_feature`
- 查询时通过 `map_feature.properties.aoi_id` 关联到 `lbs_aoi`
- 如果后续需要进一步提速，再把“对外可查围栏”同步到 `lbs_geo_fence` 发布表

这样既能复用现有 GIS 能力，也能避免后台模型和对外接口强耦合。

## 6. 开发任务清单

以下任务按实际开发顺序排列，完成后即可形成一条可运行的功能链路。

### 任务 1：冻结输入输出协议

目标：

- 固定接口入参与出参，避免后续 DTO 和缓存 Key 反复修改。

产出：

- 请求 DTO：`type`、`address`
- 响应 DTO：`lnglat`、`code`、`address`、`aoi`、`fenceInfo`
- `type` 与业务图层映射规则
- 多围栏命中时的返回优先级

### 任务 2：落 AOI 独立表 DDL

目标：

- 先把 AOI 数据模型独立下来，和 `map_feature` 解耦。

实施项：

- 新增独立 SQL 文件：`zyn-app/zyn-app-jitu/src/main/resources/sql/lbs_aoi.sql`
- 在该文件中创建 `lbs_aoi` 表
- 建立主键、常用查询索引、`GIST(geometry)` 空间索引
- 统一 AOI 主键口径，优先直接使用源数据 `linknid`

完成标准：

- `lbs_aoi` 可独立建表
- 不修改 `zyn-app/zyn-app-map/src/main/resources/sql/map_schema.sql`

### 任务 3：把 AOI 作为通用数据源接入 `map`

目标：

- 不新增 AOI 专属接口，直接复用现有地图通用能力。

实施项：

- 在 `map_data_source` 中新增 AOI 对应的 PostGIS 数据源配置
- 通过现有 `POST /map/import/postgis` 把 `lbs_aoi` 注册到目标图层
- 创建 AOI 图层，例如 `AOI_BP`、`AOI_BUILDING`
- 验证现有要素查询和 GeoJSON 查询接口可以直接浏览 AOI 数据

完成标准：

- 页面可通过现有 `map` 图层查看 AOI
- 无需在 `zyn-app-map` 新增 AOI 专属 `Controller/Service`

### 任务 4：打通 AOI 绑定链路

目标：

- 让极兔围栏要素能够关联到 AOI 主键。

实施项：

- 明确 `map_feature.properties.aoi_id` 字段口径
- 页面通过现有 `map` 图层点选 AOI
- 复用现有围栏要素更新能力，把 `aoi_id` 回写到 `map_feature.properties`
- 为 `properties ->> 'aoi_id'` 增加表达式索引

完成标准：

- 任意一条围栏要素都能绑定 AOI
- 可根据 `aoi_id` 从命中围栏反查 AOI 详情

### 任务 5：搭建极兔查询模块

目标：

- 把第三方查询能力和 `map` 后台隔离开。

实施项：

- 新增 `zyn-app-jitu`
- 在 `zyn-app-jitu` 内定义 `GeoResolveCmd`、`GeoResolveRes`
- 配置外部地图服务地址、缓存 Key 规则、图层映射配置

完成标准：

- `zyn-app-jitu` 可以独立启动
- 查询接口边界独立于 `zyn-app-map`

### 任务 6：实现地址解析与点查面

目标：

- 打通主链路查询。

实施项：

- 调用上游地图服务完成地址解析
- 根据 `type` 选择围栏图层
- 在 `map_feature` 上执行点查面
- 从命中记录中取 `properties.aoi_id`
- 按主键查询 `lbs_aoi`
- 组装返回 `lnglat`、`code`、`address`、`aoi`、`fenceInfo`

完成标准：

- 给定有效地址，可以返回完整业务结果
- 边界点使用 `ST_Covers` 仍可命中围栏

### 任务 7：补缓存和基础性能优化

目标：

- 在不改业务模型的前提下，把查询性能做到可用。

实施项：

- 地址解析结果缓存
- 最终查询结果缓存
- 预计算围栏中心点
- 预生成 AOI GeoJSON 和围栏 GeoJSON
- 校验空间索引和热点查询索引是否生效

完成标准：

- 热点地址请求不重复打上游地图服务
- 单次查询不依赖运行时实时生成大段 GeoJSON

### 任务 8：视情况增加发布态聚合表

目标：

- 当 `map_feature + lbs_aoi` 两段查询性能不足时，再做聚合提速。

实施项：

- 新增 `lbs_geo_fence` 表
- 预聚合围栏中心点、AOI GeoJSON、围栏 GeoJSON、`fenceInfo`
- 发布时同步数据到 `lbs_geo_fence`
- 查询接口切换为优先读取 `lbs_geo_fence`

完成标准：

- 该任务不是首批必做项
- 只有在压测或联调证明两段查询不够时再落地

## 7. 功能完成标准

当前阶段建议以“功能可用”为主，完成标准如下：

- `lbs_aoi` 已通过独立 SQL 文件建表
- `lbs_aoi` 已作为通用 PostGIS 数据源注册到 `map`
- 页面可直接复用现有 `map` 能力浏览和选择 AOI
- `map_feature.properties.aoi_id` 已能稳定保存 AOI 主键
- `zyn-app-jitu` 可接收 `type + address` 并返回完整业务结果
- 接口返回中包含 `lnglat`、`code`、`address`、`aoi`、`fenceInfo`
- 对同一地址重复查询时，可命中缓存且结果一致

## 8. 待确认项

以下问题建议在开发前确认，否则后续容易返工：

1. `address` 是单字符串，还是会拆成省市区街道楼栋等结构化字段。
2. `type=1/2` 分别对应哪张图层、哪个发布版本。
3. 同一点命中多个围栏时，返回哪个结果，是否有优先级或权重。
4. `fenceInfo` 的精确字段结构是什么，是否需要完整业务属性。
5. `aoi` 和 `fenceInfo` 返回完整 GeoJSON，还是返回裁剪/简化后的 GeoJSON。
6. 高并发目标的具体量化指标是什么，例如峰值 QPS、P95、超时阈值。
7. `map_feature.properties.aoi_id` 的值是否就是 AOI 数据里的 `linknid`；如果不是，需要先统一口径。

## 9. 结论

该需求适合设计为独立的对外 LBS 查询模块，核心不是“再加一个地图接口”，而是建设一条稳定的“地址解析 + 围栏匹配 + 结果缓存”查询链路。

基于仓库现有能力，推荐方案是：

- 复用现有 PostGIS / Geo / Redis / Web 基础设施
- 新建 `zyn-app-jitu`
- AOI 独立建表，但通过通用 PostGIS 数据源接入 `map`
- 第一阶段先跑通 `map_feature + lbs_aoi` 查询链路
- 只有性能不够时，再引入 `lbs_geo_fence` 发布态聚合表

这个方向最稳，后续也最容易扩展到更多第三方合作方。
