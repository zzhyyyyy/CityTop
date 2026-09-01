package com.CityTop.service;

import com.CityTop.entity.Shop;

import java.util.List;

/**
 * Shop full-text search abstraction. Implementations can use Elasticsearch or MySQL fallback.
 */
public interface ShopSearchService {

    List<Shop> search(String keyword, int current, int pageSize);

    void index(Shop shop);

    void rebuildIndex();
}
