package com.cypher.infrastructure.startup;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisSecurityStartupCheckTest {

    private final ApplicationArguments args = mock(ApplicationArguments.class);

    @Test
    void failsFastWhenNotDevAndRedisPasswordIsBlank() {
        Environment env = mock(Environment.class);
        when(env.acceptsProfiles(any(Profiles.class))).thenReturn(false);

        RedisSecurityStartupCheck check = new RedisSecurityStartupCheck(env);
        setPassword(check, "");

        assertThatThrownBy(() -> check.run(args))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("REDIS_PASSWORD");
    }

    @Test
    void allowsBlankRedisPasswordInDevProfile() {
        Environment env = mock(Environment.class);
        when(env.acceptsProfiles(any(Profiles.class))).thenReturn(true);

        RedisSecurityStartupCheck check = new RedisSecurityStartupCheck(env);
        setPassword(check, "");

        assertThatCode(() -> check.run(args)).doesNotThrowAnyException();
    }

    @Test
    void allowsNonDevWhenRedisPasswordIsConfigured() {
        Environment env = mock(Environment.class);
        when(env.acceptsProfiles(any(Profiles.class))).thenReturn(false);

        RedisSecurityStartupCheck check = new RedisSecurityStartupCheck(env);
        setPassword(check, "s3nha-forte");

        assertThatCode(() -> check.run(args)).doesNotThrowAnyException();
    }

    private void setPassword(RedisSecurityStartupCheck check, String password) {
        try {
            var field = RedisSecurityStartupCheck.class.getDeclaredField("redisPassword");
            field.setAccessible(true);
            field.set(check, password);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
