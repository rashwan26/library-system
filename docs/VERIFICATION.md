# Verification

## Local checks

- OpenJDK 17, Linux: `sh test.sh` compiled all production sources, built the executable JAR and passed all 39 regression scenarios.
- Tests use temporary data directories and generated images; they do not use or publish the supplied member records or photos.
- The suite covers all book types and legacy formats, quoted/Unicode metadata, reload after saving, borrowing/returns, duplicate loans, loan limits, reference restrictions, fines, edits, deletion rules, failed-save rollback, malformed data, image validation, CSV/log/ZIP output, sorted Swing table selection and the application lock.

## Continuous integration

The [Build and test workflow](https://github.com/rashwan26/library-system/actions/workflows/tests.yml) runs the same suite on Ubuntu and Windows with Temurin Java 17 and 21. The Ubuntu / Java 17 job also checks optional Maven packaging with `mvn -B -ntp package`.

[Verified CI run](https://github.com/rashwan26/library-system/actions/runs/44825517196), source commit `100f6dd`:

| Runner | JDK | Result |
|---|---|---|
| Ubuntu | 17 | 39 scenarios passed; Maven package succeeded |
| Ubuntu | 21 | 39 scenarios passed |
| Windows | 17 | 39 scenarios passed through `test.bat` |
| Windows | 21 | 39 scenarios passed through `test.bat` |

The Windows check exposed and verified a fix for backslash escaping in javac source argument files.

Maven packaging does not execute the standalone regression harness; CI runs that harness explicitly first.

## Scope

The full Swing GUI compiles and sorted table selection is tested with actual Swing components in headless mode. A full visual desktop walkthrough, native file chooser interaction and manual launch on macOS were not performed in this environment. Passing headless tests does not establish visual desktop behavior.
