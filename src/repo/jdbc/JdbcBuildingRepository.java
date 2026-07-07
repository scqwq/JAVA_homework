package repo.jdbc;

import model.Building;
import model.BuildingGenderPolicy;
import repo.interfaces.BuildingRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcBuildingRepository implements BuildingRepository {
    private final Connection connection;

    public JdbcBuildingRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Building save(Building building) throws SQLException {
        String sql = "INSERT INTO buildings (building_code, building_name, gender_policy) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, building.buildingCode());
            statement.setString(2, building.buildingName());
            statement.setString(3, building.genderPolicy().name());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return new Building(keys.getLong(1), building.buildingCode(), building.buildingName(), building.genderPolicy());
            }
        }
    }

    @Override
    public List<Building> findAll() throws SQLException {
        List<Building> buildings = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT building_id, building_code, building_name, gender_policy FROM buildings ORDER BY building_id");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                buildings.add(map(resultSet));
            }
        }
        return buildings;
    }

    @Override
    public Optional<Building> findById(long buildingId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT building_id, building_code, building_name, gender_policy FROM buildings WHERE building_id = ?")) {
            statement.setLong(1, buildingId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    @Override
    public Optional<Building> findByCode(String buildingCode) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT building_id, building_code, building_name, gender_policy FROM buildings WHERE building_code = ?")) {
            statement.setString(1, buildingCode);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    private Building map(ResultSet resultSet) throws SQLException {
        return new Building(
                resultSet.getLong("building_id"),
                resultSet.getString("building_code"),
                resultSet.getString("building_name"),
                BuildingGenderPolicy.valueOf(resultSet.getString("gender_policy"))
        );
    }
}

