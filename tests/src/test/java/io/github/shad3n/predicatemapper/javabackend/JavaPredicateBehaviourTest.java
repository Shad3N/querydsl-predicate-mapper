package io.github.shad3n.predicatemapper.javabackend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

@DisplayName("Generated Java predicate behaviour")
class JavaPredicateBehaviourTest {

    private static final Host UBUNTU = new Host("ubuntu-22", "ubuntu", true, 4, new Host.Owner("alice"), null, null, null);
    private static final Host DEBIAN = new Host("Debian-12", "debian", false, 2, null, null, null, null);
    private static final Host BARE = new Host("bare_metal", null, false, null, new Host.Owner(null), null, null, null);
    private static final List<Host> HOSTS = List.of(UBUNTU, DEBIAN, BARE);

    private final HostPredicateMapper mapper = new HostPredicateMapperImpl();

    private List<Host> matching(UnaryOperator<Builder> filter) {
        return HOSTS.stream().filter(mapper.hosts(filter.apply(new Builder()).build())).toList();
    }

    @Test
    @DisplayName("Null and empty filters match everything")
    void emptyFilterMatchesAll() {
        assertThat(HOSTS.stream().filter(mapper.hosts(null)).toList()).isEqualTo(HOSTS);
        assertThat(HOSTS.stream().filter(mapper.hosts(HostFilter.empty())).toList()).isEqualTo(HOSTS);
    }

    @Test
    @DisplayName("EQ is case-sensitive unless ignoreCase is set")
    void equality() {
        assertThat(matching(b -> b.osDistro("ubuntu"))).containsExactly(UBUNTU);
        assertThat(matching(b -> b.osDistro("Ubuntu"))).isEmpty();
        assertThat(matching(b -> b.osDistroIgnoringCase("Ubuntu"))).containsExactly(UBUNTU);
    }

    @Test
    @DisplayName("NOT_EQ never matches a null value")
    void notEqual() {
        assertThat(matching(b -> b.notOsDistro("ubuntu"))).containsExactly(DEBIAN);
        assertThat(matching(b -> b.notOsDistroIgnoringCase("UBUNTU"))).containsExactly(DEBIAN);
    }

    @Test
    @DisplayName("LTE and GTE are inclusive and skip null values")
    void ranges() {
        assertThat(matching(b -> b.maxCores(2))).containsExactly(DEBIAN);
        assertThat(matching(b -> b.minCores(4))).containsExactly(UBUNTU);
        assertThat(matching(b -> b.minCores(5))).isEmpty();
    }

    @Test
    @DisplayName("IN matches listed values")
    void in() {
        assertThat(matching(b -> b.names(List.of("Debian-12", "bare_metal")))).containsExactly(DEBIAN, BARE);
    }

    @Test
    @DisplayName("LIKE treats % and _ as wildcards and everything else literally")
    void like() {
        assertThat(matching(b -> b.nameLike("ubu%"))).containsExactly(UBUNTU);
        assertThat(matching(b -> b.nameLike("bare_metal"))).containsExactly(BARE);
        assertThat(matching(b -> b.nameLike("bare.metal"))).isEmpty();
        assertThat(matching(b -> b.nameLike("%-1_"))).containsExactly(DEBIAN);
        assertThat(matching(b -> b.nameLike("debian%"))).isEmpty();
        assertThat(matching(b -> b.nameLikeIgnoringCase("debian%"))).containsExactly(DEBIAN);
    }

    @Test
    @DisplayName("CONTAINS matches a literal substring, with no wildcard or regex characters")
    void contains() {
        assertThat(matching(b -> b.nameContains("-"))).containsExactly(UBUNTU, DEBIAN);
        assertThat(matching(b -> b.nameContains("bian"))).containsExactly(DEBIAN);
        assertThat(matching(b -> b.nameContains("e_m"))).containsExactly(BARE);
        assertThat(matching(b -> b.nameContains("e.m"))).isEmpty();
        assertThat(matching(b -> b.nameContains("%"))).isEmpty();
        assertThat(matching(b -> b.nameContains(".*"))).isEmpty();
        assertThat(matching(b -> b.nameContains("DEBIAN"))).isEmpty();
    }

