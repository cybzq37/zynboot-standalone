#!/bin/bash
# ----------------------------------------------------------
# 应用构建 & 部署脚本
#
# 用法:
#   ./build.sh --build-base                                   # 构建基础镜像（工具链，仅需一次）
#   ./build.sh --jar zyn-app-demo.jar --name demo              # 必填: JAR + 镜像名
#   ./build.sh --jar app.jar --name demo --tag v1.0            # 自定义版本
#   ./build.sh --jar app.jar --name demo --port 28080          # 指定端口
#   ./build.sh --jar app.jar --name demo --profile dev         # 指定 Profile
#   ./build.sh --jar app.jar --name demo --jvm-xmx 1g          # 指定最大堆
#
# 参数:
#   --build-base  构建基础镜像 zyn-base:latest（仅需执行一次）
#   --jar         JAR 文件名（必填，或自动检测当前目录唯一 JAR）
#   --name        镜像名称（必填）
#   --tag         镜像版本（默认: latest）
#   --port        端口映射（默认: 8080）
#   --profile     Spring Profile（默认: prod）
#   --jvm-xms     初始堆大小（默认: 512m）
#   --jvm-xmx     最大堆大小（默认: 512m）
#
# 目录结构:
#   deploy/app/
#   ├── build.sh          ← 本脚本
#   ├── image/
#   │   ├── Dockerfile    ← 镜像构建文件
    #   │   └── start.sh    ← 容器启动脚本
#   ├── <your-app>.jar    ← 上传 JAR 到这里
#   └── logs/             ← 运行时日志（自动生成）
#
# 流程: 复制 JAR → 构建镜像 → 停旧容器 → 清理悬空镜像 → 启新容器
# ----------------------------------------------------------
set -e
cd "$(dirname "$0")"

# ── 默认参数 ─────────────────────────────────────────────────
JAR_FILE=""
IMAGE_NAME=""
IMAGE_TAG="latest"
APP_PORT=8080
SPRING_PROFILE="prod"
JVM_XMS="512m"
JVM_XMX="512m"
EXTRA_ENV=""
BUILD_BASE=false

# ── 解析参数 ─────────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
  case "$1" in
    --build-base) BUILD_BASE=true;    shift   ;;
    --jar)      JAR_FILE="$2";       shift 2 ;;
    --name)     IMAGE_NAME="$2";     shift 2 ;;
    --tag)      IMAGE_TAG="$2";      shift 2 ;;
    --port)     APP_PORT="$2";       shift 2 ;;
    --profile)  SPRING_PROFILE="$2"; shift 2 ;;
    --jvm-xms)  JVM_XMS="$2";       shift 2 ;;
    --jvm-xmx)  JVM_XMX="$2";       shift 2 ;;
    --env)      EXTRA_ENV="${EXTRA_ENV} -e $2"; shift 2 ;;
    *)          JAR_FILE="$1";       shift   ;;
  esac
done

# ── 构建基础镜像 ─────────────────────────────────────────────
if [ "$BUILD_BASE" = true ]; then
  echo "构建基础镜像: zyn-base:latest"
  docker build -t zyn-base:latest -f image/Dockerfile.base image/
  echo "✓ 基础镜像构建完成: zyn-base:latest"
  echo "  后续部署只需: ./build.sh --jar <file.jar> --name <name> --port <port>"
  exit 0
fi

# ── 自动检测 JAR ────────────────────────────────────────────
if [ -z "$JAR_FILE" ]; then
  JAR_FILE=$(ls -t *.jar 2>/dev/null | head -1)
fi

if [ -z "$JAR_FILE" ] || [ ! -f "$JAR_FILE" ]; then
  echo "ERROR: No JAR file found. Use: ./build.sh --jar <file.jar>"
  exit 1
fi

if [ -z "$IMAGE_NAME" ]; then
  echo "ERROR: --name is required. Use: ./build.sh --jar <file.jar> --name <image-name>"
  exit 1
fi

JAR_BASENAME=$(basename "$JAR_FILE")
CONTAINER_NAME="${IMAGE_NAME}"
FULL_IMAGE="${IMAGE_NAME}:${IMAGE_TAG}"

# ── 检查基础镜像 ─────────────────────────────────────────────
if ! docker image inspect zyn-base:latest > /dev/null 2>&1; then
  echo "ERROR: 基础镜像 zyn-base:latest 不存在，请先执行:"
  echo "  ./build.sh --build-base"
  exit 1
fi

echo "============================================"
echo "  Image:    $FULL_IMAGE"
echo "  JAR:      $JAR_BASENAME"
echo "  Port:     $APP_PORT"
echo "  Profile:  $SPRING_PROFILE"
echo "  JVM:      -Xms$JVM_XMS -Xmx$JVM_XMX"
echo "============================================"

# ── 复制 JAR 到构建目录 ──────────────────────────────────────
echo "复制 JAR: $JAR_BASENAME → image/"
cp "$JAR_FILE" "image/app.jar"

# ── 构建镜像 ────────────────────────────────────────────────
echo "构建镜像: $FULL_IMAGE"

docker build -t "$FULL_IMAGE" image/

# ── 清理构建产物 ─────────────────────────────────────────────
rm -f image/app.jar

# ── 停旧容器 ────────────────────────────────────────────────
if docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
  echo "停止旧容器: $CONTAINER_NAME"
  docker stop "$CONTAINER_NAME" 2>/dev/null || true
  docker rm "$CONTAINER_NAME" 2>/dev/null || true
fi

# ── 清理悬空镜像 ─────────────────────────────────────────────
docker image prune -f > /dev/null 2>&1

# ── 启动新容器 ──────────────────────────────────────────────
echo "启动容器: $CONTAINER_NAME"

docker run -d \
  --name "$CONTAINER_NAME" \
  --network zyn_net \
  -p "${APP_PORT}:${APP_PORT}" \
  -v "$(pwd)/logs/${IMAGE_NAME}:/app/logs" \
  -e SPRING_ACTIVE_PROFILES="$SPRING_PROFILE" \
  -e JVM_XMS="$JVM_XMS" \
  -e JVM_XMX="$JVM_XMX" \
  $EXTRA_ENV \
  --restart unless-stopped \
  "$FULL_IMAGE"

# ── 启动完成 ────────────────────────────────────────────────
echo "✓ 容器已启动: $CONTAINER_NAME"
echo "  查看日志: docker logs -f $CONTAINER_NAME"
