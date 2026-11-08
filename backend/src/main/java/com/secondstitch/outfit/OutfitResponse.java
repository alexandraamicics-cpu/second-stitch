package com.secondstitch.outfit;

import com.secondstitch.common.Money;
import com.secondstitch.item.Item;
import com.secondstitch.item.ItemResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * An outfit plus its price details: what the whole look cost you,
 * what it would cost new, and how much thrifting saved.
 */
public record OutfitResponse(
        Long id,
        String name,
        String occasion,
        int wearCount,
        List<ItemResponse> items,
        BigDecimal totalPaid,
        BigDecimal totalRetail,
        BigDecimal totalSaved
) {

    public static OutfitResponse from(Outfit outfit) {
        List<Item> pieces = outfit.getItems();
        return new OutfitResponse(
                outfit.getId(),
                outfit.getName(),
                outfit.getOccasion(),
                outfit.getWearCount(),
                pieces.stream().map(ItemResponse::from).toList(),
                sum(pieces, Item::getPricePaid),
                // Only pieces where you know the price new count towards this
                sum(pieces, Item::getEstimatedRetail),
                sum(pieces, item -> Money.savings(item.getPricePaid(), item.getEstimatedRetail()))
        );
    }

    private static BigDecimal sum(List<Item> pieces, Function<Item, BigDecimal> field) {
        return pieces.stream()
                .map(field)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
