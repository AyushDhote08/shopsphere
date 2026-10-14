package com.shopsphere.exception;

public class InsufficientStockException extends RuntimeException {

    private final String productSku;
    private final int requested;
    private final int available;

    public InsufficientStockException(String productName, String productSku, int requested, int available) {
        super(String.format("Insufficient stock for '%s' (SKU: %s). Requested: %d, Available: %d",
                productName, productSku, requested, available));
        this.productSku = productSku;
        this.requested = requested;
        this.available = available;
    }

    public String getProductSku() {
        return productSku;
    }

    public int getRequested() {
        return requested;
    }

    public int getAvailable() {
        return available;
    }
}
