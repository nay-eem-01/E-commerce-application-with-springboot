package com.ecommerce.project.controller;

import com.ecommerce.project.DTO.AddressDto;
import com.ecommerce.project.Util.AuthUtil;
import com.ecommerce.project.model.Users;
import com.ecommerce.project.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AddressController {

    private final AddressService addressService;
    private final AuthUtil authUtil;

    public AddressController(AddressService addressService, AuthUtil authUtil) {
        this.addressService = addressService;
        this.authUtil = authUtil;
    }

    @PostMapping("/address/add")
    public ResponseEntity<AddressDto> createAddress(@Valid @RequestBody AddressDto addressDto) {
        Users users = authUtil.LoggedInUser();

        AddressDto savedAddressDto = addressService.createNewAddress(addressDto, users);
        return new ResponseEntity<>(savedAddressDto, HttpStatus.CREATED);
    }
    @GetMapping("/address/users/getAddress")
    public ResponseEntity<List<AddressDto>> getUserAddress(){

        Users user = authUtil.LoggedInUser();
        List<AddressDto> addressList = addressService.getUserAddress(user);
        return new ResponseEntity<>(addressList,HttpStatus.OK);
    }
    @GetMapping("/address/users/getAddress/id/{addressId}")
    public ResponseEntity<AddressDto> getAddressById(@PathVariable Long addressId){

        AddressDto addressDto = addressService.getUserAddressById(addressId);
        return new ResponseEntity<>(addressDto,HttpStatus.OK);
    }
    @GetMapping("/address/all")
    public ResponseEntity<List<AddressDto>> getAllAddress(){

        List<AddressDto> addressDtoList = addressService.getAllAddress();
        return new ResponseEntity<>(addressDtoList,HttpStatus.OK);
    }
    @PutMapping("/address/update/{addressId}")
    public ResponseEntity<AddressDto> updateAddress(@PathVariable Long addressId, @RequestBody AddressDto addressDto){

        AddressDto updatedAddress = addressService.updateAddress(addressId,addressDto);
        return new ResponseEntity<>(updatedAddress,HttpStatus.OK);
    }
    @DeleteMapping("/address/delete/{addressId}")
    public ResponseEntity<String> deleteAddress(@PathVariable Long addressId){
        String status = addressService.deleteAddress(addressId);
        return new ResponseEntity<>(status,HttpStatus.OK);
    }
}
