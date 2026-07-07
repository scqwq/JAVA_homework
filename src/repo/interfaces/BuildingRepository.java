package repo.interfaces;

import model.Building;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface BuildingRepository {
    Building save(Building building) throws SQLException;

    List<Building> findAll() throws SQLException;

    Optional<Building> findById(long buildingId) throws SQLException;

    Optional<Building> findByCode(String buildingCode) throws SQLException;
}

