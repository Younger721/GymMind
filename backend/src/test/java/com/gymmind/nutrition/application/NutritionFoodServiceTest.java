package com.gymmind.nutrition.application;

import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.nutrition.domain.repository.NutritionFoodRepository;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.TenantContextHolder;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NutritionFoodServiceTest {
    @Test
    void gymAdminCreatesTenantFood() {
        NutritionFoodRepository foods = mock(NutritionFoodRepository.class);
        when(foods.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        NutritionFoodService service = new DefaultNutritionFoodService(foods, mock(AuditRecorder.class));

        NutritionFoodView view = service.create(admin(11L), new CreateNutritionFoodCommand(null, "Oats", 389, 16.9, 66.3, 6.9, "100g"));

        assertThat(view.tenantId()).isEqualTo(11L);
        assertThat(view.name()).isEqualTo("Oats");
    }

    @Test
    void rejectsNegativeNutritionValues() {
        NutritionFoodService service = new DefaultNutritionFoodService(mock(NutritionFoodRepository.class), mock(AuditRecorder.class));
        assertThatThrownBy(() -> service.create(admin(11L), new CreateNutritionFoodCommand(null, "Bad", -1, 0, 0, 0, "100g")))
                .isInstanceOf(com.gymmind.shared.error.BusinessException.class);
    }

    @Test
    void platformAdminCreatesFoodInsideSelectedTenant() {
        NutritionFoodRepository foods = mock(NutritionFoodRepository.class);
        when(foods.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        TenantContextHolder.set(11L);
        try {
            NutritionFoodView view = new DefaultNutritionFoodService(foods, mock(AuditRecorder.class)).create(platform(),
                    new CreateNutritionFoodCommand(null, "Oats", 389, 16.9, 66.3, 6.9, "100g"));
            assertThat(view.tenantId()).isEqualTo(11L);
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Test
    void platformAdminWithoutSelectedTenantIsForbidden() {
        assertThatThrownBy(() -> new DefaultNutritionFoodService(mock(NutritionFoodRepository.class), mock(AuditRecorder.class))
                .create(platform(), new CreateNutritionFoodCommand(null, "Oats", 389, 16.9, 66.3, 6.9, "100g")))
                .isInstanceOf(com.gymmind.shared.error.BusinessException.class);
    }

    private static CurrentActor admin(long tenantId) {
        return new CurrentActor(1L, tenantId, Set.of(RoleCode.GYM_ADMIN), Set.of("nutrition:write"), 0L, "token");
    }

    private static CurrentActor platform() {
        return new CurrentActor(1L, null, Set.of(RoleCode.PLATFORM_ADMIN), Set.of(), 0L, "platform-token");
    }
}
