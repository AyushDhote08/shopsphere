package com.shopsphere.controller;

import com.shopsphere.dto.address.AddressRequestDTO;
import com.shopsphere.dto.address.AddressResponseDTO;
import com.shopsphere.dto.common.ApiResponse;
import com.shopsphere.security.SecurityUtils;
import com.shopsphere.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@Tag(name = "Customer Addresses", description = "Endpoints for managing customer shipping addresses")
public class AddressController {

    private final AddressService addressService;
    private final SecurityUtils securityUtils;

    public AddressController(AddressService addressService, SecurityUtils securityUtils) {
        this.addressService = addressService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    @Operation(summary = "Get my saved addresses", description = "Retrieves all saved shipping addresses for the authenticated customer")
    public ResponseEntity<ApiResponse<List<AddressResponseDTO>>> getMyAddresses() {
        Long userId = securityUtils.getCurrentUserId();
        List<AddressResponseDTO> addresses = addressService.getAddressesForUser(userId);
        return ResponseEntity.ok(ApiResponse.success(addresses));
    }

    @PostMapping
    @Operation(summary = "Create shipping address", description = "Adds a new shipping address for the authenticated customer")
    public ResponseEntity<ApiResponse<AddressResponseDTO>> createAddress(@Valid @RequestBody AddressRequestDTO request) {
        Long userId = securityUtils.getCurrentUserId();
        AddressResponseDTO address = addressService.createAddress(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Address created successfully", address));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get address by ID", description = "Retrieves a specific shipping address owned by the authenticated customer")
    public ResponseEntity<ApiResponse<AddressResponseDTO>> getAddressById(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        AddressResponseDTO address = addressService.getAddressById(userId, id);
        return ResponseEntity.ok(ApiResponse.success(address));
    }
}
