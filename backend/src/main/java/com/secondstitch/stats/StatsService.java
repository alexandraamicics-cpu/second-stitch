package com.secondstitch.stats;

import com.secondstitch.common.Money;
import com.secondstitch.item.Category;
import com.secondstitch.item.Item;
import com.secondstitch.item.ItemRepository;
import com.secondstitch.item.ItemResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Works out the numbers for the Stats page.
 * The closet is small (hundreds of items, not millions), so doing this in Java
 * is simple and fast. With a big dataset you'd move it into SQL queries.
 */
@Service
public class StatsService {

    private final ItemRepository items;

    public StatsService(ItemRepository items) {
        this.items = items;
    }

    @Transactional(readOnly = true)
    public StatsResponse summary() {
        List<Item> all = items.findAll();

        BigDecimal totalPaid = sum(all.stream().map(Item::getPricePaid).toList());
        BigDecimal totalRetail = sum(all.stream().map(Item::getEstimatedRetail).filter(Objects::nonNull).toList());
        BigDecimal totalSaved = sum(all.stream().map(i -> Money.savings(i.getPricePaid(), i.getEstimatedRetail())).toList());
        int totalWears = all.stream().mapToInt(Item::getWearCount).sum();

        BigDecimal avgCostPerWear = all.isEmpty()
                ? BigDecimal.ZERO.setScale(2)
                : sum(all.stream().map(i -> Money.costPerWear(i.getPricePaid(), i.getWearCount())).toList())
                    .divide(BigDecimal.valueOf(all.size()), 2, RoundingMode.HALF_UP);

        ItemResponse mostWorn = all.stream()
                .filter(i -> i.getWearCount() > 0)
                .max(Comparator.comparingInt(Item::getWearCount))
                .map(ItemResponse::from)
                .orElse(null);

        List<ItemResponse> waiting = all.stream()
                .filter(i -> i.getWearCount() == 0)
                .sorted(Comparator.comparing(Item::getCreatedAt))
                .limit(3)
                .map(ItemResponse::from)
                .toList();

        Map<Category, List<Item>> grouped = all.stream().collect(Collectors.groupingBy(Item::getCategory));
        List<StatsResponse.CategoryCount> byCategory = grouped.entrySet().stream()
                .map(e -> new StatsResponse.CategoryCount(
                        e.getKey(),
                        e.getValue().size(),
                        sum(e.getValue().stream().map(Item::getPricePaid).toList())))
                .sorted(Comparator.comparingLong(StatsResponse.CategoryCount::count).reversed())
                .toList();

        List<StatsResponse.StoreCount> topStores = all.stream()
                .map(Item::getStore)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()))
                .entrySet().stream()
                .map(e -> new StatsResponse.StoreCount(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(StatsResponse.StoreCount::count).reversed()
                        .thenComparing(StatsResponse.StoreCount::store))
                .limit(3)
                .toList();

        return new StatsResponse(
                all.size(), totalWears, totalPaid, totalRetail, totalSaved,
                avgCostPerWear, mostWorn, waiting, byCategory, topStores);
    }

    private static BigDecimal sum(List<BigDecimal> values) {
        return values.stream()
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
