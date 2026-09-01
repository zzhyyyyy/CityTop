package com.CityTop.job;

import com.CityTop.service.ShopSearchService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Handlers registered in xxl-job-admin after the executor is enabled.
 */
@Component
@Slf4j
public class ShopSearchJobHandler {

    private final ShopSearchService shopSearchService;

    public ShopSearchJobHandler(ShopSearchService shopSearchService) {
        this.shopSearchService = shopSearchService;
    }

    /**
     * Use this handler for a nightly consistency rebuild after Elasticsearch is enabled.
     */
    @XxlJob("shopSearchRebuildJob")
    public void rebuildShopSearchIndex() {
        log.info("XXL-JOB starts rebuilding the shop search index");
        shopSearchService.rebuildIndex();
        log.info("XXL-JOB completed rebuilding the shop search index");
    }
}
