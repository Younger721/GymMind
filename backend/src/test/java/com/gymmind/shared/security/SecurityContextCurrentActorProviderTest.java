package com.gymmind.shared.security;

import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.support.SecurityTestActors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityContextCurrentActorProviderTest {

    private final CurrentActorProvider provider = new SecurityContextCurrentActorProvider();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsActorOnlyForAuthenticatedGymMindPrincipal() {
        CurrentActor actor = SecurityTestActors.gymAdmin();
        GymMindPrincipal principal = new GymMindPrincipal(actor);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "", List.of()));

        assertThat(provider.current()).containsSame(actor);
        assertThat(provider.requireCurrent()).isSameAs(actor);
    }

    @Test
    void rejectsMissingAuthentication() {
        assertUnauthenticated();
    }

    @Test
    void rejectsUnauthenticatedToken() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        new GymMindPrincipal(SecurityTestActors.gymAdmin()), ""));

        assertUnauthenticated();
    }

    @Test
    void rejectsAnonymousAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "test-key",
                new GymMindPrincipal(SecurityTestActors.gymAdmin()),
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertUnauthenticated();
    }

    @Test
    void rejectsAuthenticatedForeignPrincipal() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("foreign-principal", "secret", List.of()));

        assertUnauthenticated();
    }

    @Test
    void principalNeverExposesCredentialsOrTokenId() {
        GymMindPrincipal principal = new GymMindPrincipal(SecurityTestActors.gymAdmin());

        assertThat(principal.getCredentials()).isEqualTo("");
        assertThat(principal.getName()).isEqualTo("2");
        assertThat(principal.toString())
                .contains("userId=2", "tenantId=10")
                .doesNotContain("tenant-token-id", "credentials", "tenant:settings:write");
    }

    private void assertUnauthenticated() {
        assertThat(provider.current()).isEmpty();
        assertThatThrownBy(provider::requireCurrent)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHENTICATED);
    }
}
