package com.ecommerce.user.dto.response;

/** How long the code lives and when the shopper may ask for another. */
public record OtpSentResponse(long expiresInSeconds, long resendAfterSeconds) {}
