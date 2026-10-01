package com.gymmind.shared.api;

import com.gymmind.iam.api.AuthController;
import com.gymmind.iam.api.AuthInvitationController;
import com.gymmind.iam.api.RoleController;
import com.gymmind.iam.api.UserController;
import com.gymmind.iam.api.UserInvitationController;
import com.gymmind.shared.config.OpenApiConfig;
import com.gymmind.ai.api.AiAnalysisController;
import com.gymmind.ai.api.AiWorkoutPlanController;
import com.gymmind.ai.api.CoachAssistantController;
import com.gymmind.analytics.api.ChurnRiskController;
import com.gymmind.audit.api.AuditQueryController;
import com.gymmind.knowledge.api.KnowledgeSearchController;
import com.gymmind.mcp.api.McpController;
import com.gymmind.nutrition.api.NutritionController;
import com.gymmind.nutrition.api.NutritionProfileController;
import com.gymmind.search.api.ArticleSearchController;
import com.gymmind.search.api.SearchImportController;
import com.gymmind.search.api.VideoSearchController;
import com.gymmind.workout.api.WorkoutPlanController;
import com.gymmind.workout.api.WorkoutRecordController;
import com.gymmind.tenancy.api.PlatformTenantController;
import com.gymmind.tenancy.api.TenantSettingsController;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.models.OpenAPI;
import org.springdoc.core.models.GroupedOpenApi;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiContractIT {

    private static final List<Class<?>> CONTROLLERS = List.of(
            AuthController.class,
            AuthInvitationController.class,
            UserController.class,
            UserInvitationController.class,
            RoleController.class,
            TenantSettingsController.class,
            PlatformTenantController.class);

    @Test
    void definesGymMindJwtBearerScheme() {
        OpenAPI openApi = new OpenApiConfig().gymMindOpenApi();

        assertThat(openApi.getInfo().getTitle()).isEqualTo("GymMind API");
        assertThat(openApi.getInfo().getVersion()).isEqualTo("v1");
        assertThat(openApi.getComponents().getSecuritySchemes())
                .containsKey("bearerAuth");
        assertThat(openApi.getComponents().getSecuritySchemes().get("bearerAuth").getScheme())
                .isEqualTo("bearer");
        assertThat(openApi.getComponents().getSecuritySchemes().get("bearerAuth").getBearerFormat())
                .isEqualTo("JWT");
    }

    @Test
    void definesPlatformAndTenantOpenApiGroups() {
        OpenApiConfig config = new OpenApiConfig();
        GroupedOpenApi platform = config.platformApi();
        GroupedOpenApi tenant = config.tenantApi();

        assertThat(platform.getGroup()).isEqualTo("platform");
        assertThat(platform.getPathsToMatch()).containsExactly("/api/v1/platform/**");
        assertThat(platform.getPathsToExclude()).isNullOrEmpty();
        assertThat(tenant.getGroup()).isEqualTo("tenant");
        assertThat(tenant.getPathsToMatch()).containsExactly("/api/v1/**");
        assertThat(tenant.getPathsToExclude()).containsExactly("/api/v1/platform/**");
    }

    @Test
    void allBusinessControllerMappingsUseApiV1() {
        for (Class<?> controller : CONTROLLERS) {
            RequestMapping mapping = controller.getAnnotation(RequestMapping.class);
            assertThat(mapping).as("controller %s", controller.getName()).isNotNull();
            assertThat(List.of(mapping.value()))
                    .as("controller %s", controller.getName())
                    .allMatch(path -> path.startsWith("/api/v1"));
        }
    }

    @Test
    void protectedControllersOrOperationsDeclareBearerAuth() throws NoSuchMethodException {
        assertThat(UserController.class.getAnnotation(SecurityRequirement.class)).isNotNull();
        assertThat(UserInvitationController.class.getAnnotation(SecurityRequirement.class)).isNotNull();
        assertThat(RoleController.class.getAnnotation(SecurityRequirement.class)).isNotNull();
        assertThat(TenantSettingsController.class.getAnnotation(SecurityRequirement.class)).isNotNull();
        assertThat(PlatformTenantController.class.getAnnotation(SecurityRequirement.class)).isNotNull();

        Method logout = AuthController.class.getDeclaredMethod("logout",
                String.class, com.gymmind.iam.api.request.LogoutRequest.class);
        assertThat(logout.getAnnotation(SecurityRequirement.class)).isNotNull();
        assertThat(AuthController.class.getDeclaredMethod("register", com.gymmind.iam.api.request.RegisterRequest.class)
                .getAnnotation(SecurityRequirement.class)).isNull();
        assertThat(AuthInvitationController.class.getAnnotation(SecurityRequirement.class)).isNull();

        for (Class<?> controller : List.of(
                AiAnalysisController.class, CoachAssistantController.class, AiWorkoutPlanController.class,
                McpController.class, KnowledgeSearchController.class, WorkoutPlanController.class,
                WorkoutRecordController.class, NutritionController.class, ArticleSearchController.class,
                SearchImportController.class, VideoSearchController.class, ChurnRiskController.class,
                AuditQueryController.class, NutritionProfileController.class)) {
            assertThat(controller.getAnnotation(SecurityRequirement.class))
                    .as("controller %s", controller.getName()).isNotNull();
        }
    }
}
