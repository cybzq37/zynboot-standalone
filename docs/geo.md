# 极兔 Geo 对外接口需求与实施计划

## 1. 上游地址解析示例

当前拿到的地图服务示例接口如下：

baseUrl：`https://map-api.minedata.cn`
请求接口：`/service/lbs/search/v1/geo?key=6b31b6fdc3d5497aaf1dcefb487aa74e&address=北京市海淀区永旭南路四维图新大厦`
key：`6b31b6fdc3d5497aaf1dcefb487aa74e`

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

新增一个对外部用户提供的接口，GET 请求。

请求参数：

- `type`：`1` 为取件，`2` 为收件
- `address`：结构化取件/收件地址

响应参数：

- `lnglat`：对应四段码的中心经纬度坐标
- `code`：绑定的四段码
- `address`：解析后的标准化地址
- `aoi`：AOI 数据几何信息，格式为 GeoJSON 字符串
- `fenceInfo`：JSON 格式，包含图层要素 ID、GeoJSON、绑定的 AOI

处理流程：

1. 根据 `address` 调用地图服务，获取经纬度坐标和标准化地址。
2. 根据经纬度坐标，从极兔图层 layer-jitu 中获取命中的面数据，并且按 `type` 区分图层。
3. aoi图层有两个，layer-aoi-bp 和 layer-aoi-building，拼装结果返回。

## 3. 接口协议

### 3.1 请求

```
GET /jitu/openapi/seg4code/v1/geo/resolve?type={type}&address={address}
```

| 参数 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `type` | query | int | 是 | `1`=取件，`2`=收件 |
| `address` | query | string | 是 | 结构化取件/收件地址 |

请求示例：

```
GET /jitu/openapi/seg4code/v1/geo/resolve?type=1&address=北京市海淀区永旭南路四维图新大厦
```

### 3.2 响应结构

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | string | 业务状态码，成功恒为 `"0"` |
| `message` | string | 状态描述，成功为 `"OK"` |
| `timestamp` | string | ISO-8601 时间戳，服务端生成 |
| `path` | string | 请求 URI |
| `data` | object | 业务数据，见 `data` 结构；失败时为 `null` |

#### `data` 结构

| 字段 | 类型 | 说明 |
|------|------|------|
| `data.lnglat` | string | 命中围栏的中心经纬度，格式 `lng,lat` |
| `data.code` | string | 围栏绑定的四段码 |
| `data.address` | string | 地理编码解析出的标准化地址 |
| `data.aoi` | object\|null | AOI 几何信息，GeoJSON 几何对象（如 Polygon），未命中为 `null` |
| `data.fenceInfo` | object | 围栏信息，见 `data.fenceInfo` 结构 |

#### `data.fenceInfo` 结构

| 字段 | 类型 | 说明 |
|------|------|------|
| `featureId` | long | 命中的图层要素 ID |
| `geojson` | object | 围栏几何信息，GeoJSON 对象 |
| `properties` | object | 围栏要素属性，仅返回白名单字段：`code`（四段码）、`name`（名称）、`type`（类型）、`aoi_id`（绑定 AOI ID）；未提供的字段不出现 |

### 3.3 成功响应示例

```json
{
  "code": "0",
  "message": "OK",
  "timestamp": "2026-07-08T15:30:00.123Z",
  "path": "/jitu/openapi/seg4code/v1/geo/resolve",
  "data": {
    "lnglat": "116.245583,40.072503",
    "code": "JT-BJ-001",
    "address": "北京市海淀区四维图新大厦",
    "aoi": {
      "type": "Polygon",
      "coordinates": [[[116.24,40.07],[116.25,40.07],[116.25,40.08],[116.24,40.08],[116.24,40.07]]]
    },
    "fenceInfo": {
      "featureId": 1001,
      "geojson": {
        "type": "Polygon",
        "coordinates": [[[116.24,40.07],[116.25,40.07],[116.25,40.08],[116.24,40.08],[116.24,40.07]]]
      },
      "properties": {
        "code": "JT-BJ-001",
        "name": "海淀区取件区",
        "type": 1,
        "aoi_id": "aoi-bp-001"
      }
    }
  }
}
```

### 3.4 错误响应

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | string | 业务错误码，`400`=请求参数错误，`404`=资源不存在，`500`=服务端错误 |
| `message` | string | 错误描述 |
| `timestamp` | string | ISO-8601 时间戳 |
| `path` | string | 请求 URI |
| `details` | array | 错误明细列表（校验错误等），无明细时为 `[]` |

#### 错误响应示例（参数校验失败）

```json
{
  "code": "400",
  "message": "Validation failed",
  "timestamp": "2026-07-08T15:30:01.456Z",
  "path": "/jitu/openapi/seg4code/v1/geo/resolve",
  "details": [
    "type: 必须为 1 或 2",
    "address: 不能为空"
  ]
}
```