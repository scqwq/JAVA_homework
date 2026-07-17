package handler;

import com.sun.net.httpserver.HttpExchange;
import model.Building;
import service.BuildingService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

public class BuildingDeleteHandler extends BaseHandler {
    private final BuildingService buildingService;

    public BuildingDeleteHandler(BuildingService buildingService) {
        this.buildingService = buildingService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            ensureMethod(exchange, "POST");
            Map<String, String> params = formParams(exchange);
            Building building = buildingService.deleteBuilding(
                    requireLong(params, "buildingId", "宿舍楼 ID")
            );
            redirect(exchange, "/?tab=buildings&message=" + encode("宿舍楼删除成功: " + building.buildingName()));
        } catch (IllegalArgumentException exception) {
            redirect(exchange, "/?tab=buildings&error=" + encode(exception.getMessage()));
        } catch (SQLException exception) {
            redirect(exchange, "/?tab=buildings&error=" + encode("数据库操作失败: " + exception.getMessage()));
        }
    }
}
