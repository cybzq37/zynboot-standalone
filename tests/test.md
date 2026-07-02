# System API 集成测试

## 测试架构

```
pytest 测试脚本
  ↓ HTTP
zyn-app-demo :28080          ← proxy 层（demo → system）
  ↓ HTTP (Remote*Service)
zyn-app-system :28081         ← 直连层
  ↓
PostgreSQL / Redis (中间件容器)
```

## 前置条件

| 组件 | 宿主机 (Windows) | WSL |
|---|---|---|
| Java 21 + Maven | ✅ | 不需要 |
| Docker | 不需要 | ✅ |
| Python3 + pytest | 不需要 | ✅ |

```bash
# WSL 中安装测试依赖
pip3 install pytest requests
```

## 完整流程

### Step 1：宿主机构建（Windows PowerShell / CMD）

```powershell
cd D:\GitRepo\zippyboot

# Maven 构建，prod 环境
mvn clean package -pl zyn-app/zyn-app-system,zyn-app/zyn-app-demo -am -P prod -DskipTests

# 复制 JAR 到 WSL 可访问的部署目录
copy zyn-app\zyn-app-system\target\zyn-app-system-1.0.0-SNAPSHOT.jar deploy\app\zyn-app-system.jar
copy zyn-app\zyn-app-demo\target\zyn-app-demo-1.0.0-SNAPSHOT.jar deploy\app\zyn-app-demo.jar
```

### Step 2：WSL 启动中间件（如果未启动）

```bash
cd /mnt/d/GitRepo/zippyboot/deploy/middleware
./scripts/start.sh
```

### Step 3：WSL 部署 + 测试

```bash
cd /mnt/d/GitRepo/zippyboot

# 一键：部署 + 测试
bash tests/deploy.sh

# 或分步执行
bash tests/deploy.sh --deploy    # 仅部署容器
bash tests/deploy.sh --test      # 仅运行测试
```

## 测试范围

### 直连测试 → zyn-app-system:28081

| 模块 | 用例 |
|---|---|
| Auth | login / info / logout |
| User | create / page / getById / update / delete |
| Role | create / list / getById / update / delete |
| Permission | create / list / tree / getById / update / delete |
| Resource | create / list / getById / update / delete |
| Organization | create / list / tree / getById / update / delete |

### 代理测试 → zyn-app-demo:28080 → zyn-app-system:28081

| 模块 | 用例 |
|---|---|
| Auth | login / info |
| User | page / getById |
| Role | list / getById |
| Permission | list / tree |
| Resource | list |
| Organization | list / tree |

## 手动测试

```bash
# 登录获取 token
TOKEN=$(curl -s -X POST 'http://localhost:28081/sys/api/v1/auth/login' \
  -H 'Content-Type: application/json' \
  -d '{"username":"root","password":"Zyn@secure#99"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['token'])")

# 直连: system API
curl -s -H "Authorization: $TOKEN" http://localhost:28081/sys/api/v1/user | python3 -m json.tool

# 代理: demo → system
curl -s -H "Authorization: $TOKEN" http://localhost:28080/demo/api/v1/sys-proxy/user | python3 -m json.tool
```

## 测试数据

seed 用户（密码均为 `Zyn@secure#99`）：

| 用户名 | 角色 | 说明 |
|---|---|---|
| root | root（超级用户） | 绕过 RBAC |
| admin | admin（管理员） | 走 RBAC |
