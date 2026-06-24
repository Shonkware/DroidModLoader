# Current Status

- **Latest public release:** `v0.6.0-beta`
- **Minimum Android version:** Android 11 / API 30
- **Development state:** Maintenance and planning after `v0.6.0-beta`
- **Storage model:** Direct filesystem access for shared-storage workflows

## Release and source state

The public release remains `v0.6.0-beta`. The `main` branch contains completed
post-release refactoring and documentation maintenance, but it is not a newer
release.

Implementation of the next feature release has not started. Its scope will be
set through the roadmap and current-priorities documents before feature work is
recorded as active.

## Current source structure

Recent architecture work is complete on `main`:

- `MainActivity` remains the Android composition root and delegates focused work
  to workflow controllers and supporting classes.
- `ModEngine` remains the stable engine facade while focused services handle mod
  library, plugin, deployment, inspection, and archive-history behavior.
- Profile-specific direct paths are used for game roots, `Data` folders, and
  archive libraries.
- Supported legacy Bethesda games use game-aware plugin activation and ordering.

The architecture changes were intended to preserve existing behavior. The only
intentional user-facing removal was the obsolete developer-only v0.5 repair
action.

## Latest recorded validation

The completed refactoring passed the repository documentation check, project
check, JVM unit tests, and debug APK assembly on the development machine.

Recorded Android disposable-folder checks cover:

- all-files permission onboarding;
- direct folder selection and profile-specific persistence;
- ZIP, 7Z, and RAR archive handling basics;
- deployment, full redeployment, backup, and restoration;
- Skyrim Legendary Edition text-file plugin ordering; and
- Oblivion, Fallout 3, and Fallout: New Vegas timestamp ordering.

See [Direct Storage Benchmark](docs/benchmarks/direct-storage.md) for the
recorded storage comparison and its limitations.

## Next planned work

1. Improve ZIP, 7Z, and RAR extraction compatibility and failure reporting.
2. Define which capabilities must block a stable 1.0 release.
3. Preserve the extracted architecture while adding or fixing behavior.

## Known limitations and deferred checks

- Some RAR and 7Z variants remain unsupported or need clearer errors.
- Game Root and `Data` folder guidance still needs simpler wording and stronger
  validation.
- Real-container plugin output verification remains a release or regression
  check rather than the next implementation task.
- TTW setup, `DML_output`, configuration recipes, and INI presets remain planned
  work until separately scoped.

## Last updated

2026-06-24
