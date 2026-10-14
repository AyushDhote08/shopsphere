package com.shopsphere.dto.order;

import com.shopsphere.dto.address.AddressRequestDTO;
import com.shopsphere.entity.PaymentMethod;
import jakarta.validation.Valid;

public class CheckoutRequestDTO {

    private Long shippingAddressId;

    @Valid
    private AddressRequestDTO newShippingAddress;

    private PaymentMethod paymentMethod = PaymentMethod.SIMULATED_CARD;

    /**
     * For development testing: "SUCCESS" (default) or "FAILED".
     */
    private String simulationOutcome = "SUCCESS";

    private String idempotencyKey;

    public CheckoutRequestDTO() {
    }

    public CheckoutRequestDTO(Long shippingAddressId, PaymentMethod paymentMethod, String simulationOutcome, String idempotencyKey) {
        this.shippingAddressId = shippingAddressId;
        this.paymentMethod = paymentMethod != null ? paymentMethod : PaymentMethod.SIMULATED_CARD;
        this.simulationOutcome = simulationOutcome != null ? simulationOutcome : "SUCCESS";
        this.idempotencyKey = idempotencyKey;
    }

    public Long getShippingAddressId() {
        return shippingAddressId;
    }

    public void setShippingAddressId(Long shippingAddressId) {
        this.shippingAddressId = shippingAddressId;
    }

    public AddressRequestDTO getNewShippingAddress() {
        return newShippingAddress;
    }

    public void setNewShippingAddress(AddressRequestDTO newShippingAddress) {
        this.newShippingAddress = newShippingAddress;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getSimulationOutcome() {
        return simulationOutcome;
    }

    public void setSimulationOutcome(String simulationOutcome) {
        this.simulationOutcome = simulationOutcome;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
