package com.gymmind.nutrition.api;

import com.gymmind.nutrition.domain.NutritionCalculator;
import com.gymmind.nutrition.domain.Sex;
import com.gymmind.nutrition.domain.MacroTargets;
import com.gymmind.shared.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/nutrition/profiles")
public class NutritionProfileController {
    private final NutritionCalculator calculator;
    public NutritionProfileController(NutritionCalculator calculator) { this.calculator = calculator; }

    @PostMapping
    public ApiResponse<NutritionProfileView> calculate(@Valid @RequestBody Request request) {
        double bmi = calculator.calculateBmi(request.weightKg(), request.heightCm());
        double bmr = calculator.calculateBmr(request.sex(), request.weightKg(), request.heightCm(), request.age());
        double tdee = calculator.calculateTdee(bmr, request.activityFactor());
        MacroTargets macros = request.calories() == null ? null : calculator.calculateMacros(request.calories(), request.proteinRatio(), request.carbohydrateRatio(), request.fatRatio());
        return ApiResponse.success(new NutritionProfileView(bmi, bmr, tdee, macros));
    }

    public record Request(@Positive double weightKg, @Positive double heightCm, @Positive int age,
                          @NotNull Sex sex, @Positive double activityFactor, Double calories,
                          double proteinRatio, double carbohydrateRatio, double fatRatio) {}
    public record NutritionProfileView(double bmi, double bmr, double tdee, MacroTargets macros) {}
}
