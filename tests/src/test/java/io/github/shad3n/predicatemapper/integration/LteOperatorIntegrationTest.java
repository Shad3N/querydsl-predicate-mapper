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
public class LteOperatorIntegrationTest {

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
    void testLteOperator_matchSome() {
        UserFilter filter = new UserFilter();
        filter.setMaxAge(30);

        List<User> users = (List<User>) userRepository.findAll(userPredicateMapper.filter(filter));

        assertThat(users).hasSize(2);
        assertThat(users).extracting(User::getUsername).containsExactlyInAnyOrder("alice", "bob");
    }

    @Test
    void testLteOperator_matchAll() {
        UserFilter filter = new UserFilter();
        filter.setMaxAge(50);

        List<User> users = (List<User>) userRepository.findAll(userPredicateMapper.filter(filter));

        assertThat(users).hasSize(4);
    }

    @Test
    void testLteOperator_matchNone() {
        UserFilter filter = new UserFilter();
        filter.setMaxAge(20);

        List<User> users = (List<User>) userRepository.findAll(userPredicateMapper.filter(filter));

        assertThat(users).isEmpty();
    }
}
