# Current Status

* **Mode:** Active development after the `v0.6.0-beta` release.
* **Public version:** `v0.6.0-beta`
* **Branch / baseline:** `main` at `191397a` (`fix: scope archive folder selection to profiles`)
* **Repository state:** Local `main` matched `origin/main` with a clean working tree on 2026-06-17.

## Current objective

Restore accurate repository-controlled project state after the release, then resume only roadmap-aligned work.

Before beginning another code change:

1. Review process documents and task lists for stale `v0.6.0-beta` release-preparation language.
2. Clearly separate released `v0.6.0-beta` behavior from unreleased `v0.7.0` and `v0.8.0` plans.
3. Select one bounded next DML task that follows the accepted roadmap.

## Completed most recently

Published `v0.6.0-beta` and pushed commit `191397a`, which scopes archive-folder selection, persisted folder access, archive history, and related settings to individual profiles.

The release includes the archive-folder browser workflow for:

* selecting and retaining an archive folder;
* discovering top-level ZIP, 7Z, and RAR archives;
* searching and refreshing the archive list;
* changing archive-folder locations;
* installing archives through the existing import and installer pipeline; and
* keeping archive settings and history isolated by profile.

## Last recorded validation

The earlier archive-folder browser implementation recorded successful results for:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
git diff --check
```

The exact final release validation result was not preserved in this file. Do not infer unrecorded test results.

The repository itself was confirmed clean and synchronized with `origin/main` on 2026-06-17.

## Next safe action

Audit DML’s process documentation, task lists, and roadmap references for stale release-preparation state.

Correct documentation only where it is outdated. Do not begin a new refactor or feature during that audit.

Afterward, record one concrete next implementation task here.

## Current constraints

* Generate changes only against the latest local source.
* Keep each commit focused on one coherent responsibility.
* Explain code-changing commits before acceptance.
* Provide a reviewable diff for every code change.
* Run appropriate tests and builds before committing code.
* Keep current released behavior separate from future plans.
* Update documentation and changelogs when documented behavior changes.
* Update the Nexus Mods page whenever the public app version changes.
* Do not automatically commit, push, merge, tag, publish, release, or make destructive changes.

## Known open work

* Continue the remaining `MainActivity.kt` responsibility extractions in bounded commits.
* Treat `ModEngine.kt` service extraction as a separate later project.
* Improve 7Z and RAR extraction compatibility and failure reporting.
* Continue improving beginner-facing Game Root and Data Folder wording.
* Reconcile planned TTW, game-folder validation, configuration-recipe, plugin-ordering, and deployment-output work with the version roadmap before implementation.
* Keep guide documentation accurate for the currently released DML version.

## Blockers

No repository blocker is currently recorded.

The next implementation task should not be selected until stale process and roadmap state has been checked.

## Private and public boundary

Unreleased `v0.7.0` and `v0.8.0` functionality must be identified as planned rather than current behavior.

Private experiments, unpublished research, credentials, signing material, and private project context must not be added to the public repository.

## Parking lot

* `ModEngine.kt` service extraction
* broader archive-format hardening
* additional game-specific validation
* expanded deterministic workflow tooling
* cross-project `workctl`, only after repository-local commands are proven

## Last updated

2026-06-17
