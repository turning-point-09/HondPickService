package com.example.handPick.dto;

import java.util.List;

public class ProductPageResponse {
    private List<ProductDto> products;
    private int totalPages;
    private long totalElements;
    private int currentPage;
    private int pageSize;

    public ProductPageResponse() {}

    public ProductPageResponse(List<ProductDto> products, int totalPages, long totalElements, int currentPage, int pageSize) {
        this.products = products;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
    }

    public List<ProductDto> getProducts() {
        return products;
    }

    public void setProducts(List<ProductDto> products) {
        this.products = products;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
} 