# Current Status

## Status

Active development.

The current `main` baseline is merge commit `cf57394`
(`Merge pull request #2 from CyberShonk/feat/archive-folder-browser`).
Droid Mod Loader is being prepared for the `v0.6.0-beta` release.

The archive-folder browser is now merged. The larger responsibility-extraction
work remains incomplete, but the app can still be released while that cleanup
continues later because the refactors are intended to preserve behavior.

## Current goal

Finish the `v0.6.0-beta` release preparation without mixing in additional
refactors or unrelated features. Validate the merged application, test the new
archive-folder workflow on a device, build the signed release APK, and publish
accurate release notes.

## Last completed action

Merged the archive-folder browser into `main` through pull request #2.

The merged feature:

- remembers a user-selected archive folder;
- scans top-level ZIP, 7Z, and RAR files;
- provides search, refresh, and folder switching;
- shows current and previous installation state;
- installs through the existing archive import and installer pipeline; and
- preserves main-screen and fullscreen-list scroll state during the session.

## Last verified result

The archive-folder browser working tree that became commit `4b63670` passed:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
git diff --check
```

The final clean release validation on merged `main` still needs to be recorded
before publishing `v0.6.0-beta`.

## Current constraints

- Keep release preparation separate from additional feature or refactor work.
- Fix only release-blocking defects before publishing.
- No dependency changes unless separately approved.
- Generate changes against the latest pushed commit or a current source ZIP,
  never an older repository snapshot.
- Provide a reviewable diff for every code-changing commit.
- Explain every code-changing commit before acceptance.
- Run unit tests and assemble both debug and release APKs before publishing.
- Update documentation or the changelog when structure, behavior, release
  information, or documented process changes.
- Update the Nexus Mods page whenever the app version changes.

## Current risks and open work

- Final fresh-install and upgrade testing for `v0.6.0-beta` is not yet recorded.
- Archive extraction still needs broader 7Z and RAR compatibility and more
  robust failure handling.
- `MainActivity.kt` cleanup remains active after the release.
- `ModEngine.kt` remains a separate large service-extraction project and should
  not be mixed into release preparation.
- Beginner-facing wording must continue to distinguish the Game Root from the
  Data Folder clearly and accurately.

## Next physical action

Finish the version and documentation changes, then run:

```bash
./gradlew clean
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew assembleRelease
git diff --check
```

After the builds pass, manually test at least:

- a fresh installation;
- an upgrade from `v0.5.5-beta`;
- first-use archive-folder selection and permission persistence;
- ZIP, 7Z, and RAR discovery;
- refresh and folder switching;
- simple and installer-driven archive installation;
- profile, mod, plugin, and deployment basics; and
- application launch from the signed release APK.

## Later

- Resume the remaining `MainActivity` responsibility extractions.
- Plan `ModEngine` service extraction as a separate phase.
- Improve archive extraction robustness and compatibility.
- Continue improving beginner-facing storage and folder wording.
- Perform a final integration and architecture review after the giant-file
  cleanup is complete.

## Last updated

2026-06-16
