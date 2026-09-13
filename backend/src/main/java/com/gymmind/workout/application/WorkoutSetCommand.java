package com.gymmind.workout.application; public record WorkoutSetCommand(Long exerciseId,int setNumber,double weight,int reps,int duration,boolean completed) {}
