package com.ecommerce.insights.dto.response;

import com.ecommerce.catalog.api.CatalogReport.SearchTermCount;

import java.util.List;

public record SearchesResponse(List<SearchTermCount> top, List<SearchTermCount> zeroResults) {}
