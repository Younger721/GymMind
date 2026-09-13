package com.gymmind.exercise.domain.model;

import com.gymmind.shared.persistence.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "exercise", uniqueConstraints = @UniqueConstraint(
        name = "uk_exercise_tenant_name", columnNames = {"tenant_id", "name"}))
public class Exercise extends TenantScopedEntity {
    @Column(nullable = false, length = 128)
    private String name;
    @Column(nullable = false, length = 64)
    private String category;
    @Column(name = "target_muscle", nullable = false, length = 128)
    private String targetMuscle;
    @Column(nullable = false, length = 32)
    private String difficulty;
    @Column(nullable = false, length = 128)
    private String equipment;
    @Column(nullable = false, length = 2000)
    private String description;
    @Column(nullable = false, length = 4000)
    private String steps;
    @Column(name = "common_mistakes", nullable = false, length = 2000)
    private String commonMistakes;
    @Column(name = "safety_notes", nullable = false, length = 2000)
    private String safetyNotes;
    @Column(nullable = false, length = 1000)
    private String tags;

    protected Exercise() {}

    private Exercise(Long tenantId, String name, String category, String targetMuscle, String difficulty,
                     String equipment, String description, String steps, String commonMistakes,
                     String safetyNotes, String tags) {
        super(tenantId);
        this.name = required(name, "name");
        this.category = required(category, "category");
        this.targetMuscle = required(targetMuscle, "targetMuscle");
        this.difficulty = required(difficulty, "difficulty");
        this.equipment = required(equipment, "equipment");
        this.description = required(description, "description");
        this.steps = required(steps, "steps");
        this.commonMistakes = required(commonMistakes, "commonMistakes");
        this.safetyNotes = required(safetyNotes, "safetyNotes");
        this.tags = required(tags, "tags");
    }

    public static Exercise create(Long tenantId, String name, String category, String targetMuscle,
                                  String difficulty, String equipment, String description, String steps,
                                  String commonMistakes, String safetyNotes, String tags) {
        return new Exercise(tenantId, name, category, targetMuscle, difficulty, equipment, description,
                steps, commonMistakes, safetyNotes, tags);
    }

    public void update(String name, String category, String targetMuscle, String difficulty, String equipment,
                       String description, String steps, String commonMistakes, String safetyNotes, String tags) {
        this.name = required(name, "name");
        this.category = required(category, "category");
        this.targetMuscle = required(targetMuscle, "targetMuscle");
        this.difficulty = required(difficulty, "difficulty");
        this.equipment = required(equipment, "equipment");
        this.description = required(description, "description");
        this.steps = required(steps, "steps");
        this.commonMistakes = required(commonMistakes, "commonMistakes");
        this.safetyNotes = required(safetyNotes, "safetyNotes");
        this.tags = required(tags, "tags");
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.trim();
    }

    public Long getId() { return super.getId(); }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getTargetMuscle() { return targetMuscle; }
    public String getDifficulty() { return difficulty; }
    public String getEquipment() { return equipment; }
    public String getDescription() { return description; }
    public String getSteps() { return steps; }
    public String getCommonMistakes() { return commonMistakes; }
    public String getSafetyNotes() { return safetyNotes; }
    public String getTags() { return tags; }
}
