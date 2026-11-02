package com.ecommerce.common.exception;

import com.ecommerce.common.constant.ErrorCode;
import com.ecommerce.common.logging.CorrelationId;
import com.ecommerce.common.model.Response;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * The single exception handler. Responses never contain stack traces, SQL, class names or
 * an unexpected exception's message; a 500 carries the correlation id to quote instead.
 */
@Slf4j
@Hidden
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(ResponseException.class)
	public ResponseEntity<Object> handleResponseException(ResponseException e) {
		return build(e.getHttpStatus() != null ? e.getHttpStatus() : HttpStatus.BAD_REQUEST, e.getMessage(), e.getPayload());
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<Object> handleAuthentication(AuthenticationException e) {
		return build(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, null);
	}

	/** Without this the catch-all would turn every denied @PreAuthorize into a 500. */
	@ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
	public ResponseEntity<Object> handleAccessDenied(Exception e) {
		return build(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, null);
	}

	@ExceptionHandler({ConstraintViolationException.class, MethodArgumentTypeMismatchException.class})
	public ResponseEntity<Object> handleBadParameter(Exception e) {
		return build(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_REQUEST, null);
	}

	/** Two shoppers buying the last unit at once: the loser retries with fresh data. */
	@ExceptionHandler(OptimisticLockingFailureException.class)
	public ResponseEntity<Object> handleOptimisticLock(OptimisticLockingFailureException e) {
		return build(HttpStatus.CONFLICT, ErrorCode.OUT_OF_STOCK, null);
	}

	/** A unique constraint the service did not pre-check. Never echo the SQL. */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<Object> handleIntegrity(DataIntegrityViolationException e) {
		log.warn("Data integrity violation: {}", e.getMostSpecificCause().getClass().getSimpleName());
		return build(HttpStatus.CONFLICT, ErrorCode.INVALID_REQUEST, null);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Object> handleUnexpected(Exception e, WebRequest request) {
		log.error("Unhandled exception for {}", request.getDescription(false), e);
		return build(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.SERVER_ERROR, null);
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			@NonNull MethodArgumentNotValidException e, @NonNull HttpHeaders headers,
			@NonNull HttpStatusCode status, @NonNull WebRequest request) {
		FieldError fieldError = e.getBindingResult().getFieldError();
		String message = fieldError != null && fieldError.getDefaultMessage() != null
				? fieldError.getDefaultMessage()
				: ErrorCode.INVALID_REQUEST;
		return build(HttpStatus.BAD_REQUEST, message, null);
	}

	@Override
	protected ResponseEntity<Object> handleHandlerMethodValidationException(
			@NonNull HandlerMethodValidationException e, @NonNull HttpHeaders headers,
			@NonNull HttpStatusCode status, @NonNull WebRequest request) {
		return build(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_REQUEST, null);
	}

	@Override
	protected ResponseEntity<Object> handleNoHandlerFoundException(
			@NonNull NoHandlerFoundException ex, @NonNull HttpHeaders headers,
			@NonNull HttpStatusCode status, @NonNull WebRequest request) {
		return build(HttpStatus.NOT_FOUND, ErrorCode.NO_ENDPOINT, null);
	}

	@Override
	protected ResponseEntity<Object> handleNoResourceFoundException(
			@NonNull NoResourceFoundException ex, @NonNull HttpHeaders headers,
			@NonNull HttpStatusCode status, @NonNull WebRequest request) {
		return build(HttpStatus.NOT_FOUND, ErrorCode.NO_ENDPOINT, null);
	}

	@Override
	protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
			@NonNull HttpRequestMethodNotSupportedException ex, @NonNull HttpHeaders headers,
			@NonNull HttpStatusCode status, @NonNull WebRequest request) {
		return build(HttpStatus.METHOD_NOT_ALLOWED, ErrorCode.METHOD_NOT_ALLOWED, null);
	}

	/** Every other framework exception (unreadable JSON, wrong media type, missing param). */
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(
			@NonNull Exception ex, Object body, @NonNull HttpHeaders headers,
			@NonNull HttpStatusCode status, @NonNull WebRequest request) {
		if (status.is5xxServerError()) {
			log.error("Framework error for {}", request.getDescription(false), ex);
			return build(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.SERVER_ERROR, null);
		}
		return build(HttpStatus.valueOf(status.value()), ErrorCode.INVALID_REQUEST, null);
	}

	public static ResponseEntity<Object> build(HttpStatus status, String message, Object payload) {
		Response body = new Response(status, false, message, payload);
		body.setCorrelationId(CorrelationId.current());
		return new ResponseEntity<>(body, status);
	}
}
