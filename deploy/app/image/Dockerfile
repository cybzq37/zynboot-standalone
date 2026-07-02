FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="zyn"
LABEL description="Zyn application runtime"

RUN apk add --no-cache \
      tzdata \
      curl \
      wget \
      bind-tools \
      busybox-extras \
      iproute2 \
      tcpdump && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    addgroup -g 1000 app && \
    adduser -u 1000 -G app -s /bin/sh -D app

WORKDIR /app

COPY start.sh /app/start.sh
COPY app.jar /app/app.jar
RUN chmod +x /app/start.sh && chown -R app:app /app

USER app

ENTRYPOINT ["/app/start.sh"]
