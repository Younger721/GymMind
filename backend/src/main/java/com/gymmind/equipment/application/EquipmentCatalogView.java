package com.gymmind.equipment.application;

import com.gymmind.equipment.domain.model.EquipmentCatalog;

public record EquipmentCatalogView(Long id, String name, String category, String description, String unit, boolean active) {
    public static EquipmentCatalogView from(EquipmentCatalog catalog) {
        return new EquipmentCatalogView(catalog.getId(), catalog.getName(), catalog.getCategory(), catalog.getDescription(), catalog.getUnit(), catalog.isActive());
    }
}
