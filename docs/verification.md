# Recorded verification

Verified on **29 October 2026** against `https://restful-booker.herokuapp.com`.

| Execution | Passed | Failed | Skipped | Outcome |
|---|---:|---:|---:|---|
| Default suite: 69 API cases and 12 framework checks | 81 | 0 | 0 | Successful |
| Explicit strict known-defect suite | 0 | 6 | 0 | Six documented defects reproduced |

The default run took approximately four minutes. This is a functional execution duration, not a performance benchmark or service-level guarantee. It excludes the visibly documented `known-defect` group.

Local verification used JDK 24.0.1, Maven Wrapper 3.9.16, and Java 17 compilation. GitHub Actions runs the project on Temurin 17; consult its run artifacts for the result of each CI execution.

## Reproduced defects

- OBS-001: incomplete creation returns 500 instead of the strict expected 400.
- OBS-002: check-in filtering excludes an equal boundary despite the documented inclusive rule.
- OBS-003: patching one nested date corrupts the omitted checkout date.
- OBS-004: an XML response is labelled `text/html`.
- OBS-005: form-encoded `false` becomes boolean true.
- OBS-006: checkout filtering contradicts the documented lower-bound direction.

These were genuine failed assertions, not tests converted to passes or skips. Each creates its own data and runs cleanup. See [coverage and expected outcomes](test-coverage.md) for the corresponding methods and the reason for the default exclusion.

## Reports

The default Allure HTML report was generated and opened in a browser, showing 81 cases and 100% passing for that execution. A separate HTML report records all six strict failures.

To keep separate report directories when reproducing the defect suite after a default run:

```bash
./mvnw test -Dgroups=known-defect -DexcludedGroups= \
  -Dallure.results.directory="$PWD/target/known-defects/allure-results"

# Run this even though the test command above exits with failures.
./mvnw allure:report \
  -Dallure.results.directory="$PWD/target/known-defects/allure-results" \
  -Dallure.report.directory="$PWD/target/known-defects/allure-report"
```

Default report: `target/site/allure-maven-plugin/`. Separate defect report: `target/known-defects/allure-report/`. Generated evidence is ignored by Git and retained in CI artifacts.

Use absolute paths for custom Allure directories, as shown above: the test adapter and report plugin resolve relative paths differently.

Results describe one execution against a shared, resetting practice service. They do not establish exhaustive input coverage or guarantee that a later execution will have the same outcome.
