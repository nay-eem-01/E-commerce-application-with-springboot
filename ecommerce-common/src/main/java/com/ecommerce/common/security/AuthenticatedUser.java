package com.ecommerce.common.security;

/**
 * What every module may know about the caller. The user module's principal implements it,
 * so cart, order and review read the caller without depending on the user module.
 */
public interface AuthenticatedUser {

	Long getId();

	boolean isAdmin();

	/** The account name (may be null for a phone-only customer who never gave one). */
	String getDisplayName();
}
