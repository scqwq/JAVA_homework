package handler;

import com.sun.net.httpserver.HttpExchange;
import model.Building;
import model.BuildingOccupancy;
import model.OccupancyOverview;
import model.Room;
import model.Student;
import model.StudentDormView;
import service.BuildingService;
import service.DormService;
import service.RoomService;
import service.StudentService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
// 用内嵌的CSS央视和JS标签 ，用Java StringBuilder 拼接生成前端
public class DashboardHandler extends BaseHandler {
    private static final Set<String> VALID_TABS = Set.of(
            "dashboard", "students", "buildings", "rooms", "dorms", "queries"
    );

    private final BuildingService buildingService;
    private final RoomService roomService;
    private final StudentService studentService;
    private final DormService dormService;

    public DashboardHandler(
            BuildingService buildingService,
            RoomService roomService,
            StudentService studentService,
            DormService dormService
    ) {
        this.buildingService = buildingService;
        this.roomService = roomService;
        this.studentService = studentService;
        this.dormService = dormService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Map<String, String> query = queryParams(exchange);
        List<Building> buildings = List.of();
        List<Room> rooms = List.of();
        List<Student> students = List.of();
        OccupancyOverview occupancy = new OccupancyOverview(0, 0, 0, 0.0, List.of());
        StudentDormView dormByStudent = null;
        List<StudentDormView> studentsByRoom = List.of();
        Room roomByLocation = null;
        String activeTab = normalizeTab(query.getOrDefault("tab", "dashboard"));

        try {
            ensureMethod(exchange, "GET");
            buildings = buildingService.listBuildings();
            rooms = roomService.listRooms();
            students = studentService.listStudents();
            occupancy = dormService.getOccupancyOverview();

            String lookupStudentId = query.getOrDefault("lookupStudentId", "").trim();
            if (!lookupStudentId.isBlank()) {
                dormByStudent = dormService.findDormByStudent(lookupStudentId);
                activeTab = "queries";
            }

            String lookupRoomId = query.getOrDefault("lookupRoomId", "").trim();
            if (!lookupRoomId.isBlank()) {
                studentsByRoom = dormService.findStudentsByRoom(Long.parseLong(lookupRoomId));
                activeTab = "queries";
            }

            String lookupBuildingId = query.getOrDefault("lookupBuildingId", "").trim();
            String lookupFloorNumber = query.getOrDefault("lookupFloorNumber", "").trim();
            String lookupRoomNumber = query.getOrDefault("lookupRoomNumber", "").trim();
            if (!lookupBuildingId.isBlank() || !lookupFloorNumber.isBlank() || !lookupRoomNumber.isBlank()) {
                roomByLocation = roomService.getRoomByLocation(
                        Long.parseLong(lookupBuildingId),
                        Integer.parseInt(lookupFloorNumber),
                        lookupRoomNumber
                );
                studentsByRoom = dormService.findStudentsByRoom(roomByLocation.roomId());
                activeTab = "queries";
            }

            sendHtml(exchange, 200, renderPage(
                    activeTab,
                    query.getOrDefault("message", ""),
                    query.getOrDefault("error", ""),
                    buildings,
                    rooms,
                    students,
                    occupancy,
                    dormByStudent,
                    studentsByRoom,
                    query.getOrDefault("lookupStudentId", ""),
                    query.getOrDefault("lookupRoomId", ""),
                    roomByLocation,
                    query.getOrDefault("lookupBuildingId", ""),
                    query.getOrDefault("lookupFloorNumber", ""),
                    query.getOrDefault("lookupRoomNumber", ""),
                    query.getOrDefault("changeStudentId", ""),
                    query.getOrDefault("changeBuildingId", ""),
                    query.getOrDefault("changeRoomId", ""),
                    query.getOrDefault("changeBedNumber", "")
            ));
        } catch (IllegalArgumentException exception) {
            if (buildings.isEmpty() && rooms.isEmpty() && students.isEmpty()) {
                try {
                    buildings = buildingService.listBuildings();
                    rooms = roomService.listRooms();
                    students = studentService.listStudents();
                    occupancy = dormService.getOccupancyOverview();
                } catch (SQLException sqlException) {
                    sendHtml(exchange, 500, renderErrorPage("数据库操作失败: " + sqlException.getMessage()));
                    return;
                }
            }
            sendHtml(exchange, 400, renderPage(
                    activeTab,
                    query.getOrDefault("message", ""),
                    exception.getMessage(),
                    buildings,
                    rooms,
                    students,
                    occupancy,
                    dormByStudent,
                    studentsByRoom,
                    query.getOrDefault("lookupStudentId", ""),
                    query.getOrDefault("lookupRoomId", ""),
                    roomByLocation,
                    query.getOrDefault("lookupBuildingId", ""),
                    query.getOrDefault("lookupFloorNumber", ""),
                    query.getOrDefault("lookupRoomNumber", ""),
                    query.getOrDefault("changeStudentId", ""),
                    query.getOrDefault("changeBuildingId", ""),
                    query.getOrDefault("changeRoomId", ""),
                    query.getOrDefault("changeBedNumber", "")
            ));
        } catch (SQLException exception) {
            sendHtml(exchange, 500, renderPage(
                    activeTab,
                    query.getOrDefault("message", ""),
                    "数据库操作失败: " + exception.getMessage(),
                    buildings,
                    rooms,
                    students,
                    occupancy,
                    dormByStudent,
                    studentsByRoom,
                    query.getOrDefault("lookupStudentId", ""),
                    query.getOrDefault("lookupRoomId", ""),
                    roomByLocation,
                    query.getOrDefault("lookupBuildingId", ""),
                    query.getOrDefault("lookupFloorNumber", ""),
                    query.getOrDefault("lookupRoomNumber", ""),
                    query.getOrDefault("changeStudentId", ""),
                    query.getOrDefault("changeBuildingId", ""),
                    query.getOrDefault("changeRoomId", ""),
                    query.getOrDefault("changeBedNumber", "")
            ));
        }
    }

