package com.ecommerce.project.service;

import com.ecommerce.project.DTO.AddressDto;
import com.ecommerce.project.exceptionHandler.ResourceNotFoundException;
import com.ecommerce.project.model.Addresses;
import com.ecommerce.project.model.Users;
import com.ecommerce.project.repositories.AddressRepository;
import com.ecommerce.project.repositories.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final ModelMapper modelMapper;
    private final UserRepository userRepository;

    public AddressServiceImpl(AddressRepository addressRepository, ModelMapper modelMapper, UserRepository userRepository) {
        this.addressRepository = addressRepository;
        this.modelMapper = modelMapper;
        this.userRepository = userRepository;
    }

    @Override
    public AddressDto createNewAddress(AddressDto addressDto, Users users) {

        Addresses address = modelMapper.map(addressDto, Addresses.class);

        address.setUsers(users);
        List<Addresses> addressList = users.getAddresses();
        addressList.add(address);
        users.setAddresses(addressList);
        Addresses savedAddress = addressRepository.save(address);

        return modelMapper.map(savedAddress, AddressDto.class);
    }

    @Override
    public List<AddressDto> getUserAddress(Users user) {
        List<Addresses> addressesList = user.getAddresses();
        return addressesList
                .stream()
                .map(addresses -> modelMapper.map(addresses, AddressDto.class))
                .toList();
    }

    @Override
    public AddressDto getUserAddressById(Long addressId) {
        Addresses address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "addressId", addressId));
        return modelMapper.map(address, AddressDto.class);
    }

    @Override
    public List<AddressDto> getAllAddress() {
        List<Addresses> addressList = addressRepository.findAll();
        return addressList
                .stream()
                .map(addresses -> modelMapper.map(addresses, AddressDto.class))
                .toList();
    }

    @Override
    public AddressDto updateAddress(Long addressId, AddressDto addressDto) {
        Addresses addressFromDatabase = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "addressId", addressId));

        addressFromDatabase.setRoadName(addressDto.getRoadName());
        addressFromDatabase.setHouseNameAndNumber(addressDto.getHouseNameAndNumber());
        addressFromDatabase.setCity(addressDto.getCity());
        addressFromDatabase.setCountry(addressFromDatabase.getCountry());

        Addresses updatedAddress = addressRepository.save(addressFromDatabase);

        Users user = addressFromDatabase.getUsers();
        user.getAddresses().removeIf(addresses -> addresses.getAddressID().equals(addressId));
        user.getAddresses().add(updatedAddress);
        userRepository.save(user);


        return modelMapper.map(updatedAddress, AddressDto.class);
    }


    @Override
    public String deleteAddress(Long addressId) {
        Addresses addressFromDatabase = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "addressId", addressId));

        Users user = addressFromDatabase.getUsers();
        user.getAddresses().removeIf(addresses -> addresses.getAddressID().equals(addressId));
        userRepository.save(user);

        addressRepository.delete(addressFromDatabase);

        return "Address deleted successfully with addressId: " + addressId;
    }

}
