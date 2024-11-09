package com.ecommerce.project.Security.Request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LogInRequest {
    @NotBlank
    private String userName;
    @NotBlank
    private String password;
}
