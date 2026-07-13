# Repository Collaboration Comments Design

## Goal

Make the repository layer readable as an explicit collaboration boundary for
the service layer, without changing method signatures, SQL, or runtime
behavior.

## Scope

- Document all four contracts in `src/main/java/repo/interfaces` and every
  declared method.
- Document the four JDBC implementations in `src/main/java/repo/jdbc` at the
  persistence and mapping boundaries.
- Clarify Repository accessors in `DatabaseConnection` where a Service obtains
  its data-access dependency.
- Preserve all executable code and existing public APIs.

## Comment Style

Use concise Chinese Javadoc. Every interface and public repository method must
state the collaborating Service, the business scenario that calls it, and the
return or absence semantics that the Service must handle. The comments should
read like notes written for teammates maintaining adjacent layers, for example:

```java
/**
 * 给 BuildingService 在新增宿舍楼前检查楼号是否重复。
 * 这里只负责按业务编号查询；是否提示用户、是否允许继续由业务层决定。
 */
Optional<Building> findByCode(String buildingCode) throws SQLException;
```

JDBC implementation comments must explain table-field mapping, ordering,
cross-table joins, generated-key handling, and the handoff back to the Service.
They must not paraphrase individual Java statements or claim validation that
belongs to the Service layer.

## Collaboration Boundaries

- `BuildingRepository` supports `BuildingService` for creation checks, listing,
  and ID/code resolution.
- `RoomRepository` supports `RoomService` and `DormService` through room lookup
  and building-scoped listing.
- `StudentRepository` supports `StudentService` and `DormService` through
  student lookup plus student/dorm view projections.
- `DormRepository` supports `DormService` for assignments, moves, and bed
  occupancy checks.
- `DatabaseConnection` owns concrete JDBC repository construction and supplies
  them to Services; Services own validation and user-facing errors.

## Verification

Compile with Maven after edits. Review the diff to ensure it changes comments
only in the scoped repository and connection files.
