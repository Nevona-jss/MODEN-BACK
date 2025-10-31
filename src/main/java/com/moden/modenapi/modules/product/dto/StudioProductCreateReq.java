package com.moden.modenapi.modules.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Schema(description = "Product creation request (Studio adds a product)")
public record StudioProductCreateReq(

        @NotBlank
        @Size(max = 150)
        @Schema(description = "Product fullName", example = "L'Oréal Professional Shampoo")
        String name,

        @Schema(description = "Product category", example = "Haircare")
        String category,

        @Schema(description = "Product type", example = "Shampoo")
        String type,

        @NotNull
        @DecimalMin(value = "0.00")
        @Schema(description = "Product price", example = "35.50")
        BigDecimal price,

        @PositiveOrZero
        @Schema(description = "Initial stock quantity", example = "20")
        int stock,

        @Schema(description = "Image URL for product", example = "https://cdn.moden.com/products/shampoo.jpg")
        String image
) {}
