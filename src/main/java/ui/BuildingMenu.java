package ui;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import model.Building;
import service.BuildingService;

/**
 * 宿舍楼管理菜单，负责与用户交互的终端页面，相当于前端表现层。
 * <p>
 * 接收用户输入并调用 {@link BuildingService} 完成业务操作，再将结果输出到控制台。
 */
public final class BuildingMenu {

    // 私有构造器，防止实例化；所有方法均为静态工具方法。
    private BuildingMenu() {
    }

    // 显示宿舍楼管理子菜单，并根据用户选择分发到对应操作。
    public static void start(BuildingService buildingService) throws SQLException {
        while (true) {
            ConsoleUtils.printHeader("宿舍楼管理");
            String[] headers = {"编号", "操作"};
            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"[1]", "新增宿舍楼"});
            rows.add(new String[]{"[2]", "查看宿舍楼列表"});
            rows.add(new String[]{"[3]", "删除宿舍楼"});
            rows.add(new String[]{"[0]", "返回上级菜单"});
            ConsoleUtils.printTable(headers, rows);
            ConsoleUtils.printSeparator();

            switch (ConsoleUtils.readInt("请选择操作")) {
                case 1 -> createBuilding(buildingService);
                case 2 -> listBuildings(buildingService);
                case 3 -> deleteBuilding(buildingService);
                case 0 -> {
                    return;
                }
                default -> ConsoleUtils.printError("无效的选项，请重新输入。");
            }
        }
    }

    // 读取用户输入并调用服务新增宿舍楼；失败时输出错误提示。
    private static void createBuilding(BuildingService buildingService) throws SQLException {
        ConsoleUtils.printSubHeader("新增宿舍楼");
        try {
            String code = ConsoleUtils.readLine("输入宿舍楼号");
            String name = ConsoleUtils.readLine("输入宿舍楼名");
            String genderPolicy = ConsoleUtils.readLine("输入宿舍楼性别（男/女/男女分层）");
            Building building = buildingService.createBuilding(code, name, genderPolicy);
            ConsoleUtils.printSuccess("新增成功，宿舍楼ID: " + building.buildingId());
        } catch (IllegalArgumentException exception) {
            ConsoleUtils.printError("新增失败: " + exception.getMessage());
        }
        ConsoleUtils.waitForEnter();
    }

    // 列出全部宿舍楼并以表格形式输出。
    private static void listBuildings(BuildingService buildingService) throws SQLException {
        ConsoleUtils.printSubHeader("宿舍楼列表");
        List<Building> buildings = buildingService.listBuildings();
        String[] headers = {"ID", "楼号", "楼名", "性别策略"};
        List<String[]> rows = new ArrayList<>();
        for (Building building : buildings) {
            rows.add(new String[]{
                    String.valueOf(building.buildingId()),
                    building.buildingCode(),
                    building.buildingName(),
                    building.genderPolicy().label()
            });
        }
        ConsoleUtils.printTable(headers, rows);
        ConsoleUtils.waitForEnter();
    }

    // 根据楼栋 ID 删除宿舍楼；数据库会级联删除其房间和住宿记录。
    private static void deleteBuilding(BuildingService buildingService) throws SQLException {
        ConsoleUtils.printSubHeader("删除宿舍楼");
        try {
            long buildingId = Long.parseLong(ConsoleUtils.readLine("输入要删除的宿舍楼 ID").trim());
            Building building = buildingService.deleteBuilding(buildingId);
            ConsoleUtils.printSuccess("删除成功: " + building.buildingName() + "（ID " + building.buildingId() + "）");
        } catch (NumberFormatException exception) {
            ConsoleUtils.printError("删除失败: 宿舍楼 ID 必须是数字");
        } catch (IllegalArgumentException exception) {
            ConsoleUtils.printError("删除失败: " + exception.getMessage());
        }
        ConsoleUtils.waitForEnter();
    }
}
