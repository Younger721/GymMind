package com.gymmind.nutrition.api;

import com.gymmind.nutrition.domain.DefaultNutritionCalculator;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class NutritionProfileControllerTest {
    @Test
    void calculatesDeterministicNutritionProfile() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new NutritionProfileController(new DefaultNutritionCalculator())).build();
        mvc.perform(post("/api/v1/nutrition/profiles")
                        .contentType("application/json")
                        .content("{\"weightKg\":80,\"heightCm\":180,\"age\":30,\"sex\":\"MALE\",\"activityFactor\":1.55}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bmi").value(24.691358024691358))
                .andExpect(jsonPath("$.data.bmr").value(1780.0))
                .andExpect(jsonPath("$.data.tdee").value(2759.0));
    }
}
