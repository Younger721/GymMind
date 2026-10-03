package com.gymmind.platform.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "platform-admin")
public class PlatformAdminProperties {
    private String email;
    /** 登录用户名；未配置时默认取邮箱 @ 前一段 */
    private String username;
    private String password;
    private boolean forcePasswordSync;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public boolean isForcePasswordSync() { return forcePasswordSync; }
    public void setForcePasswordSync(boolean forcePasswordSync) { this.forcePasswordSync = forcePasswordSync; }

    public boolean isConfigured() {
        return email != null && !email.isBlank() && password != null && !password.isBlank();
    }
}
