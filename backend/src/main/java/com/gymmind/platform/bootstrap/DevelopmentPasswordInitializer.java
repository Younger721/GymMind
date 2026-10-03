package com.gymmind.platform.bootstrap;

import com.gymmind.iam.domain.model.UserAccount;
import com.gymmind.iam.domain.repository.UserAccountRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/** 开发环境统一初始化演示账号密码。 */
@Component
@Profile("dev")
@Order(210)
public class DevelopmentPasswordInitializer implements ApplicationRunner {
    private static final String DEVELOPMENT_PASSWORD = "123456";
    private static final Set<String> DEMO_USERNAMES = Set.of(
            "admin", "admin-2", "gymadmin", "coach", "member", "codex_ai_test", "debugai");

    private final UserAccountRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${gymmind.dev.reset-demo-password:false}")
    private boolean resetDemoPassword;

    public DevelopmentPasswordInitializer(UserAccountRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!resetDemoPassword) {
            return;
        }
        userRepository.findAll().stream()
                .filter(user -> DEMO_USERNAMES.contains(user.getNormalizedUsername()))
                .filter(user -> !passwordEncoder.matches(DEVELOPMENT_PASSWORD, user.getPasswordHash()))
                .forEach(user -> {
                    user.resetPassword(passwordEncoder.encode(DEVELOPMENT_PASSWORD));
                    userRepository.save(user);
                });
    }
}
