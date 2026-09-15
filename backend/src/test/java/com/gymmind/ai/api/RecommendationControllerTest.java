package com.gymmind.ai.api;

import com.gymmind.ai.application.*;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.security.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RecommendationControllerTest {
    @Test
    void delegatesRecommendationWithCurrentActor() throws Exception {
        var service = mock(RecommendationService.class);
        var actor = new CurrentActor(2L, 7L, Set.of(RoleCode.MEMBER), Set.of("ai:recommend"), 0, "t");
        var actors = new CurrentActorProvider() { public Optional<CurrentActor> current(){return Optional.of(actor);} public CurrentActor requireCurrent(){return actor;} };
        when(service.recommend(eq(actor), eq("strength"), anySet())).thenReturn(List.of());
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new RecommendationController(service, actors)).build();
        mvc.perform(post("/api/v1/ai/recommendations").contentType("application/json").content("{\"goal\":\"strength\",\"healthConstraints\":[\"knee\"]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        verify(service).recommend(eq(actor), eq("strength"), eq(Set.of("knee")));
    }
}
