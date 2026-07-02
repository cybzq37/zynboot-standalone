#!/bin/bash
# ----------------------------------------------------------
# 中间件启动脚本
# 用法:
#   ./start.sh                            # 检查全部
#   ./start.sh postgres redis             # 检查指定服务
#   ./start.sh -f                         # 重建全部容器
#   ./start.sh -f postgres                # 重建指定容器
#
# 行为:
#   健康运行 → 跳过
#   不健康   → 重启
#   未启动   → 创建并启动
# ----------------------------------------------------------
cd "$(dirname "$0")/.."

# 创建数据目录
dirs=(
  postgres/data postgres/logs
  redis/data redis/logs
  kafka/data
  elasticsearch/data elasticsearch/logs
  seaweedfs/master/data seaweedfs/volume/data seaweedfs/filer/data
  infini-console/config infini-console/data infini-console/logs
)
for d in "${dirs[@]}"; do mkdir -p "$d"; done

# 修复权限
chown -R 1000:1000 postgres/logs
chown -R  999:999  redis/logs
chown -R 1000:1000 kafka/data elasticsearch/data elasticsearch/logs

# 解析参数
FORCE_RECREATE=false
SERVICES=()
for arg in "$@"; do
  if [ "$arg" = "--force-recreate" ] || [ "$arg" = "-f" ]; then
    FORCE_RECREATE=true
  else
    SERVICES+=("$arg")
  fi
done

# 获取容器状态
# 返回: healthy / unhealthy / starting / running / stopped
get_status() {
  local svc="$1"
  local container
  container=$(docker-compose ps -q "$svc" 2>/dev/null)
  if [ -z "$container" ]; then
    echo "stopped"
    return
  fi
  local health
  health=$(docker inspect --format='{{.State.Health.Status}}' "$container" 2>/dev/null)
  case "$health" in
    healthy)   echo "healthy" ;;
    unhealthy) echo "unhealthy" ;;
    starting)  echo "starting" ;;
    *)         echo "running" ;;
  esac
}

# 处理单个服务
process_service() {
  local svc="$1"
  local status
  status=$(get_status "$svc")
  case "$status" in
    healthy)
      echo "  ✓ $svc (healthy)"
      ;;
    starting)
      echo "  ⏳ $svc (starting, waiting...)"
      ;;
    unhealthy|running)
      echo "  ↻ $svc ($status, restarting)"
      docker-compose restart "$svc" > /dev/null 2>&1
      ;;
    stopped)
      echo "  ▶ $svc (creating)"
      docker-compose up -d "$svc" > /dev/null 2>&1
      ;;
  esac
}

# 执行
if [ "$FORCE_RECREATE" = true ]; then
  echo "重建容器..."
  docker-compose up -d --force-recreate "${SERVICES[@]}"
else
  if [ ${#SERVICES[@]} -eq 0 ]; then
    # 未指定服务：检查全部
    echo "检查全部服务:"
    SERVICES=($(docker-compose config --services 2>/dev/null))
  else
    echo "检查指定服务:"
  fi
  for svc in "${SERVICES[@]}"; do
    process_service "$svc"
  done
fi
