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
public class IgnoreCaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPredicateMapper userPredicateMapper;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        userRepository.save(User.builder().username("Alice").email("alice@example.com").age(25).build());
        userRepository.save(User.builder().username("bob").email("bob@example.com").age(30).build());
    }

    private List<String> usernames(UserFilter filter) {
        return ((List<User>) userRepository.findAll(userPredicateMapper.filter(filter))).stream()
                                                                                         .map(User::getUsername)
                                                                                         .toList();
    }

    @Test
    void equalityIgnoresCase() {
        UserFilter filter = new UserFilter();
        filter.setUsernameIgnoringCase("ALICE");

        assertThat(usernames(filter)).containsExactly("Alice");
    }

    @Test
    void inequalityIgnoresCase() {
        UserFilter filter = new UserFilter();
        filter.setNotUsernameIgnoringCase("ALICE");

        assertThat(usernames(filter)).containsExactly("bob");
    }

    @Test
    void likeIgnoresCase() {
        UserFilter filter = new UserFilter();
        filter.setUsernameLikeIgnoringCase("B%");

        assertThat(usernames(filter)).containsExactly("bob");
    }
}