    private String renderPage(
            String activeTab,
            String message,
            String error,
            List<Building> buildings,
            List<Room> rooms,
            List<Student> students,
            OccupancyOverview occupancy,
            StudentDormView dormByStudent,
            List<StudentDormView> studentsByRoom,
            String lookupStudentId,
            String lookupRoomId,
            Room roomByLocation,
            String lookupBuildingId,
            String lookupFloorNumber,
            String lookupRoomNumber,
            String changeStudentId,
            String changeBuildingId,
            String changeRoomId,
            String changeBedNumber
    ) {
        StringBuilder html = new StringBuilder("""
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>琉璃子管理系统</title>
                    <style>
                        :root {
                            --bg: #f4f6f8;
                            --surface: #ffffff;
                            --ink: #1f2937;
                            --subtle: #6b7280;
                            --line: #e5e7eb;
                            --primary: #2563eb;
                            --primary-soft: #dbeafe;
                            --accent: #7c3aed;
                            --accent-soft: #ede9fe;
                            --success: #059669;
                            --success-soft: #d1fae5;
                            --danger: #dc2626;
                            --danger-soft: #fee2e2;
                            --radius: 16px;
                            --shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
                        }
                        * { box-sizing: border-box; }
                        body {
                            margin: 0;
                            font-family: "Microsoft YaHei", "PingFang SC", -apple-system, BlinkMacSystemFont, sans-serif;
                            color: var(--ink);
                            background: var(--bg);
                            line-height: 1.5;
                        }
                        header {
                            position: sticky;
                            top: 0;
                            z-index: 100;
                            background: rgba(255, 255, 255, 0.92);
                            backdrop-filter: blur(10px);
                            border-bottom: 1px solid var(--line);
                            box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
                        }
                        .header-inner {
                            max-width: 1400px;
                            margin: 0 auto;
                            padding: 18px 24px;
                            display: flex;
                            flex-wrap: wrap;
                            align-items: center;
                            justify-content: space-between;
                            gap: 16px;
                        }
                        h1 {
                            margin: 0;
                            font-size: 22px;
                            display: flex;
                            align-items: center;
                            gap: 10px;
                        }
                        .subtitle {
                            margin: 4px 0 0;
                            font-size: 13px;
                            color: var(--subtle);
                        }
                        nav {
                            display: flex;
                            flex-wrap: wrap;
                            gap: 8px;
                        }
                        nav a {
                            padding: 8px 16px;
                            border-radius: 999px;
                            text-decoration: none;
                            font-size: 14px;
                            font-weight: 500;
                            color: var(--subtle);
                            transition: all 0.15s ease;
                        }
                        nav a:hover {
                            background: var(--primary-soft);
                            color: var(--primary);
                        }
                        nav a.active {
                            background: var(--primary);
                            color: #fff;
                            box-shadow: 0 2px 8px rgba(37, 99, 235, 0.25);
                        }
                        main {
                            max-width: 1400px;
                            margin: 0 auto;
                            padding: 24px;
                        }
                        .notice, .error {
                            padding: 14px 18px;
                            border-radius: var(--radius);
                            margin-bottom: 20px;
                            font-size: 14px;
                            display: flex;
                            align-items: center;
                            gap: 10px;
                        }
                        .notice {
                            background: var(--success-soft);
                            color: var(--success);
                            border: 1px solid #a7f3d0;
                        }
                        .error {
                            background: var(--danger-soft);
                            color: var(--danger);
                            border: 1px solid #fecaca;
                        }
                        .tab-content { display: none; animation: fadeIn 0.25s ease; }
                        .tab-content.active { display: block; }
                        @keyframes fadeIn {
                            from { opacity: 0; transform: translateY(6px); }
                            to { opacity: 1; transform: translateY(0); }
                        }
                        .stats {
                            display: grid;
                            grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
                            gap: 16px;
                            margin-bottom: 24px;
                        }
                        .stat-card, .card {
                            background: var(--surface);
                            border-radius: var(--radius);
                            box-shadow: var(--shadow);
                            border: 1px solid var(--line);
                        }
                        .stat-card { padding: 20px; }
                        .stat-card h3 {
                            margin: 0 0 6px;
                            font-size: 13px;
                            color: var(--subtle);
                            font-weight: 500;
                        }
                        .stat-card .number {
                            font-size: 32px;
                            font-weight: 700;
                            color: var(--primary);
                        }
                        .card {
                            padding: 24px;
                            margin-bottom: 20px;
                        }
                        .card h2 {
                            margin: 0 0 18px;
                            font-size: 18px;
                            display: flex;
                            align-items: center;
                            gap: 8px;
                        }
                        .two-col {
                            display: grid;
                            grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
                            gap: 20px;
                        }
                        .form-grid {
                            display: grid;
                            grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
                            gap: 16px;
                        }
                        .form-group {
                            display: flex;
                            flex-direction: column;
                            gap: 6px;
                        }
                        label {
                            font-size: 13px;
                            color: var(--subtle);
                            font-weight: 500;
                        }
                        input, select {
                            padding: 10px 12px;
                            border-radius: 10px;
                            border: 1px solid var(--line);
                            font-size: 14px;
                            background: #fff;
                            outline: none;
                            transition: border 0.15s;
                            width: 100%;
                        }
                        input:focus, select:focus {
                            border-color: var(--primary);
                            box-shadow: 0 0 0 3px var(--primary-soft);
                        }
                        button {
                            padding: 10px 18px;
                            border: none;
                            border-radius: 10px;
                            background: var(--primary);
                            color: #fff;
                            font-size: 14px;
                            font-weight: 600;
                            cursor: pointer;
                            transition: all 0.15s;
                        }
                        button:hover {
                            filter: brightness(1.05);
                            box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);
                        }
                        button.secondary {
                            background: var(--accent);
                        }
                        button.secondary:hover {
                            box-shadow: 0 4px 12px rgba(124, 58, 237, 0.25);
                        }
                        button.danger {
                            background: var(--danger);
                        }
                        button.danger:hover {
                            box-shadow: 0 4px 12px rgba(220, 38, 38, 0.25);
                        }
                        .actions {
                            display: flex;
                            justify-content: flex-end;
                            margin-top: 8px;
                        }
                        .inline-form {
                            margin: 0;
                        }
                        .occupancy-summary {
                            display: grid;
                            grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
                            gap: 16px;
                            margin-bottom: 20px;
                        }
                        .summary-item { display: flex; flex-direction: column; gap: 4px; }
                        .summary-label { font-size: 13px; color: var(--subtle); }
                        .summary-value { font-size: 24px; font-weight: 700; color: var(--ink); }
                        .summary-value.primary { color: var(--primary); }
                        .summary-value.accent { color: var(--accent); }
                        .progress-bar {
                            height: 14px;
                            background: #e5e7eb;
                            border-radius: 999px;
                            overflow: hidden;
                        }
                        .progress-bar.slim {
                            height: 8px;
                            margin-top: 8px;
                        }
                        .progress-fill {
                            height: 100%;
                            background: linear-gradient(90deg, var(--primary), #60a5fa);
                            border-radius: 999px;
                        }
                        .occupancy-row {
                            padding: 14px 0;
                            border-bottom: 1px solid var(--line);
                        }
                        .occupancy-row:last-child {
                            border-bottom: none;
                            padding-bottom: 0;
                        }
                        .occupancy-info {
                            display: flex;
                            justify-content: space-between;
                            align-items: center;
                            gap: 12px;
                        }
                        .occupancy-name { font-weight: 600; }
                        .occupancy-detail { font-size: 13px; color: var(--subtle); }
                        .occupancy-rate {
                            font-size: 18px;
                            font-weight: 700;
                            color: var(--primary);
                            text-align: right;
                            margin-bottom: 4px;
                        }
                        .quick-links {
                            display: grid;
                            grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
                            gap: 12px;
                        }
                        .quick-links a {
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            gap: 8px;
                            padding: 14px;
                            border-radius: 12px;
                            background: var(--surface);
                            border: 1px solid var(--line);
                            text-decoration: none;
                            color: var(--ink);
                            font-weight: 500;
                            transition: all 0.15s;
                        }
                        .quick-links a:hover {
                            border-color: var(--primary);
                            background: var(--primary-soft);
                            color: var(--primary);
                        }
                        .scroll-x { overflow-x: auto; }
                        table {
                            width: 100%;
                            border-collapse: collapse;
                            font-size: 14px;
                        }
                        th, td {
                            padding: 12px;
                            text-align: left;
                            border-bottom: 1px solid var(--line);
                        }
                        th {
                            background: #f9fafb;
                            color: var(--subtle);
                            font-weight: 600;
                            font-size: 13px;
                        }
                        tr:hover td { background: #f9fafb; }
                        .pill {
                            display: inline-block;
                            padding: 3px 10px;
                            border-radius: 999px;
                            background: var(--primary-soft);
                            color: var(--primary);
                            font-size: 12px;
                            font-weight: 600;
                        }
                        .pill.accent {
                            background: var(--accent-soft);
                            color: var(--accent);
                        }
                        .result {
                            margin-top: 16px;
                            padding: 16px;
                            border-radius: var(--radius);
                            background: #f9fafb;
                            border: 1px dashed var(--line);
                            font-size: 14px;
                        }
                        .empty {
                            color: var(--subtle);
                            font-size: 14px;
                            padding: 16px;
                            text-align: center;
                        }
                    </style>
                </head>
                <body>
                <header>
                    <div class="header-inner">
                        <div>
                            <h1>🏫 琉璃子管理系统</h1>
                            <div class="subtitle">沿用当前项目稳定逻辑，当前 Web 呈现切换为琉璃子管理系统。</div>
                        </div>
                        <nav>
                """);

        html.append(renderNavLink("dashboard", "概览", activeTab));
        html.append(renderNavLink("students", "学生管理", activeTab));
        html.append(renderNavLink("buildings", "楼栋管理", activeTab));
        html.append(renderNavLink("rooms", "房间管理", activeTab));
        html.append(renderNavLink("dorms", "宿舍分配", activeTab));
        html.append(renderNavLink("queries", "查询中心", activeTab));

        html.append("""
                        </nav>
                    </div>
                </header>
                <main>
                """);

        if (!message.isBlank()) {
            html.append("<div class=\"notice\">✔ ").append(escapeHtml(message)).append("</div>");
        }
        if (!error.isBlank()) {
            html.append("<div class=\"error\">✖ ").append(escapeHtml(error)).append("</div>");
        }

        html.append(renderDashboardSection(activeTab, occupancy));
        html.append(renderStudentSection(activeTab, students));
        html.append(renderBuildingSection(activeTab, buildings));
        html.append(renderRoomSection(activeTab, buildings, rooms));
        html.append(renderDormSection(activeTab, buildings, changeStudentId, changeBuildingId, changeRoomId, changeBedNumber));
        html.append(renderQuerySection(
                activeTab,
                buildings,
                lookupStudentId,
                dormByStudent,
                lookupRoomId,
                studentsByRoom,
                roomByLocation,
                lookupBuildingId,
                lookupFloorNumber,
                lookupRoomNumber
        ));
        html.append(renderFormScript(rooms)).append("""
                </main>
                </body>
                </html>
                """);
        return html.toString();
    }

