package com.taja.crm.crm_backend.model;

import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LeadStatus {
    PENDING("pending", "待聯繫"),
    CONTACTING("contacting", "聯繫中"),
    QUALIFIED("qualified", "已合格"),
    UNQUALIFIED("unqualified", "不合格");

    private final String key;
    private final String value;

    public static LeadStatus fromKey(String key) {
        return Arrays.stream(values())
                .filter(status -> status.key.equals(key))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("無效的 Lead 狀態 key：" + key));
    }

    public static LeadStatus fromValue(String value) {
        return Arrays.stream(values())
                .filter(status -> status.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("無效的 Lead 狀態：" + value));
    }

    public static List<Option> options() {
        return Arrays.stream(values())
                .map(status -> new Option(status.key, status.value))
                .toList();
    }
}
