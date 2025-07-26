package com.example.handPick.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Data
public class UserStatsDto {
    private long totalUsers;
    private long activeUsers;
    private long inactiveUsers;
    private long suspendedUsers;
    private long newUsersThisMonth;
    private BigDecimal monthlyRevenue;
    private BigDecimal totalRevenue;
    // Order statistics
    private long monthlyPendingOrders;
    private long monthlyDeliveredOrders;
    private long totalPendingOrders;
    private long totalDeliveredOrders;
    private Map<String, Long> usersByRole;
} 