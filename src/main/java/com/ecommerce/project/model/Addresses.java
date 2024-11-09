package com.ecommerce.project.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "addresses")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Addresses {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private Long addressID;

    @NotBlank
    @Size(min = 3, message = "Road name must consist minimum five characters")
    private String roadName;

    @NotBlank
    @Size(min = 4, message = "House name/number name must consist minimum eight characters")
    private String houseNameAndNumber;
    @NotBlank
    @Size(min = 2, message = "City name must consist minimum two characters")
    private String city;
    @NotBlank
    @Size(min = 2, message = "Country name must consist minimum four characters")
    private String country;


    @ManyToOne
    @JoinColumn(name = "user_id")
    private Users users;

    public Addresses(String roadName, String houseNameAndNumber, String city, String country) {
        this.roadName = roadName;
        this.houseNameAndNumber = houseNameAndNumber;
        this.city = city;
        this.country = country;
    }
}
