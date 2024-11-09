package com.ecommerce.project.Security.Response;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@NoArgsConstructor
public class UserResponse {
    private Long id;
    private String jwtToken;
    private String userName;
    private List<String> roles;

    public UserResponse(Long id, String jwtToken, String userName, List<String> roles) {
        this.id = id;
        this.jwtToken = jwtToken;
        this.userName = userName;
        this.roles = roles;
    }

    public UserResponse(Long id, String userName, List<String> roles) {
        this.id = id;
        this.userName = userName;
        this.roles = roles;
    }
}