    private String renderNavLink(String tab, String label, String activeTab) {
        return "<a href=\"/?tab=" + tab + "\" class=\"" + (tab.equals(activeTab) ? "active" : "") + "\">"
                + escapeHtml(label) + "</a>";
    }

    private String renderDashboardSection(String activeTab, OccupancyOverview occupancy) {
        StringBuilder buildingRows = new StringBuilder();
        if (occupancy.buildingStats().isEmpty()) {
            buildingRows.append("<div class=\"empty\">暂无楼栋入住统计</div>");
        } else {
            for (BuildingOccupancy item : occupancy.buildingStats()) {
                buildingRows.append("<div class=\"occupancy-row\">")
                        .append("<div class=\"occupancy-info\"><div>")
                        .append("<div class=\"occupancy-name\">").append(escapeHtml(item.buildingName())).append("</div>")
                        .append("<div class=\"occupancy-detail\">房间 ").append(item.totalRooms())
                        .append(" 间 / 床位 ").append(item.totalBeds())
                        .append(" / 已住 ").append(item.occupiedBeds()).append("</div>")
                        .append("</div><div style=\"min-width:120px;\">")
                        .append("<div class=\"occupancy-rate\">").append(item.occupancyRate()).append("%</div>")
                        .append("<div class=\"progress-bar slim\"><div class=\"progress-fill\" style=\"width:")
                        .append(Math.min(item.occupancyRate(), 100.0)).append("%\"></div></div>")
                        .append("</div></div></div>");
            }
        }

        return """
                <section class="tab-content """ + activeClass(activeTab, "dashboard") + """
                ">
                    <div class="stats">
                        <div class="stat-card"><h3>宿舍楼数量</h3><div class="number">""" + occupancy.buildingStats().size() + "</div></div>\n" +
                "        <div class=\"stat-card\"><h3>房间总数</h3><div class=\"number\">" + occupancy.totalRooms() + "</div></div>\n" +
                "        <div class=\"stat-card\"><h3>床位总数</h3><div class=\"number\">" + occupancy.totalBeds() + "</div></div>\n" +
                "        <div class=\"stat-card\"><h3>已入住床位</h3><div class=\"number\">" + occupancy.occupiedBeds() + "</div></div>\n" +
                "    </div>\n" +
                "    <div class=\"two-col\">\n" +
                "        <div class=\"card\">\n" +
                "            <h2>📊 入住率概览</h2>\n" +
                "            <div class=\"occupancy-summary\">\n" +
                "                <div class=\"summary-item\"><span class=\"summary-label\">整体入住率</span><span class=\"summary-value primary\">" + occupancy.occupancyRate() + "%</span></div>\n" +
                "                <div class=\"summary-item\"><span class=\"summary-label\">空闲床位</span><span class=\"summary-value accent\">" + (occupancy.totalBeds() - occupancy.occupiedBeds()) + "</span></div>\n" +
                "            </div>\n" +
                "            <div class=\"progress-bar\"><div class=\"progress-fill\" style=\"width:" + Math.min(occupancy.occupancyRate(), 100.0) + "%\"></div></div>\n" +
                "        </div>\n" +
                "        <div class=\"card\">\n" +
                "            <h2>🚀 快捷入口</h2>\n" +
                "            <div class=\"quick-links\">\n" +
                "                <a href='/?tab=students'>学生管理</a>\n" +
                "                <a href='/?tab=buildings'>楼栋管理</a>\n" +
                "                <a href='/?tab=rooms'>房间管理</a>\n" +
                "                <a href='/?tab=dorms'>宿舍分配</a>\n" +
                "                <a href='/?tab=queries'>查询中心</a>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "    <div class=\"card\">\n" +
                "        <h2>🏢 各楼栋入住情况</h2>\n" +
                buildingRows +
                "    </div>\n" +
                "</section>";
    }

