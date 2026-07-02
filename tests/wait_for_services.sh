#!/bin/bash
unset http_proxy https_proxy HTTP_PROXY HTTPS_PROXY
echo "等待服务启动..."
for i in $(seq 1 12); do
  SYS=$(curl -s -o /dev/null -w '%{http_code}' http://localhost:28081/sys/api/v1/auth/login 2>/dev/null || echo '000')
  DEMO=$(curl -s -o /dev/null -w '%{http_code}' http://localhost:28080/demo/api/v1/sys-proxy/auth/login 2>/dev/null || echo '000')
  echo "  尝试 $i/12: sys=$SYS demo=$DEMO"
  if [ "$SYS" != "000" ] && [ "$DEMO" != "000" ]; then
    echo "✓ 服务已就绪"
    exit 0
  fi
  sleep 5
done
echo "✗ 服务启动超时"
exit 1
