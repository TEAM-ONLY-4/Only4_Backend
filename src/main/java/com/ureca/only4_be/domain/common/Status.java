package com.ureca.only4_be.domain.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Status {
    ACTIVE("ACTIVE", "활성"),
    DELETE("ACTIVE", "비활성");

    String key;
    String value;
}