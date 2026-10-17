# CI configuration

## `github-actions-android.yml`

The Android CI pipeline. On every push and pull request to `main` it:

1. sets up JDK 17,
2. restores the Gradle cache,
3. installs Android SDK platform 35 and build-tools 35.0.0,
4. runs `./gradlew assembleDevDebug test lint --continue`,
5. uploads the dev debug APK, plus the test and lint reports.

**It is parked here rather than in `.github/workflows/` because the GitHub token
used to push this repository does not carry the `workflow` scope.** GitHub
refuses any commit that creates or updates a file under `.github/workflows/`
unless the credential has that scope, and the push is rejected wholesale.

### To activate it

1. Open
   <https://github.com/Mdjavad01/mdbapp/blob/main/ci/github-actions-android.yml>
   and click **Raw** (or the copy icon at the top right of the file view).
2. Copy the whole file.
3. Open
   <https://github.com/Mdjavad01/mdbapp/edit/main/.github/workflows/android.yml>
   — if the file already exists, select everything (`Ctrl+A`) and delete it
   first.
4. Paste, then **Commit changes** with the message
   `ci: build, test and lint on GitHub Actions`.
5. Open the **Actions** tab → **Android CI** → **Run workflow** (or just push
   any commit).

### What the pipeline guarantees

- **Lint is not advisory.** `lint` runs with `abortOnError = true`
  (`app/build.gradle.kts`), so a lint error fails the job. Nothing in the
  workflow downgrades it.
- **Tests are not filtered.** `test` runs every variant's unit tests; no
  `-x`, no `continue-on-error`, no `--tests` filter.
- **`--continue` does not weaken the build.** It still fails the job on the
  first failing task, but lets the remaining tasks run so their HTML reports
  exist in the artifacts when the build is red.

### Artifacts

| Artifact | Contents | When |
|---|---|---|
| `mdbapp-dev-debug-apk` | `app-dev-debug.apk` | whenever the APK was produced |
| `test-reports` | JUnit HTML + XML per variant | always |
| `lint-reports` | `lint-results-*.html` / `.xml` / `.txt` | always |

### Why the SDK step exists

The `ubuntu-latest` image ships an Android SDK, but the installed platforms
change between image releases. `compileSdk = 35` means the build needs
`platforms;android-35` and `build-tools;35.0.0`; the workflow installs them
explicitly so the build does not depend on the image's contents that week. The
step also writes `local.properties` (`sdk.dir`), which is gitignored and
therefore absent from a fresh checkout.
