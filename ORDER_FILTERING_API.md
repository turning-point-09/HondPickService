# Order Filtering, Sorting, and Date Range API Documentation

This document describes the enhanced order management API with advanced filtering, sorting, and date range functionality for both user and admin order pages.

## Overview

The order API now supports:
- **Advanced Sorting** by multiple fields with individual directions
- **Multiple Sort Fields** with comma-separated syntax
- **Field Aliases** for easier sorting
- **Filtering** by order status, date range, and month
- **Pagination** with customizable page size
- **JSON Body Requests** for complex filtering scenarios

## New Endpoints

### 1. Get Available Sorting Options
```
GET /api/v1/orders/sort-options
```
Returns all available sorting fields, aliases, and directions.

### 2. Get Sorting Examples
```
GET /api/v1/orders/sort-examples
```
Returns practical examples of how to use the sorting features.

## Enhanced Sorting Features

### Single Field Sorting
```
GET /api/v1/orders/filtered?sortBy=orderDate&sortDirection=asc
```

### Multiple Field Sorting
```
GET /api/v1/orders/filtered?sortBy=orderDate:desc,totalAmount:asc,status:asc
```

### JSON Body with Multiple Sort Fields
```json
POST /api/v1/orders/filtered
{
  "sortFields": "orderDate:desc,totalAmount:asc,status:asc",
  "page": 0,
  "size": 20
}
```

## Available Sort Fields

### Order Fields
| Field | Aliases | Description |
|-------|---------|-------------|
| `orderDate` | `date`, `created`, `createdat` | Order creation date |
| `totalAmount` | `amount`, `price`, `total` | Order total amount |
| `status` | `orderstatus` | Order status |
| `paymentMethod` | `method`, `paymethod` | Payment method |
| `paymentStatus` | `paystatus`, `paid` | Payment status |
| `id` | `orderid` | Order ID |
| `transactionId` | `txn`, `txnid` | Transaction ID |
| `feedbackComments` | `comments` | Feedback comments |
| `feedbackOnTime` | `ontime` | Feedback on-time status |

### User Fields (Admin Only)
| Field | Aliases | Description |
|-------|---------|-------------|
| `user.id` | `userid`, `customer`, `customerid` | User ID |
| `user.firstName` | `username`, `customername` | Customer name |
| `user.mobileNumber` | `mobile`, `phone`, `phonenumber` | Mobile number |

### Shipping Address Fields
| Field | Aliases | Description |
|-------|---------|-------------|
| `shippingAddress.city` | `city` | Shipping city |
| `shippingAddress.state` | `state` | Shipping state |
| `shippingAddress.postalCode` | `zip`, `zipcode` | Postal code |

## API Endpoints

### User Order Endpoints

#### 1. Basic Order History (Existing)
```
GET /api/v1/orders?page=0&size=10
```

#### 2. Enhanced Order History with Query Parameters
```
GET /api/v1/orders/filtered?page=0&size=10&status=PENDING&startDate=2024-01-01&endDate=2024-01-31&sortBy=orderDate&sortDirection=desc
```

#### 3. Enhanced Order History with JSON Body
```
POST /api/v1/orders/filtered
Content-Type: application/json

{
  "page": 0,
  "size": 10,
  "status": "PENDING",
  "startDate": "2024-01-01",
  "endDate": "2024-01-31",
  "sortFields": "orderDate:desc,totalAmount:asc",
  "sortDirection": "desc"
}
```

#### 4. Orders by Month
```
GET /api/v1/orders/month?page=0&size=10&year=2024&month=1&sortBy=orderDate&sortDirection=desc
```

### Admin Order Endpoints

#### 1. Basic Admin Orders (Existing)
```
GET /api/v1/orders/admin?page=0&size=10
```

#### 2. Enhanced Admin Orders with Query Parameters
```
GET /api/v1/orders/admin/filtered?page=0&size=10&status=PENDING&startDate=2024-01-01&endDate=2024-01-31&sortBy=orderDate&sortDirection=desc
```

#### 3. Enhanced Admin Orders with JSON Body
```
POST /api/v1/orders/admin/filtered
Content-Type: application/json

{
  "page": 0,
  "size": 10,
  "status": "PENDING",
  "startDate": "2024-01-01",
  "endDate": "2024-01-31",
  "sortFields": "orderDate:desc,totalAmount:asc,user.firstName:asc"
}
```

#### 4. Admin Orders by Month
```
GET /api/v1/orders/admin/month?page=0&size=10&year=2024&month=1&sortBy=orderDate&sortDirection=desc
```

## Advanced Sorting Examples

