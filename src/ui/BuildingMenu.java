package ui;

import model.Building;
import service.BuildingService;

import java.sql.SQLException;

public final class BuildingMenu {
    private BuildingMenu() {
    }

    public static void start(BuildingService buildingService) throws SQLException {
        while (true) {
            ConsoleUtils.printHeader("宿舍楼管理");
            System.out.println("1. 新增宿舍楼");
            System.out.println("2. 查看宿舍楼列表");
            System.out.println("0. 返回上级菜单");

            switch (ConsoleUtils.readInt("请选择操作")) {
                case 1 -> createBuilding(buildingService);
                case 2 -> listBuildings(buildingService);
                case 0 -> {
                    return;
                }
                default -> System.out.println("无效的选项，请重新输入。");
            }
        }
    }

    private static void createBuilding(BuildingService buildingService) throws SQLException {
        try {
            String code = ConsoleUtils.readLine("输入宿舍楼号");
            String name = ConsoleUtils.readLine("输入宿舍楼名");
            String genderPolicy = ConsoleUtils.readLine("输入宿舍楼性别（男/女/男女分层）");
            Building building = buildingService.createBuilding(code, name, genderPolicy);
            System.out.println("新增成功，宿舍楼ID: " + building.buildingId());
        } catch (IllegalArgumentException exception) {
            System.out.println("新增失败: " + exception.getMessage());
        }
        ConsoleUtils.waitForEnter();
    }

    private static void listBuildings(BuildingService buildingService) throws SQLException {
        for (Building building : buildingService.listBuildings()) {
            System.out.printf("ID=%d, 楼号=%s, 楼名=%s, 性别=%s%n",
                    building.buildingId(),
                    building.buildingCode(),
                    building.buildingName(),
                    building.genderPolicy().label());
        }
        ConsoleUtils.waitForEnter();
    }
}