    private String renderStudentSection(String activeTab, List<Student> students) {
        return """
                <section class="tab-content """ + activeClass(activeTab, "students") + """
                ">
                    <div class="two-col">
                        <div class="card">
                            <h2>👤 新增学生</h2>
                            <form method="post" action="/students/create">
                                <input type="hidden" name="tab" value="students">
                                <div class="form-grid">
                                    <div class="form-group"><label>学号</label><input name="studentId" placeholder="例如 20240001" required></div>
                                    <div class="form-group"><label>姓名</label><input name="name" placeholder="例如 张三" required></div>
                                    <div class="form-group"><label>班级</label><input name="className" placeholder="例如 计科 1 班" required></div>
                                    <div class="form-group"><label>年级</label><input name="grade" placeholder="例如 2024" required></div>
                                    <div class="form-group"><label>性别</label><select name="gender"><option value="男">男</option><option value="女">女</option></select></div>
                                </div>
                                <div class="actions"><button type="submit">创建学生</button></div>
                            </form>
                        </div>
                        <div class="card">
                            <h2>📋 学生列表</h2>
                            """ + renderStudentTable(students) + """
                        </div>
                    </div>
                </section>
                """;
    }

    private String renderBuildingSection(String activeTab, List<Building> buildings) {
        return """
                <section class="tab-content """ + activeClass(activeTab, "buildings") + """
                ">
                    <div class="two-col">
                        <div class="card">
                            <h2>🏢 新增宿舍楼</h2>
                            <form method="post" action="/buildings/create">
                                <input type="hidden" name="tab" value="buildings">
                                <div class="form-grid">
                                    <div class="form-group"><label>楼栋编号</label><input name="code" placeholder="例如 A1" required></div>
                                    <div class="form-group"><label>楼栋名称</label><input name="name" placeholder="例如 一舍" required></div>
                                    <div class="form-group"><label>入住策略</label><select name="genderPolicy"><option value="男">男生楼</option><option value="女">女生楼</option><option value="男女分层">男女分层</option></select></div>
                                </div>
                                <div class="actions"><button type="submit">创建宿舍楼</button></div>
                            </form>
                        </div>
                        <div class="card">
                            <h2>📋 宿舍楼列表</h2>
                            """ + renderBuildingTable(buildings) + """
                        </div>
                    </div>
                </section>
                """;
    }

