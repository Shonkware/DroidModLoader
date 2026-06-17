# Droid Mod Loader Changelog

This file tracks user-facing release history.

## Format

Use this format for each release:

## Version - Date

### Added

- New features.

### Changed

- Behavior changes.

### Fixed

- Bug fixes.

### Known Issues

- Problems users should know about.

### Upgrade Notes

- Anything users should do before or after updating.

## v0.6.0-beta - 2026-06-16

### Added

- Added a remembered archive-folder browser for top-level ZIP, 7Z, and RAR files.
- Added archive search, manual refresh, and folder switching.
- Added active-profile-aware Installed and Previously installed archive states.
- Added focused tests for archive-folder scanning and archive-browser workflow behavior.

### Changed

- Install Mod now asks for an archive folder on first use and opens the remembered folder directly afterward.
- Archive files selected from the folder browser now use the existing archive import and installer pipeline.
- Archives available to install are shown before currently installed archives, with newest files first in each group.
- Main-screen and fullscreen-list scroll positions are retained during the current app session.
- Continued extracting cohesive responsibilities from `MainActivity` without intentionally changing existing mod-management behavior.
- Updated architecture, requirements, testing, decisions, user-guide, and current-priority documentation for the archive-folder workflow.

### Fixed

- Removed the duplicate archive-library installation implementation.
- Fixed recursive Kotlin type inference in the archive workflow wiring.

### Known Issues

- Some 7Z and RAR archives may still fail depending on their compression method or archive format.
- Recovery tools and unfinished deployment warnings still need more polish.
- Droid Mod Loader remains beta software; back up important game folders before testing deployment.

### Upgrade Notes

- Existing users can install this version over `v0.5.5-beta`.
- The first time you tap Install Mod, select the folder where you keep downloaded mod archives.
- DML reads the original archive from that folder and keeps its own managed copy when the mod is installed.
- Existing profiles and installed-mod records should remain available after upgrading.

## v0.5.5-beta

### Added

- Project vision documentation.
- Requirements documentation.
- Brand asset documentation.
- Handmade icon source tracking.
- Android launcher icon assets.
- Project documentation index.
- Architecture documentation.
- Decision log.
- Testing documentation.
- Release checklist.
- Development loop documentation.
- Pull request checklist template.
- Expanded decision log rules and core accepted decisions.
- Source map documentation for major app files and engine areas.
- JVM unit tests for path normalization, deploy file classification, mod display name cleanup, and plugin discovery.
- Starter user guide.
- Troubleshooting documentation.
- Glossary for DML and modding terms.
- Versioning documentation.
- Public release notes template.
- APK upload checklist template.
- Git workflow documentation.
- Local project check scripts.
- Documentation structure check script.
- Release notes preparation script.
- GitHub Actions CI for unit tests, debug builds, and documentation checks.
- Added unit coverage for archive metadata tracking, Nexus URL parsing, and archive metadata summaries.
- Added a Developer Tools action to print the saved archive library metadata summary without changing files.
- Added a pre-push documentation gate so project docs stay current before GitHub pushes.
- Continued release-readiness cleanup for the archive import/download metadata path.

### Changed

- README now links to project documentation.
- Expanded architecture documentation maintenance rules.
- Clarified versionName, versionCode, APK filename, and changelog expectations.
- Expanded release checklist links to release templates and versioning documentation.
- Documented Bazzite terminal setup notes for Gradle, Java/JBR, and executable Gradle wrapper usage.
- Added repository automation to catch missing docs, committed artifacts, and failing unit tests earlier.

### Fixed

- Removed accidental documentation backup file.

### Known Issues

- Release checklist is still being formalized.
- Recovery tools and unfinished deploy warnings still need more polish.

### Upgrade Notes

- Back up important game folders before testing deployment features.
