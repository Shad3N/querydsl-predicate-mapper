package io.github.shad3n.predicatemapper.javabackend;

import io.github.shad3n.predicatemapper.annotation.FilterField;
import io.github.shad3n.predicatemapper.annotation.Op;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record HostFilter(
        @FilterField(path = "osDistro", op = Op.EQ) String osDistro,
        @FilterField(path = "osDistro", op = Op.EQ, ignoreCase = true) String osDistroIgnoringCase,
        @FilterField(path = "osDistro", op = Op.NOT_EQ) String notOsDistro,
        @FilterField(path = "osDistro", op = Op.NOT_EQ, ignoreCase = true) String notOsDistroIgnoringCase,
        @FilterField(path = "cores", op = Op.LTE) Integer maxCores,
        @FilterField(path = "cores", op = Op.GTE) Integer minCores,
        @FilterField(path = "name", op = Op.IN) List<String> names,
        @FilterField(path = "name", op = Op.LIKE) String nameLike,
        @FilterField(path = "name", op = Op.LIKE, ignoreCase = true) String nameLikeIgnoringCase,
        @FilterField(path = "name", op = Op.REGEX) String nameRegex,
        @FilterField(path = "name", op = Op.REGEX, ignoreCase = true) String nameRegexIgnoringCase,
        @FilterField(path = "osDistro", op = Op.IS_NULL) Boolean noOsDistro,
        @FilterField(path = "osDistro", op = Op.IS_NOT_NULL) Boolean hasOsDistro,
        @FilterField(path = "guiAccess", op = Op.EQ) Boolean guiAccess,
        @FilterField(path = "owner.name", op = Op.EQ) String ownerName,
        @FilterField(path = "name", op = Op.CONTAINS) String nameContains,
        @FilterField(path = "name", op = Op.CONTAINS, ignoreCase = true) String nameContainsIgnoringCase,
        @FilterField(path = "owner.name", op = Op.CONTAINS) String ownerNameContains,
        @FilterField(path = "owner.name", op = Op.NOT_EQ) String notOwnerName,
        @FilterField(path = "owner.name", op = Op.IS_NULL) Boolean noOwnerName,
        @FilterField(path = "owner.name", op = Op.IS_NOT_NULL) Boolean hasOwnerName,
        @FilterField(path = "weight", op = Op.EQ) BigDecimal weight,
        @FilterField(path = "weight", op = Op.NOT_EQ) BigDecimal notWeight,
        @FilterField(path = "weight", op = Op.IN) List<BigDecimal> weights,
        @FilterField(path = "bootedAt", op = Op.EQ) OffsetDateTime bootedAt,
        @FilterField(path = "bootedAt", op = Op.IN) List<OffsetDateTime> bootedAtOneOf,
        @FilterField(path = "bootedAt", op = Op.LTE) OffsetDateTime latestBootedAt,
        @FilterField(path = "bootedAt", op = Op.GTE) OffsetDateTime earliestBootedAt,
        @FilterField(path = "load", op = Op.EQ) Double load,
        @FilterField(path = "load", op = Op.NOT_EQ) Double notLoad,
        @FilterField(path = "load", op = Op.LTE) Double maxLoad,
        @FilterField(path = "load", op = Op.GTE) Double minLoad,
        @FilterField(path = "load", op = Op.IN) List<Double> loads) {

    public static HostFilter empty() {
        return new HostFilter(null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                              null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                              null, null, null, null, null);
    }
}
