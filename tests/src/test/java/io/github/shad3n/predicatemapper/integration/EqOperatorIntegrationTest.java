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
public class EqOperatorIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPredicateMapper userPredicateMapper;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        userRepository.save(User.builder().username("alice").email("alice@example.com").age(25).build());
        userRepository.save(User.builder().username("bob").email("bob@example.com").age(30).build());
        userRepository.save(User.builder().username("charlie").email("charlie@example.com").age(25).build());
    }

    @Test
    void testEqOperator_withUsername() {
        UserFilter filter = new UserFilter();
        filter.setExactUsername("alice");

        List<User> users = (List<User>) userRepository.findAll(userPredicateMapper.filter(filter));

        assertThat(users).hasSize(1);
        assertThat(users.get(0).getUsername()).isEqualTo("alice");
    }

    @Test
    void testEqOperator_withAge() {
        UserFilter filter = new UserFilter();
        filter.setExactAge(25);

        List<User> users = (List<User>) userRepository.findAll(userPredicateMapper.filter(filter));

        assertThat(users).hasSize(2);
        assertThat(users).extracting(User::getUsername).containsExactlyInAnyOrder("alice", "charlie");
    }

    @Test
    void testEqOperator_withMultipleFields() {
        UserFilter filter = new UserFilter();
        filter.setExactUsername("alice");
        filter.setExactAge(25);

        List<User> users = (List<User>) userRepository.findAll(userPredicateMapper.filter(filter));

        assertThat(users).hasSize(1);
        assertThat(users.get(0).getUsername()).isEqualTo("alice");
    }

    @Test
    void testEqOperator_noMatch() {
        UserFilter filter = new UserFilter();
        filter.setExactUsername("david");

        List<User> users = (List<User>) userRepository.findAll(userPredicateMapper.filter(filter));

        assertThat(users).isEmpty();
    }

    @Test
    void testEqOperator_nullFilterFields_returnsAll() {
        UserFilter filter = new UserFilter();

        List<User> users = (List<User>) userRepository.findAll(userPredicateMapper.filter(filter));

        assertThat(users).hasSize(3);
    }
}
