#!/bin/sh
# ----------------------------------------------------------
# 应用启动脚本（容器内执行）
#
# 环境变量:
#   SPRING_ACTIVE_PROFILES  - Spring Profile（默认 prod）
#   JVM_XMS          - 初始堆大小（默认 1g）
#   JVM_XMX          - 最大堆大小（默认 2g）
#   JVM_OPTS         - 额外 JVM 参数
# ----------------------------------------------------------
set -e

SPRING_ACTIVE_PROFILES=${SPRING_ACTIVE_PROFILES:-prod}
JVM_XMS=${JVM_XMS:-1g}
JVM_XMX=${JVM_XMX:-2g}

APP_JAR=/app/app.jar

echo "============================================"
echo "  Starting: $(basename $APP_JAR)"
echo "  Profile:  $SPRING_ACTIVE_PROFILES"
echo "  JVM:      -Xms$JVM_XMS -Xmx$JVM_XMX"
echo "  Timezone: $(date +%Z)"
echo "============================================"

exec java \
  -Xms$JVM_XMS \
  -Xmx$JVM_XMX \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:InitiatingHeapOccupancyPercent=35 \
  -XX:+ParallelRefProcEnabled \
  -XX:+UseStringDeduplication \
  -XX:TieredStopAtLevel=4 \
  -Xlog:gc*:file=logs/gc.log:time,level,tags:filecount=10,filesize=100m \
  -Djava.security.egd=file:/dev/./urandom \
  -Dfile.encoding=UTF-8 \
  -Duser.timezone=Asia/Shanghai \
  $JVM_OPTS \
  -jar "$APP_JAR" \
  --spring.profiles.active=$SPRING_ACTIVE_PROFILES