    private String renderRoomSection(String activeTab, List<Building> buildings, List<Room> rooms) {
        return """
                <section class="tab-content """ + activeClass(activeTab, "rooms") + """
                ">
                    <div class="two-col">
                        <div class="card">
                            <h2>🚪 新增房间</h2>
                            """ + renderRoomFormCard(buildings) + """
                        </div>
                        <div class="card">
                            <h2>📋 房间列表</h2>
                            """ + renderRoomTable(rooms, buildings) + """
                        </div>
                    </div>
                </section>
                """;
    }

    private String renderDormSection(
            String activeTab,
            List<Building> buildings,
            String changeStudentId,
            String changeBuildingId,
            String changeRoomId,
            String changeBedNumber
    ) {
        return """
                <section class="tab-content """ + activeClass(activeTab, "dorms") + """
                ">
                    <div class="two-col">
                        <div class="card">
                            <h2>🛏️ 分配宿舍</h2>
                            """ + renderDormForm(
                "/dorms/assign",
                "assign-form",
                "assign-dorm",
                "输入已存在的学号",
                "分配床位",
                false,
                buildings,
                "",
                "",
                "",
                ""
        ) + """
                        </div>
                        <div class="card">
                            <h2>🔄 调换宿舍</h2>
                            """ + renderDormForm(
                "/dorms/change",
                "change-form",
                "change-dorm",
                "输入已入住的学号",
                "提交调宿",
                true,
                buildings,
                changeStudentId,
                changeBuildingId,
                changeRoomId,
                changeBedNumber
        ) + """
                        </div>
                    </div>
                </section>
                """;
    }

    private String renderQuerySection(
            String activeTab,
            List<Building> buildings,
            String lookupStudentId,
            StudentDormView dormByStudent,
            String lookupRoomId,
            List<StudentDormView> studentsByRoom,
            Room roomByLocation,
            String lookupBuildingId,
            String lookupFloorNumber,
            String lookupRoomNumber
    ) {
        return """
                <section class="tab-content """ + activeClass(activeTab, "queries") + """
                ">
                    <div class="two-col">
                        <div class="card">
                            <h2>🔍 按学号查宿舍</h2>
                            <form method="get" action="/">
                                <input type="hidden" name="tab" value="queries">
                                <div class="form-grid">
                                    <div class="form-group">
                                        <label>学号</label>
                                        <input name="lookupStudentId" value="
                """ + escapeHtml(lookupStudentId) + """
                " placeholder="例如 20240001" required>
                                    </div>
                                </div>
                                <div class="actions"><button type="submit">查询住宿信息</button></div>
                            </form>
                            """ + renderStudentDormResult(dormByStudent) + """
                        </div>
                        <div class="card">
                            <h2>🔍 按房间查学生</h2>
                            <form method="get" action="/">
                                <input type="hidden" name="tab" value="queries">
                                <div class="form-grid">
                                    <div class="form-group">
                                        <label>房间 ID</label>
                                        <input name="lookupRoomId" value="
                """ + escapeHtml(lookupRoomId) + """
                " placeholder="例如 1">
                                    </div>
                                </div>
                                <div class="actions"><button type="submit">按房间 ID 查询</button></div>
                            </form>
                            <form method="get" action="/" style="margin-top:16px;">
                                <input type="hidden" name="tab" value="queries">
                                <div class="form-grid">
                                    <div class="form-group">
                                        <label>所属宿舍楼</label>
                                        <select name="lookupBuildingId" required>
                                            """ + renderBuildingOptionsWithSelected(buildings, lookupBuildingId) + """
                                        </select>
                                    </div>
                                    <div class="form-group">
                                        <label>楼层</label>
                                        <input type="number" name="lookupFloorNumber" min="1" value="
                """ + escapeHtml(lookupFloorNumber) + """
                " placeholder="例如 3" required>
                                    </div>
                                    <div class="form-group">
                                        <label>房间号</label>
                                        <input name="lookupRoomNumber" value="
                """ + escapeHtml(lookupRoomNumber) + """
                " placeholder="例如 301" required>
                                    </div>
                                </div>
                                <div class="actions"><button type="submit">按楼层和房间号查询</button></div>
                            </form>
                            """ + renderRoomStudentsResult(roomByLocation, studentsByRoom) + """
                        </div>
                    </div>
                </section>
                """;
    }

