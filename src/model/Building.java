package model;

import model.BuildingGenderPolicy;

public record Building(
        long buildingId,
        String buildingCode,
        String buildingName,
        BuildingGenderPolicy genderPolicy
) {
}

