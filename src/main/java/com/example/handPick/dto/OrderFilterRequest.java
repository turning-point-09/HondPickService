package com.example.handPick.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderFilterRequest {
    @Min(value = 0, message = "Page number must be 0 or greater")
    private Integer page = 0;
    
    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size cannot exceed 100")
    private Integer size = 10;
    
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    
    // Single sort field (backward compatibility)
    private String sortBy = "orderDate";
    
    // Multiple sort fields (comma-separated, e.g., "orderDate:desc,totalAmount:asc")
    private String sortFields;
    
    // Default sort direction (used when sortFields doesn't specify direction)
    private String sortDirection = "desc";
    
    private Integer year;
    private Integer month;
    
    // Helper method to get the effective sort fields
    public String getEffectiveSortFields() {
        if (sortFields != null && !sortFields.trim().isEmpty()) {
            return sortFields;
        }
        return sortBy;
    }
    
    // Helper method to validate sort direction
    public boolean isValidSortDirection(String direction) {
        return direction != null && (direction.equalsIgnoreCase("asc") || direction.equalsIgnoreCase("desc"));
    }
} 