    private String renderRoomFormCard(List<Building> buildings) {
        if (buildings.isEmpty()) {
            return "<div class=\"empty\">请先创建宿舍楼，再为楼栋添加房间。</div>";
        }
        return """
                <form method="post" action="/rooms/create">
                    <input type="hidden" name="tab" value="rooms">
                    <div class="form-grid">
                        <div class="form-group"><label>房间号</label><input name="roomNumber" placeholder="例如 101" required></div>
                        <div class="form-group"><label>所属宿舍楼</label><select name="buildingId" required>
                            """ + renderBuildingOptions(buildings) + """
                        </select></div>
                        <div class="form-group"><label>楼层</label><input type="number" name="floorNumber" min="1" placeholder="例如 1" required></div>
                    </div>
                    <div class="actions"><button type="submit">创建房间</button></div>
                </form>
                """;
    }

    private String renderDormForm(
            String action,
            String formClass,
            String sectionId,
            String studentPlaceholder,
            String buttonLabel,
            boolean secondary,
            List<Building> buildings,
            String selectedStudentId,
            String selectedBuildingId,
            String selectedRoomId,
            String selectedBedNumber
    ) {
        if (buildings.isEmpty()) {
            return "<div class=\"empty\">当前没有宿舍楼，请先创建宿舍楼和房间。</div>";
        }

        String buttonClass = secondary ? " class=\"secondary\"" : "";
        return """
                <form method="post" action="%s" class="dorm-form %s" data-selected-room-id="%s" id="%s">
                    <input type="hidden" name="tab" value="dorms">
                    <div class="form-grid">
                        <div class="form-group">
                            <label>学号</label>
                            <input name="studentId" value="%s" placeholder="%s" required>
                        </div>
                        <div class="form-group">
                            <label>宿舍楼</label>
                            <select name="buildingId" class="building-select" required>
                                %s
                            </select>
                        </div>
                        <div class="form-group">
                            <label>楼层</label>
                            <select class="floor-select" required disabled>
                                <option value="">请先选择宿舍楼</option>
                            </select>
                        </div>
                        <div class="form-group">
                            <label>房间</label>
                            <select name="roomId" class="room-select" required disabled>
                                <option value="">请先选择楼层</option>
                            </select>
                        </div>
                        <div class="form-group">
                            <label>床号</label>
                            <input type="number" name="bedNumber" min="1" max="4" value="%s" required>
                        </div>
                    </div>
                    <div class="actions"><button type="submit"%s>%s</button></div>
                </form>
                <div class="result">先选宿舍楼，再选楼层，最后选择该楼层下的房间。</div>
                """.formatted(
                action,
                escapeHtml(formClass),
                escapeHtml(selectedRoomId),
                escapeHtml(sectionId),
                escapeHtml(selectedStudentId),
                escapeHtml(studentPlaceholder),
                renderBuildingOptionsWithSelected(buildings, selectedBuildingId),
                escapeHtml(selectedBedNumber),
                buttonClass,
                escapeHtml(buttonLabel)
        );
    }

    private String renderStudentDormResult(StudentDormView dormByStudent) {
        if (dormByStudent == null) {
            return "";
        }
        return """
                <div class="result">
                    <strong>查询结果</strong><br><br>
                    学号：""" + escapeHtml(dormByStudent.studentId()) + """
                    <br>姓名：""" + escapeHtml(dormByStudent.studentName()) + """
                    <br>宿舍楼：""" + escapeHtml(nullToDash(dormByStudent.buildingName())) + """
                    <br>房间号：""" + escapeHtml(nullToDash(dormByStudent.roomNumber())) + """
                    <br>床号：""" + (dormByStudent.bedNumber() == null ? "-" : dormByStudent.bedNumber()) + """
                </div>
                """;
    }

    private String renderRoomStudentsResult(Room roomByLocation, List<StudentDormView> studentsByRoom) {
        if (roomByLocation == null && (studentsByRoom == null || studentsByRoom.isEmpty())) {
            return "";
        }
        StringBuilder result = new StringBuilder("<div class=\"result\"><strong>查询结果</strong><br><br>");
        if (roomByLocation != null) {
            result.append("当前查询房间：房间 ID ").append(roomByLocation.roomId())
                    .append(" / 楼层 ").append(roomByLocation.floorNumber())
                    .append(" / 房间号 ").append(escapeHtml(roomByLocation.roomNumber()))
                    .append("<br><br>");
        }
        if (studentsByRoom == null || studentsByRoom.isEmpty()) {
            result.append("当前房间暂无学生入住。");
        } else {
            for (StudentDormView view : studentsByRoom) {
                result.append(escapeHtml(view.studentName()))
                        .append("（").append(escapeHtml(view.studentId())).append("）")
                        .append(" - 床号 ").append(view.bedNumber()).append("<br>");
            }
        }
        result.append("</div>");
        return result.toString();
    }

