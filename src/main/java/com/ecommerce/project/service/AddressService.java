package com.ecommerce.project.service;

import com.ecommerce.project.DTO.AddressDto;
import com.ecommerce.project.model.Users;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface AddressService {

    AddressDto createNewAddress(AddressDto addressDto, Users users);

    List<AddressDto> getUserAddress(Users user);

    AddressDto getUserAddressById(Long addressId);

    List<AddressDto> getAllAddress();

    AddressDto updateAddress(Long addressId, AddressDto addressDto);

    String deleteAddress(Long addressId);
}
