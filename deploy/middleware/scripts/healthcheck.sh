#!/bin/bash
# ----------------------------------------------------------
# 中间件健康检查脚本
# 用法: ./healthcheck.sh
# ----------------------------------------------------------
cd "$(dirname "$0")/.."

PASS=0
FAIL=0
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

AWS_ENV="-e AWS_ACCESS_KEY_ID=zynex -e AWS_SECRET_ACCESS_KEY=Zyn@secure#99 -e AWS_DEFAULT_REGION=cn-north-1"
S3_ENDPOINT="http://seaweedfs-s3:8333"

pass() { ((PASS++)); echo -e "  ${GREEN}✓${NC} $1"; }
fail() { ((FAIL++)); echo -e "  ${RED}✗${NC} $1"; }
info() { echo -e "${YELLOW}[$1]${NC}"; }

# ----------------------------------------------------------
# PostgreSQL
# ----------------------------------------------------------
info "PostgreSQL"
result=$(docker exec postgres psql -U postgres -d zyn_base -t -c "SELECT 1;" 2>/dev/null | tr -d ' ')
[ "$result" = "1" ] && pass "连接查询: SELECT 1" || fail "连接查询失败"

docker exec postgres psql -U postgres -d zyn_base -c "CREATE EXTENSION IF NOT EXISTS postgis;" > /dev/null 2>&1
result=$(docker exec postgres psql -U postgres -d zyn_base -t -c "SELECT PostGIS_Version();" 2>/dev/null | tr -d ' ')
[ -n "$result" ] && pass "PostGIS: $result" || fail "PostGIS 未安装"

# ----------------------------------------------------------
# Redis
# ----------------------------------------------------------
info "Redis"
docker exec redis redis-cli -a 'Zyn@secure#99' SET zyn:test "hello" > /dev/null 2>&1
result=$(docker exec redis redis-cli -a 'Zyn@secure#99' GET zyn:test 2>/dev/null)
[ "$result" = "hello" ] && pass "读写: SET/GET" || fail "读写失败"
docker exec redis redis-cli -a 'Zyn@secure#99' DEL zyn:test > /dev/null 2>&1

result=$(docker exec redis redis-cli -a 'Zyn@secure#99' PING 2>/dev/null)
[ "$result" = "PONG" ] && pass "PING: PONG" || fail "PING 失败"

# ----------------------------------------------------------
# Kafka
# ----------------------------------------------------------
info "Kafka"
echo "zyn-test-message" | docker exec -i kafka /opt/kafka/bin/kafka-console-producer.sh \
  --bootstrap-server localhost:9092 --topic zyn-test-topic > /dev/null 2>&1 && \
  pass "生产消息" || fail "生产消息失败"

sleep 1
result=$(docker exec kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 --topic zyn-test-topic --from-beginning --timeout-ms 5000 2>/dev/null)
echo "$result" | grep -q "zyn-test-message" && pass "消费消息" || fail "消费消息失败"

docker exec kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 --delete --topic zyn-test-topic > /dev/null 2>&1

# ----------------------------------------------------------
# Elasticsearch
# ----------------------------------------------------------
info "Elasticsearch"
ES_AUTH="-u elastic:Zyn@secure#99"
result=$(curl -sf $ES_AUTH http://localhost:9200/_cluster/health 2>/dev/null | python3 -c "import sys,json;print(json.load(sys.stdin)['status'])" 2>/dev/null)
[ "$result" = "green" ] || [ "$result" = "yellow" ] && pass "集群状态: $result" || fail "集群状态异常: $result"

result=$(curl -sf $ES_AUTH http://localhost:9200/_cat/plugins 2>/dev/null)
echo "$result" | grep -q "analysis-ik"       && pass "插件: IK"        || fail "插件缺失: IK"
echo "$result" | grep -q "analysis-pinyin"    && pass "插件: Pinyin"    || fail "插件缺失: Pinyin"
echo "$result" | grep -q "analysis-stconvert" && pass "插件: STConvert" || fail "插件缺失: STConvert"

result=$(curl -sf $ES_AUTH -X POST "http://localhost:9200/_analyze" \
  -H "Content-Type: application/json" \
  -d '{"analyzer": "ik_smart", "text": "中华人民共和国"}' 2>/dev/null \
  | python3 -c "import sys,json;tokens=json.load(sys.stdin)['tokens'];print(','.join(t['token'] for t in tokens))" 2>/dev/null)
[ -n "$result" ] && pass "IK 分词: $result" || fail "IK 分词失败"

result=$(curl -sf $ES_AUTH -X POST "http://localhost:9200/_analyze" \
  -H "Content-Type: application/json" \
  -d '{"analyzer": "pinyin", "text": "中国"}' 2>/dev/null \
  | python3 -c "import sys,json;tokens=json.load(sys.stdin)['tokens'];print(','.join(t['token'] for t in tokens))" 2>/dev/null)
[ -n "$result" ] && pass "Pinyin: $result" || fail "Pinyin 分词失败"

# ----------------------------------------------------------
# SeaweedFS (S3)
# ----------------------------------------------------------
info "SeaweedFS (S3)"
result=$(docker run --rm --network zyn_net $AWS_ENV amazon/aws-cli:2.17.0 --endpoint-url $S3_ENDPOINT s3 ls 2>/dev/null)
echo "$result" | grep -q "zyn" && pass "Bucket: zyn 已存在" || fail "Bucket zyn 不存在"

echo "zyn-s3-test" | docker run --rm -i --network zyn_net $AWS_ENV amazon/aws-cli:2.17.0 \
  --endpoint-url $S3_ENDPOINT s3 cp - s3://zyn/test/zyn-s3-test.txt > /dev/null 2>&1 && \
  pass "上传文件" || fail "上传文件失败"

docker run --rm --network zyn_net $AWS_ENV amazon/aws-cli:2.17.0 \
  --endpoint-url $S3_ENDPOINT s3 cp s3://zyn/test/zyn-s3-test.txt /tmp/zyn-test.txt > /dev/null 2>&1 && \
  pass "下载文件" || fail "下载文件失败"

docker run --rm --network zyn_net $AWS_ENV amazon/aws-cli:2.17.0 \
  --endpoint-url $S3_ENDPOINT s3 rm s3://zyn/test/zyn-s3-test.txt > /dev/null 2>&1

# ----------------------------------------------------------
# INFINI Console
# ----------------------------------------------------------
info "INFINI Console"
code=$(curl -sf -o /dev/null -w "%{http_code}" http://localhost:9000 2>/dev/null)
[ "$code" = "200" ] && pass "HTTP 访问: $code" || fail "HTTP 访问失败: $code"

# ----------------------------------------------------------
# 汇总
# ----------------------------------------------------------
echo ""
echo "=============================="
echo -e "  ${GREEN}通过: ${PASS}${NC}  ${RED}失败: ${FAIL}${NC}"
echo "=============================="

[ $FAIL -eq 0 ] && echo -e "  ${GREEN}所有组件正常${NC}" || echo -e "  ${RED}存在异常，请检查${NC}"
exit $FAIL