    private String renderStudentTable(List<Student> students) {
        if (students.isEmpty()) {
            return "<div class=\"empty\">暂无学生记录</div>";
        }
        StringBuilder rows = new StringBuilder();
        for (Student student : students) {
            rows.append("<tr><td>")
                    .append(escapeHtml(student.studentId()))
                    .append("</td><td>")
                    .append(escapeHtml(student.studentName()))
                    .append("</td><td>")
                    .append(escapeHtml(student.className()))
                    .append("</td><td>")
                    .append(escapeHtml(student.grade()))
                    .append("</td><td>")
                    .append(escapeHtml(student.gender().label()))
                    .append("</td><td>")
                    .append("<form method=\"post\" action=\"/students/delete\" class=\"inline-form\" ")
                    .append("onsubmit=\"return confirm('确认删除学生 ").append(escapeHtml(student.studentName()))
                    .append("（").append(escapeHtml(student.studentId())).append("）吗？');\">")
                    .append("<input type=\"hidden\" name=\"tab\" value=\"students\">")
                    .append("<input type=\"hidden\" name=\"studentId\" value=\"").append(escapeHtml(student.studentId())).append("\">")
                    .append("<button type=\"submit\" class=\"danger\">删除</button>")
                    .append("</form>")
                    .append("</td></tr>");
        }
        return "<div class=\"scroll-x\"><table><thead><tr><th>学号</th><th>姓名</th><th>班级</th><th>年级</th><th>性别</th><th>操作</th></tr></thead><tbody>"
                + rows + "</tbody></table></div>";
    }

    private String renderBuildingTable(List<Building> buildings) {
        if (buildings.isEmpty()) {
            return "<div class=\"empty\">暂无宿舍楼记录</div>";
        }
        StringBuilder rows = new StringBuilder();
        for (Building building : buildings) {
            rows.append("<tr><td>")
                    .append(building.buildingId())
                    .append("</td><td>")
                    .append(escapeHtml(building.buildingCode()))
                    .append("</td><td>")
                    .append(escapeHtml(building.buildingName()))
                    .append("</td><td><span class=\"pill accent\">")
                    .append(escapeHtml(building.genderPolicy().label()))
                    .append("</span></td><td>")
                    .append("<form method=\"post\" action=\"/buildings/delete\" class=\"inline-form\" ")
                    .append("onsubmit=\"return confirm('确认删除楼栋 ").append(escapeHtml(building.buildingName()))
                    .append("（ID ").append(building.buildingId()).append("）吗？删除后其房间和住宿记录也会一并移除。');\">")
                    .append("<input type=\"hidden\" name=\"tab\" value=\"buildings\">")
                    .append("<input type=\"hidden\" name=\"buildingId\" value=\"").append(building.buildingId()).append("\">")
                    .append("<button type=\"submit\" class=\"danger\">删除</button>")
                    .append("</form>")
                    .append("</td></tr>");
        }
        return "<div class=\"scroll-x\"><table><thead><tr><th>ID</th><th>编号</th><th>名称</th><th>入住策略</th><th>操作</th></tr></thead><tbody>"
                + rows + "</tbody></table></div>";
    }

    private String renderRoomTable(List<Room> rooms, List<Building> buildings) {
        if (rooms.isEmpty()) {
            return "<div class=\"empty\">暂无房间记录</div>";
        }
        Map<Long, Building> buildingsById = new HashMap<>();
        for (Building building : buildings) {
            buildingsById.put(building.buildingId(), building);
        }
        StringBuilder rows = new StringBuilder();
        for (Room room : rooms) {
            Building building = buildingsById.get(room.buildingId());
            rows.append("<tr><td>")
                    .append(room.roomId())
                    .append("</td><td>")
                    .append(escapeHtml(room.roomNumber()))
                    .append("</td><td>")
                    .append(building == null ? "-" : escapeHtml(building.buildingName()))
                    .append("</td><td>")
                    .append(room.floorNumber())
                    .append("</td></tr>");
        }
        return "<div class=\"scroll-x\"><table><thead><tr><th>ID</th><th>房间号</th><th>所属楼栋</th><th>楼层</th></tr></thead><tbody>"
                + rows + "</tbody></table></div>";
    }

    private String renderBuildingOptions(List<Building> buildings) {
        return renderBuildingOptionsWithSelected(buildings, "");
    }

    private String renderBuildingOptionsWithSelected(List<Building> buildings, String selectedBuildingId) {
        StringBuilder options = new StringBuilder("<option value=\"\">请选择宿舍楼</option>");
        for (Building building : buildings) {
            options.append("<option value=\"").append(building.buildingId()).append("\"");
            if (String.valueOf(building.buildingId()).equals(selectedBuildingId)) {
                options.append(" selected");
            }
            options.append(">")
                    .append(escapeHtml(building.buildingCode()))
                    .append(" - ")
                    .append(escapeHtml(building.buildingName()))
                    .append("</option>");
        }
        return options.toString();
    }

