// ReviewRequestDto.java
package com.example.handPick.dto;

import lombok.Data;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Data
public class ReviewRequestDto {
    @NotNull
    private Long productId;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    private String comment;
}