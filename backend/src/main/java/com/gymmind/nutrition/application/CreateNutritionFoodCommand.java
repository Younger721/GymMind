package com.gymmind.nutrition.application; public record CreateNutritionFoodCommand(Long tenantId,String name,double calories,double protein,double carbohydrate,double fat,String servingSize) {}
