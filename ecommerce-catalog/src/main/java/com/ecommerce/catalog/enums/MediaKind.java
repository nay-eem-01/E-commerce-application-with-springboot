package com.ecommerce.catalog.enums;

/**
 * PHOTO: gallery shots (front, back, detail, in hand). SPIN_FRAME: one turntable photo per
 * step, ordered by sortOrder, for the 360 viewer (24 to 36 frames). STORY: the image beside
 * the product story.
 */
public enum MediaKind {
	PHOTO,
	SPIN_FRAME,
	STORY
}
