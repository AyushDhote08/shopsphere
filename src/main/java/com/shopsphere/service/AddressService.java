package com.shopsphere.service;

import com.shopsphere.dto.address.AddressRequestDTO;
import com.shopsphere.dto.address.AddressResponseDTO;
import com.shopsphere.entity.Address;

import java.util.List;

public interface AddressService {

    List<AddressResponseDTO> getAddressesForUser(Long userId);

    AddressResponseDTO getAddressById(Long userId, Long addressId);

    AddressResponseDTO createAddress(Long userId, AddressRequestDTO request);

    Address resolveOrCreateAddress(Long userId, Long existingAddressId, AddressRequestDTO newAddressDTO);
}
