package com.taja.crm.crm_backend.dto.search;

import java.util.List;

public record EntitySearchRequest(Integer page, Integer size, String keyword, Object filter, List<SortRule> sort) {
    public record SortRule(String field, String direction) {}
}
