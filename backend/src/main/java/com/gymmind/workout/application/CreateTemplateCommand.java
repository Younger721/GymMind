package com.gymmind.workout.application;
import java.util.*;
public record CreateTemplateCommand(String name,String category,String goal,String description,List<Item> items){
 public record Item(Long exerciseId,String exerciseName,String phase,int sortOrder,int sets,int reps,int restSeconds,double weight,String notes){
  public Item(String exerciseName,String phase,int sortOrder,int sets,int reps,int restSeconds,double weight,String notes){this(null,exerciseName,phase,sortOrder,sets,reps,restSeconds,weight,notes);}
 }
}
