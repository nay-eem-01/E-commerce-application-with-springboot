package com.ecommerce.project.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class AddressDto {
    private Long addressID;
    private String roadName;
    private String houseNameAndNumber;
    private String city;
    private String country;
}
