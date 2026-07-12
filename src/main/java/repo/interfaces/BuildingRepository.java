package repo.interfaces;

import model.Building;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface BuildingRepository {
    // 新增宿舍楼时，将缓存存入数据库
    Building save(Building building) throws SQLException;
    // 查看所有宿舍楼
    List<Building> findAll() throws SQLException;
    
    Optional<Building> findById(long buildingId) throws SQLException;
    
    Optional<Building> findByCode(String buildingCode) throws SQLException;
}

