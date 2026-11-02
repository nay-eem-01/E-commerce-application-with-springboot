package com.ecommerce.common.constant;

/**
 * Every failure message the API can return. The value is the shopper-facing English text.
 * Add a constant for each new failure; never pass free text or an exception message.
 */
public final class ErrorCode {
	private ErrorCode() {}

	// generic
	public static final String SERVER_ERROR = "Something went wrong. Please try again later.";
	public static final String INVALID_REQUEST = "The request is invalid. Check the fields and try again.";
	public static final String INVALID_SORT_FIELD = "This list cannot be sorted by that field.";
	public static final String NO_ENDPOINT = "No endpoint found for this request.";
	public static final String METHOD_NOT_ALLOWED = "This action is not supported on this address.";
	public static final String UNAUTHORIZED = "Please sign in to continue.";
	public static final String FORBIDDEN = "You are not allowed to perform this action.";
	public static final String TOO_MANY_ATTEMPTS = "Too many attempts. Please wait a few minutes and try again.";
	public static final String IDEMPOTENCY_KEY_REQUIRED = "Missing Idempotency-Key header.";

	// auth
	public static final String INVALID_CREDENTIALS = "Invalid email or password.";
	public static final String INVALID_PHONE = "Use an 11-digit mobile number starting with 01, like 01712345678.";
	public static final String INVALID_OTP = "That code is wrong or has expired. Request a new code.";
	public static final String INVALID_REFRESH_TOKEN = "Your session has ended. Please sign in again.";
	public static final String ACCOUNT_BANNED = "This account is suspended. Contact support if you think this is a mistake.";
	public static final String WRONG_CURRENT_PASSWORD = "Your current password is wrong.";
	public static final String PASSWORD_NOT_SET = "This account signs in with a phone code and has no password.";
	public static final String EMAIL_TAKEN = "This email is already used by another account.";
	public static final String USER_NOT_FOUND = "User not found.";
	public static final String PASSWORD_CHANGE_REQUIRED = "Set a new password before continuing.";
	public static final String CANNOT_BAN_SELF = "You cannot suspend your own account.";

	// address
	public static final String ADDRESS_NOT_FOUND = "Address not found.";
	public static final String ADDRESS_LIMIT = "You can save up to 10 addresses. Remove one first.";

	// catalog
	public static final String CATEGORY_NOT_FOUND = "Category not found.";
	public static final String CATEGORY_SLUG_TAKEN = "A category with this slug already exists.";
	public static final String CATEGORY_NOT_EMPTY = "This category still has products. Move or archive them first.";
	public static final String PRODUCT_NOT_FOUND = "Product not found.";
	public static final String PRODUCT_SLUG_TAKEN = "A product with this slug already exists.";
	public static final String OPTION_NOT_FOUND = "That option is not available for this product.";
	public static final String OPTION_REQUIRED = "Choose an option for this product first.";
	public static final String OUT_OF_STOCK = "Some items are out of stock or have less stock than you asked for.";
	public static final String PRODUCT_UNAVAILABLE = "This product is not available right now.";
	public static final String PREORDER_FIELDS_REQUIRED = "A pre-order product needs a release month and a deposit.";

	// cart
	public static final String CART_ITEM_NOT_FOUND = "This item is not in your cart.";
	public static final String CART_LIMIT = "Your cart can hold up to 50 different items.";
	public static final String WISHLIST_LIMIT = "Your wishlist can hold up to 200 products.";

	// order
	public static final String ORDER_NOT_FOUND = "Order not found.";
	public static final String TOO_MANY_LINES = "An order can have up to 30 different items.";
	public static final String EMPTY_ORDER = "Add at least one item before placing an order.";
	public static final String ILLEGAL_ORDER_TRANSITION = "This order cannot move to that step from its current status.";
	public static final String ORDER_NOT_CANCELLABLE = "This order is already packed and can no longer be cancelled online. Call us instead.";
	public static final String PAYMENT_PROVIDER_ERROR = "We could not reach the payment service. Your order is saved; try paying again in a minute.";
	public static final String PAYMENT_NOT_FOUND = "Payment not found.";
	public static final String PAYMENT_NOT_RETRYABLE = "This order cannot be paid online now.";
	public static final String ORDER_NOT_PAID = "This order is not paid yet. Online orders are packed after payment.";
	public static final String PAYMENT_METHOD_UNAVAILABLE = "This payment method is not available yet. Choose cash on delivery.";
	public static final String UNKNOWN_DISTRICT = "We do not deliver to that district yet.";
	public static final String IDEMPOTENCY_KEY_REUSED = "This Idempotency-Key was already used for a different order.";

	// notification
	public static final String NOTIFICATION_NOT_FOUND = "Notification not found.";
	public static final String NOTIFICATION_NOT_RETRYABLE = "Only failed notifications can be retried.";

	// review
	public static final String REVIEW_NOT_FOUND = "Review not found.";
	public static final String REVIEW_NOT_PURCHASED = "You can review products after they are delivered to you.";
	public static final String REVIEW_EXISTS = "You already reviewed this product. Edit your review instead.";
}
