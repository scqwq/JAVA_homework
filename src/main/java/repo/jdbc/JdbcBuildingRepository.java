package repo.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import model.Building;
import model.BuildingGenderPolicy;
import repo.interfaces.BuildingRepository;

/**
 * {@link BuildingRepository} 的 JDBC 实现。
 * <p>
 * 给 BuildingService 提供宿舍楼持久化能力：业务层传入领域对象或查询条件，
 * 本类负责在 buildings 表完成映射；楼号重复时如何提示、是否允许创建仍由业务层决定。
 */
public class JdbcBuildingRepository implements BuildingRepository {
    private final Connection connection;

    public JdbcBuildingRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Building save(Building building) throws SQLException {
        // BuildingService 已完成重复楼号校验；这里仅把领域字段写入对应表列。
        String sql = "INSERT INTO buildings (building_code, building_name, gender_policy) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, building.buildingCode());//第一个参数是占位符的位置
            statement.setString(2, building.buildingName());
            statement.setString(3, building.genderPolicy().name());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                // 将数据库生成的 ID 回填给业务层，UI 才能展示可继续使用的宿舍楼 ID。
                return new Building(keys.getLong(1), building.buildingCode(), building.buildingName(), building.genderPolicy());
            }
        }
    }

    @Override
    public List<Building> findAll() throws SQLException {
        List<Building> buildings = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                // 列表按主键稳定排序，BuildingService 不需要了解底层排序规则即可直接交给 UI 展示。
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
                // 空结果交还 BuildingService 决定是报“未找到”还是走其他业务分支。
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
                // 新增前的重复检查只需要存在性信息，业务层再决定是否拒绝保存。
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    private Building map(ResultSet resultSet) throws SQLException {
        // 表中保存枚举常量名；转换集中在这里，避免向 BuildingService 泄露存储格式。
        return new Building(
                resultSet.getLong("building_id"),
                resultSet.getString("building_code"),
                resultSet.getString("building_name"),
                BuildingGenderPolicy.valueOf(resultSet.getString("gender_policy"))
        );
    }
}

