package com.gymmind.equipment.api;

import com.gymmind.equipment.application.EquipmentService;
import com.gymmind.equipment.application.EquipmentRequestView;
import com.gymmind.equipment.application.PlatformEquipmentInventoryView;
import com.gymmind.shared.error.GlobalExceptionHandler;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.CurrentActorProvider;
import com.gymmind.iam.domain.model.RoleCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EquipmentControllerTest {
    private EquipmentService service;
    private CurrentActorProvider actors;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(EquipmentService.class);
        actors = mock(CurrentActorProvider.class);
        mvc = MockMvcBuilders.standaloneSetup(new EquipmentController(service, actors))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createRequestMapsCatalogItemsAndNote() throws Exception {
        CurrentActor actor = new CurrentActor(7L, 11L, java.util.Set.of(RoleCode.GYM_ADMIN), java.util.Set.of(), 0L, "token");
        when(actors.requireCurrent()).thenReturn(actor);
        when(service.createRequest(eq(actor), eq("补充有氧器械"), any())).thenReturn(mock(EquipmentRequestView.class));

        mvc.perform(post("/api/v1/equipment/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"note":"补充有氧器械","items":[{"catalogId":3,"quantity":4}]}
                                """))
                .andExpect(status().isCreated());

        verify(service).createRequest(eq(actor), eq("补充有氧器械"), any());
    }

    @Test
    void warehouseInventoryRequiresCurrentActorAndReturnsPlatformData() throws Exception {
        CurrentActor actor = new CurrentActor(1L, null, java.util.Set.of(RoleCode.PLATFORM_ADMIN), java.util.Set.of(), 0L, "platform-token");
        when(actors.requireCurrent()).thenReturn(actor);
        when(service.listWarehouseInventory(actor)).thenReturn(java.util.List.of(
                new PlatformEquipmentInventoryView(3L, "跑步机", "有氧器械", "台", 10, 2, 4, 8, 20, java.time.Instant.now())));

        mvc.perform(get("/api/v1/equipment/warehouse/inventory"))
                .andExpect(status().isOk());

        verify(service).listWarehouseInventory(actor);
    }
}
