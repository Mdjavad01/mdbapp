# CI configuration

## `github-actions-android.yml`

The Android CI pipeline: assemble → lint → unit tests on every push and pull
request to `main`.

**It is parked here rather than in `.github/workflows/` because the GitHub token
used to push this repository does not carry the `workflow` scope.** GitHub
refuses any commit that creates or updates a file under `.github/workflows/`
unless the credential has that scope, and the push is rejected wholesale — so
the file would have blocked the entire phase from landing.

### To activate it, either

**Option A — via the GitHub web UI (no token change needed):**

1. Open <https://github.com/Mdjavad01/mdbapp/new/main/.github/workflows>
2. Name the file `android.yml`
3. Paste the contents of `github-actions-android.yml`
4. Commit

**Option B — grant the scope and let the tooling move it:**

Regenerate the personal access token with the **`workflow`** scope added
(<https://github.com/settings/tokens>), then move the file:

```bash
mkdir -p .github/workflows
git mv ci/github-actions-android.yml .github/workflows/android.yml
git commit -m "ci: activate the Android pipeline"
git push
```

### Note on the pipeline

`./gradlew lint` runs with `abortOnError = true` (see `app/build.gradle.kts`), so
a lint error fails the build rather than producing a warning nobody reads. The
`lintDevDebug` and `testDevDebugUnitTest` tasks target the `dev` flavour because
`staging` currently points at a host that does not exist yet.
