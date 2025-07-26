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
        document.add(new Paragraph("Invoice for Order #" + orderId));
        document.add(new Paragraph("Order Date: " + order.getOrderDate()));
        document.add(new Paragraph("Total: " + order.getTotalAmount()));
        // Add more order details/items as needed
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
}