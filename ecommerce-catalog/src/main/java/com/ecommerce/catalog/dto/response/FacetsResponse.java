package com.ecommerce.catalog.dto.response;

import java.util.List;

/** Filter groups for a listing: each value with how many active products carry it. */
public record FacetsResponse(List<Group> groups) {
	public record Group(String name, List<Value> values) {}
	public record Value(String value, long count) {}
}
