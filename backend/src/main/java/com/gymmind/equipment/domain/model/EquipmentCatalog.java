package com.gymmind.equipment.domain.model;

import com.gymmind.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "equipment_catalog", uniqueConstraints = @UniqueConstraint(name = "uk_equipment_catalog_name", columnNames = "name"))
public class EquipmentCatalog extends AuditableEntity {
    @Column(nullable = false, length = 128)
    private String name;
    @Column(nullable = false, length = 64)
    private String category;
    @Column(length = 1000)
    private String description;
    @Column(nullable = false, length = 16)
    private String unit = "台";
    @Column(nullable = false)
    private boolean active = true;

    protected EquipmentCatalog() {
    }

    private EquipmentCatalog(String name, String category, String description, String unit) {
        update(name, category, description, unit);
    }

    public static EquipmentCatalog create(String name, String category, String description, String unit) {
        return new EquipmentCatalog(name, category, description, unit);
    }

    public void update(String name, String category, String description, String unit) {
        if (name == null || name.isBlank() || category == null || category.isBlank()) {
            throw new IllegalArgumentException("Equipment catalog name and category are required");
        }
        this.name = name.trim();
        this.category = category.trim();
        this.description = description == null || description.isBlank() ? null : description.trim();
        this.unit = unit == null || unit.isBlank() ? "台" : unit.trim();
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public String getUnit() { return unit; }
    public boolean isActive() { return active; }
}
