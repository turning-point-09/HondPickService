package com.example.handPick.controller;

import com.example.handPick.dto.OrderFeedbackDto;
import com.example.handPick.model.Order;
import com.example.handPick.service.OrderService;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    // Paginated order history for the logged-in user
    @GetMapping
    public Page<com.example.handPick.dto.OrderDto> getOrderHistory(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return orderService.getOrdersForUser(userDetails.getUsername(), pageable)
                .map(orderService::convertToDto);
    }

    // Admin: Get all orders (paged)
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public org.springframework.data.domain.Page<com.example.handPick.dto.OrderDto> getAllOrdersForAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return orderService.getAllOrders(pageable).map(orderService::convertToDto);
    }

    // Get a single order (detail view)
    @GetMapping("/{orderId}")
    public org.springframework.http.ResponseEntity<com.example.handPick.dto.OrderDto> getOrderDetail(
            @PathVariable Long orderId,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails
    ) {
        com.example.handPick.model.Order order = orderService.getOrderByIdForUser(orderId, userDetails.getUsername());
        return org.springframework.http.ResponseEntity.ok(orderService.convertToDto(order));
    }

    // Download invoice as PDF
    @GetMapping("/invoice/{orderId}")
    public void downloadInvoice(@PathVariable Long orderId, HttpServletResponse response) throws Exception {
        Order order = orderService.getOrderById(orderId);
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=invoice_" + orderId + ".pdf");

        Document document = new Document();
        PdfWriter.getInstance(document, response.getOutputStream());
        document.open();
        
        // Company Header
        com.lowagie.text.Font titleFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 18, com.lowagie.text.Font.BOLD);
        com.lowagie.text.Font headerFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 12, com.lowagie.text.Font.BOLD);
        com.lowagie.text.Font normalFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.NORMAL);
        
        Paragraph companyTitle = new Paragraph("HondPick Service", titleFont);
        companyTitle.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        document.add(companyTitle);
        
        Paragraph companySubtitle = new Paragraph("Your Trusted Handpick Service", normalFont);
        companySubtitle.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        document.add(companySubtitle);
        
        document.add(new Paragraph(" ")); // Spacing
        
        // Invoice Details
        Paragraph invoiceTitle = new Paragraph("INVOICE", headerFont);
        invoiceTitle.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        document.add(invoiceTitle);
        
        document.add(new Paragraph(" ")); // Spacing
        
        // Invoice and Order Information
        com.lowagie.text.pdf.PdfPTable infoTable = new com.lowagie.text.pdf.PdfPTable(2);
        infoTable.setWidthPercentage(100);
        
        infoTable.addCell(new com.lowagie.text.pdf.PdfPCell(new Paragraph("Invoice #: " + orderId, normalFont)));
        infoTable.addCell(new com.lowagie.text.pdf.PdfPCell(new Paragraph("Order Date: " + order.getOrderDate(), normalFont)));
        infoTable.addCell(new com.lowagie.text.pdf.PdfPCell(new Paragraph("Payment Method: " + order.getPaymentMethod(), normalFont)));
        infoTable.addCell(new com.lowagie.text.pdf.PdfPCell(new Paragraph("Payment Status: " + order.getPaymentStatus(), normalFont)));
        
        document.add(infoTable);
        document.add(new Paragraph(" ")); // Spacing
        
        // Customer Information
        Paragraph customerHeader = new Paragraph("Customer Information", headerFont);
        document.add(customerHeader);
        
        if (order.getUser() != null) {
            document.add(new Paragraph("Name: " + order.getUser().getFirstName() + " " + order.getUser().getLastName(), normalFont));
            document.add(new Paragraph("Mobile: " + order.getUser().getMobileNumber(), normalFont));
            if (order.getUser().getEmail() != null) {
                document.add(new Paragraph("Email: " + order.getUser().getEmail(), normalFont));
            }
        }
        
        document.add(new Paragraph(" ")); // Spacing
        
        // Shipping Address
        if (order.getShippingAddress() != null) {
            Paragraph shippingHeader = new Paragraph("Shipping Address", headerFont);
            document.add(shippingHeader);
            
            document.add(new Paragraph(order.getShippingAddress().getStreet(), normalFont));
            document.add(new Paragraph(order.getShippingAddress().getCity() + ", " + order.getShippingAddress().getState(), normalFont));
            document.add(new Paragraph(order.getShippingAddress().getPostalCode() + ", " + order.getShippingAddress().getCountry(), normalFont));
            
            document.add(new Paragraph(" ")); // Spacing
        }
        
        // Order Items Table
        Paragraph itemsHeader = new Paragraph("Order Items", headerFont);
        document.add(itemsHeader);
        
        com.lowagie.text.pdf.PdfPTable itemsTable = new com.lowagie.text.pdf.PdfPTable(5);
        itemsTable.setWidthPercentage(100);
        
        // Table Headers
        itemsTable.addCell(new com.lowagie.text.pdf.PdfPCell(new Paragraph("Product", headerFont)));
        itemsTable.addCell(new com.lowagie.text.pdf.PdfPCell(new Paragraph("Size", headerFont)));
        itemsTable.addCell(new com.lowagie.text.pdf.PdfPCell(new Paragraph("Quantity", headerFont)));
        itemsTable.addCell(new com.lowagie.text.pdf.PdfPCell(new Paragraph("Unit Price", headerFont)));
        itemsTable.addCell(new com.lowagie.text.pdf.PdfPCell(new Paragraph("Subtotal", headerFont)));
        
        // Table Data
        for (com.example.handPick.model.OrderItem item : order.getItems()) {
            itemsTable.addCell(new com.lowagie.text.pdf.PdfPCell(new Paragraph(item.getProductName(), normalFont)));
            itemsTable.addCell(new com.lowagie.text.pdf.PdfPCell(new Paragraph(item.getSize() != null ? item.getSize() : "-", normalFont)));
            itemsTable.addCell(new com.lowagie.text.pdf.PdfPCell(new Paragraph(String.valueOf(item.getQuantity()), normalFont)));
            itemsTable.addCell(new com.lowagie.text.pdf.PdfPCell(new Paragraph("₹" + item.getUnitPrice(), normalFont)));
            itemsTable.addCell(new com.lowagie.text.pdf.PdfPCell(new Paragraph("₹" + item.getSubtotal(), normalFont)));
        }
        
        document.add(itemsTable);
        document.add(new Paragraph(" ")); // Spacing
        
        // Total Amount
        Paragraph totalAmount = new Paragraph("Total Amount: ₹" + order.getTotalAmount(), headerFont);
        totalAmount.setAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
        document.add(totalAmount);
        
        if (order.getTransactionId() != null) {
            Paragraph transactionId = new Paragraph("Transaction ID: " + order.getTransactionId(), normalFont);
            transactionId.setAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
            document.add(transactionId);
        }
        
        document.add(new Paragraph(" ")); // Spacing
        
        // Footer
        Paragraph footer = new Paragraph("Thank you for choosing HondPick Service!", normalFont);
        footer.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        document.add(footer);
        
        document.close();
    }

    // Submit feedback for an order
    @PostMapping("/{orderId}/feedback")
    public ResponseEntity<?> submitOrderFeedback(
            @PathVariable Long orderId,
            @RequestBody OrderFeedbackDto feedbackDto,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        orderService.saveOrderFeedback(orderId, feedbackDto, userDetails.getUsername());
        return ResponseEntity.ok("Feedback submitted!");
    }

    // Admin: Update order status
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/admin/{orderId}/status")
    public org.springframework.http.ResponseEntity<com.example.handPick.dto.OrderDto> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody com.example.handPick.dto.OrderStatusUpdateDto statusUpdateDto
    ) {
        com.example.handPick.model.Order updatedOrder = orderService.updateOrderStatus(orderId, statusUpdateDto.getStatus());
        return org.springframework.http.ResponseEntity.ok(orderService.convertToDto(updatedOrder));
    }
}