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

## v0.7.0-beta - Unreleased

### Added

- Added Tale of Two Wastelands as a selectable profile with legacy
  timestamp-based plugin ordering.
- Added content-signature detection for ZIP, 7Z, RAR4, and RAR5 archives.
- Added bounded extraction checks for unsafe paths, duplicate destinations,
  case collisions, excessive entries, oversized files, excessive total output,
  long paths, and insufficient storage headroom.
- Added persistent installed-mod replacement transactions and automatic recovery
  at startup and profile activation.
- Added cooperative cancellation for archive copying, extraction, preparation,
  and installation.
- Added a canonical `version.properties` source for release version name and
  version code.

### Changed

- Replaced production Storage Access Framework paths with one direct-filesystem
  backend for Game Root, `Data`, Archive Library, import, scanning, deployment,
  overwrite inspection, and plugin timestamp ordering.
- Archive Library scanning and stored archive metadata now use detected content
  rather than filename extensions.
- Mod replacement now stages complete content and preserves the existing
  installed mod until promotion succeeds.
- Archive import keeps an archive that was already registered when later
  installation is cancelled, while removing copied files that were never
  registered.
- Split reusable activity workflows and engine responsibilities into focused
  coordinators and services while preserving `MainActivity` as the Android
  composition root and `ModEngine` as the stable facade.
- The displayed app version now comes from the APK build configuration instead
  of a hardcoded UI string.

### Fixed

- Prevented failed, cancelled, or partial extraction from becoming a successful
  installed-mod state.
- Preserved the previous installed mod when replacement staging or promotion
  fails.
- Recovered retained replacement transactions after interruption.
- Removed partial copied and extracted files during cooperative cancellation
  where cleanup is possible.
- Reported RAR5, encrypted, multipart, corrupt, and unsupported archive failures
  more precisely.
- Recognized supported archives with missing or incorrect filename extensions.

### Known Issues

- RAR5 is detected but is not supported for installation.
- Password-protected or encrypted archives and multipart RAR archives are not
  supported.
- Some uncommon 7Z compression or encryption variants may remain unsupported.
- Android still blocks access to other applications' protected
  `Android/data` directories.
- Game Root and `Data` folder validation still needs stronger game-specific
  guidance.
- Droid Mod Loader remains beta software; back up important game folders before
  testing deployment.

### Upgrade Notes

- Install this version over `v0.6.0-beta`; do not uninstall the existing app
  first if you want to retain app-managed state.
- Android 11 and newer require all-files access for DML's shared-storage
  workflows.
- Existing profiles and unrelated managed state are retained by the migration.
  Legacy URI-only Game Root, `Data`, or Archive Library selections require
  explicit reselection as direct filesystem paths.
- Final upgrade validation from the public `v0.6.0-beta` APK is required before
  this release is published.

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
