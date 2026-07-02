FROM elasticsearch:8.15.3

COPY plugins/*.zip /tmp/plugins/

RUN for plugin in analysis-ik analysis-pinyin analysis-stconvert; do \
      file=$(ls /tmp/plugins/elasticsearch-${plugin}-*.zip 2>/dev/null | head -1); \
      if [ -n "$file" ]; then \
        echo "Installing $plugin from local: $file"; \
        elasticsearch-plugin install --batch "file://${file}"; \
      else \
        echo "Downloading $plugin from network..."; \
        elasticsearch-plugin install --batch "https://release.infinilabs.com/${plugin}/stable/elasticsearch-${plugin}-8.15.3.zip"; \
      fi; \
    done || true
