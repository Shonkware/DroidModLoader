<p align="center">
  <img
    src="assets/source/icons/dml_default_round_reference_v10.png"
    width="128"
    alt="Droid Mod Loader app icon"
  >
</p>

<h1 align="center">Droid Mod Loader</h1>

<p align="center">
  <strong>Profile-based Bethesda mod management for Android.</strong>
  <br>
  Organize mods, plugins, deployment, and recovery for games running through
  Android Windows compatibility environments.
</p>

<p align="center">
  <a href="https://github.com/CyberShonk/DroidModLoader/releases">
    <img
      alt="Latest release"
      src="https://img.shields.io/github/v/release/CyberShonk/DroidModLoader?include_prereleases&style=flat-square&label=release&color=b3202a"
    >
  </a>
  <img
    alt="Android 11 or newer"
    src="https://img.shields.io/badge/Android-11%2B-3d8b5f?style=flat-square&logo=android&logoColor=white"
  >
  <a href="https://github.com/CyberShonk/DroidModLoader/actions/workflows/ci.yml">
    <img
      alt="Continuous integration status"
      src="https://github.com/CyberShonk/DroidModLoader/actions/workflows/ci.yml/badge.svg"
    >
  </a>
  <a href="LICENSE">
    <img
      alt="MIT License"
      src="https://img.shields.io/badge/license-MIT-555555?style=flat-square"
    >
  </a>
</p>

<p align="center">
  <a href="https://github.com/CyberShonk/DroidModLoader/releases"><strong>Releases</strong></a>
  ·
  <a href="docs/user-guide.md"><strong>User Guide</strong></a>
  ·
  <a href="https://discord.gg/Q9dM262KRc"><strong>Discord</strong></a>
  ·
  <a href="ROADMAP.md"><strong>Roadmap</strong></a>
</p>

---

Droid Mod Loader keeps installed mods separate, tracks profiles and plugin
state, and prepares files for a selected game folder. It is intended to replace
repetitive manual copying with a process that is easier to review, repeat, and
recover.

> [!WARNING]
> Droid Mod Loader is beta software that can write to game folders. Back up
> important files before testing a new build or deployment setup.

## What it does

Droid Mod Loader currently includes early support for:

- profile creation and switching;
- browsing a remembered archive folder;
- basic ZIP, 7Z, and RAR archive import;
- separate managed folders for installed mods;
- mod enable, disable, and priority controls;
- `.esm`, `.esp`, and `.esl` discovery;
- plugin activation and ordering for supported games;
- deployment planning, journals, backups, and recovery foundations;
- overwrite, baseline, diagnostics, and logging foundations; and
- an experimental second-screen plugin display.

Archive compatibility is still being improved, especially for newer or unusual
RAR and 7Z variants.

## Games and environments

The main early game targets are:

- Skyrim Legendary Edition;
- Fallout: New Vegas;
- Fallout 3;
- Oblivion; and
- Tale of Two Wastelands setups based on Fallout: New Vegas.

Skyrim Special Edition and Fallout 4 remain longer-term targets.

Droid Mod Loader is intended for games running through Android Windows
compatibility environments such as GameNative, Winlator, GameHub Lite, and
BannerHub. It prepares files those environments can use without modifying or
depending on their private files.

## Storage and profiles

The app uses user-granted all-files access and direct filesystem paths for
shared-storage workflows. Each profile can remember its own game root, `Data`
folder, and archive library.

Profiles keep different mod setups separate, making it easier to test changes or
maintain different setups for the same game.

## Project status

The latest public release is `v0.6.0-beta`. The `main` branch contains
post-release maintenance and architecture cleanup, but no newer public version
has been released.

The next feature work is still being planned. Near-term priorities are archive
extraction reliability, clearer failure reporting, and defining the required
scope for a stable 1.0 release.

See [Current Status](CURRENT_STATUS.md),
[Current Priorities](docs/tasks/current-priorities.md), and the
[Roadmap](ROADMAP.md) for details.

