package com.zynboot.infra.es;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHitSupport;
import org.springframework.data.elasticsearch.core.SearchPage;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;

import java.util.List;
import java.util.Optional;

/**
 * Elasticsearch 客户端封装。
 * <p>
 * 持有 {@link ElasticsearchOperations} 引用，通过 {@link #getOperations()} 获取底层操作对象。
 * 额外提供 {@link #page} 和 {@link #searchPage} 等便捷分页方法。
 */
@Getter
@RequiredArgsConstructor
public class EsClient {

    private final ElasticsearchOperations operations;

    public <T> Optional<SearchHit<T>> searchOne(Query query, Class<T> documentClass) {
        return Optional.ofNullable(operations.searchOne(query, documentClass));
    }

    public <T> SearchHits<T> search(Query query, Class<T> documentClass) {
        return operations.search(query, documentClass);
    }

    public <T> SearchPage<T> searchPage(Query query, Class<T> documentClass) {
        return toSearchPage(operations.search(query, documentClass), query.getPageable());
    }

    public <T> Page<T> page(Query query, Class<T> documentClass) {
        return toPage(operations.search(query, documentClass), query.getPageable());
    }

    private <T> SearchPage<T> toSearchPage(SearchHits<T> hits, Pageable pageable) {
        return SearchHitSupport.searchPageFor(hits, pageable);
    }

    private <T> Page<T> toPage(SearchHits<T> hits, Pageable pageable) {
        List<T> content = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();
        return new PageImpl<>(content, pageable, hits.getTotalHits());
    }
}
