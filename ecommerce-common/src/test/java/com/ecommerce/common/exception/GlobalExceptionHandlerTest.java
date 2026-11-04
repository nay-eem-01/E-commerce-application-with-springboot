package com.ecommerce.common.exception;

import com.ecommerce.common.model.Response;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

	static class Form {
		private String name;

		public String getName() {
			return name;
		}
	}

	/** The admin editor shows a validation message next to its field, so the response names the field. */
	@Test
	void validationErrorNamesTheField() throws Exception {
		BeanPropertyBindingResult result = new BeanPropertyBindingResult(new Form(), "productRequest");
		result.rejectValue("name", "NotBlank", "Enter a name.");
		MethodArgumentNotValidException e = new MethodArgumentNotValidException(
				new org.springframework.core.MethodParameter(Form.class.getDeclaredConstructor(), -1), result);

		ResponseEntity<Object> response = new GlobalExceptionHandler().handleMethodArgumentNotValid(
				e, new HttpHeaders(), HttpStatus.BAD_REQUEST, new ServletWebRequest(new MockHttpServletRequest()));

		Response body = (Response) response.getBody();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(body.getMessage()).isEqualTo("Enter a name.");
		assertThat(body.getPayload()).isEqualTo(Map.of("field", "name"));
	}
}
