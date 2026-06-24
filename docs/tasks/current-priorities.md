# Droid Mod Loader Current Priorities

This page tracks the next focused work after `v0.6.0-beta`. It is shorter than
the full roadmap and does not describe planned features as released behavior.

At this update, the next feature implementation task has not started.

## 1. Improve archive extraction robustness

**Status:** Planned
**Requirement:** `REQ-MOD-001`

Improve ZIP, 7Z, and RAR compatibility. Unsupported archive variants should fail
with a clear explanation instead of a generic extraction error.

Expected work includes:

- identifying the archive variants that currently fail;
- adding focused tests for confirmed failures;
- preserving safe cleanup when extraction stops; and
- documenting formats that remain unsupported.

## 2. Define the stable 1.0 acceptance boundary

**Status:** Planning

Separate the capabilities required for a safe stable release from larger
follow-up work.

The review should classify Nexus integration, LOOT and xEdit support, guides,
collections, storage tools, and presets as required for 1.0, staged before 1.0,
or planned after 1.0. The accepted boundary should be recorded in the roadmap
and decision log.

## 3. Preserve the extracted architecture

**Status:** Ongoing maintenance rule

Keep `MainActivity` as the Android composition root and `ModEngine` as the stable
engine facade. New behavior should be placed in the focused workflow or engine
service that owns it.

## Last updated

2026-06-24