## Get started

1. Download the latest APK from [Releases](https://github.com/CyberShonk/DroidModLoader/releases).
2. Back up any game folders you plan to use for testing.
3. Read the [User Guide](docs/user-guide.md) before selecting storage or deploying mods.

## Documentation

<<<<<<< HEAD
Droid Mod Loader is an Android app for managing Bethesda game mods.

**Minimum Android version:** Android 11 (API 30).

The simple version:

You install mods into Droid Mod Loader.
Droid Mod Loader keeps those mods organized.
Then it prepares the right files so your game can use them.

The app is being built for people who want to mod Bethesda games on Android while using Windows container apps like GameNative.

Right now, Android Bethesda modding usually means copying files by hand, guessing where things go, and hoping the game still launches. Droid Mod Loader is meant to make that process safer, clearer, and easier to repeat.

## What this app does

Droid Mod Loader helps with things like:

* Browsing ZIP, 7Z, and RAR mod archives from a remembered direct
  filesystem folder
* Keeping each installed mod in its own folder
* Turning mods on and off
* Keeping separate profiles for different setups
* Scanning mod files so the app knows what each mod contains
* Showing plugin files like `.esp`, `.esm`, and `.esl`
* Applying game-aware plugin activation and load order through text files or plugin timestamps
* Preparing files for a shared game folder
* Helping users understand conflicts and overwrites
* Creating useful reports when something goes wrong

The long term goal is to bring a Mod Organizer/Vortex style experience to
Android, using user-granted all-files access and direct filesystem paths for game
and mod storage.

## Why this exists

Bethesda games can be heavily modded on PC because there are strong tools like Mod Organizer 2, Vortex, xEdit, and LOOT.

Android does not have the same kind of modding setup.

If you are trying to play games like Skyrim or Fallout: New Vegas through a Windows container on Android, you often have to manage files yourself. That can get confusing fast.

Droid Mod Loader exists to help with that.

It is not just an archive extractor. The goal is to build a real Android mod manager that understands installed mods, profiles, conflicts, plugin order, deployment, and diagnostics.

## Who this is for

This project is mainly for:

* people modding Bethesda games on Android
* GameNative, Winlator, GameHub Lite, and BannerHub users
* Android handheld users
* people testing Windows game containers on ARM devices
* Bethesda modders who want cleaner mobile tools
* users who want less manual file copying

You do not need to understand every technical term to use the app. The app should eventually explain what it is doing and why it matters.

## Target games

The project is focused on Bethesda style modding.

Current and planned targets include:

* Skyrim Legendary Edition
* Fallout: New Vegas
* Fallout 3
* Oblivion
* Skyrim Special Edition
* Fallout 4

Skyrim Legendary Edition and Fallout: New Vegas are the most important early targets.

## GameNative support

GameNative is the main test environment for this project.

The goal is not to modify GameNative directly or depend on private GameNative files. Droid Mod Loader is being built to work with shared folders and exported files that GameNative can use.

Droid Mod Loader should help users with:

* picking the correct game folder
* preparing the Data folder
* handling optional game root files
* exporting plugin order files
* exporting config files later
* creating reports that include GameNative setup notes
* reducing the amount of guessing needed to get a modded game running

In plain English:

Droid Mod Loader prepares the modded game files.
GameNative runs the Windows game.
The two need to work together.

## Important terms

### Mod

A mod is a fan made change for a game. It might add textures, weapons, quests, bug fixes, menus, scripts, or new gameplay features.

### Mod manager

A mod manager helps install, organize, enable, disable, and troubleshoot mods.

### Data folder

Bethesda games usually load most mods from a folder called `Data`.

For example, a Skyrim mod might place files into:

```text
Data/textures
Data/meshes
Data/scripts
Data/SkyUI.esp
```

Droid Mod Loader needs to know where the correct Data folder is so it can prepare files safely.

### Game root folder

Some mods need files outside the Data folder.

Examples include script extenders like SKSE, NVSE, OBSE, or FOSE.

These files often live beside the game executable instead of inside Data. Droid Mod Loader treats this as a more advanced action because putting files in the wrong root folder can cause problems.

### Plugin

A plugin is usually an `.esp`, `.esm`, or `.esl` file.

Plugins tell Bethesda games about new records, quests, items, cells, scripts, and other changes.

### Load order

Load order is the order plugins load in.

The order matters because one plugin can overwrite or depend on another. Bad load order can cause missing content, crashes, broken quests, or strange bugs.

### Profile

A profile is a separate mod setup.

For example, you could have:

* a clean testing profile
* a Skyrim graphics profile
* a Fallout: New Vegas stability profile
* a Tale of Two Wastelands profile

The goal is for each profile to stay separate so one setup does not leak into another.

## Current project status

Droid Mod Loader is in beta development.

Some parts work now. Some parts are still experimental. Some parts are planned but not finished yet.

Current focus areas include:

* safer file deployment
* better profile isolation
* better recovery tools
* better diagnostics
* better plugin handling
* better installer support
* GameNative focused testing

This is still early software. Back up important game folders before testing.

## What works now

The app already has early support for:

* profile creation and switching
* direct-path archive-folder browsing, search, refresh, and folder switching
* ZIP, 7Z, and RAR archive import basics. RAR5 support is still being worked on.
* managed mod folders
* mod enable and disable
* mod priority order
* plugin scanning
* plugin enable and disable
* plugin output files
* deployment planning basics
* deployment journal and recovery work
* baseline and overwrite foundations
* diagnostics and logs
* experimental second screen plugin display

Some of these systems are still being improved.

## What is still being built

Major work still planned before a stable 1.0 release includes:

* stronger deploy safety
* better unfinished deploy recovery
* game folder validation
* support report export
* Nexus downloads and mod metadata
* advanced FOMOD installer support
* better BAIN installer support
* stronger plugin intelligence
* basic xEdit report bridge
* practical LOOT support
* better conflict view
* better overwrite management
* settings screen
* storage manager
* external SD support
* cleaner public guides
* more tests for risky file logic

## Project Documentation

- [Documentation Index](docs/index.md)
=======
>>>>>>> 004361b (docs: refresh public project documentation)
- [User Guide](docs/user-guide.md)
- [Troubleshooting](docs/troubleshooting.md)
- [Glossary](docs/glossary.md)
- [Current Status](CURRENT_STATUS.md)
- [Roadmap](ROADMAP.md)
- [Changelog](releases/changelog.md)
- [Documentation Index](docs/index.md)

## Community and support

Support, testing, project discussion, and development updates are handled mainly
through the [Droid Mod Loader Discord server](https://discord.gg/Q9dM262KRc).

Useful bug reports include the Android device and version, the game and
compatibility environment, the selected folders, installed mods, reproduction
steps, and any relevant screenshots or logs.

Special thanks to Deno and his Discord community for testing, troubleshooting,
and shared Android modding knowledge.

Project support links: [Ko-fi](https://ko-fi.com/D1D81ZHV4E) ·
[Patreon](https://patreon.com/SeanBottoms)

## Contributing

Useful contributions include bug reports, compatibility notes, documentation
fixes, reproducible test cases, safer file-handling ideas, and Bethesda modding
knowledge.

Keep changes focused and include enough information for another person to review
and test them. See the [documentation index](docs/index.md) for architecture,
testing, workflow, and release references.

## License

Droid Mod Loader is released under the [MIT License](LICENSE).

## Disclaimer

Droid Mod Loader is an independent project. Users are responsible for following
the permissions, licenses, and distribution terms for any games, mods, or
third-party tools they use.

This project does not claim ownership of Bethesda game assets, third-party mods,
GameNative, Winlator, xEdit, LOOT, Nexus Mods, or other community tools.
