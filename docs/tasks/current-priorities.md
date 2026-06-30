# Droid Mod Loader Current Priorities

This page tracks the focused work required to finish `v0.7.0-beta`. It is
shorter than the full roadmap and does not describe release-candidate behavior
as publicly released until the tagged APK is published.

## 1. Complete `v0.7.0-beta` release validation

**Status:** Active
**Requirements:** `REQ-MOD-001`, `REQ-RELEASE-001`, `REQ-RELEASE-002`,
`REQ-RELEASE-003`, `REQ-STORAGE-002`

The reliable-foundation implementation is complete. Remaining work is release
validation rather than another archive-engine feature phase.

Required checks:

- run the current real-archive compatibility matrix for ZIP, 7Z, RAR4, RAR5,
  encrypted, multipart, corrupt, renamed, and extensionless samples;
- verify cancellation and cleanup during large archive copying and extraction;
- verify interrupted replacement recovery;
- install the candidate over the public `v0.6.0-beta` APK and confirm profile,
  path, archive, mod, plugin, and deployment state retention;
- build and verify the signed APK; and
- install and test the exact artifact intended for upload.

## 2. Finish release documentation and packaging

**Status:** In progress

Before publication:

- complete the `v0.7.0-beta` changelog and release notes;
- record known unsupported archive variants accurately;
- update the release date after final approval;
- generate the versioned APK filename and SHA-256 checksum; and
- prepare matching GitHub and Nexus Mods upload packages.

Publication remains manual for this release.

## 3. Preserve archive-install safety guarantees

**Status:** Ongoing release rule

Archive changes must continue to preserve these guarantees:

- archive type is determined from content rather than the filename extension;
- unsafe or excessive extraction is rejected before becoming installed state;
- cancellation removes partial output where possible;
- replacing a mod preserves the previous installation until promotion succeeds;
- interrupted replacement state remains recoverable; and
- unsupported variants fail with a specific, understandable message.

## 4. Define the stable 1.0 acceptance boundary

**Status:** Next planning task after `v0.7.0-beta`

Classify Nexus integration, target validation, deployment and recovery polish,
conflict presentation, adaptive navigation, separators, details views, themes,
LOOT and xEdit support, guides, collections, storage tools, and presets as
stable blockers, staged pre-1.0 work, optional enhancements, or post-1.0 work.

## 5. Preserve the extracted architecture

**Status:** Ongoing maintenance rule

Keep `MainActivity` as the Android composition root and `ModEngine` as the stable
engine facade. Add behavior to the focused workflow, service, repository, or
engine component that owns it.

## Last updated

2026-06-29
