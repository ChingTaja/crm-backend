package com.taja.crm.crm_backend.dto;

import java.util.List;
import org.springframework.data.domain.Page;

@io.swagger.v3.oas.annotations.media.Schema(requiredProperties={"content","page","size","totalElements","totalPages","first","last"})
public record PageResponse<T>(List<T> content, int page, int size,
                              long totalElements, int totalPages, boolean first, boolean last) {
    public static <T> PageResponse<T> fromPage(Page<T> result) {
        return new PageResponse<>(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(), result.isFirst(), result.isLast());
    }
}
