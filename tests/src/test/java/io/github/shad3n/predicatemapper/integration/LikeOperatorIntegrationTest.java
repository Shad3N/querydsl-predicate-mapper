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
public class LikeOperatorIntegrationTest {

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
    }

    @Test
    void testLikeOperator_contains() {
        UserFilter filter = new UserFilter();
        filter.setUsernameLike("%a%"); // alice, charlie, david

        List<User> users = (List<User>) userRepository.findAll(userPredicateMapper.filter(filter));

        assertThat(users).hasSize(3);
        assertThat(users).extracting(User::getUsername).containsExactlyInAnyOrder("alice", "charlie", "david");
    }

    @Test
    void testLikeOperator_startsWith() {
        UserFilter filter = new UserFilter();
        filter.setUsernameLike("b%"); // bob

        List<User> users = (List<User>) userRepository.findAll(userPredicateMapper.filter(filter));

        assertThat(users).hasSize(1);
        assertThat(users.get(0).getUsername()).isEqualTo("bob");
    }

    @Test
    void testLikeOperator_endsWith() {
        UserFilter filter = new UserFilter();
        filter.setUsernameLike("%e"); // alice, charlie

        List<User> users = (List<User>) userRepository.findAll(userPredicateMapper.filter(filter));

        assertThat(users).hasSize(2);
        assertThat(users).extracting(User::getUsername).containsExactlyInAnyOrder("alice", "charlie");
    }

    @Test
    void testLikeOperator_noWildcard() {
        UserFilter filter = new UserFilter();
        filter.setUsernameLike("alice"); // exact match

        List<User> users = (List<User>) userRepository.findAll(userPredicateMapper.filter(filter));

        assertThat(users).hasSize(1);
        assertThat(users.get(0).getUsername()).isEqualTo("alice");
    }
}