    @Test
    @DisplayName("CONTAINS with ignoreCase ignores case and still treats every character literally")
    void containsIgnoringCase() {
        assertThat(matching(b -> b.nameContainsIgnoringCase("DEBIAN"))).containsExactly(DEBIAN);
        assertThat(matching(b -> b.nameContainsIgnoringCase("E_M"))).containsExactly(BARE);
        assertThat(matching(b -> b.nameContainsIgnoringCase("."))).isEmpty();
        assertThat(matching(b -> b.nameContainsIgnoringCase("%"))).isEmpty();
    }

    @Test
    @DisplayName("CONTAINS with an empty filter value matches every non-null value")
    void containsEmptyValue() {
        assertThat(matching(b -> b.nameContains(""))).isEqualTo(HOSTS);
        assertThat(matching(b -> b.nameContainsIgnoringCase(""))).isEqualTo(HOSTS);
        assertThat(matching(b -> b.ownerNameContains(""))).containsExactly(UBUNTU);
    }

    @Test
    @DisplayName("CONTAINS on a nested path never matches a null intermediate or a null value")
    void containsNestedPath() {
        assertThat(matching(b -> b.ownerNameContains("lic"))).containsExactly(UBUNTU);
    }

    @Test
    @DisplayName("REGEX is found anywhere in the value")
    void regex() {
        assertThat(matching(b -> b.nameRegex("^ubu"))).containsExactly(UBUNTU);
        assertThat(matching(b -> b.nameRegex("-\\d+"))).containsExactly(UBUNTU, DEBIAN);
        assertThat(matching(b -> b.nameRegex("debian"))).isEmpty();
        assertThat(matching(b -> b.nameRegexIgnoringCase("DEBIAN"))).containsExactly(DEBIAN);
    }

    @Test
    @DisplayName("Null checks apply only when the filter is true")
    void nullChecks() {
        assertThat(matching(b -> b.noOsDistro(true))).containsExactly(BARE);
        assertThat(matching(b -> b.hasOsDistro(true))).containsExactly(UBUNTU, DEBIAN);
        assertThat(matching(b -> b.noOsDistro(false))).isEqualTo(HOSTS);
    }

    @Test
    @DisplayName("Boxed filter compares against a primitive component")
    void primitiveTarget() {
        assertThat(matching(b -> b.guiAccess(true))).containsExactly(UBUNTU);
        assertThat(matching(b -> b.guiAccess(false))).containsExactly(DEBIAN, BARE);
    }

    @Test
    @DisplayName("Nested path fails every operator when an intermediate is null")
    void nestedPath() {
        assertThat(matching(b -> b.ownerName("alice"))).containsExactly(UBUNTU);
        assertThat(matching(b -> b.notOwnerName("bob"))).containsExactly(UBUNTU);
        assertThat(matching(b -> b.noOwnerName(true))).containsExactly(BARE);
        assertThat(matching(b -> b.hasOwnerName(true))).containsExactly(UBUNTU);
    }

    @Test
    @DisplayName("Nested IS_NULL matches a present owner with a null leaf but not a null owner")
    void nestedNullLeaf() {
        assertThat(matching(b -> b.noOwnerName(true))).doesNotContain(DEBIAN).containsExactly(BARE);
    }

