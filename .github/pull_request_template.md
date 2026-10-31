## What this changes

<!-- One paragraph. What the user sees differently, not which files you touched. -->

## Which parts

<!-- Tick what this PR touches. A response-shape change touches at least two. -->

- [ ] `app/` — Android
- [ ] `web/` — PWA
- [ ] `wordpress-plugin/` — backend

## Contract check

<!-- The plugin is the contract. Delete this section if no endpoint changed. -->

- Endpoints added / changed:
- Do the clients need this to ship first? `plugin` / `app` / `web` / together:

## Checks

- [ ] `./gradlew test` (if `app/` changed)
- [ ] `npm run lint` and `npm run build` (if `web/` changed)
- [ ] `php -l` on every touched PHP file + the plugin tests (if the plugin changed)
- [ ] no secrets in the diff — no `.env`, no keystore, no token in a fixture

## Screenshots

<!-- For anything visual. Before/after if it is a fix. -->
