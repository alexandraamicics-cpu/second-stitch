package com.secondstitch.item;

import com.secondstitch.common.Money;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * What the API sends back. Separate from the entity so we control exactly
 * which fields leave the server, and can add computed ones like costPerWear.
 */
public record ItemResponse(
        Long id,
        String name,
        Category category,
        String brand,
        String size,
        String color,
        String colorHex,
        BigDecimal pricePaid,
        BigDecimal estimatedRetail,
        String store,
        LocalDate dateFound,
        int wearCount,
        String notes,
        String photoUrl,
        BigDecimal costPerWear,
        BigDecimal savings
) {

    public static ItemResponse from(Item item) {
        String photoUrl = item.getPhotoFilename() == null
                ? null
                // the ?v= part makes the browser fetch a new photo after you replace it
                : "/api/items/" + item.getId() + "/photo?v=" + item.getPhotoFilename().hashCode();
        return new ItemResponse(
                item.getId(),
                item.getName(),
                item.getCategory(),
                item.getBrand(),
                item.getSize(),
                item.getColor(),
                item.getColorHex(),
                item.getPricePaid(),
                item.getEstimatedRetail(),
                item.getStore(),
                item.getDateFound(),
                item.getWearCount(),
                item.getNotes(),
                photoUrl,
                Money.costPerWear(item.getPricePaid(), item.getWearCount()),
                Money.savings(item.getPricePaid(), item.getEstimatedRetail())
        );
    }
}
