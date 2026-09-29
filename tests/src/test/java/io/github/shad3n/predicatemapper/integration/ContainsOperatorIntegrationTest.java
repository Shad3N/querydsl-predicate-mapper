package io.github.shad3n.predicatemapper.integration;

import io.github.shad3n.predicatemapper.integration.dto.UserFilter;
import io.github.shad3n.predicatemapper.integration.entity.User;
import io.github.shad3n.predicatemapper.integration.mapper.UserPredicateMapper;
import io.github.shad3n.predicatemapper.integration.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = TestApplication.class)
@ComponentScan(basePackages = "io.github.shad3n.predicatemapper.integration")
public class ContainsOperatorIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPredicateMapper userPredicateMapper;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        userRepository.save(User.builder().username("Alice").age(25).build());
        userRepository.save(User.builder().username("100%").age(30).build());
        userRepository.save(User.builder().username("100x").age(35).build());
        userRepository.save(User.builder().username("a_b").age(40).build());
        userRepository.save(User.builder().username("axb").age(45).build());
        userRepository.save(User.builder().username("a!b").age(50).build());
        userRepository.save(User.builder().username(null).age(55).build());
    }

    private List<String> usernames(UserFilter filter) {
        return ((List<User>) userRepository.findAll(userPredicateMapper.filter(filter))).stream()
                                                                                         .map(User::getUsername)
                                                                                         .toList();
    }

    @Test
    void matchesSubstringAnywhere() {
        UserFilter filter = new UserFilter();
        filter.setUsernameContains("lic");

        assertThat(usernames(filter)).containsExactly("Alice");
    }

    @Test
    void isCaseSensitiveWithoutIgnoreCase() {
        UserFilter filter = new UserFilter();
        filter.setUsernameContains("ALI");

        assertThat(usernames(filter)).isEmpty();
    }

    @Test
    void treatsPercentLiterally() {
        UserFilter filter = new UserFilter();
        filter.setUsernameContains("0%");

        assertThat(usernames(filter)).containsExactly("100%");
    }

    @Test
    void treatsUnderscoreLiterally() {
        UserFilter filter = new UserFilter();
        filter.setUsernameContains("a_b");

        assertThat(usernames(filter)).containsExactly("a_b");
    }

    @Test
    void treatsEscapeCharacterLiterally() {
        UserFilter filter = new UserFilter();
        filter.setUsernameContains("!");

        assertThat(usernames(filter)).containsExactly("a!b");
    }

    @Test
    void ignoresCaseWhenRequested() {
        UserFilter filter = new UserFilter();
        filter.setUsernameContainsIgnoringCase("ALI");

        assertThat(usernames(filter)).containsExactly("Alice");
    }

    @Test
    void ignoringCaseStillTreatsWildcardsLiterally() {
        UserFilter filter = new UserFilter();
        filter.setUsernameContainsIgnoringCase("A_B");

        assertThat(usernames(filter)).containsExactly("a_b");
    }

    @Test
    void emptyValueMatchesEveryNonNullValue() {
        UserFilter filter = new UserFilter();
        filter.setUsernameContains("");

        assertThat(usernames(filter)).containsExactlyInAnyOrder("Alice", "100%", "100x", "a_b", "axb", "a!b");
    }
}
