package io.github.shad3n.predicatemapper.integration;

import io.github.shad3n.predicatemapper.integration.entity.User;
import io.github.shad3n.predicatemapper.integration.repository.UserRepository;
import io.github.shad3n.predicatemapper.testretention.RetentionTestDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class RetentionIntegrationTest {
    @Autowired
    private RetentionTestMapper mapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testRetentionOfAnnotationsInCompiledClass() {
        User user = new User();
        user.setUsername("Retention Admin");
        userRepository.save(user);

        RetentionTestDto filter = new RetentionTestDto();
        filter.setUsername("Retention Admin");
        var predicate = mapper.createPredicate(filter);
        var users = (List<io.github.shad3n.predicatemapper.integration.entity.User>) userRepository.findAll(predicate);
        assertThat(users).extracting("username").contains("Retention Admin");
    }
}
