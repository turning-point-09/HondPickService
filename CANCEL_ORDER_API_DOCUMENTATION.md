# Cancel Order API Documentation

## Overview
The cancel order feature allows users to cancel their own orders and admins to cancel any order. The system automatically handles stock restoration and refund processing.

## API Endpoints

### 1. Cancel Order (User/Admin)
**POST** `/api/v1/orders/{orderId}/cancel`

**Description:** Cancel an order. Users can only cancel their own orders, while admins can cancel any order.

**Authentication:** Required (JWT token)

**Path Parameters:**
- `orderId` (Long, required): The ID of the order to cancel

**Request Body:**
```json
{
  "reason": "Changed my mind" // Optional reason for cancellation
}
```

**Response:**
```json
{
  "id": 123,
  "items": [
    {
      "id": 1,
      "productId": 456,
      "productName": "Organic Tomatoes",
      "quantity": 2,
      "unitPrice": 50.00,
      "totalPrice": 100.00,
      "size": "1kg"
    }
  ],
  "totalAmount": 118.00,
  "status": "CANCELLED",
  "paymentMethod": "COD",
  "paymentStatus": "PENDING",
  "transactionId": null,
  "shippingAddress": {
    "street": "123 Main St",
    "city": "Mumbai",
    "state": "Maharashtra",
    "postalCode": "400001",
    "country": "India"
  },
  "orderDate": "2024-01-15T10:30:00"
}
```

**Error Responses:**
- `400 Bad Request`: Order cannot be cancelled (wrong status or access denied)
- `401 Unauthorized`: Authentication required
- `404 Not Found`: Order not found
- `500 Internal Server Error`: Server error

## Business Rules

### Order Cancellation Rules
1. **User Permissions:**
   - Regular users can only cancel their own orders
   - Admins can cancel any order

2. **Order Status Requirements:**
   - Only `PENDING` and `COMPLETED` orders can be cancelled
   - `SHIPPED` and `DELIVERED` orders cannot be cancelled

3. **Automatic Actions on Cancellation:**
   - Order status changes to `CANCELLED`
   - Stock quantities are restored to products
   - Payment status changes to `REFUNDED` if payment was completed
   - Admin notification is sent (email + database)

### Stock Management
- When an order is cancelled, the stock quantities are automatically restored
- Stock restoration considers items in active carts
- Failed stock restoration doesn't prevent order cancellation

### Payment Processing
- For COD orders: No refund processing needed
- For online payments: Payment status updated to `REFUNDED`
- Actual refund processing should be integrated with payment gateway

## Admin Notifications

### Database Notification
```json
{
  "id": 1,
  "title": "Order Cancelled",
  "message": "Order #123 cancelled by John Doe (9876543210). Reason: Changed my mind. Amount: ₹118.00",
  "type": "ORDER_CANCELLED",
  "isRead": false,
  "createdAt": "2024-01-15T10:35:00",
  "orderId": 123,
  "customerName": "John Doe",
  "customerMobile": "9876543210"
}
```

### Email Notification
Subject: `Order Cancelled #123 - HandPick`

Content:
```
❌ ORDER CANCELLATION ALERT!

Order ID: 123
Customer: John Doe (9876543210)
Amount: ₹118.00
Reason: Changed my mind

Please process the cancellation and refund if applicable.
```

## Usage Examples

### JavaScript/TypeScript
```javascript
// Cancel an order
const cancelOrder = async (orderId, reason) => {
  try {
    const response = await fetch(`/api/v1/orders/${orderId}/cancel`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${jwtToken}`
      },
      body: JSON.stringify({ reason })
    });
    
    if (response.ok) {
      const cancelledOrder = await response.json();
      console.log('Order cancelled:', cancelledOrder);
    } else {
      console.error('Failed to cancel order');
    }
  } catch (error) {
    console.error('Error cancelling order:', error);
  }
};

// Usage
cancelOrder(123, "Changed my mind");
```

### cURL
```bash
curl -X POST \
  http://localhost:8081/api/v1/orders/123/cancel \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer YOUR_JWT_TOKEN' \
  -d '{
    "reason": "Changed my mind"
  }'
```

## Integration Notes

### Frontend Integration
1. Add cancel button to order details page
2. Show cancellation confirmation dialog
3. Display cancellation reason input field
4. Update order status display after cancellation
5. Show success/error messages

### Admin Dashboard
1. Display cancelled orders in admin order list
2. Show cancellation notifications
3. Track cancellation reasons for analytics
4. Monitor refund processing status

### Error Handling
- Handle network errors gracefully
- Show appropriate error messages to users
- Log cancellation attempts for audit trail
- Implement retry mechanism for failed cancellations

## Security Considerations

1. **Authentication:** All requests require valid JWT token
2. **Authorization:** Users can only cancel their own orders
3. **Validation:** Order status validation prevents invalid cancellations
4. **Audit Trail:** All cancellations are logged with user and reason
5. **Rate Limiting:** Consider implementing rate limiting for cancellation requests

## Testing Scenarios

### Valid Cancellations
- User cancels their own pending order
- User cancels their own completed order
- Admin cancels any order
- Cancellation with reason
- Cancellation without reason

### Invalid Cancellations
- User tries to cancel another user's order
- Cancelling shipped order
- Cancelling delivered order
- Cancelling non-existent order
- Cancelling already cancelled order

### Edge Cases
- Network failure during cancellation
- Stock restoration failure
- Email notification failure
- Concurrent cancellation attempts 