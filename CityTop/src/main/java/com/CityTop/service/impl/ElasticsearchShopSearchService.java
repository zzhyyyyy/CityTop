package com.CityTop.service.impl;

import cn.hutool.core.util.StrUtil;
import com.CityTop.entity.Shop;
import com.CityTop.mapper.ShopMapper;
import com.CityTop.search.ShopSearchDocument;
import com.CityTop.search.ShopSearchRepository;
import com.CityTop.service.ShopSearchService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Full-text shop search backed by Elasticsearch. Enable it after the cluster is ready.
 */
@Service
@ConditionalOnProperty(prefix = "citytop.search.elasticsearch", name = "enabled", havingValue = "true")
public class ElasticsearchShopSearchService implements ShopSearchService {

    private final ShopSearchRepository shopSearchRepository;
    private final ShopMapper shopMapper;

    public ElasticsearchShopSearchService(ShopSearchRepository shopSearchRepository, ShopMapper shopMapper) {
        this.shopSearchRepository = shopSearchRepository;
        this.shopMapper = shopMapper;
    }

    @Override
    public List<Shop> search(String keyword, int current, int pageSize) {
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        if (StrUtil.isBlank(keyword)) {
            queryBuilder.must(QueryBuilders.matchAllQuery());
        } else {
            queryBuilder.must(QueryBuilders.multiMatchQuery(keyword, "name", "area", "address"));
        }
        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(queryBuilder)
                .withPageable(PageRequest.of(Math.max(current - 1, 0), pageSize))
                .build();
        return shopSearchRepository.search(query).getContent().stream()
                .map(ShopSearchDocument::toShop)
                .collect(Collectors.toList());
    }

    @Override
    public void index(Shop shop) {
        if (shop != null && shop.getId() != null) {
            shopSearchRepository.save(ShopSearchDocument.from(shop));
        }
    }

    @Override
    public void rebuildIndex() {
        List<ShopSearchDocument> documents = shopMapper.selectList(new QueryWrapper<Shop>()).stream()
                .map(ShopSearchDocument::from)
                .collect(Collectors.toList());
        shopSearchRepository.saveAll(documents);
    }
}
