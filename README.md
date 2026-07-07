# JAVA_homework

学生宿舍管理系统，基于 Java 21 + JDBC + Maven，实现了：

- 学生管理
- 宿舍楼管理
- 宿舍房间管理
- 宿舍分配与调换
- 根据宿舍查找学生
- 根据学生查找宿舍

目前实现逻辑/注释还未完成，看得懂JAVA的可以帮忙看看程序逻辑，提提修改意见

项目使用接口 + JDBC 实现仓储层多态，当前支持 `PostgreSQL` 和 `MySQL`。

## 快速开始

1. 复制 `.env.example` 为 `.env`
2. 配置数据库连接串
3. 如果你的电脑已经安装 Maven，执行：

```bash
mvn clean compile
mvn exec:java
```

4. 如果另一台电脑没有全局安装 Maven，也可以直接用项目自带的 Maven Wrapper：

Windows:

```powershell
.\mvnw.cmd clean compile
.\mvnw.cmd exec:java
```

macOS / Linux:

```bash
chmod +x mvnw
./mvnw clean compile
./mvnw exec:java
```

程序启动时会自动建表。`mvnw` / `mvnw.cmd` 首次运行时会从 `.mvn/wrapper/maven-wrapper.properties` 指定的地址下载 Maven，因此第一次执行需要联网。

## 表结构设计

- `buildings`
  - 主键：`building_id`
  - 唯一键：`building_code`
- `rooms`
  - 主键：`room_id`
  - 外键：`building_id -> buildings.building_id`
  - 唯一键：`(building_id, room_number)`
- `students`
  - 主键：`student_id`
- `dorm_assignments`
  - 主键：`assignment_id`
  - 外键：`student_id -> students.student_id`
  - 外键：`building_id -> buildings.building_id`
  - 外键：`room_id -> rooms.room_id`
  - 唯一键：`student_id`
  - 唯一键：`(room_id, bed_number)`

约束说明：

- 每个房间固定 4 张床，床号范围 `1-4`
- 同一房间同一床位不能重复占用
- 一个学生同时只能有一条住宿分配记录

## 说明

- 宿舍楼性别支持：`男`、`女`、`男女分层`
- `男女分层` 在本实现中表示该楼允许男女入住；如果后续需要精确到“某层仅男/某层仅女”，可以继续在 `rooms` 或楼层规则表上扩展
