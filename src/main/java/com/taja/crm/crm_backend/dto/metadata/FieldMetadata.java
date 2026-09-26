package com.taja.crm.crm_backend.dto.metadata;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.taja.crm.crm_backend.model.Option;
import java.util.List;

/** name 為資料庫欄名；apiFieldName 為 JSON 欄位路徑。 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FieldMetadata(
        String name,
        String displayName,
        String type,
        String apiFieldName,
        boolean readOnly,
        List<Option> options,
        String relatedEntityName,
        String relatedFiledName) {
}
