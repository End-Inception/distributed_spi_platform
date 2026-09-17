package com.distributedplatform;

import com.distributedplatform.exception.UserNotFoundException;
import com.distributedplatform.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class DistributedApiPlatformApplicationTests {

    @Autowired
    private UserService userService;

    @Test
    void contextLoads() {
    }

    @Test
    void getUserById_shouldThrowExceptionWhenUserDoesNotExist() {

        UUID randomId = UUID.randomUUID();

        assertThrows(
                UserNotFoundException.class,
                () -> userService.getUserById(randomId)
        );
    }
}