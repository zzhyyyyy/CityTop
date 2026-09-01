package com.CityTop.job;

import com.CityTop.service.ShopSearchService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ShopSearchJobHandlerTest {

    @Test
    void shouldDelegateIndexRebuildToSearchService() {
        ShopSearchService shopSearchService = mock(ShopSearchService.class);
        ShopSearchJobHandler handler = new ShopSearchJobHandler(shopSearchService);

        handler.rebuildShopSearchIndex();

        verify(shopSearchService).rebuildIndex();
    }
}
