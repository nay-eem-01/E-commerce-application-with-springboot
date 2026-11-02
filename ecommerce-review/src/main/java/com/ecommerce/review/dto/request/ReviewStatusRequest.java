package com.ecommerce.review.dto.request;

import com.ecommerce.review.enums.ReviewStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewStatusRequest {

	@NotNull(message = "Choose a status.")
	private ReviewStatus status;
}
