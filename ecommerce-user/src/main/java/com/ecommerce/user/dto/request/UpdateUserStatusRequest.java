package com.ecommerce.user.dto.request;

import com.ecommerce.user.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserStatusRequest {

	@NotNull(message = "Choose a status.")
	private UserStatus status;
}
