package com.secondstitch.item;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * What the frontend sends to create or update an item.
 * The annotations are checked automatically because the controller uses @Valid.
 */
public record ItemRequest(
        @NotBlank(message = "Give the piece a name") @Size(max = 80) String name,
        @NotNull(message = "Pick a category") Category category,
        @Size(max = 60) String brand,
        @Size(max = 20) String size,
        @Size(max = 40) String color,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Use a hex colour like #9CAF88") String colorHex,
        @NotNull(message = "Add what you paid") @PositiveOrZero BigDecimal pricePaid,
        @PositiveOrZero BigDecimal estimatedRetail,
        @Size(max = 80) String store,
        LocalDate dateFound,
        @Size(max = 1000) String notes
) {

    /** Copy the request's values onto an entity (used for both create and update). */
    void applyTo(Item item) {
        item.setName(name.trim());
        item.setCategory(category);
        item.setBrand(blankToNull(brand));
        item.setSize(blankToNull(size));
        item.setColor(blankToNull(color));
        item.setColorHex(blankToNull(colorHex));
        item.setPricePaid(pricePaid);
        item.setEstimatedRetail(estimatedRetail);
        item.setStore(blankToNull(store));
        item.setDateFound(dateFound);
        item.setNotes(blankToNull(notes));
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