    @Test
    @DisplayName("LIKE, REGEX and CONTAINS ignoring case fold Unicode case")
    void unicodeIgnoringCase() {
        Host upper = new Host("ČESKÝ", "x", true, 1, null, null, null, null);
        Host title = new Host("Český", "x", true, 1, null, null, null, null);
        Host other = new Host("polish", "x", true, 1, null, null, null, null);
        List<Host> hosts = List.of(upper, title, other);

        for (UnaryOperator<Builder> filter : List.<UnaryOperator<Builder>>of(
                b -> b.nameLikeIgnoringCase("český"),
                b -> b.nameRegexIgnoringCase("český"),
                b -> b.nameContainsIgnoringCase("český"))) {
            assertThat(hosts.stream().filter(mapper.hosts(filter.apply(new Builder()).build())).toList())
                    .containsExactly(upper, title);
        }
    }

    @Test
    @DisplayName("LIKE escape character makes percent, underscore and itself match literally")
    void likeEscape() {
        Host percent = new Host("100%", "x", true, 1, null, null, null, null);
        Host underscore = new Host("a_b", "x", true, 1, null, null, null, null);
        Host bang = new Host("hey!", "x", true, 1, null, null, null, null);
        Host decoy = new Host("100x", "x", true, 1, null, null, null, null);
        Host decoyUnderscore = new Host("axb", "x", true, 1, null, null, null, null);
        List<Host> hosts = List.of(percent, underscore, bang, decoy, decoyUnderscore);

        assertThat(hosts.stream().filter(mapper.hosts(new Builder().nameLike("100!%").build())).toList())
                .containsExactly(percent);
        assertThat(hosts.stream().filter(mapper.hosts(new Builder().nameLike("a!_b").build())).toList())
                .containsExactly(underscore);
        assertThat(hosts.stream().filter(mapper.hosts(new Builder().nameLike("hey!!").build())).toList())
                .containsExactly(bang);
        assertThat(hosts.stream().filter(mapper.hosts(new Builder().nameLikeIgnoringCase("A!_B").build())).toList())
                .containsExactly(underscore);
        assertThat(hosts.stream().filter(mapper.hosts(new Builder().nameLikeIgnoringCase("HEY!!").build())).toList())
                .containsExactly(bang);
    }

    @Test
    @DisplayName("EQ and NOT_EQ on a Comparable path compare with compareTo")
    void comparableEquality() {
        Host onePointZero = new Host("a", "x", true, 1, null, new BigDecimal("1.0"), null, null);
        Host two = new Host("b", "x", true, 1, null, new BigDecimal("2"), null, null);
        Host unweighed = new Host("c", "x", true, 1, null, null, null, null);
        List<Host> hosts = List.of(onePointZero, two, unweighed);

        assertThat(hosts.stream().filter(mapper.hosts(new Builder().weight(new BigDecimal("1.00")).build())).toList())
                .containsExactly(onePointZero);
        assertThat(hosts.stream().filter(mapper.hosts(new Builder().notWeight(new BigDecimal("1.00")).build())).toList())
                .containsExactly(two);
    }

