package com.fs.adapter.moex;

import java.util.Optional;

/**
 * Synthetic FIGI for MOEX ISS instruments ({@code MOEX:SBER}).
 */
public final class MoexFigi {

    public static final String PREFIX = "MOEX:";

    private MoexFigi() {
    }

    public static String toFigi(String ticker) {
        return PREFIX + ticker.toUpperCase();
    }

    public static Optional<String> toTicker(String figi) {
        if (figi != null && figi.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) {
            return Optional.of(figi.substring(PREFIX.length()).toUpperCase());
        }
        return Optional.empty();
    }
}
