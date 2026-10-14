package com.shopsphere.service.impl;

import com.shopsphere.dto.address.AddressRequestDTO;
import com.shopsphere.dto.address.AddressResponseDTO;
import com.shopsphere.entity.Address;
import com.shopsphere.entity.User;
import com.shopsphere.exception.BadRequestException;
import com.shopsphere.exception.ResourceNotFoundException;
import com.shopsphere.repository.AddressRepository;
import com.shopsphere.repository.UserRepository;
import com.shopsphere.service.AddressService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public AddressServiceImpl(AddressRepository addressRepository, UserRepository userRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponseDTO> getAddressesForUser(Long userId) {
        return addressRepository.findByUserId(userId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AddressResponseDTO getAddressById(Long userId, Long addressId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));
        return mapToDTO(address);
    }

    @Override
    public AddressResponseDTO createAddress(Long userId, AddressRequestDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // If this is the user's first address or marked default, update others
        List<Address> existing = addressRepository.findByUserId(userId);
        boolean isFirst = existing.isEmpty();
        boolean shouldBeDefault = isFirst || request.isDefault();

        if (shouldBeDefault) {
            existing.forEach(a -> a.setDefault(false));
            addressRepository.saveAll(existing);
        }

        Address address = new Address(
                user,
                request.getRecipientName().trim(),
                request.getPhone().trim(),
                request.getStreetAddress().trim(),
                request.getCity().trim(),
                request.getState().trim(),
                request.getPostalCode().trim(),
                request.getCountry() != null ? request.getCountry().trim() : "United States",
                shouldBeDefault
        );

        Address saved = addressRepository.save(address);
        return mapToDTO(saved);
    }

    @Override
    public Address resolveOrCreateAddress(Long userId, Long existingAddressId, AddressRequestDTO newAddressDTO) {
        if (existingAddressId != null) {
            return addressRepository.findByIdAndUserId(existingAddressId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Address", "id", existingAddressId));
        }

        if (newAddressDTO != null) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

            Address address = new Address(
                    user,
                    newAddressDTO.getRecipientName().trim(),
                    newAddressDTO.getPhone().trim(),
                    newAddressDTO.getStreetAddress().trim(),
                    newAddressDTO.getCity().trim(),
                    newAddressDTO.getState().trim(),
                    newAddressDTO.getPostalCode().trim(),
                    newAddressDTO.getCountry() != null ? newAddressDTO.getCountry().trim() : "United States",
                    newAddressDTO.isDefault()
            );
            return addressRepository.save(address);
        }

        throw new BadRequestException("Either an existing address ID or new shipping address details must be provided");
    }

    private AddressResponseDTO mapToDTO(Address address) {
        AddressResponseDTO dto = new AddressResponseDTO();
        dto.setId(address.getId());
        dto.setRecipientName(address.getRecipientName());
        dto.setPhone(address.getPhone());
        dto.setStreetAddress(address.getStreetAddress());
        dto.setCity(address.getCity());
        dto.setState(address.getState());
        dto.setPostalCode(address.getPostalCode());
        dto.setCountry(address.getCountry());
        dto.setDefault(address.isDefault());
        return dto;
    }
}
