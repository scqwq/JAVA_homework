# Repository Collaboration Comments Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Document the repository layer as a practical contract between JDBC persistence and the service layer.

**Architecture:** Interface Javadocs define the handoff to Services: who calls each method, for which business operation, and how absence or results are handled. JDBC comments document field mapping, ordering, joins, generated keys, and the responsibility boundary; `DatabaseConnection` comments identify the injected Repository consumer.

**Tech Stack:** Java 21, JDBC, Maven 3.9.9.

## Global Constraints

- Do not change executable statements, method signatures, SQL text, or public APIs.
- Use concise Chinese Javadoc that addresses teammates maintaining the service layer.
- State the collaborating Service, business scenario, and result/absence meaning on each Repository contract method.
- Do not stage `.env`, `target/`, or `.claude/`.

---

### Task 1: Document Repository Contracts

**Files:**

- Modify: `src/main/java/repo/interfaces/BuildingRepository.java`
- Modify: `src/main/java/repo/interfaces/RoomRepository.java`
- Modify: `src/main/java/repo/interfaces/StudentRepository.java`
- Modify: `src/main/java/repo/interfaces/DormRepository.java`

**Interfaces:**

- `BuildingRepository` is consumed by `BuildingService`.
- `RoomRepository` is consumed by `RoomService` and indirectly by `DormService`.
- `StudentRepository` is consumed by `StudentService` and indirectly by `DormService`.
- `DormRepository` is consumed by `DormService`.

- [ ] **Step 1: Replace minimal comments with service-facing interface Javadocs**

  Add an interface-level Javadoc describing the table/entity responsibility and
  the Service it supports. Add method Javadocs in this pattern:

  ```java
  /**
   * 给 BuildingService 在新增宿舍楼前检查楼号是否重复。
   * 这里只负责按业务编号查询；是否提示用户、是否允许继续由业务层决定。
   *
   * @return 已存在的宿舍楼；没有同编号记录时返回 Optional.empty()
   */
  Optional<Building> findByCode(String buildingCode) throws SQLException;
  ```

  Apply equivalent collaboration notes to all 17 methods, including generated
  entity return values for `save`, room/bed occupancy semantics for
  `existsByRoomIdAndBedNumber`, and view-projection semantics for
  `StudentDormView` methods.

- [ ] **Step 2: Review contract coverage before compiling**

  Run:

  ```powershell
  rg -n "^\s*/\*\*|\b(save|findAll|findById|findByCode|findByBuildingId|findByStudentId|findByRoomId|findStudentsByRoom|findDormByStudentId|existsByRoomIdAndBedNumber|update)\b" src/main/java/repo/interfaces
  ```

  Expected: every interface and every declared method has a nearby Javadoc;
  comments name the relevant Service and do not change code.

### Task 2: Document JDBC Persistence Boundaries

**Files:**

- Modify: `src/main/java/repo/jdbc/JdbcBuildingRepository.java`
- Modify: `src/main/java/repo/jdbc/JdbcRoomRepository.java`
- Modify: `src/main/java/repo/jdbc/JdbcStudentRepository.java`
- Modify: `src/main/java/repo/jdbc/JdbcDormRepository.java`

**Interfaces:**

- Each class implements the same Repository contract documented in Task 1.
- Services receive domain models or `Optional`/list results; JDBC-specific
  mapping and SQL remain encapsulated in these classes.

- [ ] **Step 1: Add class-level handoff comments**

  Document the table(s) owned by each implementation and the Service-facing
  boundary. Use this shape:

  ```java
  /**
   * BuildingRepository 的 JDBC 实现。
   * 给 BuildingService 提供宿舍楼持久化能力；业务层传入领域对象，
   * 本类负责把它转换为 buildings 表记录，不在这里判断楼号是否允许创建。
   */
  public class JdbcBuildingRepository implements BuildingRepository {
  ```

- [ ] **Step 2: Add comments at persistence decision points**

  Explain generated-key retrieval in every `save`, ordered list queries,
  `Optional.empty()` mapping, student/dorm join projections, and the
  `excludeStudentId` condition that lets `DormService.changeDorm` ignore the
  current student's own bed assignment. Do not comment individual
  `setString`/`setLong` calls.

- [ ] **Step 3: Review that implementation comments preserve layer boundaries**

  Run:

  ```powershell
  git diff -- src/main/java/repo/jdbc
  ```

  Expected: diff changes comments only and repeatedly makes clear that user
  messages and business validation remain in Service/UI layers.

### Task 3: Clarify Connection-to-Service Injection

**Files:**

- Modify: `src/main/java/database/DatabaseConnection.java`

**Interfaces:**

- `buildingRepository()` supplies `BuildingRepository` to `BuildingService`.
- `roomRepository()` supplies `RoomRepository` to `RoomService`.
- `studentRepository()` supplies `StudentRepository` to `StudentService`.
- `dormRepository()` supplies `DormRepository` to `DormService`.

- [ ] **Step 1: Expand Repository accessor Javadocs**

  Add concise notes such as:

  ```java
  /**
   * 供 BuildingService 获取宿舍楼数据访问入口。
   * 连接和 JDBC 实现由本类统一持有，业务层只依赖 BuildingRepository 约定。
   */
  public BuildingRepository buildingRepository() {
  ```

  Apply the same handoff wording to room, student, and dorm accessors.

- [ ] **Step 2: Compile and inspect the scoped diff**

  Run:

  ```powershell
  mvn test
  git diff --check
  git diff -- src/main/java/repo src/main/java/database/DatabaseConnection.java
  ```

  Expected: Maven reports `BUILD SUCCESS`, no whitespace errors, and the diff
  contains comments only in the planned files.

- [ ] **Step 3: Commit the collaboration comments**

  ```powershell
  git add src/main/java/repo/interfaces src/main/java/repo/jdbc src/main/java/database/DatabaseConnection.java
  git commit -m "docs: clarify repository service collaboration"
  ```
