package com.example.handPick.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderFeedbackDto {
    private Boolean onTime;   // true if delivery was on time, false otherwise
    private String comments;  // optional feedback comments
}