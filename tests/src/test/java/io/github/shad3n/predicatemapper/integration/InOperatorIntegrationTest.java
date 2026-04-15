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
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = TestApplication.class)
@ComponentScan(basePackages = "io.github.shad3n.predicatemapper.integration")
public class InOperatorIntegrationTest {

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
    void testInOperator_matchMultiple() {
        UserFilter filter = new UserFilter();
        filter.setAgeIn(Arrays.asList(25, 40));

        List<User> users = (List<User>) userRepository.findAll(userPredicateMapper.filter(filter));

        assertThat(users).hasSize(2);
        assertThat(users).extracting(User::getUsername).containsExactlyInAnyOrder("alice", "david");
    }

    @Test
    void testInOperator_matchSingle() {
        UserFilter filter = new UserFilter();
        filter.setAgeIn(Collections.singletonList(30));

        List<User> users = (List<User>) userRepository.findAll(userPredicateMapper.filter(filter));

        assertThat(users).hasSize(1);
        assertThat(users.get(0).getUsername()).isEqualTo("bob");
    }

    @Test
    void testInOperator_emptyList() {
        UserFilter filter = new UserFilter();
        filter.setAgeIn(Collections.emptyList());

        List<User> users = (List<User>) userRepository.findAll(userPredicateMapper.filter(filter));

        // QueryDSL in(emptyList) evaluates to false, so it should return empty
        assertThat(users).isEmpty();
    }
}
