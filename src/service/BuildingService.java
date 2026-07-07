package service;

import model.Building;
import model.BuildingGenderPolicy;
import database.DatabaseConnection;

import java.sql.SQLException;
import java.util.List;

public class BuildingService {
    private final DatabaseConnection databaseConnection;

    public BuildingService(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    public Building createBuilding(String code, String name, String genderPolicy) throws SQLException {
        databaseConnection.buildingRepository().findByCode(code).ifPresent(existing -> {
            throw new IllegalArgumentException("宿舍楼编号已存在: " + code);
        });
        return databaseConnection.buildingRepository().save(
                new Building(0L, code, name, BuildingGenderPolicy.fromLabel(genderPolicy))
        );
    }

    public List<Building> listBuildings() throws SQLException {
        return databaseConnection.buildingRepository().findAll();
    }

    public Building getBuilding(long buildingId) throws SQLException {
        return databaseConnection.buildingRepository()
                .findById(buildingId)
                .orElseThrow(() -> new IllegalArgumentException("未找到宿舍楼: " + buildingId));
    }
}

