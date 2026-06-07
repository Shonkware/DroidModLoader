# Droid Mod Loader Decision Log

This file records major project decisions.

## Format

Each decision should use this format:

- Decision
- Reason
- Result

## 2026-06-06 - Use Indexed Physical Deployment Instead of VFS

Decision:

Droid Mod Loader will use indexed physical deployment instead of trying to recreate Mod Organizer 2's virtual file system.

Reason:

Android storage restrictions, shared folders, and GameNative behavior make physical deployment the better core model for this app.

Result:

The app must track installed mods, file indexes, resolved winners, deployment manifests, verification, diagnostics, and recovery.

## 2026-06-06 - GameNative Is the Main Early Target

Decision:

GameNative is the main early target environment.

Reason:

The app is being built around Android Bethesda modding through Windows-container setups. GameNative offers simplicity in game container configuration.

Result:

Droid Mod Loader should work through shared folders, exported plugin files, and user-selected game folders.

## 2026-06-06 - File Safety Comes First

Decision:

Deployment safety is more important than fast feature completion.

Reason:

The app can modify user game folders. Bad file operations can break installs or destroy user work.

Result:

Deployment needs preflight checks, deployment plans, journals, backups where needed, and recovery tools.

## 2026-06-06 - Profiles Are a Core Concept

Decision:

Profiles are first-class app state, not just a visual grouping.

Reason:

Users need separate setups for clean tests, personal mod lists, game-specific setups, and future guide or collection states.

Result:

Profiles should eventually isolate mods, plugins, target identity, deployment manifests, diagnostics, and recovery state.

## 2026-06-06 - Developer Tools Must Be Hidden

Decision:

Developer tools should not be visible in the normal user path.

Reason:

Visible developer tools confuse normal users and make the app feel unfinished.

Result:

Developer tools should be hidden behind developer mode. Recovery tools should remain available outside developer mode.

## 2026-06-06 - Handmade Branding Assets Are Required

Decision:

Official DML branding assets should be handmade and source files should be preserved.

Reason:

The project needs consistent visual identity and should not depend on AI-generated official branding.

Result:

Editable source assets live under assets/source/. Generated Android resources live under app/src/main/res/.