package io.github.shad3n.predicatemapper.javabackend;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record Host(String name, String osDistro, boolean guiAccess, Integer cores, Owner owner, BigDecimal weight,
                   OffsetDateTime bootedAt, Double load) {

    public record Owner(String name) {
    }
}
