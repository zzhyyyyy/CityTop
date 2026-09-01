package com.CityTop.search;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ShopSearchRepository extends ElasticsearchRepository<ShopSearchDocument, Long> {
}
