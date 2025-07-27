package com.example.handPick.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancellationDto {
    private String reason; // Optional reason for cancellation
} 