package com.CityTop.service.impl;

import cn.hutool.core.util.StrUtil;
import com.CityTop.entity.Shop;
import com.CityTop.mapper.ShopMapper;
import com.CityTop.service.ShopSearchService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Safe fallback used when an Elasticsearch cluster is not configured.
 */
@Service
@ConditionalOnProperty(prefix = "citytop.search.elasticsearch", name = "enabled", havingValue = "false", matchIfMissing = true)
public class MysqlShopSearchService implements ShopSearchService {

    private final ShopMapper shopMapper;

    public MysqlShopSearchService(ShopMapper shopMapper) {
        this.shopMapper = shopMapper;
    }

    @Override
    public List<Shop> search(String keyword, int current, int pageSize) {
        QueryWrapper<Shop> query = new QueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            query.and(wrapper -> wrapper.like("name", keyword)
                    .or().like("area", keyword)
                    .or().like("address", keyword));
        }
        Page<Shop> page = shopMapper.selectPage(new Page<>(Math.max(current, 1), pageSize), query);
        return page.getRecords();
    }

    @Override
    public void index(Shop shop) {
        // MySQL is the source of truth in fallback mode.
    }

    @Override
    public void rebuildIndex() {
        // MySQL is queried directly in fallback mode.
    }
}
