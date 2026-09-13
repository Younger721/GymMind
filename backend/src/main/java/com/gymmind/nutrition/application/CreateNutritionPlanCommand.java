package com.gymmind.nutrition.application; public record CreateNutritionPlanCommand(Long tenantId,Long memberId,double calories,double protein,double carbohydrate,double fat,String description) {}
