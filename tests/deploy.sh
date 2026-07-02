#!/bin/bash
# ============================================================
# 部署 & 测试 zyn-app-system + zyn-app-demo（WSL 内执行）
#
# 前置条件:
#   1. 宿主机已完成 Maven 构建（见 README.md）
#   2. WSL 中间件已启动
#   3. Python3 + pytest 已安装: pip3 install pytest requests
#
# 用法:
#   bash tests/deploy.sh              # 部署 + 测试
#   bash tests/deploy.sh --deploy     # 仅部署
#   bash tests/deploy.sh --test       # 仅测试
# ============================================================
set -e
cd "$(dirname "$0")/.."

DEPLOY=true
TEST=true

for arg in "$@"; do
  case "$arg" in
    --deploy) TEST=false ;;
    --test)   DEPLOY=false ;;
  esac
done

# Docker 网络中的中间件容器名
COMMON_ENV="--env DB_HOST=postgres --env REDIS_HOST=redis --env ES_HOST=elasticsearch --env KAFKA_HOST=kafka --env S3_HOST=seaweedfs-s3 --env SYS_BASE_URL=http://sys:28081"

# ── 部署 ──────────────────────────────────────────────────────
if [ "$DEPLOY" = true ]; then
  if [ ! -f deploy/app/zyn-app-system.jar ] || [ ! -f deploy/app/zyn-app-demo.jar ]; then
    echo "ERROR: JAR 文件不存在，请先在宿主机执行 Maven 构建并复制 JAR"
    echo "  见 tests/README.md"
    exit 1
  fi

  echo "============================================"
  echo "  部署 zyn-app-system :28081"
  echo "============================================"
  cd deploy/app
  bash build.sh --jar zyn-app-system.jar --name sys --port 28081 --profile dev \
    $COMMON_ENV --env SYS_HOST=sys
  cd ../..

  echo "============================================"
  echo "  部署 zyn-app-demo :28080"
  echo "============================================"
  cd deploy/app
  bash build.sh --jar zyn-app-demo.jar --name demo --port 28080 --profile dev \
    $COMMON_ENV --env SYS_HOST=sys
  cd ../..

  echo ""
  echo "等待服务启动..."
  bash tests/wait_for_services.sh
fi

# ── 测试 ──────────────────────────────────────────────────────
if [ "$TEST" = true ]; then
  echo "============================================"
  echo "  运行集成测试"
  echo "============================================"
  cd tests
  python3 -m pytest test_system_api.py -v --tb=short
  cd ..
fi
