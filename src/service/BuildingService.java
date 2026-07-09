package service;

import java.sql.SQLException;
import java.util.List;

import database.DatabaseConnection;
import model.Building;
import model.BuildingGenderPolicy;

//service主要关注业务层，根据不同要求（链式）调用接口类普通类的方法实现与数据库的交互

// 宿舍楼服务类，负责处理宿舍楼相关的业务逻辑。
public class BuildingService {
    // 持有数据库连接对象，用于访问宿舍楼仓储。
    private final DatabaseConnection databaseConnection;

    // 通过构造器注入数据库连接，供当前服务复用。
    public BuildingService(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    // 新增宿舍楼：先检查楼号是否重复，再把输入转换成 Building 对象保存到数据库。
    public Building createBuilding(String code, String name, String genderPolicy) throws SQLException {
        databaseConnection.buildingRepository().findByCode(code).ifPresent(existing -> {
            throw new IllegalArgumentException("宿舍楼编号已存在: " + code);
        });
        return databaseConnection.buildingRepository().save(
                new Building(0L, code, name, BuildingGenderPolicy.fromLabel(genderPolicy))
        );
    }

    // 查询全部宿舍楼列表，供界面层展示。
    public List<Building> listBuildings() throws SQLException {
        return databaseConnection.buildingRepository().findAll();
    }

    // 根据宿舍楼 ID 查询单个宿舍楼；若不存在，则抛出业务异常。
    public Building getBuilding(long buildingId) throws SQLException {
        return databaseConnection.buildingRepository()
                .findById(buildingId)
                .orElseThrow(() -> new IllegalArgumentException("未找到宿舍楼: " + buildingId));
    }
}

