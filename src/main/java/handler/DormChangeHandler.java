package handler;

import com.sun.net.httpserver.HttpExchange;
import model.DormAssignment;
import service.DormService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

public class DormChangeHandler extends BaseHandler {
    private final DormService dormService;

    public DormChangeHandler(DormService dormService) {
        this.dormService = dormService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Map<String, String> params = formParams(exchange);
        try {
            ensureMethod(exchange, "POST");
            DormAssignment dormAssignment = dormService.changeDorm(
                    requireText(params, "studentId", "学号"),
                    requireLong(params, "buildingId", "请选择宿舍楼"),
                    requireLong(params, "roomId", "请选择房间"),
                    requireInt(params, "bedNumber", "床号")
            );
            redirect(exchange, buildRedirectUrl("调宿成功，新的床号: " + dormAssignment.bedNumber(), null, params));
        } catch (IllegalArgumentException exception) {
            redirect(exchange, buildRedirectUrl(null, exception.getMessage(), params));
        } catch (SQLException exception) {
            redirect(exchange, buildRedirectUrl(null, "数据库操作失败: " + exception.getMessage(), params));
        }
    }

    private String buildRedirectUrl(String message, String error, Map<String, String> params) {
        StringBuilder url = new StringBuilder("/?");
        boolean needsAmpersand = false;
        if (message != null && !message.isBlank()) {
            url.append("message=").append(encode(message));
            needsAmpersand = true;
        }
        if (error != null && !error.isBlank()) {
            if (needsAmpersand) {
                url.append("&");
            }
            url.append("error=").append(encode(error));
            needsAmpersand = true;
        }
        needsAmpersand = appendParam(url, "changeStudentId", params.get("studentId"), needsAmpersand);
        needsAmpersand = appendParam(url, "changeBuildingId", params.get("buildingId"), needsAmpersand);
        needsAmpersand = appendParam(url, "changeRoomId", params.get("roomId"), needsAmpersand);
        appendParam(url, "changeBedNumber", params.get("bedNumber"), needsAmpersand);
        url.append("#change-dorm");
        return url.toString();
    }

    private boolean appendParam(StringBuilder url, String key, String value, boolean needsAmpersand) {
        if (value == null || value.isBlank()) {
            return needsAmpersand;
        }
        if (needsAmpersand) {
            url.append("&");
        }
        url.append(key).append("=").append(encode(value));
        return true;
    }
}
