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

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = TestApplication.class)
@ComponentScan(basePackages = "io.github.shad3n.predicatemapper.integration")
public class CombinedOperatorsIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPredicateMapper userPredicateMapper;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        userRepository.save(User.builder().username("alice").email("alice@example.com").age(25).build());
        userRepository.save(User.builder().username("bob").email("bob@example.com").age(30).build());
        userRepository.save(User.builder().username("charlie").email(null).age(35).build());
        userRepository.save(User.builder().username("david").email("david@example.com").age(40).build());
        userRepository.save(User.builder().username("eve").email("eve@example.com").age(25).build());
    }

    @Test
    void testCombined_GteAndLteAndLike() {
        UserFilter filter = new UserFilter();
        filter.setMinAge(25);
        filter.setMaxAge(35);
        filter.setUsernameLike("%e%");

        List<User> users = (List<User>) userRepository.findAll(userPredicateMapper.filter(filter));

        // age between 25 and 35: alice(25), bob(30), charlie(35), eve(25)
        // username like %e%: alice, charlie, eve
        assertThat(users).hasSize(3);
        assertThat(users).extracting(User::getUsername).containsExactlyInAnyOrder("alice", "charlie", "eve");
    }

    @Test
    void testCombined_InAndIsNotNullAndNotEq() {
        UserFilter filter = new UserFilter();
        filter.setAgeIn(Arrays.asList(25, 35, 40));
        filter.setEmailIsNotNull(true);
        filter.setNotUsername("alice");

        List<User> users = (List<User>) userRepository.findAll(userPredicateMapper.filter(filter));

        // age in (25, 35, 40): alice, charlie, david, eve
        // email is not null: alice, david, eve
        // not username alice: david, eve
        assertThat(users).hasSize(2);
        assertThat(users).extracting(User::getUsername).containsExactlyInAnyOrder("david", "eve");
    }

    @Test
    void testCombined_EqAndIsNull() {
        UserFilter filter = new UserFilter();
        filter.setExactAge(35);
        filter.setEmailIsNull(true);

        List<User> users = (List<User>) userRepository.findAll(userPredicateMapper.filter(filter));

        // age 35: charlie
        // email is null: charlie
        assertThat(users).hasSize(1);
        assertThat(users.get(0).getUsername()).isEqualTo("charlie");
    }
}
