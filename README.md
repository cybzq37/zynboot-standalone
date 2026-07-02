# zyn

Java 21 + Spring Boot 3.5.x 多模块框架

## 模块结构

```
zyn/
├── zyn-kit/          ← 工具层（工具类、Jackson、OkHttp、线程池、树结构）
├── zyn-conf/         ← 配置层（环境配置、日志、中间件连接）
├── zyn-infra/        ← 基础设施层
│   ├── zyn-infra-exchange      ← HTTP 客户端（ExchangeClient 自动注册）
│   ├── zyn-infra-redis         ← Redis 客户端
│   ├── zyn-infra-kafka         ← Kafka 客户端
│   ├── zyn-infra-es            ← Elasticsearch 客户端
│   ├── zyn-infra-mybatis       ← MyBatis-Plus 扩展
│   ├── zyn-infra-storage       ← 文件存储（本地/S3）
│   ├── zyn-infra-geo           ← GIS（Shapefile/GeoJSON/CRS/GDAL）
│   ├── zyn-infra-web           ← Web 基础（全局异常、响应包装、CORS）
│   └── zyn-infra-satoken       ← Sa-Token 认证
├── zyn-api/          ← API 契约层（DTO/Query/ExchangeClient 接口）
│   └── zyn-api-system          ← 系统管理 API
└── zyn-app/          ← 应用层（业务实现）
    ├── zyn-app-system          ← 系统管理服务
    ├── zyn-app-netty           ← Netty 服务
    └── zyn-app-demo            ← 示例应用
```

## 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Java 21 |
| 框架 | Spring Boot 3.5.x |
| 数据库 | PostgreSQL + PostGIS |
| 缓存 | Redis |
| 消息队列 | Kafka |
| 搜索引擎 | Elasticsearch + IK/Pinyin/STConvert |
| 对象存储 | SeaweedFS (S3 兼容) |
| ORM | MyBatis-Plus |
| 认证 | Sa-Token |
| 文档 | SpringDoc (Swagger) |
| 日志 | Log4j2 + Disruptor |
| HTTP 客户端 | OkHttp + HttpExchange |
| GIS | GeoTools + GDAL |

## 构建

```bash
# 开发环境
mvn clean package

# 生产环境
mvn clean package -P prod
```

## 中间件

```bash
cd deploy/middleware
bash scripts/start.sh           # 启动全部
bash scripts/start.sh postgres  # 启动指定服务
bash scripts/start.sh -f redis  # 重建指定服务
bash scripts/healthcheck.sh     # 健康检查
```

## 部署

```bash
cd deploy/app
./build.sh --jar zyn-app-demo.jar --name demo --port 28080
```

## 集成测试

```bash
# 宿主机构建
mvn clean package -pl zyn-app/zyn-app-system,zyn-app/zyn-app-demo -am -P prod -DskipTests
cp zyn-app/zyn-app-system/target/zyn-app-system-*.jar deploy/app/zyn-app-system.jar
cp zyn-app/zyn-app-demo/target/zyn-app-demo-*.jar deploy/app/zyn-app-demo.jar

# WSL 部署 + 测试
bash tests/deploy.sh
```

详见 [tests/README.md](tests/README.md)。

---

## API 设计

接口风格 RESTful，架构模式 CQRS，DDD 建模。

### 命名规范

| 类型 | 后缀 | 用途 | 示例 |
|------|------|------|------|
| 响应对象 | `Res` | API 响应 | `UserRes`、`LoginRes` |
| 查询参数 | `Query` | GET 请求参数 | `UserQuery`、`UserPageQuery` |
| 写命令 | `Cmd` | POST/PUT 请求体 | `UserSaveCmd`、`ResetPasswordCmd` |
| ExchangeClient | `Sys*Api` | 跨服务调用 | `SysUserApi`、`SysRoleApi` |

### zyn-api-system 包结构

```
com.zynboot.sys/
├── api/               ← ExchangeClient 接口（服务端路由 + 客户端代理）
│   ├── SysAuthApi.java
│   ├── SysUserApi.java
│   ├── SysRoleApi.java
│   ├── SysPermissionApi.java
│   ├── SysResourceApi.java
│   └── SysOrganizationApi.java
├── command/           ← 写命令（Cmd 后缀）
│   ├── user/UserSaveCmd.java
│   ├── role/RoleSaveCmd.java
│   ├── permission/PermissionSaveCmd.java
│   ├── resource/ResourceSaveCmd.java
│   └── org/OrgSaveCmd.java
├── query/             ← 查询参数（Query 后缀）
│   ├── user/UserPageQuery.java
│   ├── role/RoleQuery.java
│   ├── permission/PermissionQuery.java
│   └── org/OrgQuery.java
└── response/          ← 响应对象（Res 后缀）
    ├── user/UserRes.java, LoginRes.java, LoginUserRes.java, UserInfoRes.java
    ├── role/RoleRes.java
    ├── permission/PermissionRes.java, MenuTreeRes.java
    ├── resource/ResourceRes.java
    └── org/OrgRes.java, OrgTreeRes.java
```

---

## zyn-infra-exchange（HTTP 客户端）

声明式 HTTP 客户端模块，自动扫描 `@ExchangeClient` 接口并注册为 Spring Bean。

### 快速开始

```java
// 1. 定义接口
@ExchangeClient("sys")
@HttpExchange("/sys/api/v1/user")
public interface SysUserApi {
    @GetExchange("/{id}")
    ApiResponse<UserRes> getById(@PathVariable String id);
}

// 2. 直接注入使用
@RestController
@RequiredArgsConstructor
public class DemoController {
    private final SysUserApi userApi;

    @GetMapping("/demo/{id}")
    public ApiResponse<UserRes> demo(@PathVariable String id) {
        return userApi.getById(id);
    }
}
```

### 服务地址配置

```yaml
# zyn-conf-prod.yml
zyn:
  exchange:
    services:
      sys: http://zyn-sys:28081
      biz: http://zyn-biz:8082
```

### 运行流程

```
① ExchangeClientRegistrar
   ↓ 扫描 com.zynboot 下所有 @ExchangeClient 接口
   ↓ 注册 ExchangeClientFactoryBean

② ExchangeClientFactoryBean
   ↓ 从 zyn.exchange.services.<name> 读取 URL
   ↓ ServiceProxyBuilder 创建 HttpExchange 代理

③ ServiceProxyBuilder
   ↓ RestClient + HttpServiceProxyFactory → 代理对象
   ↓ 自动转发 Authorization header
   ↓ SPI 加载 HttpServiceArgumentResolver（如 @HttpQuery）
```

### @HttpQuery 注解

Spring 6.x 不支持 POJO 自动展开为查询参数，`@HttpQuery` 解决此问题：

```java
@GetExchange("/sys/api/v1/user")
ApiResponse<Map<String, Object>> page(@HttpQuery UserPageQuery query);
```

通过 SPI 自动注册，无需额外配置。Spring 7.1+ 将原生支持（#32142）。

### 禁用

```yaml
zyn:
  exchange:
    enabled: false
```
