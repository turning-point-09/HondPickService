// ReviewDto.java
package com.example.handPick.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReviewDto {
    private Long id;
    private Long productId;
    private Long userId;
    private String userName;
    private Integer rating;
    private String comment;
    private String adminReply;
    private LocalDateTime createdAt;
}