    @Test
    @DisplayName("EQ on OffsetDateTime matches the same instant across offsets")
    void sameInstantDifferentOffset() {
        OffsetDateTime utc = OffsetDateTime.of(2024, 5, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        Host booted = new Host("a", "x", true, 1, null, null, utc, null);
        Host later = new Host("b", "x", true, 1, null, null, utc.plusHours(1), null);
        OffsetDateTime sameInstantPlusTwo = utc.withOffsetSameInstant(ZoneOffset.ofHours(2));

        assertThat(List.of(booted, later).stream()
                       .filter(mapper.hosts(new Builder().bootedAt(sameInstantPlusTwo).build())).toList())
                .containsExactly(booted);
    }

    @Test
    @DisplayName("IN on a Comparable path compares with compareTo")
    void comparableMembership() {
        Host weighed = new Host("a", "x", true, 1, null, new BigDecimal("1.0"), null, null);
        Host unweighed = new Host("b", "x", true, 1, null, null, null, null);
        List<Host> hosts = List.of(weighed, unweighed);

        assertThat(hosts.stream().filter(mapper.hosts(new Builder().weights(List.of(new BigDecimal("1.00"))).build()))
                       .toList()).containsExactly(weighed);
    }

    @Test
    @DisplayName("IN on an instant-based path matches the same instant across offsets")
    void instantMembership() {
        OffsetDateTime utc = OffsetDateTime.of(2024, 5, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        Host booted = new Host("a", "x", true, 1, null, null, utc, null);
        Host unbooted = new Host("b", "x", true, 1, null, null, null, null);

        assertThat(List.of(booted, unbooted).stream().filter(mapper.hosts(new Builder().bootedAtOneOf(
                List.of(utc.withOffsetSameInstant(ZoneOffset.ofHours(-5)))).build())).toList())
                .containsExactly(booted);
    }

    @Test
    @DisplayName("IN skips null elements and still matches the other elements")
    void membershipSkipsNullElements() {
        Host weighed = new Host("a", "x", true, 1, null, new BigDecimal("1.0"), null, null);
        Host heavier = new Host("b", "x", true, 1, null, new BigDecimal("3"), null, null);
        Host unweighed = new Host("c", "x", true, 1, null, null, null, null);
        List<Host> hosts = List.of(weighed, heavier, unweighed);

        assertThat(hosts.stream().filter(mapper.hosts(new Builder().weights(Arrays.asList(null, new BigDecimal("1.00")))
                                                                   .build())).toList()).containsExactly(weighed);
        assertThat(hosts.stream().filter(mapper.hosts(new Builder().weights(Arrays.asList((BigDecimal) null)).build()))
                       .toList()).isEmpty();
    }

    @Test
    @DisplayName("LTE and GTE on OffsetDateTime compare by instant across offsets")
    void instantRanges() {
        OffsetDateTime utc = OffsetDateTime.of(2024, 5, 1, 9, 0, 0, 0, ZoneOffset.UTC);
        Host booted = new Host("a", "x", true, 1, null, null, utc, null);
        Host unbooted = new Host("b", "x", true, 1, null, null, null, null);
        List<Host> hosts = List.of(booted, unbooted);
        OffsetDateTime sameInstantPlusOne = OffsetDateTime.of(2024, 5, 1, 10, 0, 0, 0, ZoneOffset.ofHours(1));
        OffsetDateTime oneMinuteBefore = sameInstantPlusOne.minusMinutes(1);
        OffsetDateTime oneMinuteAfter = sameInstantPlusOne.plusMinutes(1);

        assertThat(hosts.stream().filter(mapper.hosts(new Builder().latestBootedAt(sameInstantPlusOne).build()))
                        .toList()).containsExactly(booted);
        assertThat(hosts.stream().filter(mapper.hosts(new Builder().earliestBootedAt(sameInstantPlusOne).build()))
                        .toList()).containsExactly(booted);
        assertThat(hosts.stream().filter(mapper.hosts(new Builder().latestBootedAt(oneMinuteBefore).build()))
                        .toList()).isEmpty();
        assertThat(hosts.stream().filter(mapper.hosts(new Builder().earliestBootedAt(oneMinuteAfter).build()))
                        .toList()).isEmpty();
        assertThat(hosts.stream().filter(mapper.hosts(new Builder().latestBootedAt(oneMinuteAfter).build()))
                        .toList()).containsExactly(booted);
        assertThat(hosts.stream().filter(mapper.hosts(new Builder().earliestBootedAt(oneMinuteBefore).build()))
                        .toList()).containsExactly(booted);
    }

    @Test
    @DisplayName("Double EQ, NOT_EQ, LTE, GTE and IN compare numerically, so 0.0 equals -0.0")
    void floatingPointComparesNumerically() {
        Host negativeZero = new Host("a", "x", true, 1, null, null, null, -0.0);
        Host loaded = new Host("b", "x", true, 1, null, null, null, 1.5);
        Host unloaded = new Host("c", "x", true, 1, null, null, null, null);
        List<Host> hosts = List.of(negativeZero, loaded, unloaded);

        assertThat(hosts.stream().filter(mapper.hosts(new Builder().load(0.0).build())).toList())
                .containsExactly(negativeZero);
        assertThat(hosts.stream().filter(mapper.hosts(new Builder().notLoad(0.0).build())).toList())
                .containsExactly(loaded);
        assertThat(hosts.stream().filter(mapper.hosts(new Builder().maxLoad(0.0).build())).toList())
                .containsExactly(negativeZero);
        assertThat(hosts.stream().filter(mapper.hosts(new Builder().minLoad(0.0).build())).toList())
                .containsExactly(negativeZero, loaded);
        assertThat(hosts.stream().filter(mapper.hosts(new Builder().loads(Arrays.asList(null, 0.0)).build())).toList())
                .containsExactly(negativeZero);
    }

    @Test
    @DisplayName("REGEX with an invalid pattern fails building the predicate and names the DTO field")
    void invalidRegexPattern() {
        HostFilter filter = new Builder().nameRegex("(").build();

        assertThatThrownBy(() -> mapper.hosts(filter)).isInstanceOf(IllegalArgumentException.class)
                                                      .hasMessageContaining("nameRegex")
                                                      .hasMessageContaining("is not a valid regular expression");
    }

    @Test
    @DisplayName("REGEX with a catastrophic pattern fails once it exceeds its time limit instead of hanging")
    void catastrophicRegexPattern() {
        Host backtracking = new Host("a".repeat(40) + "!", "x", true, 1, null, null, null, null);
        Predicate<Host> predicate = mapper.hosts(new Builder().nameRegex("(a+)+\\1$").build());

        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> assertThatThrownBy(() -> predicate.test(backtracking))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("time limit"));
    }

    @Test
    @DisplayName("IN, LIKE, CONTAINS and REGEX never match a null value and never throw")
    void nullValueNeverMatchesTextOperators() {
        Host nameless = new Host(null, "ubuntu", true, 4, null, null, null, null);
        List<UnaryOperator<Builder>> filters = List.of(b -> b.names(List.of("a")),
                                                       b -> b.nameLike("%"),
                                                       b -> b.nameLikeIgnoringCase("%"),
                                                       b -> b.nameContains(""),
                                                       b -> b.nameContainsIgnoringCase(""),
                                                       b -> b.nameRegex(".*"),
                                                       b -> b.nameRegexIgnoringCase(".*"));

        for (UnaryOperator<Builder> filter : filters) {
            assertThat(mapper.hosts(filter.apply(new Builder()).build()).test(nameless)).isFalse();
        }
    }

    @Test
    @DisplayName("Every set field has to hold")
    void combined() {
        assertThat(matching(b -> b.minCores(2).nameRegex("-"))).containsExactly(UBUNTU, DEBIAN);
        assertThat(matching(b -> b.minCores(2).nameRegex("-").guiAccess(false))).containsExactly(DEBIAN);
    }

    @Test
    @DisplayName("Getter-based targets and DTOs work alike")
    void getterBasedTarget() {
        Server paris = new Server("web-1", true, new Server.Location("Paris"));
        Server homeless = new Server("db-1", false, null);
        ServerFilter filter = new ServerFilter();
        filter.setCity("PARIS");
        filter.setActive(true);
        filter.setHostnameRegex("web");

        assertThat(List.of(paris, homeless).stream().filter(mapper.servers(filter)).toList()).containsExactly(paris);
    }

    private static final class Builder {
        private String osDistro;
        private String osDistroIgnoringCase;
        private String notOsDistro;
        private String notOsDistroIgnoringCase;
        private Integer maxCores;
        private Integer minCores;
        private List<String> names;
        private String nameLike;
        private String nameLikeIgnoringCase;
        private String nameRegex;
        private String nameRegexIgnoringCase;
        private Boolean noOsDistro;
        private Boolean hasOsDistro;
        private Boolean guiAccess;
        private String ownerName;
        private String nameContains;
        private String nameContainsIgnoringCase;
        private String ownerNameContains;
        private String notOwnerName;
        private Boolean noOwnerName;
        private Boolean hasOwnerName;
        private BigDecimal weight;
        private BigDecimal notWeight;
        private List<BigDecimal> weights;
        private OffsetDateTime bootedAt;
        private List<OffsetDateTime> bootedAtOneOf;
        private OffsetDateTime latestBootedAt;
        private OffsetDateTime earliestBootedAt;
        private Double load;
        private Double notLoad;
        private Double maxLoad;
        private Double minLoad;
        private List<Double> loads;

        Builder osDistro(String value) { osDistro = value; return this; }
        Builder osDistroIgnoringCase(String value) { osDistroIgnoringCase = value; return this; }
        Builder notOsDistro(String value) { notOsDistro = value; return this; }
        Builder notOsDistroIgnoringCase(String value) { notOsDistroIgnoringCase = value; return this; }
        Builder maxCores(Integer value) { maxCores = value; return this; }
        Builder minCores(Integer value) { minCores = value; return this; }
        Builder names(List<String> value) { names = value; return this; }
        Builder nameLike(String value) { nameLike = value; return this; }
        Builder nameLikeIgnoringCase(String value) { nameLikeIgnoringCase = value; return this; }
        Builder nameRegex(String value) { nameRegex = value; return this; }
        Builder nameRegexIgnoringCase(String value) { nameRegexIgnoringCase = value; return this; }
        Builder noOsDistro(Boolean value) { noOsDistro = value; return this; }
        Builder hasOsDistro(Boolean value) { hasOsDistro = value; return this; }
        Builder guiAccess(Boolean value) { guiAccess = value; return this; }
        Builder ownerName(String value) { ownerName = value; return this; }
        Builder nameContains(String value) { nameContains = value; return this; }
        Builder nameContainsIgnoringCase(String value) { nameContainsIgnoringCase = value; return this; }
        Builder ownerNameContains(String value) { ownerNameContains = value; return this; }
        Builder notOwnerName(String value) { notOwnerName = value; return this; }
        Builder noOwnerName(Boolean value) { noOwnerName = value; return this; }
        Builder hasOwnerName(Boolean value) { hasOwnerName = value; return this; }
        Builder weight(BigDecimal value) { weight = value; return this; }
        Builder notWeight(BigDecimal value) { notWeight = value; return this; }
        Builder weights(List<BigDecimal> value) { weights = value; return this; }
        Builder bootedAt(OffsetDateTime value) { bootedAt = value; return this; }
        Builder bootedAtOneOf(List<OffsetDateTime> value) { bootedAtOneOf = value; return this; }
        Builder latestBootedAt(OffsetDateTime value) { latestBootedAt = value; return this; }
        Builder earliestBootedAt(OffsetDateTime value) { earliestBootedAt = value; return this; }
        Builder load(Double value) { load = value; return this; }
        Builder notLoad(Double value) { notLoad = value; return this; }
        Builder maxLoad(Double value) { maxLoad = value; return this; }
        Builder minLoad(Double value) { minLoad = value; return this; }
        Builder loads(List<Double> value) { loads = value; return this; }

        HostFilter build() {
            return new HostFilter(osDistro, osDistroIgnoringCase, notOsDistro, notOsDistroIgnoringCase, maxCores,
                                  minCores, names, nameLike, nameLikeIgnoringCase, nameRegex, nameRegexIgnoringCase,
                                  noOsDistro, hasOsDistro, guiAccess, ownerName, nameContains,
                                  nameContainsIgnoringCase, ownerNameContains, notOwnerName, noOwnerName,
                                  hasOwnerName, weight, notWeight, weights, bootedAt, bootedAtOneOf,
                                  latestBootedAt, earliestBootedAt, load, notLoad, maxLoad, minLoad, loads);
        }
    }
}
