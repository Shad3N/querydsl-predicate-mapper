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
public class OperatorIntegrationTest {

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
    void testNotEqOperator() {
        UserFilter filter = new UserFilter();
        filter.setNotUsername("alice");

        List<User> users = (List<User>) userRepository.findAll(userPredicateMapper.filter(filter));

        assertThat(users).hasSize(3);
        assertThat(users).extracting(User::getUsername).containsExactlyInAnyOrder("bob", "charlie", "david");
    }

    @Test
    void testLteOperator() {
        UserFilter filter = new UserFilter();
        filter.setMaxAge(30);

        List<User> users = (List<User>) userRepository.findAll(userPredicateMapper.filter(filter));

        assertThat(users).hasSize(2);
        assertThat(users).extracting(User::getUsername).containsExactlyInAnyOrder("alice", "bob");
    }

    @Test
    void testGteOperator() {
        UserFilter filter = new UserFilter();
        filter.setMinAge(35);

        List<User> users = (List<User>) userRepository.findAll(userPredicateMapper.filter(filter));

        assertThat(users).hasSize(2);
        assertThat(users).extracting(User::getUsername).containsExactlyInAnyOrder("charlie", "david");
    }

    @Test
    void testLikeOperator() {
        UserFilter filter = new UserFilter();
        filter.setUsernameLike("%a%"); // alice, charlie, david

        List<User> users = (List<User>) userRepository.findAll(userPredicateMapper.filter(filter));

        assertThat(users).hasSize(3);
        assertThat(users).extracting(User::getUsername).containsExactlyInAnyOrder("alice", "charlie", "david");
    }

    @Test
    void testInOperator() {
        UserFilter filter = new UserFilter();
        filter.setAgeIn(Arrays.asList(25, 40));

        List<User> users = (List<User>) userRepository.findAll(userPredicateMapper.filter(filter));

        assertThat(users).hasSize(2);
        assertThat(users).extracting(User::getUsername).containsExactlyInAnyOrder("alice", "david");
    }

    @Test
    void testIsNullOperator() {
        UserFilter filter = new UserFilter();
        filter.setEmailIsNull(true);

        List<User> users = (List<User>) userRepository.findAll(userPredicateMapper.filter(filter));

        assertThat(users).hasSize(1);
        assertThat(users.get(0).getUsername()).isEqualTo("charlie");
    }

    @Test
    void testIsNullOperator_false() {
        UserFilter filter = new UserFilter();
        filter.setEmailIsNull(false);

        List<User> users = (List<User>) userRepository.findAll(userPredicateMapper.filter(filter));

        // Assuming false means we don't apply the filter
        assertThat(users).hasSize(4);
    }

    @Test
    void testIsNotNullOperator() {
        UserFilter filter = new UserFilter();
        filter.setEmailIsNotNull(true);

        List<User> users = (List<User>) userRepository.findAll(userPredicateMapper.filter(filter));

        assertThat(users).hasSize(3);
        assertThat(users).extracting(User::getUsername).containsExactlyInAnyOrder("alice", "bob", "david");
    }

    @Test
    void testCombinations() {
        UserFilter filter = new UserFilter();
        filter.setMinAge(25);
        filter.setMaxAge(35);
        filter.setUsernameLike("%a%");

        List<User> users = (List<User>) userRepository.findAll(userPredicateMapper.filter(filter));

        // age between 25 and 35: alice(25), bob(30), charlie(35)
        // username like %a%: alice, charlie
        assertThat(users).hasSize(2);
        assertThat(users).extracting(User::getUsername).containsExactlyInAnyOrder("alice", "charlie");
    }
}
