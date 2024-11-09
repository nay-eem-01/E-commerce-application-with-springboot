package com.ecommerce.project.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "user_name"),
                @UniqueConstraint(columnNames = "user_email")
        })
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;
    @NotBlank
    @Size(max = 20)
    @Column(name = "user_name")
    private String userName;
    @NotBlank
    @Size(max = 50)
    @Email
    @Column(name = "user_email")
    private String userEmail;
    @NotBlank
    @Size(max = 120)
    @Column(name = "user_password")
    private String userPassword;

    public Users(String userName, String userEmail, String userPassword) {
        this.userName = userName;
        this.userEmail = userEmail;
        this.userPassword = userPassword;
    }
    @Getter
    @Setter
    @ManyToMany(cascade = {
            CascadeType.PERSIST,
            CascadeType.MERGE},
            fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Roles> roles = new HashSet<>();

    @Getter
    @Setter
    @OneToMany(mappedBy = "users",
            cascade = {
            CascadeType.PERSIST, CascadeType.MERGE},
            orphanRemoval = true)
    private List<Addresses> addresses = new ArrayList<>();

    @ToString.Exclude
    @OneToMany(mappedBy = "users",
                cascade = {CascadeType.PERSIST,CascadeType.MERGE},
                orphanRemoval = true)
    private Set<Product> products = new HashSet<>();

    @ToString.Exclude
    @OneToOne(mappedBy = "users",cascade = {CascadeType.PERSIST,CascadeType.MERGE},orphanRemoval = true)
    private Cart cart;

}
