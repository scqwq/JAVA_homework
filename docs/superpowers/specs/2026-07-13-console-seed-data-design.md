# Console Validation and Demo Seed Data

## Goal

Provide repeatable demo data for the dormitory management console and validate
its main interaction paths against PostgreSQL. The seed SQL must also run on
MySQL without database-specific syntax.

## Scope

- Fill `src/main/resources/sql/init.sql` with manual seed data.
- Keep application startup behavior unchanged: it creates tables only and does
  not import seed data.
- Make the seed script safe to execute repeatedly by inserting each logical
  record only when it is absent.
- Seed buildings, rooms, students, and dorm assignments that make every
  console query and assignment workflow demonstrable.
- Exercise scripted console paths for navigation, input validation, listing,
  lookup, assignment, and dorm changes.
- Fix only bugs reproduced by those paths.

## Seed Data Design

The SQL will not hard-code generated primary keys. It will look up buildings by
`building_code`, rooms by `(building_id, room_number)`, and students by
`student_id`. This avoids auto-increment/sequence differences between
PostgreSQL and MySQL.

Each insert uses `INSERT ... SELECT ... WHERE NOT EXISTS`, which is supported
by both target databases. The script contains one male building, one female
building, one mixed-policy building, rooms on multiple floors, students of both
genders, several assignments, and at least one unassigned student.

## Execution

Run the script manually after the application has created the schema:

```powershell
psql -h 127.0.0.1 -U postgres -d JAVA -f src/main/resources/sql/init.sql
```

The corresponding MySQL command is documented alongside the script or README
only if it is necessary to make execution unambiguous. Re-running the script
must not duplicate data.

## Console Validation

Validation uses a dedicated temporary PostgreSQL database so production/demo
data in `JAVA` is not altered. It runs the application with scripted input and
checks for expected console messages and database state. The paths cover:

- invalid numeric input and return navigation;
- list and lookup views with seeded data;
- student-to-dorm lookup, room-to-student lookup, and unassigned student;
- valid assignment and dorm change;
- duplicate assignment, occupied-bed, gender-policy, and room/building mismatch
  rejection.

## Non-goals

- No automatic seed execution at application startup.
- No migration framework or external test database dependency.
- No changes to remote branches or repository configuration.
