package com.ecommerce.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * The one response envelope every endpoint returns:
 * {@code { "status": "OK", "message": "...", "payload": {...}, "success": true }}.
 * {@code message} is always safe to show to a shopper; it never carries exception detail.
 */
@Getter
@Setter
@NoArgsConstructor
public class Response {

	private HttpStatus status;
	private String message;
	private Object payload;
	private boolean success;

	/** Set on error responses so a bug report can quote it. Omitted when null. */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String correlationId;

	public Response(HttpStatus status, boolean success, String message, Object payload) {
		this.status = status;
		this.success = success;
		this.message = message;
		this.payload = payload;
	}

	public static ResponseEntity<Response> ok(String message, Object payload) {
		return of(HttpStatus.OK, message, payload);
	}

	public static ResponseEntity<Response> ok(String message) {
		return of(HttpStatus.OK, message, null);
	}

	public static ResponseEntity<Response> created(String message, Object payload) {
		return of(HttpStatus.CREATED, message, payload);
	}

	public static ResponseEntity<Response> of(HttpStatus status, String message, Object payload) {
		return new ResponseEntity<>(new Response(status, status.is2xxSuccessful(), message, payload), status);
	}
}