### Example 1: Sort by Order Date Ascending
```bash
curl -X GET "http://localhost:8081/api/v1/orders/filtered?sortBy=orderDate&sortDirection=asc" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Example 2: Multiple Field Sorting
```bash
curl -X GET "http://localhost:8081/api/v1/orders/filtered?sortBy=orderDate:desc,totalAmount:asc,status:asc" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Example 3: Using Field Aliases
```bash
curl -X GET "http://localhost:8081/api/v1/orders/filtered?sortBy=date:desc,amount:asc" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Example 4: Admin - Sort by Customer Name and Order Date
```bash
curl -X POST "http://localhost:8081/api/v1/orders/admin/filtered" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "sortFields": "user.firstName:asc,orderDate:desc",
    "page": 0,
    "size": 20
  }'
```

### Example 5: Complex Filtering with Multiple Sort Fields
```bash
curl -X POST "http://localhost:8081/api/v1/orders/filtered" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "PENDING",
    "startDate": "2024-01-01",
    "endDate": "2024-01-31",
    "sortFields": "totalAmount:desc,orderDate:desc,status:asc",
    "page": 0,
    "size": 15
  }'
```

## Filter Options

### Order Status Values
- `PENDING`: Orders that are pending
- `COMPLETED`: Orders that are completed
- `CANCELLED`: Orders that are cancelled
- `SHIPPED`: Orders that are shipped
- `DELIVERED`: Orders that are delivered

### Sort Directions
- `asc`: Ascending order
- `desc`: Descending order (default)

## Multiple Sort Fields Syntax

### Format
```
field1:direction1,field2:direction2,field3:direction3
```

### Examples
- `orderDate:desc,totalAmount:asc` - Sort by order date descending, then by total amount ascending
- `status:asc,orderDate:desc` - Sort by status ascending, then by order date descending
- `user.firstName:asc,orderDate:desc,totalAmount:desc` - Sort by customer name, then date, then amount

### Rules
1. Fields are separated by commas
2. Each field can have its own direction (field:direction)
3. If no direction is specified for a field, the default sortDirection is used
4. Order matters - first field is primary sort, second is secondary, etc.

## Response Format

All endpoints return a paginated response with the following structure:

```json
{
  "content": [
    {
      "id": 1,
      "items": [...],
      "totalAmount": 1500.00,
      "status": "PENDING",
      "paymentMethod": "COD",
      "paymentStatus": "PENDING",
      "transactionId": null,
      "shippingAddress": {...},
      "orderDate": "2024-01-15T10:30:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": {
      "sorted": true,
      "unsorted": false
    }
  },
  "totalElements": 25,
  "totalPages": 3,
  "last": false,
  "first": true,
  "numberOfElements": 10
}
```

## Error Handling

- **400 Bad Request**: Invalid filter parameters, invalid sort fields, or invalid date format
- **401 Unauthorized**: Missing or invalid JWT token
- **403 Forbidden**: Admin endpoints accessed without admin role
- **500 Internal Server Error**: Server-side errors

## Validation

The `OrderFilterRequest` DTO includes validation:
- Page number must be 0 or greater
- Page size must be between 1 and 100
- Sort direction must be "asc" or "desc"

## Notes

1. **Date Format**: Use YYYY-MM-DD format for dates (e.g., "2024-01-15")
2. **Time Zones**: All dates are handled in the server's local timezone
3. **Default Sorting**: If no sort parameters are provided, orders are sorted by orderDate in descending order (newest first)
4. **Pagination**: Page numbers start from 0
5. **Case Insensitive**: Status, sort field names, and directions are case-insensitive
6. **Combined Filters**: You can combine status, date range, and sorting filters
7. **Month Filtering**: The month endpoints automatically set the date range to cover the entire specified month
8. **Field Aliases**: Use aliases for shorter, more intuitive field names
9. **Multiple Sort Fields**: Combine multiple sort criteria for complex sorting scenarios

## Frontend Integration

For frontend applications, you can use either:
- **GET endpoints with query parameters** for simple filtering
- **POST endpoints with JSON body** for complex filter combinations

The JSON body approach is recommended for complex filtering scenarios as it's more readable and maintainable.

### Frontend Example (JavaScript)
```javascript
// Get sorting options
const sortOptions = await fetch('/api/v1/orders/sort-options').then(r => r.json());

// Apply complex filtering
const filterRequest = {
  status: 'PENDING',
  startDate: '2024-01-01',
  endDate: '2024-01-31',
  sortFields: 'orderDate:desc,totalAmount:asc',
  page: 0,
  size: 20
};

const orders = await fetch('/api/v1/orders/filtered', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  },
  body: JSON.stringify(filterRequest)
}).then(r => r.json());
``` 