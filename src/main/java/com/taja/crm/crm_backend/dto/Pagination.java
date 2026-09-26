package com.taja.crm.crm_backend.dto;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public final class Pagination {
    private Pagination() {
    }

    public static PageRequest of(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page 必須大於等於 0，size 必須介於 1 到 100");
        }
        // 固定排序，避免相同資料在不同頁面重複出現。
        return PageRequest.of(page, size, Sort.by("id").ascending());
    }
}
