# Web Regression Fixes Design

## Scope

Fix two confirmed regressions on local `main` without creating or pushing a new branch:

1. The bundled Maven launchers treat an incomplete Maven distribution as usable and fail with `-classpath requires class path specification`.
2. The room lookup form renders line breaks inside input `value` attributes, producing an invalid numeric value and a Chromium warning.

Unknown-route HTTP semantics and unrelated UI or service changes are outside this change.

## Design

### Maven launchers

Keep the existing custom launchers and distribution layout. Before launching Maven, both `mvnw.cmd` and `mvnw` must verify that the launcher and the `plexus-classworlds` boot JAR exist. If either is missing, the launcher downloads and extracts the configured Maven distribution before continuing.

This is smaller than deleting the checked-in partial distribution or replacing the wrapper implementation, while addressing the actual incomplete-install condition on Windows and Unix-like systems.

### Room lookup HTML

Build each affected `value` attribute without text-block line breaks. The room ID, floor number, and room number values must render as exact escaped values, including an exact empty string when no query state exists.

## Error Handling

Existing download failures remain fatal and return a nonzero launcher exit code. Existing HTML escaping remains unchanged. No database or service behavior changes are introduced.

## Verification

Use red-green regression checks:

- Before the launcher change, `mvnw.cmd -v` and `bash ./mvnw -v` fail against the incomplete checked-in Maven directory.
- Before the HTML change, the rendered homepage contains whitespace inside the numeric lookup value and Chromium logs a parsing warning.
- After the changes, both launchers report Maven successfully, `clean verify` succeeds, the numeric input has an exact empty value, and Chromium reports no warning.

The final verification also confirms that no test server remains running and no generated browser artifacts are committed.
