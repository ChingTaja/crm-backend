package com.taja.crm.crm_backend.model;

import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum QualificationDecision {
    APPROVED("approved", "通過"),
    REJECTED("rejected", "不通過");

    private final String key;
    private final String value;

    public static List<Option> options() {
        return Arrays.stream(values())
                .map(decision -> new Option(decision.key, decision.value))
                .toList();
    }
}
