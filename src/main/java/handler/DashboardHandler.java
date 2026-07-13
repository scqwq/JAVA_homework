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

public class DashboardHandler extends BaseHandler {
    private final BuildingService buildingService;
    private final RoomService roomService;
    private final StudentService studentService;
    private final DormService dormService;

    private static final Set<String> VALID_TABS = Set.of(
            "dashboard", "students", "buildings", "rooms", "dorms", "queries"
    );

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
        StudentDormView dormByStudent = null;
        List<StudentDormView> studentsByRoom = List.of();
        Room roomByLocation = null;
        try {
            ensureMethod(exchange, "GET");
            buildings = buildingService.listBuildings();
            rooms = roomService.listRooms();
            students = studentService.listStudents();

            if (!query.getOrDefault("lookupStudentId", "").isBlank()) {
                dormByStudent = dormService.findDormByStudent(query.get("lookupStudentId").trim());
            }

            String lookupRoomId = query.getOrDefault("lookupRoomId", "").trim();
            if (!lookupRoomId.isBlank()) {
                studentsByRoom = dormService.findStudentsByRoom(Long.parseLong(lookupRoomId));
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
                    lookupRoomId,
                    roomByLocation,
                    lookupBuildingId,
                    lookupFloorNumber,
                    lookupRoomNumber,
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
                } catch (SQLException sqlException) {
                    sendHtml(exchange, 500, renderErrorPage("数据库操作失败: " + sqlException.getMessage()));
                    return;
                }
            }
            sendHtml(exchange, 400, renderPage(
                    query.getOrDefault("message", ""),
                    exception.getMessage(),
                    buildings,
                    rooms,
                    students,
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
                    query.getOrDefault("message", ""),
                    "数据库操作失败: " + exception.getMessage(),
                    buildings,
                    rooms,
                    students,
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
                    <title>学生宿舍管理系统</title>
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
                        .tab-content {
                            display: none;
                            animation: fadeIn 0.25s ease;
                        }
                        .tab-content.active {
                            display: block;
                        }
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
                        .stat-card {
                            background: var(--surface);
                            border-radius: var(--radius);
                            padding: 20px;
                            box-shadow: var(--shadow);
                            border: 1px solid var(--line);
                        }
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
                        .occupancy-summary {
                            display: grid;
                            grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
                            gap: 16px;
                            margin-bottom: 20px;
                        }
                        .summary-item {
                            display: flex;
                            flex-direction: column;
                            gap: 4px;
                        }
                        .summary-label {
                            font-size: 13px;
                            color: var(--subtle);
                        }
                        .summary-value {
                            font-size: 24px;
                            font-weight: 700;
                            color: var(--ink);
                        }
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
                            transition: width 0.4s ease;
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
                        .occupancy-name {
                            font-weight: 600;
                        }
                        .occupancy-detail {
                            font-size: 13px;
                            color: var(--subtle);
                        }
                        .occupancy-rate {
                            font-size: 18px;
                            font-weight: 700;
                            color: var(--primary);
                            text-align: right;
                            margin-bottom: 4px;
                        }
                        .card {
                            background: var(--surface);
                            border-radius: var(--radius);
                            padding: 24px;
                            box-shadow: var(--shadow);
                            border: 1px solid var(--line);
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
                        .form-group.full {
                            grid-column: 1 / -1;
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
                        .actions {
                            display: flex;
                            justify-content: flex-end;
                            margin-top: 8px;
                        }
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
                            position: sticky;
                            top: 0;
                        }
                        tr:hover td {
                            background: #f9fafb;
                        }
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
                            color: var(--primary);
                            background: var(--primary-soft);
                        }
                        .scroll-x {
                            overflow-x: auto;
                            border-radius: var(--radius);
                            border: 1px solid var(--line);
                        }
                        @media (max-width: 640px) {
                            .header-inner { flex-direction: column; align-items: flex-start; }
                            nav { width: 100%; }
                            nav a { flex: 1; text-align: center; }
                        }
                    </style>
                </head>
                <body>
                <header>
                    <div class="header-inner">
                        <div>
                            <h1>🏠 学生宿舍管理系统</h1>
                            <p class="subtitle">浏览器端管理学生、楼栋、房间与住宿分配</p>
                        </div>
                        <nav>
                """);

        html.append(navLink("dashboard", "🏠 首页", activeTab))
                .append(navLink("students", "🎓 学生", activeTab))
                .append(navLink("buildings", "🏢 楼栋", activeTab))
                .append(navLink("rooms", "🚪 房间", activeTab))
                .append(navLink("dorms", "🛏️ 分配/调宿", activeTab))
                .append(navLink("queries", "🔍 查询", activeTab));

        html.append("""
                        </nav>
                    </div>
                </header>
                <main>
                """);

        if (!message.isBlank()) {
            html.append("<div class=\"notice\">✅ ").append(escapeHtml(message)).append("</div>");
        }
        if (!error.isBlank()) {
            html.append("<div class=\"error\">❌ ").append(escapeHtml(error)).append("</div>");
        }

        // Dashboard
        html.append("<section id=\"dashboard\" class=\"tab-content").append(activeClass("dashboard", activeTab)).append("\">");
        html.append(renderDashboard(students.size(), buildings.size(), rooms.size(), occupancy));
        html.append("</section>");

        // Students
        html.append("<section id=\"students\" class=\"tab-content").append(activeClass("students", activeTab)).append("\">");
        html.append(renderStudentSection(students));
        html.append("</section>");

        // Buildings
        html.append("<section id=\"buildings\" class=\"tab-content").append(activeClass("buildings", activeTab)).append("\">");
        html.append(renderBuildingSection(buildings));
        html.append("</section>");

        // Rooms
        html.append("<section id=\"rooms\" class=\"tab-content").append(activeClass("rooms", activeTab)).append("\">");
        html.append(renderRoomSection(buildings, rooms));
        html.append("</section>");

        // Dorms
        html.append("<section id=\"dorms\" class=\"tab-content").append(activeClass("dorms", activeTab)).append("\">");
        html.append(renderDormSection(buildings, rooms));
        html.append("</section>");

        // Queries
        html.append("<section id=\"queries\" class=\"tab-content").append(activeClass("queries", activeTab)).append("\">");
        html.append(renderQuerySection(lookupStudentId, dormByStudent, lookupRoomId, studentsByRoom));
        html.append("</section>");

        html.append("""
                    <div class="grid">
                """);
        html.append(renderStudentForm());
        html.append(renderBuildingForm(buildings));
        html.append(renderRoomForm(buildings));
        html.append(renderAssignForm(buildings, rooms));
        html.append(renderChangeForm(
                buildings,
                rooms,
                changeStudentId,
                changeBuildingId,
                changeRoomId,
                changeBedNumber
        ));
        html.append(renderStudentLookupForm(lookupStudentId, dormByStudent));
        html.append(renderRoomLookupForm(
                buildings,
                lookupRoomId,
                studentsByRoom,
                roomByLocation,
                lookupBuildingId,
                lookupFloorNumber,
                lookupRoomNumber
        ));
        html.append(renderStudentTable(students));
        html.append(renderBuildingTable(buildings));
        html.append(renderRoomTable(rooms, buildings));
        html.append(renderFormScript());
        html.append("""
                    </div>
                </div>
                </body>
                </html>
                """);
        return html.toString();
    }

    private String activeClass(String tab, String activeTab) {
        return tab.equals(activeTab) ? " active" : "";
    }

    private String navLink(String tab, String label, String activeTab) {
        String active = tab.equals(activeTab) ? " class=\"active\"" : "";
        return "<a" + active + " href=\"/?tab=" + tab + "\">" + label + "</a>";
    }

    private String renderDashboard(int studentCount, int buildingCount, int roomCount, OccupancyOverview occupancy) {
        long vacantBeds = occupancy.totalBeds() - occupancy.occupiedBeds();
        double overallRate = Math.min(100.0, occupancy.occupancyRate());
        return """
                <div class="stats">
                    <div class="stat-card">
                        <h3>学生总数</h3>
                        <div class="number">""" + studentCount + """
                        </div>
                    </div>
                    <div class="stat-card">
                        <h3>宿舍楼栋</h3>
                        <div class="number">""" + buildingCount + """
                        </div>
                    </div>
                    <div class="stat-card">
                        <h3>房间总数</h3>
                        <div class="number">""" + roomCount + """
                        </div>
                    </div>
                </div>
                """ + renderOccupancyPanel(occupancy.totalBeds(), occupancy.occupiedBeds(), vacantBeds, overallRate)
                + renderBuildingOccupancy(occupancy.buildingStats()) + """
                <div class="card">
                    <h2>⚡ 快速入口</h2>
                    <div class="quick-links">
                        <a href="/?tab=students">🎓 管理学生</a>
                        <a href="/?tab=buildings">🏢 管理楼栋</a>
                        <a href="/?tab=rooms">🚪 管理房间</a>
                        <a href="/?tab=dorms">🛏️ 分配宿舍</a>
                        <a href="/?tab=queries">🔍 住宿查询</a>
                    </div>
                </div>
                """;
    }

    private String renderOccupancyPanel(long totalBeds, long occupiedBeds, long vacantBeds, double occupancyRate) {
        return "<div class=\"card\">"
                + "<h2>📊 入住率概览</h2>"
                + "<div class=\"occupancy-summary\">"
                + "<div class=\"summary-item\"><span class=\"summary-label\">总床位</span><span class=\"summary-value\">" + totalBeds + "</span></div>"
                + "<div class=\"summary-item\"><span class=\"summary-label\">已入住</span><span class=\"summary-value primary\">" + occupiedBeds + "</span></div>"
                + "<div class=\"summary-item\"><span class=\"summary-label\">空置床位</span><span class=\"summary-value\">" + vacantBeds + "</span></div>"
                + "<div class=\"summary-item\"><span class=\"summary-label\">整体入住率</span><span class=\"summary-value accent\">" + occupancyRate + "%</span></div>"
                + "</div>"
                + "<div class=\"progress-bar\"><div class=\"progress-fill\" style=\"width: " + occupancyRate + "%\"></div></div>"
                + "</div>";
    }

    private String renderRoomForm(List<Building> buildings) {
        if (buildings.isEmpty()) {
            return """
                    <section class="card">
                        <h2>新增房间</h2>
                        <div class="result">请先创建宿舍楼，再为楼栋添加房间。</div>
                    </section>
                    """;
        }

        StringBuilder html = new StringBuilder();
        html.append("""
                <section class="card">
                    <h2>新增房间</h2>
                    <form method="post" action="/rooms/create">
                        <label>房间号<input name="roomNumber" placeholder="例如 101" required></label>
                        <label>所属宿舍楼
                            <select name="buildingId" required>
                """);
        appendBuildingOptions(html, buildings, "请选择宿舍楼");
        html.append("""
                            </select>
                        </label>
                        <label>楼层<input type="number" name="floorNumber" min="1" placeholder="例如 1" required></label>
                        <button type="submit">创建房间</button>
                    </form>
                </section>
                """);
        return html.toString();
    }

    private String renderAssignForm(List<Building> buildings, List<Room> rooms) {
        return renderDormForm(
                "分配宿舍",
                "/dorms/assign",
                "assign-form",
                "输入已存在的学号",
                "分配床位",
                "当前没有宿舍楼，请先创建宿舍楼和房间，再进行分配。",
                buildings,
                rooms,
                "assign-dorm",
                "",
                "",
                "",
                ""
        );
    }

    private String renderChangeForm(
            List<Building> buildings,
            List<Room> rooms,
            String studentId,
            String buildingId,
            String roomId,
            String bedNumber
    ) {
        return renderDormForm(
                "调换宿舍",
                "/dorms/change",
                "change-form",
                "输入已入住的学号",
                "提交调宿",
                "当前没有可调换的宿舍资源，请先创建宿舍楼和房间。",
                buildings,
                rooms,
                "change-dorm",
                studentId,
                buildingId,
                roomId,
                bedNumber
        );
    }

    private String renderDormSection(List<Building> buildings, List<Room> rooms) {
        return """
                <section class="card" id="student-lookup">
                    <h2>按学号查宿舍</h2>
                    <form method="get" action="/#student-lookup">
                        <label>学号<input name="lookupStudentId" value="
                """ + escapeHtml(lookupStudentId) + """
                " placeholder="例如 20240001" required></label>
                        <button type="submit">查询住宿信息</button>
                    </form>
                """ + result + """
                </section>
                """;
    }

    private String renderRoomLookupForm(
            List<Building> buildings,
            String lookupRoomId,
            List<StudentDormView> studentsByRoom,
            Room roomByLocation,
            String lookupBuildingId,
            String lookupFloorNumber,
            String lookupRoomNumber
    ) {
        StringBuilder result = new StringBuilder();
        if (!lookupRoomId.isBlank() || roomByLocation != null) {
            result.append("<div class=\"result\">");
            if (roomByLocation != null) {
                result.append("当前查询房间：房间 ID ").append(roomByLocation.roomId())
                        .append(" / 楼层 ").append(roomByLocation.floorNumber())
                        .append(" / 房间号 ").append(escapeHtml(roomByLocation.roomNumber()))
                        .append("<br>");
            }
            if (studentsByRoom.isEmpty()) {
                result.append("当前房间暂无学生入住。");
            } else {
                for (StudentDormView view : studentsByRoom) {
                    result.append(escapeHtml(view.studentName()))
                            .append("（")
                            .append(escapeHtml(view.studentId()))
                            .append("）- 床号 ")
                            .append(view.bedNumber())
                            .append("<br>");
                }
            }
            result.append("</div>");
        }

        StringBuilder html = new StringBuilder();
        html.append("""
                <section class="card" id="room-lookup">
                    <h2>按房间查学生</h2>
                    <form method="get" action="/#room-lookup">
                """);
        html.append("        <label>房间 ID<input name=\"lookupRoomId\" value=\"")
                .append(escapeHtml(lookupRoomId))
                .append("\" placeholder=\"例如 1\"></label>\n");
        html.append("""
                        <button type="submit">按房间 ID 查询</button>
                    </form>
                """);

        html.append("""
                    <form method="get" action="/#room-lookup">
                        <label>所属宿舍楼
                            <select name="lookupBuildingId" required>
                """);
        appendBuildingOptionsWithSelected(html, buildings, "请选择宿舍楼", lookupBuildingId);
        html.append("""
                            </select>
                        </label>
                """);
        html.append("        <label>楼层<input type=\"number\" name=\"lookupFloorNumber\" min=\"1\" value=\"")
                .append(escapeHtml(lookupFloorNumber))
                .append("\" placeholder=\"例如 3\" required></label>\n");
        html.append("        <label>房间号<input name=\"lookupRoomNumber\" value=\"")
                .append(escapeHtml(lookupRoomNumber))
                .append("\" placeholder=\"例如 301\" required></label>\n");
        html.append("""
                        <button type="submit">按楼层和房间号查询</button>
                    </form>
                """);

        html.append(result).append("""
                </section>
                """);
        return html.toString();
    }

    private String renderRoomStudentsResult(List<StudentDormView> studentsByRoom) {
        if (studentsByRoom == null || studentsByRoom.isEmpty()) {
            return "";
        }
        StringBuilder rows = new StringBuilder();
        for (StudentDormView view : studentsByRoom) {
            rows.append(escapeHtml(view.studentName()))
                    .append("（").append(escapeHtml(view.studentId())).append("）")
                    .append(" - 床号 ").append(view.bedNumber()).append("<br>");
        }
        return "<div class=\"result\"><strong>查询结果</strong><br><br>" + rows + "</div>";
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
                    .append("</td></tr>");
        }
        return """
                <div class="scroll-x">
                    <table>
                        <thead>
                        <tr><th>学号</th><th>姓名</th><th>班级</th><th>年级</th><th>性别</th></tr>
                        </thead>
                        <tbody>
                """ + rows + """
                        </tbody>
                    </table>
                </div>
                """;
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
                    .append("</td><td>")
                    .append(escapeHtml(building.genderPolicy().label()))
                    .append("</td></tr>");
        }
        return """
                <div class="scroll-x">
                    <table>
                        <thead>
                        <tr><th>ID</th><th>编号</th><th>名称</th><th>入住策略</th></tr>
                        </thead>
                        <tbody>
                """ + rows + """
                        </tbody>
                    </table>
                </div>
                """;
    }

    private String renderRoomTable(List<Room> rooms, List<Building> buildings) {
        Map<Long, Building> buildingsById = new HashMap<>();
        for (Building building : buildings) {
            buildingsById.put(building.buildingId(), building);
        }
        if (rooms.isEmpty()) {
            return "<div class=\"empty\">暂无房间记录</div>";
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
        return """
                <div class="scroll-x">
                    <table>
                        <thead>
                        <tr><th>ID</th><th>房间号</th><th>所属楼栋</th><th>楼层</th></tr>
                        </thead>
                        <tbody>
                """ + rows + """
                        </tbody>
                    </table>
                </div>
                """;
    }

    private String renderDormForm(
            String title,
            String action,
            String formClass,
            String studentPlaceholder,
            String buttonLabel,
            String emptyMessage,
            List<Building> buildings,
            List<Room> rooms,
            String sectionId,
            String selectedStudentId,
            String selectedBuildingId,
            String selectedRoomId,
            String selectedBedNumber
    ) {
        if (buildings.isEmpty()) {
            return "<section class=\"card\" id=\"" + escapeHtml(sectionId) + "\"><h2>" + escapeHtml(title)
                    + "</h2><div class=\"result\">" + escapeHtml(emptyMessage) + "</div></section>";
        }

        Map<Long, List<Room>> roomsByBuilding = groupRoomsByBuilding(rooms);
        StringBuilder html = new StringBuilder();
        html.append("<section class=\"card\" id=\"").append(escapeHtml(sectionId)).append("\">");
        html.append("<h2>").append(escapeHtml(title)).append("</h2>");
        html.append("<form method=\"post\" action=\"").append(action).append("\" class=\"dorm-form ")
                .append(formClass).append("\" data-selected-room-id=\"").append(escapeHtml(selectedRoomId)).append("\">");
        html.append("<label>学号<input name=\"studentId\" placeholder=\"").append(escapeHtml(studentPlaceholder))
                .append("\" value=\"").append(escapeHtml(selectedStudentId)).append("\" required></label>");
        html.append("<label>宿舍楼<select name=\"buildingId\" class=\"building-select\" required>");
        appendBuildingOptionsWithSelected(html, buildings, "请选择宿舍楼", selectedBuildingId);
        html.append("</select></label>");
        html.append("<label>房间");
        html.append("<select name=\"roomId\" class=\"room-select\" required disabled>");
        html.append("<option value=\"\">请先选择宿舍楼</option>");
        html.append("</select></label>");
        html.append("<label>床号<input type=\"number\" name=\"bedNumber\" min=\"1\" max=\"4\" value=\"")
                .append(escapeHtml(selectedBedNumber)).append("\" required></label>");
        html.append("<button type=\"submit\">").append(escapeHtml(buttonLabel)).append("</button>");
        html.append("</form>");

        if (rooms.isEmpty()) {
            html.append("<div class=\"result\">当前还没有房间，请先给宿舍楼添加房间。</div>");
        } else {
            html.append("<div class=\"result\">");
            html.append("先选宿舍楼，再选该楼栋下的房间。");
            for (Building building : buildings) {
                int roomCount = roomsByBuilding.getOrDefault(building.buildingId(), List.of()).size();
                html.append("<br>").append(escapeHtml(building.buildingName()))
                        .append("：").append(roomCount).append(" 个房间");
            }
            html.append("</div>");
        }

        html.append("</section>");
        return html.toString();
    }

    private void appendBuildingOptions(StringBuilder html, List<Building> buildings, String placeholder) {
        html.append("<option value=\"\">").append(escapeHtml(placeholder)).append("</option>");
        for (Building building : buildings) {
            html.append("<option value=\"").append(building.buildingId()).append("\">")
                    .append(escapeHtml(building.buildingCode()))
                    .append(" - ")
                    .append(escapeHtml(building.buildingName()))
                    .append("</option>");
        }
    }

    private void appendBuildingOptionsWithSelected(
            StringBuilder html,
            List<Building> buildings,
            String placeholder,
            String selectedBuildingId
    ) {
        html.append("<option value=\"\">").append(escapeHtml(placeholder)).append("</option>");
        for (Building building : buildings) {
            html.append("<option value=\"").append(building.buildingId()).append("\"");
            if (String.valueOf(building.buildingId()).equals(selectedBuildingId)) {
                html.append(" selected");
            }
            html.append(">")
                    .append(escapeHtml(building.buildingCode()))
                    .append(" - ")
                    .append(escapeHtml(building.buildingName()))
                    .append("</option>");
        }
    }

    private Map<Long, List<Room>> groupRoomsByBuilding(List<Room> rooms) {
        Map<Long, List<Room>> roomsByBuilding = new HashMap<>();
        for (Room room : rooms) {
            roomsByBuilding.computeIfAbsent(room.buildingId(), ignored -> new ArrayList<>()).add(room);
        }
        return roomsByBuilding;
    }

    private String renderFormScript() {
        return """
                <script>
                    const roomMap = {
                """ + buildRoomMapJson() + """
                    };

                    if (window.location.hash) {
                        const target = document.querySelector(window.location.hash);
                        if (target) {
                            target.scrollIntoView({ behavior: "smooth", block: "start" });
                        }
                    }

                    document.querySelectorAll(".dorm-form").forEach((form) => {
                        const buildingSelect = form.querySelector(".building-select");
                        const roomSelect = form.querySelector(".room-select");
                        const selectedRoomId = form.dataset.selectedRoomId || "";

                        const syncRooms = () => {
                            const buildingId = buildingSelect.value;
                            const rooms = roomMap[buildingId] || [];
                            roomSelect.innerHTML = "";

                            if (!buildingId) {
                                roomSelect.disabled = true;
                                roomSelect.innerHTML = '<option value="">请先选择宿舍楼</option>';
                                return;
                            }

                            if (rooms.length === 0) {
                                roomSelect.disabled = true;
                                roomSelect.innerHTML = '<option value="">该宿舍楼下暂无房间</option>';
                                return;
                            }

                            roomSelect.disabled = false;
                            roomSelect.innerHTML = '<option value="">请选择房间</option>';
                            rooms.forEach((room) => {
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
                        syncRooms();
                    });
                </script>
                """;
    }

    private String buildRoomMapJson() {
        try {
            List<Room> rooms = roomService.listRooms();
            Map<Long, List<Room>> roomsByBuilding = groupRoomsByBuilding(rooms);
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
                    json.append("{\"id\":\"").append(room.roomId()).append("\",\"label\":\"房间 ID ")
                            .append(room.roomId()).append(" / 房间号 ")
                            .append(escapeJs(room.roomNumber())).append("\"}");
                }
                json.append("]");
            }
            return json.toString();
        } catch (SQLException exception) {
            return "";
        }
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
                        a:hover { filter: brightness(1.05); }
                    </style>
                </head>
                <body>
                    <div class="box">
                        <h2>❌ 请求失败</h2>
                        <p>""" + escapeHtml(error) + """
                        </p>
                        <a href="/">返回首页</a>
                    </div>
                </body>
                </html>
                """;
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
