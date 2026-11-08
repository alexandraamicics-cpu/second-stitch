package com.secondstitch.stats;

import com.secondstitch.item.Category;
import com.secondstitch.item.ItemResponse;

import java.math.BigDecimal;
import java.util.List;

public record StatsResponse(
        int totalItems,
        int totalWears,
        BigDecimal totalPaid,
        BigDecimal totalRetailValue,
        BigDecimal totalSaved,
        BigDecimal averageCostPerWear,
        ItemResponse mostWorn,
        List<ItemResponse> waitingToBeWorn,
        List<CategoryCount> byCategory,
        List<StoreCount> topStores
) {

    public record CategoryCount(Category category, long count, BigDecimal paid) {
    }

    public record StoreCount(String store, long count) {
    }
}
