package com.secondstitch.outfit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record OutfitRequest(
        @NotBlank(message = "Name your outfit") @Size(max = 80) String name,
        @Size(max = 60) String occasion,
        @NotEmpty(message = "Pick at least one piece") List<Long> itemIds
) {
}
