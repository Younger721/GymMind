package com.gymmind.platform.bootstrap;

import com.gymmind.iam.domain.model.UserAccount;
import com.gymmind.iam.domain.repository.UserAccountRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 为升级前已存在的账号补全登录用户名，避免新增唯一约束后无法登录。
 */
@Component
@Order(150)
public class UserAccountUsernameBackfillInitializer implements ApplicationRunner {

    private final UserAccountRepository users;
    private final DatabaseInitializationLock initializationLock;

    public UserAccountUsernameBackfillInitializer(UserAccountRepository users,
                                                    DatabaseInitializationLock initializationLock) {
        this.users = users;
        this.initializationLock = initializationLock;
    }

    @Override
    public void run(ApplicationArguments args) {
        initializationLock.execute("gymmind:user-username-backfill", this::backfill);
    }

    private void backfill() {
        for (UserAccount user : users.findAll()) {
            if (user.getNormalizedUsername() != null && !user.getNormalizedUsername().isBlank()) {
                continue;
            }
            String base = UserAccount.defaultUsernameFromEmail(user.getNormalizedEmail());
            String candidate = base;
            if (usernameTaken(candidate, user.getId())) {
                candidate = base + "-" + user.getId();
            }
            user.backfillUsername(candidate);
            users.save(user);
        }
    }

    private boolean usernameTaken(String username, Long userId) {
        return users.findByNormalizedUsername(username)
                .filter(existing -> !existing.getId().equals(userId))
                .isPresent();
    }
}
