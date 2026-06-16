# Current Status

## Status

Active development.

The latest pushed refactor baseline is commit `93842a6`
(`refactor: extract pending installer workflow`).

`MainActivity.kt` is approximately 2,194 lines at this baseline. The current
cleanup remains focused on reducing its responsibilities without changing
user-facing behavior.

## Current goal

Apply and validate the deployment execution workflow extraction, then continue
reducing `MainActivity` through cohesive, behavior-preserving extractions.

## Last completed action

Extracted pending installer execution from `MainActivity` into
`PendingInstallerWorkflow`, with focused tests covering finalization, option
selection, cancellation, cleanup, and failure behavior.

## Last verified result

A specific local build and test result has not yet been recorded in this file.

Before accepting the next code-changing commit, record the result of:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Include the tested commit hash when this section is updated.

## Current constraints

- No UI redesign.
- No feature removal.
- No intentional behavior change during extraction refactors.
- No dependency changes unless separately approved.
- One cohesive responsibility area per commit.
- Refactors may be moderately larger when the moved code belongs to the same
  responsibility area.
- Generate changes against the latest pushed commit or a current source ZIP,
  never an older repository snapshot.
- Provide a reviewable diff for every code-changing commit.
- Explain every code-changing commit before acceptance.
- Run unit tests and assemble the debug APK before accepting code changes.
- Update documentation or the changelog only when structure, behavior,
  release information, or documented process changes.
- Update the Nexus Mods page whenever the app version changes.

## Current risks and open work

- `MainActivity.kt` still owns deployment-adjacent orchestration, dashboard
  refresh/setup loading, plugin discovery, operation reporting, developer
  tools, recovery summaries, and engine construction.
- `ModEngine.kt` remains a separate large service-extraction project and
  should not be mixed into the remaining `MainActivity` cleanup.
- Archive extraction still needs broader 7z and RAR compatibility and more
  robust failure handling.
- Beginner-facing wording must distinguish the game root folder from the Data
  folder clearly and accurately.

## Next physical action

Review and apply the deployment execution workflow patch against commit
`93842a6`.

Then run:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
git diff --check
```

If validation passes, manually test normal deployment and force-full-redeploy
against a safe test target before committing.

## Later

- Finish the remaining `MainActivity` responsibility extractions.
- Plan `ModEngine` service extraction as a separate phase.
- Improve archive extraction robustness and compatibility.
- Review beginner-facing wording when touched by related work.
- Perform a final integration and architecture review after the giant-file
  cleanup is complete.

## Last updated

2026-06-15
