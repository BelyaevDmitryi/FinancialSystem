package com.fs.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Type {
    BOND_TYPE("BOND_TYPE"),
    SHARE_TYPE_COMMON("SHARE_TYPE_COMMON"),
    ETF_TYPE("ETF_TYPE");

    private final String value;

    Type(String value) {
        this.value = value;
    }

    @JsonCreator
    public static Type fromValue(String value) {
        for (Type type : Type.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown enum type: " + value);
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
