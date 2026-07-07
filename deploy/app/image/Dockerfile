FROM zyn-base:latest

LABEL maintainer="zyn"
LABEL description="Zyn application"

WORKDIR /app

COPY start.sh /app/start.sh
COPY app.jar /app/app.jar
RUN chmod +x /app/start.sh && chown -R app:app /app

USER app

ENTRYPOINT ["/app/start.sh"]
