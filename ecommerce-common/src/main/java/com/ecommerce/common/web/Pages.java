package com.ecommerce.common.web;

import com.ecommerce.common.constant.AppConstants;
import com.ecommerce.common.constant.ErrorCode;
import com.ecommerce.common.enums.AscOrDescType;
import com.ecommerce.common.exception.ResponseException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

/**
 * Builds a Pageable for every list endpoint: pageSize clamped to 100, sortBy checked
 * against the endpoint's allowlist (unknown field = 400, never a 500 from Hibernate).
 */
public final class Pages {
	private Pages() {}

	public static Pageable of(int pageNo, int pageSize, String sortBy, AscOrDescType ascOrDesc, Set<String> allowedSorts) {
		int page = Math.max(0, pageNo);
		int size = Math.min(Math.max(1, pageSize), AppConstants.MAX_PAGE_SIZE);
		String field = (sortBy == null || sortBy.isBlank()) ? AppConstants.SORT_BY_VALUE : sortBy;
		if (!allowedSorts.contains(field)) {
			throw ResponseException.badRequest(ErrorCode.INVALID_SORT_FIELD);
		}
		Sort.Direction direction = ascOrDesc == AscOrDescType.asc ? Sort.Direction.ASC : Sort.Direction.DESC;
		return PageRequest.of(page, size, Sort.by(direction, field).and(Sort.by(Sort.Direction.DESC, "id")));
	}
}
