package com.zynboot.map.service.datasource;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.zynboot.infra.es.EsClient;
import com.zynboot.map.infrastructure.entity.MapDataSource;
import com.zynboot.map.infrastructure.mapper.MapDataSourceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ES 客户端管理器。
 * 为每个 ELASTICSEARCH 类型的 map_data_source 创建 ElasticsearchClient + EsClient，
 * 缓存复用，支持集群多节点。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EsClientManager {

    private final MapDataSourceMapper dataSourceMapper;

    private final Map<String, EsClient> esClientCache = new ConcurrentHashMap<>();
    private final Map<String, RestClient> restClientCache = new ConcurrentHashMap<>();

    public EsClient getOrCreateEsClient(String dataSourceId) {
        return esClientCache.computeIfAbsent(dataSourceId, id -> {
            MapDataSource ds = dataSourceMapper.selectById(id);
            if (ds == null) throw new IllegalArgumentException("数据源不存在: " + id);
            return createEsClient(ds);
        });
    }

    public void removeEsClient(String dataSourceId) {
        esClientCache.remove(dataSourceId);
        RestClient restClient = restClientCache.remove(dataSourceId);
        if (restClient != null) {
            try { restClient.close(); } catch (Exception ignored) {}
            log.info("ES client closed: {}", dataSourceId);
        }
    }

    public boolean testConnection(MapDataSource ds) {
        try {
            RestClient restClient = createRestClient(ds);
            var transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
            var client = new ElasticsearchClient(transport);
            boolean ok = client.ping().value();
            transport.close();
            restClient.close();
            return ok;
        } catch (Exception e) {
            log.warn("ES connection test failed: {}", e.getMessage());
            return false;
        }
    }

    @PreDestroy
    public void closeAll() {
        restClientCache.values().forEach(c -> {
            try { c.close(); } catch (Exception ignored) {}
        });
        esClientCache.clear();
        restClientCache.clear();
    }

    private EsClient createEsClient(MapDataSource ds) {
        RestClient restClient = createRestClient(ds);
        restClientCache.put(ds.getId(), restClient);

        var transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        var esClient = new ElasticsearchClient(transport);

        // ElasticsearchTemplate 使用新的 ElasticsearchClient
        ElasticsearchOperations operations = new ElasticsearchTemplate(esClient);

        log.info("ES client created: id={}, nodes={}", ds.getId(), ds.getUrl());
        return new EsClient(operations);
    }

    private RestClient createRestClient(MapDataSource ds) {
        String[] urls = ds.getUrl().split(",");
        List<HttpHost> hosts = new ArrayList<>();
        for (String url : urls) {
            url = url.trim().replaceAll("/$", "");
            try {
                java.net.URI uri = java.net.URI.create(url);
                String scheme = uri.getScheme() != null ? uri.getScheme() : "http";
                int port = uri.getPort() > 0 ? uri.getPort() : ("https".equals(scheme) ? 443 : 80);
                hosts.add(new HttpHost(uri.getHost(), port, scheme));
            } catch (Exception e) {
                log.warn("Invalid ES URL: {}", url);
            }
        }

        if (hosts.isEmpty()) throw new IllegalArgumentException("ES URL 无效: " + ds.getUrl());

        var builder = RestClient.builder(hosts.toArray(new HttpHost[0]))
                .setRequestConfigCallback(config -> config
                        .setConnectTimeout(10000)
                        .setSocketTimeout(30000)
                        .setConnectionRequestTimeout(5000));

        if (ds.getUsername() != null && !ds.getUsername().isBlank()) {
            BasicCredentialsProvider cp = new BasicCredentialsProvider();
            cp.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(ds.getUsername(),
                            ds.getPassword() != null ? ds.getPassword() : ""));
            builder.setHttpClientConfigCallback(http -> http.setDefaultCredentialsProvider(cp));
        }

        return builder.build();
    }
}
