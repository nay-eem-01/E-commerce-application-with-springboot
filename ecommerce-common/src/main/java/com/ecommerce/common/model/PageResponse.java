package com.ecommerce.common.model;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Stable page shape for the frontend. Spring's {@code PageImpl} JSON is not a supported
 * contract (Spring Data warns about it), so lists return this record instead.
 */
public record PageResponse<T>(
		List<T> content,
		int pageNo,
		int pageSize,
		long totalElements,
		int totalPages,
		boolean last
) {
	public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
		return new PageResponse<>(
				page.getContent().stream().map(mapper).toList(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.isLast()
		);
	}
}
