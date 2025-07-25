// ReviewReplyDto.java
package com.example.handPick.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class ReviewReplyDto {
    @NotNull
    private String reply;
}