    private String renderFormScript(List<Room> rooms) {
        return """
                <script>
                    const roomMap = {
                """ + buildRoomMapJson(rooms) + """
                    };

                    document.querySelectorAll(".dorm-form").forEach((form) => {
                        const buildingSelect = form.querySelector(".building-select");
                        const floorSelect = form.querySelector(".floor-select");
                        const roomSelect = form.querySelector(".room-select");
                        const selectedRoomId = form.dataset.selectedRoomId || "";

                        const syncRooms = () => {
                            const buildingId = buildingSelect.value;
                            const rooms = roomMap[buildingId] || [];
                            const selectedRoom = rooms.find((room) => selectedRoomId && selectedRoomId === room.id);
                            const floors = [...new Set(rooms.map((room) => String(room.floor)))].sort((a, b) => Number(a) - Number(b));

                            floorSelect.innerHTML = "";
                            roomSelect.innerHTML = "";

                            if (!buildingId) {
                                floorSelect.disabled = true;
                                floorSelect.innerHTML = '<option value="">请先选择宿舍楼</option>';
                                roomSelect.disabled = true;
                                roomSelect.innerHTML = '<option value="">请先选择宿舍楼</option>';
                                return;
                            }

                            if (rooms.length === 0) {
                                floorSelect.disabled = true;
                                floorSelect.innerHTML = '<option value="">该宿舍楼下暂无楼层</option>';
                                roomSelect.disabled = true;
                                roomSelect.innerHTML = '<option value="">该宿舍楼下暂无房间</option>';
                                return;
                            }

                            floorSelect.disabled = false;
                            floorSelect.innerHTML = '<option value="">请选择楼层</option>';
                            floors.forEach((floor) => {
                                const option = document.createElement("option");
                                option.value = floor;
                                option.textContent = floor + ' 层';
                                if (selectedRoom && floor === String(selectedRoom.floor)) {
                                    option.selected = true;
                                }
                                floorSelect.appendChild(option);
                            });

                            syncRoomOptions();
                        };

                        const syncRoomOptions = () => {
                            const buildingId = buildingSelect.value;
                            const floorNumber = floorSelect.value;
                            const rooms = roomMap[buildingId] || [];

                            roomSelect.innerHTML = "";

                            if (!buildingId) {
                                roomSelect.disabled = true;
                                roomSelect.innerHTML = '<option value="">请先选择宿舍楼</option>';
                                return;
                            }

                            if (!floorNumber) {
                                roomSelect.disabled = true;
                                roomSelect.innerHTML = '<option value="">请先选择楼层</option>';
                                return;
                            }

                            const filteredRooms = rooms.filter((room) => String(room.floor) === floorNumber);
                            if (filteredRooms.length === 0) {
                                roomSelect.disabled = true;
                                roomSelect.innerHTML = '<option value="">该楼层下暂无房间</option>';
                                return;
                            }

                            roomSelect.disabled = false;
                            roomSelect.innerHTML = '<option value="">请选择房间</option>';
                            filteredRooms.forEach((room) => {
                                const option = document.createElement("option");
                                option.value = room.id;
                                option.textContent = room.label;
                                if (selectedRoomId && selectedRoomId === room.id) {
                                    option.selected = true;
                                }
                                roomSelect.appendChild(option);
                            });
                        };

                        buildingSelect.addEventListener("change", syncRooms);
                        floorSelect.addEventListener("change", syncRoomOptions);
                        syncRooms();
                    });
                </script>
                """;
    }

    private String buildRoomMapJson(List<Room> rooms) {
        Map<Long, List<Room>> roomsByBuilding = new HashMap<>();
        for (Room room : rooms) {
            roomsByBuilding.computeIfAbsent(room.buildingId(), ignored -> new ArrayList<>()).add(room);
        }

        StringBuilder json = new StringBuilder();
        boolean firstBuilding = true;
        for (Map.Entry<Long, List<Room>> entry : roomsByBuilding.entrySet()) {
            if (!firstBuilding) {
                json.append(",");
            }
            firstBuilding = false;
            json.append("\"").append(entry.getKey()).append("\":[");
            boolean firstRoom = true;
            for (Room room : entry.getValue()) {
                if (!firstRoom) {
                    json.append(",");
                }
                firstRoom = false;
                json.append("{\"id\":\"").append(room.roomId()).append("\",\"floor\":")
                        .append(room.floorNumber()).append(",\"label\":\"房间 ID ")
                        .append(room.roomId()).append(" / 房间号 ")
                        .append(escapeJs(room.roomNumber())).append("\"}");
            }
            json.append("]");
        }
        return json.toString();
    }

    private String renderErrorPage(String error) {
        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>请求失败</title>
                    <style>
                        body {
                            margin: 0;
                            font-family: "Microsoft YaHei", "PingFang SC", sans-serif;
                            background: #f4f6f8;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            min-height: 100vh;
                        }
                        .box {
                            background: #fff;
                            border-radius: 20px;
                            padding: 40px;
                            box-shadow: 0 8px 30px rgba(0,0,0,0.08);
                            max-width: 480px;
                            text-align: center;
                        }
                        h2 { margin: 0 0 16px; color: #dc2626; }
                        p { color: #4b5563; margin-bottom: 24px; }
                        a {
                            display: inline-block;
                            padding: 10px 20px;
                            background: #2563eb;
                            color: #fff;
                            text-decoration: none;
                            border-radius: 10px;
                            font-weight: 600;
                        }
                    </style>
                </head>
                <body>
                    <div class="box">
                        <h2>请求失败</h2>
                        <p>""" + escapeHtml(error) + """
                        </p>
                        <a href="/">返回首页</a>
                    </div>
                </body>
                </html>
                """;
    }

    private String activeClass(String activeTab, String tab) {
        return normalizeTab(activeTab).equals(tab) ? "active" : "";
    }

    private String normalizeTab(String tab) {
        return VALID_TABS.contains(tab) ? tab : "dashboard";
    }

    private String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String escapeJs(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
