package com.CityTop.service.impl;

import com.CityTop.entity.Shop;
import com.CityTop.mapper.ShopMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MysqlShopSearchServiceTest {

    @Test
    void shouldReturnPagedRecordsFromFallbackSearch() {
        ShopMapper shopMapper = mock(ShopMapper.class);
        Shop shop = new Shop();
        shop.setId(1L);
        shop.setName("咖啡店");
        Page<Shop> expected = new Page<>(1, 10);
        expected.setRecords(Collections.singletonList(shop));
        when(shopMapper.selectPage(any(Page.class), any())).thenReturn(expected);

        MysqlShopSearchService service = new MysqlShopSearchService(shopMapper);

        assertEquals("咖啡店", service.search("咖啡", 1, 10).get(0).getName());
    }
}
