package handler;

import com.sun.net.httpserver.HttpExchange;
import model.Building;
import service.BuildingService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

public class BuildingCreateHandler extends BaseHandler {
    private final BuildingService buildingService;

    public BuildingCreateHandler(BuildingService buildingService) {
        this.buildingService = buildingService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            ensureMethod(exchange, "POST");
            Map<String, String> params = formParams(exchange);
            Building building = buildingService.createBuilding(
                    requireText(params, "code", "楼栋编号"),
                    requireText(params, "name", "楼栋名称"),
                    requireText(params, "genderPolicy", "入住策略")
            );
            redirect(exchange, "/?message=" + encode("宿舍楼创建成功: " + building.buildingName()));
        } catch (IllegalArgumentException exception) {
            redirect(exchange, "/?error=" + encode(exception.getMessage()));
        } catch (SQLException exception) {
            redirect(exchange, "/?error=" + encode("数据库操作失败: " + exception.getMessage()));
        }
    }
}
