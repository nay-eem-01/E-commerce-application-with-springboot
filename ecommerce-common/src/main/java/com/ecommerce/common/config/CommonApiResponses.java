package com.ecommerce.common.config;

import com.ecommerce.common.model.Response;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** The shared error responses, documented once per controller. All use the envelope. */
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses(value = {
		@ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = Response.class))),
		@ApiResponse(responseCode = "401", description = "Not signed in or session ended", content = @Content(schema = @Schema(implementation = Response.class))),
		@ApiResponse(responseCode = "403", description = "Signed in but not allowed", content = @Content(schema = @Schema(implementation = Response.class))),
		@ApiResponse(responseCode = "404", description = "Not found, or private and not yours", content = @Content(schema = @Schema(implementation = Response.class))),
		@ApiResponse(responseCode = "409", description = "Conflict: duplicate, out of stock or illegal state", content = @Content(schema = @Schema(implementation = Response.class))),
		@ApiResponse(responseCode = "429", description = "Too many attempts", content = @Content(schema = @Schema(implementation = Response.class))),
		@ApiResponse(responseCode = "500", description = "Unexpected error; quote the correlationId", content = @Content(schema = @Schema(implementation = Response.class)))
})
public @interface CommonApiResponses {}
