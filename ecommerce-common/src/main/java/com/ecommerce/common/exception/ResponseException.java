package com.ecommerce.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * The one expected-failure exception. Message must be an {@code ErrorCode} constant: it
 * is returned to the client verbatim.
 */
@Getter
public class ResponseException extends RuntimeException {

	private final HttpStatus httpStatus;
	private final transient Object payload;

	public ResponseException(HttpStatus httpStatus, String message) {
		this(httpStatus, message, null);
	}

	public ResponseException(HttpStatus httpStatus, String message, Object payload) {
		super(message);
		this.httpStatus = httpStatus;
		this.payload = payload;
	}

	public static ResponseException notFound(String message) {
		return new ResponseException(HttpStatus.NOT_FOUND, message);
	}

	public static ResponseException badRequest(String message) {
		return new ResponseException(HttpStatus.BAD_REQUEST, message);
	}

	public static ResponseException conflict(String message) {
		return new ResponseException(HttpStatus.CONFLICT, message);
	}

	public static ResponseException forbidden(String message) {
		return new ResponseException(HttpStatus.FORBIDDEN, message);
	}

	public static ResponseException unauthorized(String message) {
		return new ResponseException(HttpStatus.UNAUTHORIZED, message);
	}

	public static ResponseException tooManyRequests() {
		return new ResponseException(HttpStatus.TOO_MANY_REQUESTS, com.ecommerce.common.constant.ErrorCode.TOO_MANY_ATTEMPTS);
	}
}
