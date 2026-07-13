# Web Regression Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make both custom Maven launchers recover from the repository's incomplete Maven distribution and render exact room lookup input values without embedded line breaks.

**Architecture:** Preserve the existing launcher and server-rendered HTML designs. Add an integrity check at each launcher boundary, and replace only the three malformed value-attribute fragments in `DashboardHandler` with explicit string appends.

**Tech Stack:** Windows batch, POSIX shell, Java 21, Maven 3.9.9, JDK HTTP server, Playwright CLI.

## Global Constraints

- Modify existing local `main`; do not create or push a branch.
- Do not change database or service behavior.
- Keep existing HTML escaping.
- Do not include downloaded Maven binaries, build output, or browser artifacts in commits.

---

### Task 1: Maven Distribution Integrity Checks

**Files:**
- Modify: `mvnw.cmd:25-34`
- Modify: `mvnw:27-45`

**Interfaces:**
- Consumes: `MAVEN_DIR`, `MAVEN_CMD`, and `DISTRIBUTION_URL` already resolved by each launcher.
- Produces: launcher behavior that downloads Maven when either its command or `boot/plexus-classworlds-*.jar` is absent.

- [ ] **Step 1: Run the failing Windows launcher check**

Run: `cmd /c "mvnw.cmd -v"`

Expected: exit `1` with `Error: -classpath requires class path specification` because the command exists but the boot JAR does not.

- [ ] **Step 2: Run the failing POSIX launcher check**

Run: `bash ./mvnw -v`

Expected: nonzero exit with a missing classpath/main-class error for the same incomplete distribution.

- [ ] **Step 3: Add the minimal integrity checks**

In `mvnw.cmd`, replace the command-only condition with:

```bat
IF EXIST "%MAVEN_CMD%" IF EXIST "%MAVEN_DIR%\boot\plexus-classworlds-*.jar" GOTO run_maven
```

In `mvnw`, replace the command-only condition with:

```sh
MAVEN_BOOT_JAR=$(find "$MAVEN_DIR/boot" -maxdepth 1 -name 'plexus-classworlds-*.jar' -print -quit 2>/dev/null)
if [ ! -x "$MAVEN_CMD" ] || [ -z "$MAVEN_BOOT_JAR" ]; then
```

- [ ] **Step 4: Verify Windows recovery**

Run: `cmd /c "mvnw.cmd -v"`

Expected: the launcher downloads Maven and reports `Apache Maven 3.9.9` with exit `0`.

- [ ] **Step 5: Recreate the incomplete state and verify POSIX recovery**

Remove only ignored JAR files and the ignored downloaded ZIP from `.mvn/wrapper/apache-maven-3.9.9` and `.mvn/wrapper`, then run `bash ./mvnw -v`.

Expected: the launcher downloads Maven and reports `Apache Maven 3.9.9` with exit `0`.

- [ ] **Step 6: Commit the launcher fix**

```powershell
git add -- mvnw mvnw.cmd
git commit -m "fix: recover incomplete Maven wrapper distribution"
```

### Task 2: Exact Room Lookup Input Values

**Files:**
- Modify: `src/main/java/handler/DashboardHandler.java:526-547`

**Interfaces:**
- Consumes: escaped `lookupRoomId`, `lookupFloorNumber`, and `lookupRoomNumber` strings.
- Produces: exact HTML attributes `value="<escaped value>"` with no text-block whitespace.

- [ ] **Step 1: Start the application and verify the failing HTML assertion**

Run the application with a complete Maven installation, fetch `/`, and assert that the numeric lookup input does not match `value="\s+"`.

Expected: FAIL because the current response renders `value="\n"`.

- [ ] **Step 2: Replace the three malformed fragments**

Use explicit appends for each input:

```java
html.append("<label>房间 ID<input name=\"lookupRoomId\" value=\"")
        .append(escapeHtml(lookupRoomId))
        .append("\" placeholder=\"例如 1\"></label>\n");

html.append("<label>楼层<input type=\"number\" name=\"lookupFloorNumber\" min=\"1\" value=\"")
        .append(escapeHtml(lookupFloorNumber))
        .append("\" placeholder=\"例如 3\" required></label>\n");

html.append("<label>房间号<input name=\"lookupRoomNumber\" value=\"")
        .append(escapeHtml(lookupRoomNumber))
        .append("\" placeholder=\"例如 301\" required></label>\n");
```

- [ ] **Step 3: Run the HTML assertion again**

Expected: PASS; the empty numeric value is exactly `value=""`.

- [ ] **Step 4: Verify query-state values and Chromium console**

Open `/?lookupFloorNumber=3&lookupRoomNumber=301` through Playwright, inspect the rendered input value, and list console messages.

Expected: the floor input preserves `3`, and the previous value-parsing warning is absent.

- [ ] **Step 5: Commit the HTML fix**

```powershell
git add -- src/main/java/handler/DashboardHandler.java
git commit -m "fix: render exact room lookup values"
```

### Task 3: Final Regression Verification

**Files:**
- Verify: `pom.xml`
- Verify: repository status and generated artifacts

**Interfaces:**
- Consumes: both completed fixes.
- Produces: reproducible build and browser evidence for local `main`.

- [ ] **Step 1: Run the full build through the repaired Wrapper**

Run: `.\mvnw.cmd clean verify`

Expected: `BUILD SUCCESS`, with no test failures.

- [ ] **Step 2: Run browser smoke checks**

Start the application, load the dashboard at desktop and 375-pixel widths, snapshot the page, and inspect console output.

Expected: HTTP `200`, no horizontal overflow, and no value-parsing warning.

- [ ] **Step 3: Clean generated artifacts and stop services**

Close Playwright, stop the application, remove `.playwright-cli/` and `output/playwright/` if created, and restore tracked `target/` files generated by verification.

- [ ] **Step 4: Verify final repository state**

Run: `git status --short --branch` and `git log -3 --oneline`.

Expected: local `main` contains only the intended local commits, `.claude/` remains untracked, and ports `8080`/`18080` have no test listeners.
