# Current Status

- **Latest public release:** `v0.6.0-beta`
- **Release candidate:** `v0.7.0-beta`
- **Minimum Android version:** Android 11 / API 30
- **Development state:** Release preparation and validation
- **Storage model:** Direct filesystem access for shared-storage workflows

## Release and source state

The public release remains `v0.6.0-beta`. The current source contains the
`v0.7.0-beta` reliable-foundation work, and the release branch is preparing the
versioned candidate, changelog, release notes, packaging, and final checks.

Broad `v0.8.0-beta` setup and deployment work is not active. The immediate goal
is to validate and release the completed foundation without expanding scope.

## Completed `v0.7.0-beta` foundation

The current source includes:

- Tale of Two Wastelands as a selectable profile with the correct legacy
  timestamp-based plugin-order policy;
- one direct-filesystem backend for Game Root, `Data`, Archive Library,
  deployment, scanning, imports, and plugin timestamp ordering;
- extracted `MainActivity` workflow ownership and focused services behind the
  `ModEngine` facade;
- content-signature detection for ZIP, 7Z, RAR4, and RAR5 archives;
- signature-based Archive Library scanning and stored format metadata;
- precise errors for unsupported, encrypted, multipart, corrupt, or unsafe
  archives where the reader can classify the failure;
- bounded extraction with path, duplicate, case-collision, entry-count,
  per-file size, total-size, path-length, and storage-headroom checks;
- staged and transactional installed-mod replacement;
- recovery of interrupted replacement transactions at startup and profile
  activation; and
- cooperative cancellation for archive copying, preparation, extraction, and
  automatic or user-selected installation.

RAR5 is detected accurately but is not currently installable.

## Latest recorded validation

The merged foundation passed:

- `git diff --check`;
- the full JVM unit-test suite;
- `./tools/check-project.sh`; and
- debug APK assembly.

The release-version automation also passed shell syntax checks, JVM tests,
debug assembly, and the project check while retaining the public
`v0.6.0-beta` / version-code `2` identity.

These results do not replace final validation of the versioned release
candidate.

## Remaining before publication

1. Complete the real-archive compatibility matrix on the current candidate.
2. Verify an in-place upgrade from the public `v0.6.0-beta` APK.
3. Confirm direct-path profile, mod, plugin, and Archive Library state survives
   the upgrade.
4. Build the signed release APK and verify its package name, version name,
   version code, signature, and signing-certificate fingerprint.
5. Install and test the exact signed artifact, including restart, cancellation,
   recovery, and safe-folder deployment checks.
6. Generate final checksums and upload packages, then publish the Git tag,
   GitHub release, and Nexus Mods update.

## Known limitations

- RAR5, password-protected or encrypted archives, and multipart RAR archives are
  not supported for installation.
- Unsupported 7Z compression or encryption variants may still be rejected.
- Android continues to protect other applications' private `Android/data`
  directories even when all-files access is granted.
- Game Root and `Data` validation still needs stronger game-specific guidance in
  a later release.
- Droid Mod Loader remains beta software. Back up important game folders before
  testing deployment.

## Last updated

2026-06-29
